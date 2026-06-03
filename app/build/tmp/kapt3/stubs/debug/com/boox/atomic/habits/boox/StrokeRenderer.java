package com.boox.atomic.habits.boox;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u00008\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0010\u0007\n\u0002\b\u0003\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\b\u00c6\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002J,\u0010\u0003\u001a\u00020\u00042\b\u0010\u0005\u001a\u0004\u0018\u00010\u00062\u0006\u0010\u0007\u001a\u00020\b2\b\b\u0002\u0010\t\u001a\u00020\n2\b\b\u0002\u0010\u000b\u001a\u00020\nJ@\u0010\f\u001a\u00020\u00042\u0006\u0010\u0005\u001a\u00020\u00062\u001e\u0010\r\u001a\u001a\u0012\u0016\u0012\u0014\u0012\u0004\u0012\u00020\n\u0012\u0004\u0012\u00020\n\u0012\u0004\u0012\u00020\n0\u000f0\u000e2\u0006\u0010\t\u001a\u00020\n2\u0006\u0010\u000b\u001a\u00020\nH\u0002J,\u0010\u0010\u001a\u00020\u00042\b\u0010\u0005\u001a\u0004\u0018\u00010\u00062\u0006\u0010\u0007\u001a\u00020\b2\b\b\u0002\u0010\t\u001a\u00020\n2\b\b\u0002\u0010\u000b\u001a\u00020\nJ\u000e\u0010\u0011\u001a\u00020\u00122\u0006\u0010\u0007\u001a\u00020\b\u00a8\u0006\u0013"}, d2 = {"Lcom/boox/atomic/habits/boox/StrokeRenderer;", "", "()V", "drawStrikethrough", "", "canvas", "Landroid/graphics/Canvas;", "strokeData", "", "offsetX", "", "offsetY", "drawStroke", "points", "", "Lkotlin/Triple;", "drawStrokes", "measureStrokes", "Landroid/graphics/RectF;", "app_debug"})
public final class StrokeRenderer {
    @org.jetbrains.annotations.NotNull()
    public static final com.boox.atomic.habits.boox.StrokeRenderer INSTANCE = null;
    
    private StrokeRenderer() {
        super();
    }
    
    /**
     * Draw strokes onto a Canvas from serialized stroke data.
     */
    public final void drawStrokes(@org.jetbrains.annotations.Nullable()
    android.graphics.Canvas canvas, @org.jetbrains.annotations.NotNull()
    java.lang.String strokeData, float offsetX, float offsetY) {
    }
    
    /**
     * Draw a strikethrough line across the bounding box of the strokes.
     */
    public final void drawStrikethrough(@org.jetbrains.annotations.Nullable()
    android.graphics.Canvas canvas, @org.jetbrains.annotations.NotNull()
    java.lang.String strokeData, float offsetX, float offsetY) {
    }
    
    /**
     * Calculate the bounding box of all strokes.
     */
    @org.jetbrains.annotations.NotNull()
    public final android.graphics.RectF measureStrokes(@org.jetbrains.annotations.NotNull()
    java.lang.String strokeData) {
        return null;
    }
    
    /**
     * Draw a single path of points with varying stroke width per segment.
     */
    private final void drawStroke(android.graphics.Canvas canvas, java.util.List<kotlin.Triple<java.lang.Float, java.lang.Float, java.lang.Float>> points, float offsetX, float offsetY) {
    }
}