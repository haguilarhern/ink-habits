package com.boox.atomic.habits.boox;

/**
 * Detects stylus gestures from raw MotionEvent data (no Onyx SDK dependency).
 *
 * Captures touch points during ACTION_MOVE and analyzes the stroke
 * on ACTION_UP to determine if a gesture was drawn.
 */
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u00004\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010!\n\u0002\u0018\u0002\n\u0002\u0010\u0007\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0010 \n\u0000\n\u0002\u0010\u000b\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\u0018\u00002\u00020\u0001B\u0005\u00a2\u0006\u0002\u0010\u0002J\u0006\u0010\u0007\u001a\u00020\bJ\u0018\u0010\t\u001a\u0014\u0012\u0010\u0012\u000e\u0012\u0004\u0012\u00020\u0006\u0012\u0004\u0012\u00020\u00060\u00050\nJ\u000e\u0010\u000b\u001a\u00020\f2\u0006\u0010\r\u001a\u00020\u000eJ\u0006\u0010\u000f\u001a\u00020\fR \u0010\u0003\u001a\u0014\u0012\u0010\u0012\u000e\u0012\u0004\u0012\u00020\u0006\u0012\u0004\u0012\u00020\u00060\u00050\u0004X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0010"}, d2 = {"Lcom/boox/atomic/habits/boox/GestureStrokeDetector;", "", "()V", "points", "", "Lkotlin/Pair;", "", "clear", "", "getPoints", "", "handleMotionEvent", "", "event", "Landroid/view/MotionEvent;", "isStrikethrough", "app_debug"})
public final class GestureStrokeDetector {
    @org.jetbrains.annotations.NotNull()
    private final java.util.List<kotlin.Pair<java.lang.Float, java.lang.Float>> points = null;
    
    public GestureStrokeDetector() {
        super();
    }
    
    /**
     * Call from onTouchEvent. Returns true if the event was consumed.
     */
    public final boolean handleMotionEvent(@org.jetbrains.annotations.NotNull()
    android.view.MotionEvent event) {
        return false;
    }
    
    /**
     * After ACTION_UP, analyze the captured points to detect a strikethrough.
     * A strikethrough = mostly horizontal line across the view.
     */
    public final boolean isStrikethrough() {
        return false;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.util.List<kotlin.Pair<java.lang.Float, java.lang.Float>> getPoints() {
        return null;
    }
    
    public final void clear() {
    }
}