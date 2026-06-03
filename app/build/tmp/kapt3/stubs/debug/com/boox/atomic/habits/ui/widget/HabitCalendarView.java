package com.boox.atomic.habits.ui.widget;

/**
 * Compact month calendar grid for e-ink displays.
 *
 * Shows the current month as a grid where each day is:
 *  ● filled circle = completed
 *  ○ empty circle = not completed/missed
 *  no circle = future date
 *
 * Designed to be narrow (fixed 7-column week layout) and shallow
 * (4-6 rows depending on month). E-ink optimized: grayscale only,
 * no colors, anti-aliasing off.
 */
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000R\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\b\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010#\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010 \n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\t\u0018\u00002\u00020\u0001B%\b\u0007\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\n\b\u0002\u0010\u0004\u001a\u0004\u0018\u00010\u0005\u0012\b\b\u0002\u0010\u0006\u001a\u00020\u0007\u00a2\u0006\u0002\u0010\bJ\u0010\u0010\u0019\u001a\u00020\u001a2\u0006\u0010\u001b\u001a\u00020\u001cH\u0014J\u0018\u0010\u001d\u001a\u00020\u001a2\u0006\u0010\u001e\u001a\u00020\u00072\u0006\u0010\u001f\u001a\u00020\u0007H\u0014J\u0014\u0010 \u001a\u00020\u001a2\f\u0010!\u001a\b\u0012\u0004\u0012\u00020\r0\u0012J\u0016\u0010\"\u001a\u00020\u001a2\u0006\u0010#\u001a\u00020\u00072\u0006\u0010$\u001a\u00020\u0007R\u000e\u0010\t\u001a\u00020\nX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010\u000b\u001a\b\u0012\u0004\u0012\u00020\r0\fX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u000e\u001a\u00020\u000fX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0010\u001a\u00020\nX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010\u0011\u001a\b\u0012\u0004\u0012\u00020\r0\u0012X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0013\u001a\u00020\u0014X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0015\u001a\u00020\nX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0016\u001a\u00020\u000fX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0017\u001a\u00020\nX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0018\u001a\u00020\nX\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006%"}, d2 = {"Lcom/boox/atomic/habits/ui/widget/HabitCalendarView;", "Landroid/view/View;", "context", "Landroid/content/Context;", "attrs", "Landroid/util/AttributeSet;", "defStyleAttr", "", "(Landroid/content/Context;Landroid/util/AttributeSet;I)V", "circlePaint", "Landroid/graphics/Paint;", "completedDates", "", "", "dateFormat", "Ljava/text/SimpleDateFormat;", "dayNamePaint", "dayNames", "", "displayDate", "Ljava/util/Calendar;", "fillPaint", "monthYearFormat", "textPaint", "todayPaint", "onDraw", "", "canvas", "Landroid/graphics/Canvas;", "onMeasure", "widthMeasureSpec", "heightMeasureSpec", "setCompletedDates", "dates", "setDisplayMonth", "year", "month", "app_debug"})
public final class HabitCalendarView extends android.view.View {
    @org.jetbrains.annotations.NotNull()
    private final android.graphics.Paint circlePaint = null;
    @org.jetbrains.annotations.NotNull()
    private final android.graphics.Paint fillPaint = null;
    @org.jetbrains.annotations.NotNull()
    private final android.graphics.Paint textPaint = null;
    @org.jetbrains.annotations.NotNull()
    private final android.graphics.Paint dayNamePaint = null;
    @org.jetbrains.annotations.NotNull()
    private final android.graphics.Paint todayPaint = null;
    @org.jetbrains.annotations.NotNull()
    private final java.util.Set<java.lang.String> completedDates = null;
    @org.jetbrains.annotations.NotNull()
    private java.util.Calendar displayDate;
    @org.jetbrains.annotations.NotNull()
    private final java.text.SimpleDateFormat monthYearFormat = null;
    @org.jetbrains.annotations.NotNull()
    private final java.text.SimpleDateFormat dateFormat = null;
    @org.jetbrains.annotations.NotNull()
    private final java.util.List<java.lang.String> dayNames = null;
    
    @kotlin.jvm.JvmOverloads()
    public HabitCalendarView(@org.jetbrains.annotations.NotNull()
    android.content.Context context, @org.jetbrains.annotations.Nullable()
    android.util.AttributeSet attrs, int defStyleAttr) {
        super(null);
    }
    
    /**
     * Set which dates are completed. Dates should be "yyyy-MM-dd" format.
     */
    public final void setCompletedDates(@org.jetbrains.annotations.NotNull()
    java.util.List<java.lang.String> dates) {
    }
    
    /**
     * Set the month/year to display. Defaults to current month.
     */
    public final void setDisplayMonth(int year, int month) {
    }
    
    @java.lang.Override()
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    }
    
    @java.lang.Override()
    protected void onDraw(@org.jetbrains.annotations.NotNull()
    android.graphics.Canvas canvas) {
    }
    
    @kotlin.jvm.JvmOverloads()
    public HabitCalendarView(@org.jetbrains.annotations.NotNull()
    android.content.Context context) {
        super(null);
    }
    
    @kotlin.jvm.JvmOverloads()
    public HabitCalendarView(@org.jetbrains.annotations.NotNull()
    android.content.Context context, @org.jetbrains.annotations.Nullable()
    android.util.AttributeSet attrs) {
        super(null);
    }
}