package com.boox.atomic.habits.boox;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.view.SurfaceView;

import com.onyx.android.sdk.device.Device;
import com.onyx.android.sdk.api.device.epd.EpdController;
import com.onyx.android.sdk.api.device.epd.UpdateMode;
import com.onyx.android.sdk.pen.RawInputCallback;
import com.onyx.android.sdk.pen.TouchHelper;
import com.onyx.android.sdk.data.note.TouchPoint;
import com.onyx.android.sdk.pen.data.TouchPointList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Wraps the Boox TouchHelper + RawInputCallback to provide stylus drawing
 * capabilities ("scribble mode") on an E-Ink screen.
 * <p>
 * Manages the scribble lifecycle:
 * <ul>
 *   <li>Opening/closing the scribble engine (enter/leave scribble mode)</li>
 *   <li>Collecting raw touch points into Stroke objects</li>
 *   <li>Drawing all collected strokes onto a Canvas</li>
 *   <li>E-Ink refresh optimizations (DU for drawing, GC after stroke end)</li>
 * </ul>
 */
public class ScribbleEngine {

    /**
     * Represents a single continuous pen stroke on the E-Ink canvas.
     */
    public static final class Stroke {
        private final List<TouchPoint> points = new ArrayList<>();
        private final float width;
        private final int color;
        private final long timestamp;

        Stroke(float width, int color, long timestamp) {
            this.width = width;
            this.color = color;
            this.timestamp = timestamp;
        }

        void addPoint(TouchPoint point) {
            points.add(point);
        }

        /** Returns an unmodifiable view of the touch points composing this stroke. */
        public List<TouchPoint> getPoints() {
            return Collections.unmodifiableList(points);
        }

        public float getWidth() {
            return width;
        }

        public int getColor() {
            return color;
        }

        public long getTimestamp() {
            return timestamp;
        }

        /**
         * Draws this stroke onto the given Canvas using the provided Paint.
         * Builds a {@link Path} from all touch points and strokes it.
         */
        public void draw(Canvas canvas, Paint paint) {
            if (points.isEmpty()) return;

            paint.setStrokeWidth(width);
            paint.setColor(color);
            paint.setStyle(Paint.Style.STROKE);
            paint.setAntiAlias(true);
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setStrokeJoin(Paint.Join.ROUND);

            Path path = new Path();
            TouchPoint start = points.get(0);
            path.moveTo(start.getX(), start.getY());

            for (int i = 1; i < points.size(); i++) {
                TouchPoint p = points.get(i);
                // Use quadratic bezier for smoother curves
                TouchPoint prev = points.get(i - 1);
                float midX = (prev.getX() + p.getX()) / 2f;
                float midY = (prev.getY() + p.getY()) / 2f;
                path.quadTo(prev.getX(), prev.getY(), midX, midY);
            }

            // Connect to the last point
            TouchPoint last = points.get(points.size() - 1);
            path.lineTo(last.getX(), last.getY());

            canvas.drawPath(path, paint);
        }
    }

    // ─── Fields ────────────────────────────────────────────────────────

    private final SurfaceView surfaceView;
    private final Paint defaultPaint;
    private TouchHelper touchHelper;
    private final CopyOnWriteArrayList<Stroke> strokes = new CopyOnWriteArrayList<>();

    private Stroke currentStroke;
    private float strokeWidth;
    private int strokeColor;

    private boolean isOpen = false;

    // ─── Constructor ───────────────────────────────────────────────────

    /**
     * Creates a new ScribbleEngine bound to the given SurfaceView.
     *
     * @param surfaceView the SurfaceView that will receive stylus input
     * @param strokeWidth default pen width in pixels
     * @param strokeColor default pen color (ARGB)
     */
    public ScribbleEngine(SurfaceView surfaceView, float strokeWidth, int strokeColor) {
        this.surfaceView = surfaceView;
        this.strokeWidth = strokeWidth;
        this.strokeColor = strokeColor;

        this.defaultPaint = new Paint();
        this.defaultPaint.setAntiAlias(true);
        this.defaultPaint.setStyle(Paint.Style.STROKE);
        this.defaultPaint.setStrokeCap(Paint.Cap.ROUND);
        this.defaultPaint.setStrokeJoin(Paint.Join.ROUND);
    }

    /**
     * Convenience constructor with default black pen (4px width).
     */
    public ScribbleEngine(SurfaceView surfaceView) {
        this(surfaceView, 4f, 0xFF000000);
    }

    // ─── Public API ────────────────────────────────────────────────────

    /**
     * Opens the scribble engine: creates the TouchHelper, registers the
     * raw input callback, and enters scribble mode on the Boox device.
     * <p>
     * Safe to call multiple times — subsequent calls are no-ops if already open.
     */
    public void open() {
        if (isOpen) return;

        try {
            touchHelper = TouchHelper.create(surfaceView, rawInputCallback);
            touchHelper.setStrokeWidth(strokeWidth);
            touchHelper.setStrokeStyle(strokeColor);

            Device.currentDevice().enterScribbleMode(surfaceView);

            isOpen = true;
        } catch (Exception e) {
            // Boox SDK not available or device does not support scribble mode
        }
    }

