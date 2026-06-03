package com.boox.atomic.habits.ui.widget;

/**
 * A lightweight handwriting capture View that replaces EditText fields with a small
 * stylus/finger drawing canvas. Stores strokes as serializable data for persistence.
 *
 * Used for goals, habits, and todos where stylus input is preferred over typing.
 */
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0080\u0001\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\b\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0007\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010!\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000b\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\u0010\u0002\n\u0002\b\u0003\n\u0002\u0010 \n\u0002\b\u000e\n\u0002\u0010\u000e\n\u0002\b\f\n\u0002\u0018\u0002\n\u0002\b\n\u0018\u00002\u00020\u0001B%\b\u0007\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\n\b\u0002\u0010\u0004\u001a\u0004\u0018\u00010\u0005\u0012\b\b\u0002\u0010\u0006\u001a\u00020\u0007\u00a2\u0006\u0002\u0010\bJ\u0006\u0010,\u001a\u00020\'J\u0010\u0010-\u001a\u00020\'2\u0006\u0010.\u001a\u00020\u0014H\u0002J\u0010\u0010/\u001a\u00020\'2\u0006\u0010.\u001a\u00020\u0014H\u0002J0\u00100\u001a\u00020\'2\u0006\u0010.\u001a\u00020\u00142\u001e\u00101\u001a\u001a\u0012\u0016\u0012\u0014\u0012\u0004\u0012\u00020\f\u0012\u0004\u0012\u00020\f\u0012\u0004\u0012\u00020\f0\u00180+H\u0002J:\u00102\u001a\u00020\'2\b\u00103\u001a\u0004\u0018\u00010\u00142\u0006\u00104\u001a\u00020\f2\u0006\u00105\u001a\u00020\f2\u0006\u00106\u001a\u00020\f2\u0006\u00107\u001a\u00020\f2\u0006\u00108\u001a\u00020\fH\u0002J\u0006\u00109\u001a\u00020:J\u0006\u0010;\u001a\u00020 J\u0006\u0010\u001f\u001a\u00020 J\u0010\u0010<\u001a\u00020\'2\u0006\u0010\u0013\u001a\u00020\u0014H\u0014J\u0018\u0010=\u001a\u00020\'2\u0006\u0010>\u001a\u00020\u00072\u0006\u0010?\u001a\u00020\u0007H\u0014J(\u0010@\u001a\u00020\'2\u0006\u0010A\u001a\u00020\u00072\u0006\u0010B\u001a\u00020\u00072\u0006\u0010C\u001a\u00020\u00072\u0006\u0010D\u001a\u00020\u0007H\u0014J\u0010\u0010E\u001a\u00020 2\u0006\u0010F\u001a\u00020GH\u0016J\b\u0010H\u001a\u00020\'H\u0002J\u000e\u0010I\u001a\u00020\'2\u0006\u0010J\u001a\u00020 J\u0014\u0010K\u001a\u00020\'2\f\u0010L\u001a\b\u0012\u0004\u0012\u00020\'0&J\u0014\u0010M\u001a\u00020\'2\f\u0010L\u001a\b\u0012\u0004\u0012\u00020\'0&J\u000e\u0010N\u001a\u00020\'2\u0006\u0010O\u001a\u00020:J\b\u0010P\u001a\u00020\'H\u0002R\u000e\u0010\t\u001a\u00020\nX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u000b\u001a\u00020\fX\u0082D\u00a2\u0006\u0002\n\u0000R\u000e\u0010\r\u001a\u00020\nX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u000e\u001a\u00020\fX\u0082D\u00a2\u0006\u0002\n\u0000R\u0010\u0010\u000f\u001a\u0004\u0018\u00010\u0010X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0011\u001a\u00020\nX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0012\u001a\u00020\fX\u0082D\u00a2\u0006\u0002\n\u0000R\u0010\u0010\u0013\u001a\u0004\u0018\u00010\u0014X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0015\u001a\u00020\fX\u0082D\u00a2\u0006\u0002\n\u0000R&\u0010\u0016\u001a\u001a\u0012\u0016\u0012\u0014\u0012\u0004\u0012\u00020\f\u0012\u0004\u0012\u00020\f\u0012\u0004\u0012\u00020\f0\u00180\u0017X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0019\u001a\u00020\u001aX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u001b\u001a\u00020\u0007X\u0082D\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u001c\u001a\u00020\u0007X\u0082D\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u001d\u001a\u00020\u001eX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u001f\u001a\u00020 X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010!\u001a\u00020\fX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\"\u001a\u00020\fX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010#\u001a\u00020\fX\u0082D\u00a2\u0006\u0002\n\u0000R\u000e\u0010$\u001a\u00020\fX\u0082D\u00a2\u0006\u0002\n\u0000R\u0016\u0010%\u001a\n\u0012\u0004\u0012\u00020\'\u0018\u00010&X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0016\u0010(\u001a\n\u0012\u0004\u0012\u00020\'\u0018\u00010&X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010)\u001a\u00020\nX\u0082\u0004\u00a2\u0006\u0002\n\u0000R,\u0010*\u001a \u0012\u001c\u0012\u001a\u0012\u0016\u0012\u0014\u0012\u0004\u0012\u00020\f\u0012\u0004\u0012\u00020\f\u0012\u0004\u0012\u00020\f0\u00180+0\u0017X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006Q"}, d2 = {"Lcom/boox/atomic/habits/ui/widget/HandwritingFieldView;", "Landroid/view/View;", "context", "Landroid/content/Context;", "attrs", "Landroid/util/AttributeSet;", "defStyleAttr", "", "(Landroid/content/Context;Landroid/util/AttributeSet;I)V", "badgeBgPaint", "Landroid/graphics/Paint;", "badgePadding", "", "badgePaint", "badgeRadius", "bitmap", "Landroid/graphics/Bitmap;", "borderPaint", "borderWidthPx", "canvas", "Landroid/graphics/Canvas;", "confirmedBorderWidthPx", "currentStroke", "", "Lkotlin/Triple;", "dashedEffect", "Landroid/graphics/PathEffect;", "defaultHeightDp", "defaultWidthDp", "gestureDetector", "Lcom/boox/atomic/habits/boox/GestureStrokeDetector;", "isConfirmed", "", "lastTouchX", "lastTouchY", "maxStrokeWidth", "minStrokeWidth", "onConfirmListener", "Lkotlin/Function0;", "", "onStrikethroughListener", "strokePaint", "strokes", "", "clear", "drawBorder", "c", "drawConfirmBadge", "drawStroke", "points", "drawStrokeSegment", "targetCanvas", "x1", "y1", "x2", "y2", "width", "getStrokeData", "", "hasStrokes", "onDraw", "onMeasure", "widthMeasureSpec", "heightMeasureSpec", "onSizeChanged", "w", "h", "oldw", "oldh", "onTouchEvent", "event", "Landroid/view/MotionEvent;", "redrawAllStrokes", "setConfirmed", "confirmed", "setOnConfirmListener", "l", "setOnStrikethroughListener", "setStrokeData", "data", "triggerHapticFeedback", "app_debug"})
public final class HandwritingFieldView extends android.view.View {
    
