package com.boox.atomic.habits.boox;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\"\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0002\u0010\u0007\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0003\b\u00c6\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002J&\u0010\u0003\u001a\u001a\u0012\u0016\u0012\u0014\u0012\u0004\u0012\u00020\u0006\u0012\u0004\u0012\u00020\u0006\u0012\u0004\u0012\u00020\u00060\u00050\u00042\u0006\u0010\u0007\u001a\u00020\bJ&\u0010\t\u001a\u00020\b2\u001e\u0010\n\u001a\u001a\u0012\u0016\u0012\u0014\u0012\u0004\u0012\u00020\u0006\u0012\u0004\u0012\u00020\u0006\u0012\u0004\u0012\u00020\u00060\u00050\u0004\u00a8\u0006\u000b"}, d2 = {"Lcom/boox/atomic/habits/boox/StrokeSerializer;", "", "()V", "deserialize", "", "Lkotlin/Triple;", "", "data", "", "serialize", "points", "app_debug"})
public final class StrokeSerializer {
    @org.jetbrains.annotations.NotNull()
    public static final com.boox.atomic.habits.boox.StrokeSerializer INSTANCE = null;
    
    private StrokeSerializer() {
        super();
    }
    
    /**
     * Serialize a list of touch points (x, y, pressure) to a compact JSON string.
     * Format: [[x1,y1,w1],[x2,y2,w2],...]
     * where w = stroke width derived from pressure (2f + pressure * 10f).
     */
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String serialize(@org.jetbrains.annotations.NotNull()
    java.util.List<kotlin.Triple<java.lang.Float, java.lang.Float, java.lang.Float>> points) {
        return null;
    }
    
    /**
     * Deserialize a JSON string back to a list of touch points.
     * Returns triple of (x, y, width).
     */
    @org.jetbrains.annotations.NotNull()
    public final java.util.List<kotlin.Triple<java.lang.Float, java.lang.Float, java.lang.Float>> deserialize(@org.jetbrains.annotations.NotNull()
    java.lang.String data) {
        return null;
    }
}