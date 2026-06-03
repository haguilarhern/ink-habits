package com.boox.atomic.habits.ui.dashboard;

/**
 * ViewHolder for a single habit check-in row.
 *
 * Renders handwriting strokes + shows a compact month calendar heatmap
 * below the habit name. Calendar updates with completion data from DB.
 */
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000l\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0010\t\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u000b\n\u0000\n\u0002\u0010\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0010\b\n\u0002\b\n\u0018\u00002\u00020\u0001BE\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00126\u0010\u0004\u001a2\u0012\u0013\u0012\u00110\u0006\u00a2\u0006\f\b\u0007\u0012\b\b\b\u0012\u0004\b\b(\t\u0012\u0013\u0012\u00110\n\u00a2\u0006\f\b\u0007\u0012\b\b\b\u0012\u0004\b\b(\u000b\u0012\u0004\u0012\u00020\f0\u0005\u00a2\u0006\u0002\u0010\rJj\u0010#\u001a\u00020\f2\u0006\u0010\t\u001a\u00020\u00062\u0006\u0010\b\u001a\u00020\u00112\b\u0010$\u001a\u0004\u0018\u00010\u00112\u0006\u0010\u000b\u001a\u00020\n2\u0006\u0010%\u001a\u00020\u00112\u0006\u0010&\u001a\u00020\'2\u0006\u0010(\u001a\u00020\u00112\u0006\u0010)\u001a\u00020\'2\b\b\u0002\u0010*\u001a\u00020\u00112\n\b\u0002\u0010+\u001a\u0004\u0018\u00010\u00162\n\b\u0002\u0010 \u001a\u0004\u0018\u00010!J \u0010,\u001a\u00020\f2\u0006\u0010-\u001a\u00020\u001a2\u0006\u0010$\u001a\u00020\u00112\u0006\u0010.\u001a\u00020\nH\u0002J\b\u0010/\u001a\u00020\fH\u0002J\u0010\u00100\u001a\u00020\u00112\u0006\u0010(\u001a\u00020\u0011H\u0002R\u0010\u0010\u000e\u001a\u0004\u0018\u00010\u000fX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0010\u001a\u00020\u0011X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0012\u001a\u00020\u0006X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0013\u001a\u00020\nX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0010\u0010\u0014\u001a\u0004\u0018\u00010\u0011X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0010\u0010\u0015\u001a\u0004\u0018\u00010\u0016X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0017\u001a\u00020\u0018X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0019\u001a\u00020\u001aX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u001b\u001a\u00020\u001cX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u001d\u001a\u00020\u001eX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u001f\u001a\u00020\u001aX\u0082\u0004\u00a2\u0006\u0002\n\u0000R>\u0010\u0004\u001a2\u0012\u0013\u0012\u00110\u0006\u00a2\u0006\f\b\u0007\u0012\b\b\b\u0012\u0004\b\b(\t\u0012\u0013\u0012\u00110\n\u00a2\u0006\f\b\u0007\u0012\b\b\b\u0012\u0004\b\b(\u000b\u0012\u0004\u0012\u00020\f0\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0010\u0010 \u001a\u0004\u0018\u00010!X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\"\u001a\u00020\u001aX\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u00061"}, d2 = {"Lcom/boox/atomic/habits/ui/dashboard/HabitCheckInViewHolder;", "Landroidx/recyclerview/widget/RecyclerView$ViewHolder;", "itemView", "Landroid/view/View;", "onCheckIn", "Lkotlin/Function2;", "", "Lkotlin/ParameterName;", "name", "habitId", "", "isCompleted", "", "(Landroid/view/View;Lkotlin/jvm/functions/Function2;)V", "calendarView", "Lcom/boox/atomic/habits/ui/widget/HabitCalendarView;", "currentDateStr", "", "currentHabitId", "currentIsCompleted", "currentStrokeData", "dbRef", "Lcom/boox/atomic/habits/data/AppDatabase;", "df", "Ljava/text/SimpleDateFormat;", "frequencyHint", "Landroid/widget/TextView;", "gestureDetector", "Lcom/boox/atomic/habits/boox/GestureStrokeDetector;", "habitCheckbox", "Landroid/widget/CheckBox;", "habitName", "scope", "Lkotlinx/coroutines/CoroutineScope;", "streakBadge", "bind", "strokeData", "frequencyType", "intervalDays", "", "daysOfWeek", "streak", "dateStr", "db", "drawStrokesOnTextView", "textView", "isChecked", "loadCalendar", "parseDaysOfWeek", "app_debug"})
public final class HabitCheckInViewHolder extends androidx.recyclerview.widget.RecyclerView.ViewHolder {
    @org.jetbrains.annotations.NotNull()
    private final kotlin.jvm.functions.Function2<java.lang.Long, java.lang.Boolean, kotlin.Unit> onCheckIn = null;
    @org.jetbrains.annotations.NotNull()
    private final android.widget.CheckBox habitCheckbox = null;
    @org.jetbrains.annotations.NotNull()
    private final android.widget.TextView habitName = null;
    @org.jetbrains.annotations.NotNull()
    private final android.widget.TextView frequencyHint = null;
    @org.jetbrains.annotations.NotNull()
    private final android.widget.TextView streakBadge = null;
    private long currentHabitId = 0L;
    @org.jetbrains.annotations.Nullable()
    private java.lang.String currentStrokeData;
    private boolean currentIsCompleted = false;
    @org.jetbrains.annotations.NotNull()
    private java.lang.String currentDateStr = "";
    @org.jetbrains.annotations.Nullable()
    private com.boox.atomic.habits.data.AppDatabase dbRef;
    @org.jetbrains.annotations.Nullable()
    private kotlinx.coroutines.CoroutineScope scope;
    @org.jetbrains.annotations.Nullable()
    private com.boox.atomic.habits.ui.widget.HabitCalendarView calendarView;
    @org.jetbrains.annotations.NotNull()
    private final java.text.SimpleDateFormat df = null;
    @org.jetbrains.annotations.NotNull()
    private final com.boox.atomic.habits.boox.GestureStrokeDetector gestureDetector = null;
    
    public HabitCheckInViewHolder(@org.jetbrains.annotations.NotNull()
    android.view.View itemView, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function2<? super java.lang.Long, ? super java.lang.Boolean, kotlin.Unit> onCheckIn) {
        super(null);
    }
    
    public final void bind(long habitId, @org.jetbrains.annotations.NotNull()
    java.lang.String name, @org.jetbrains.annotations.Nullable()
    java.lang.String strokeData, boolean isCompleted, @org.jetbrains.annotations.NotNull()
    java.lang.String frequencyType, int intervalDays, @org.jetbrains.annotations.NotNull()
    java.lang.String daysOfWeek, int streak, @org.jetbrains.annotations.NotNull()
    java.lang.String dateStr, @org.jetbrains.annotations.Nullable()
    com.boox.atomic.habits.data.AppDatabase db, @org.jetbrains.annotations.Nullable()
    kotlinx.coroutines.CoroutineScope scope) {
    }
    
    private final void loadCalendar() {
    }
    
    private final void drawStrokesOnTextView(android.widget.TextView textView, java.lang.String strokeData, boolean isChecked) {
    }
    
    private final java.lang.String parseDaysOfWeek(java.lang.String daysOfWeek) {
        return null;
    }
}