    /**
     * Closes the scribble engine: leaves scribble mode and cleans up the
     * TouchHelper. Safe to call multiple times.
     */
    public void close() {
        if (!isOpen) return;

        try {
            if (touchHelper != null) {
                touchHelper.closeRawDrawing();
                touchHelper = null;
            }
            Device.currentDevice().leaveScribbleMode(surfaceView);
        } catch (Exception e) {
            // Boox SDK not available
        }

        isOpen = false;
    }

    /**
     * Clears all stored strokes from the engine and performs a clean
     * E-Ink refresh on the surface.
     */
    public void clearAll() {
        strokes.clear();
        currentStroke = null;
        cleanRefresh();
    }

    /**
     * Draws all stored strokes onto the given Canvas using the default paint.
     *
     * @param canvas the Canvas to draw onto
     */
    public void drawAll(Canvas canvas) {
        drawAll(canvas, defaultPaint);
    }

    /**
     * Draws all stored strokes onto the given Canvas using a custom Paint.
     *
     * @param canvas the Canvas to draw onto
     * @param paint  the Paint to use for rendering
     */
    public void drawAll(Canvas canvas, Paint paint) {
        for (Stroke stroke : strokes) {
            stroke.draw(canvas, paint);
        }
    }

    /**
     * Returns an unmodifiable view of all completed strokes.
     */
    public List<Stroke> getStrokes() {
        return Collections.unmodifiableList(new ArrayList<>(strokes));
    }

    // ─── Configuration ─────────────────────────────────────────────────

    /**
     * Updates the stroke width for subsequent strokes.
     *
     * @param strokeWidth new width in pixels
     */
    public void setStrokeWidth(float strokeWidth) {
        this.strokeWidth = strokeWidth;
        if (touchHelper != null) {
            touchHelper.setStrokeWidth(strokeWidth);
        }
    }

    /**
     * Updates the stroke color for subsequent strokes.
     *
     * @param strokeColor new ARGB color value
     */
    public void setStrokeColor(int strokeColor) {
        this.strokeColor = strokeColor;
        if (touchHelper != null) {
            touchHelper.setStrokeStyle(strokeColor);
        }
    }

    /**
     * Returns whether the scribble engine is currently open.
     */
    public boolean isOpen() {
        return isOpen;
    }

    // ─── Internal helpers ──────────────────────────────────────────────

    private void cleanRefresh() {
        try {
            EpdController.invalidate(surfaceView, UpdateMode.GC);
        } catch (Exception e) {
            // Non-Boox device or SDK not available
        }
    }

    private void dirtyRectRefresh(Rect rect) {
        try {
            EpdController.invalidate(surfaceView, UpdateMode.DU);
        } catch (Exception e) {
            // Non-Boox device or SDK not available
        }
    }

    // ─── Raw Input Callback ────────────────────────────────────────────

    private final RawInputCallback rawInputCallback = new RawInputCallback() {
        @Override
        public void onBeginRawDrawing(boolean stylusUsed, TouchPoint point) {
            // Enter scribble mode and start a new stroke
            try {
                Device.currentDevice().enterScribbleMode(surfaceView);
            } catch (Exception e) {
                // Non-Boox device
            }

            currentStroke = new Stroke(strokeWidth, strokeColor, System.currentTimeMillis());
            if (currentStroke != null && point != null) {
                currentStroke.addPoint(point);
            }
        }

        @Override
        public void onEndRawDrawing(boolean stylusUsed, TouchPoint point) {
            // Finalize the current stroke
            if (currentStroke != null) {
                if (point != null) {
                    currentStroke.addPoint(point);
                }
                strokes.add(currentStroke);
                currentStroke = null;
            }

            // Leave scribble mode and perform a clean GC refresh to eliminate ghosting
            try {
                Device.currentDevice().leaveScribbleMode(surfaceView);
            } catch (Exception e) {
                // Non-Boox device
            }

            cleanRefresh();
        }

        @Override
        public void onRawDrawingTouchPointMoveReceived(TouchPoint touchPoint) {
            if (currentStroke != null) {
                currentStroke.addPoint(touchPoint);
            }

            // Use DU (dirty rect) refresh for low-latency visual feedback during drawing
            dirtyRectRefresh(new Rect(
                    (int) touchPoint.getX() - 20,
                    (int) touchPoint.getY() - 20,
                    (int) touchPoint.getX() + 20,
                    (int) touchPoint.getY() + 20
            ));
        }

        @Override
        public void onRawDrawingTouchPointListReceived(TouchPointList touchPointList) {
            // Full point list delivered at end of stroke; per-point moves already
            // accumulate into currentStroke, so no extra handling needed here.
        }

        @Override
        public void onBeginRawErasing(boolean stylusUsed, TouchPoint point) {
            // Eraser pressed — a clean refresh helps clear residual pixels.
            cleanRefresh();
        }

        @Override
        public void onEndRawErasing(boolean stylusUsed, TouchPoint point) {
            cleanRefresh();
        }

        @Override
        public void onRawErasingTouchPointMoveReceived(TouchPoint touchPoint) {
            // Erasing handled by the system overlay; nothing to accumulate.
        }

        @Override
        public void onRawErasingTouchPointListReceived(TouchPointList touchPointList) {
            // No-op: erasing point list not used by this engine.
        }
    };
}