    /**
     * Each stroke is a list of (x, y, width) triples.
     */
    @org.jetbrains.annotations.NotNull()
    private final java.util.List<java.util.List<kotlin.Triple<java.lang.Float, java.lang.Float, java.lang.Float>>> strokes = null;
    
    /**
     * Points being accumulated during the current touch gesture.
     */
    @org.jetbrains.annotations.NotNull()
    private final java.util.List<kotlin.Triple<java.lang.Float, java.lang.Float, java.lang.Float>> currentStroke = null;
    private boolean isConfirmed = false;
    
    /**
     * Backing bitmap for persistent rendering.
     */
    @org.jetbrains.annotations.Nullable()
    private android.graphics.Bitmap bitmap;
    
    /**
     * Canvas that draws onto [bitmap].
     */
    @org.jetbrains.annotations.Nullable()
    private android.graphics.Canvas canvas;
    
    /**
     * Paint for drawing ink strokes.
     */
    @org.jetbrains.annotations.NotNull()
    private final android.graphics.Paint strokePaint = null;
    
    /**
     * Paint for the faint border around the writing area.
     */
    @org.jetbrains.annotations.NotNull()
    private final android.graphics.Paint borderPaint = null;
    
    /**
     * Dashed path effect used when the field is in editing mode.
     */
    @org.jetbrains.annotations.NotNull()
    private final android.graphics.PathEffect dashedEffect = null;
    
    /**
     * Paint for the confirm checkmark badge.
     */
    @org.jetbrains.annotations.NotNull()
    private final android.graphics.Paint badgePaint = null;
    
    /**
     * Paint for the badge background rectangle.
     */
    @org.jetbrains.annotations.NotNull()
    private final android.graphics.Paint badgeBgPaint = null;
    @org.jetbrains.annotations.Nullable()
    private kotlin.jvm.functions.Function0<kotlin.Unit> onConfirmListener;
    @org.jetbrains.annotations.Nullable()
    private kotlin.jvm.functions.Function0<kotlin.Unit> onStrikethroughListener;
    @org.jetbrains.annotations.NotNull()
    private final com.boox.atomic.habits.boox.GestureStrokeDetector gestureDetector = null;
    private float lastTouchX = 0.0F;
    private float lastTouchY = 0.0F;
    private final int defaultWidthDp = 200;
    private final int defaultHeightDp = 80;
    private final float borderWidthPx = 1.0F;
    private final float confirmedBorderWidthPx = 2.0F;
    private final float minStrokeWidth = 3.0F;
    private final float maxStrokeWidth = 12.0F;
    private final float badgePadding = 8.0F;
    private final float badgeRadius = 16.0F;
    
    @kotlin.jvm.JvmOverloads()
    public HandwritingFieldView(@org.jetbrains.annotations.NotNull()
    android.content.Context context, @org.jetbrains.annotations.Nullable()
    android.util.AttributeSet attrs, int defStyleAttr) {
        super(null);
    }
    
    /**
     * Restore stroke data previously obtained from [getStrokeData].
     * Replaces all current strokes and redraws.
     */
    public final void setStrokeData(@org.jetbrains.annotations.NotNull()
    java.lang.String data) {
    }
    
    /**
     * Serialize all stored strokes to JSON via StrokeSerializer.
     */
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getStrokeData() {
        return null;
    }
    
    /**
     * Whether this field is locked (read-only / confirmed).
     */
    public final boolean isConfirmed() {
        return false;
    }
    
    /**
     * Lock or unlock editing.
     * @param confirmed true to make the field read-only, false to allow editing.
     */
    public final void setConfirmed(boolean confirmed) {
    }
    
    /**
     * Register a callback invoked when the field is confirmed (locked).
     */
    public final void setOnConfirmListener(@org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> l) {
    }
    
    public final void setOnStrikethroughListener(@org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> l) {
    }
    
    public final void clear() {
    }
    
    /**
     * Returns true when at least one completed stroke exists.
     */
    public final boolean hasStrokes() {
        return false;
    }
    
    @java.lang.Override()
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
    }
    
    @java.lang.Override()
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    }
    
    @java.lang.Override()
    protected void onDraw(@org.jetbrains.annotations.NotNull()
    android.graphics.Canvas canvas) {
    }
    
    @java.lang.Override()
    public boolean onTouchEvent(@org.jetbrains.annotations.NotNull()
    android.view.MotionEvent event) {
        return false;
    }
    
    /**
     * Draws the appropriate border style depending on confirmation state.
     *
     * - Editing mode: 1px gray dashed border.
     * - Confirmed, empty: 1px gray solid border.
     * - Confirmed, with content: 2px green-ish solid border.
     */
    private final void drawBorder(android.graphics.Canvas c) {
    }
    
    /**
     * Draws a completed stroke as a series of connected segments.
     */
    private final void drawStroke(android.graphics.Canvas c, java.util.List<kotlin.Triple<java.lang.Float, java.lang.Float, java.lang.Float>> points) {
    }
    
    /**
     * Draws a single line segment between two points at the given width.
     * Used for incremental drawing during ACTION_MOVE for performance.
     */
    private final void drawStrokeSegment(android.graphics.Canvas targetCanvas, float x1, float y1, float x2, float y2, float width) {
    }
    
    /**
     * Redraws all completed strokes onto the backing bitmap from scratch.
     * Called after setStrokeData / onSizeChanged.
     */
    private final void redrawAllStrokes() {
    }
    
    /**
     * Draws a subtle dark-gray rounded rect with a white "✓" character
     * at the bottom-right corner.
     */
    private final void drawConfirmBadge(android.graphics.Canvas c) {
    }
    
    /**
     * Triggers a short haptic feedback vibration on stylus down.
     * Compatible with both old and new Android vibration APIs.
     */
    private final void triggerHapticFeedback() {
    }
    
    @kotlin.jvm.JvmOverloads()
    public HandwritingFieldView(@org.jetbrains.annotations.NotNull()
    android.content.Context context) {
        super(null);
    }
    
    @kotlin.jvm.JvmOverloads()
    public HandwritingFieldView(@org.jetbrains.annotations.NotNull()
    android.content.Context context, @org.jetbrains.annotations.Nullable()
    android.util.AttributeSet attrs) {
        super(null);
    }
}