package com.boox.atomic.habits.ui.dashboard;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000f\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u0002\n\u0000\n\u0002\u0010\t\n\u0000\n\u0002\u0010\u000b\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0006\u0018\u00002\u00020\u0001B\u0005\u00a2\u0006\u0002\u0010\u0002J\u0018\u0010\u0019\u001a\u00020\u001a2\u0006\u0010\u001b\u001a\u00020\u001c2\u0006\u0010\u001d\u001a\u00020\u001eH\u0002J\b\u0010\u001f\u001a\u00020\u001aH\u0002J\u0012\u0010 \u001a\u00020\u001a2\b\u0010!\u001a\u0004\u0018\u00010\"H\u0014J\b\u0010#\u001a\u00020\u001aH\u0014J\b\u0010$\u001a\u00020\u001aH\u0002J\u0018\u0010%\u001a\u00020\u001a2\u0006\u0010&\u001a\u00020\u001c2\u0006\u0010\'\u001a\u00020\u001eH\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082.\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0006X\u0082.\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0007\u001a\u00020\bX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\t\u001a\u00020\nX\u0082.\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u000b\u001a\u00020\bX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\f\u001a\u00020\rX\u0082.\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u000e\u001a\u00020\u000fX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0016\u0010\u0010\u001a\n \u0011*\u0004\u0018\u00010\u000f0\u000fX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010\u0012\u001a\b\u0012\u0004\u0012\u00020\u00140\u0013X\u0082.\u00a2\u0006\u0002\n\u0000R\u0014\u0010\u0015\u001a\b\u0012\u0004\u0012\u00020\u00170\u0016X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0018\u001a\u00020\rX\u0082.\u00a2\u0006\u0002\n\u0000\u00a8\u0006("}, d2 = {"Lcom/boox/atomic/habits/ui/dashboard/DashboardActivity;", "Lcom/boox/atomic/habits/ui/EInkActivity;", "()V", "currentDateText", "Landroid/widget/TextView;", "dashboardAdapter", "Lcom/boox/atomic/habits/ui/dashboard/DashboardAdapter;", "dateFormat", "Ljava/text/SimpleDateFormat;", "db", "Lcom/boox/atomic/habits/data/AppDatabase;", "dbDateFormat", "identityGoalsRecyclerView", "Landroidx/recyclerview/widget/RecyclerView;", "selectedDate", "Ljava/util/Calendar;", "todayCal", "kotlin.jvm.PlatformType", "todoAdapter", "Landroidx/recyclerview/widget/RecyclerView$Adapter;", "Landroidx/recyclerview/widget/RecyclerView$ViewHolder;", "todoList", "", "Lcom/boox/atomic/habits/data/entity/ToDo;", "todosRecyclerView", "handleCheckIn", "", "habitId", "", "isCompleted", "", "loadDashboard", "onCreate", "savedInstanceState", "Landroid/os/Bundle;", "onResume", "setupRecyclerViews", "toggleTodo", "todoId", "completed", "app_debug"})
public final class DashboardActivity extends com.boox.atomic.habits.ui.EInkActivity {
    private com.boox.atomic.habits.data.AppDatabase db;
    private com.boox.atomic.habits.ui.dashboard.DashboardAdapter dashboardAdapter;
    private androidx.recyclerview.widget.RecyclerView.Adapter<androidx.recyclerview.widget.RecyclerView.ViewHolder> todoAdapter;
    private androidx.recyclerview.widget.RecyclerView identityGoalsRecyclerView;
    private androidx.recyclerview.widget.RecyclerView todosRecyclerView;
    private android.widget.TextView currentDateText;
    @org.jetbrains.annotations.NotNull()
    private java.util.List<com.boox.atomic.habits.data.entity.ToDo> todoList;
    @org.jetbrains.annotations.NotNull()
    private java.util.Calendar selectedDate;
    @org.jetbrains.annotations.NotNull()
    private final java.text.SimpleDateFormat dateFormat = null;
    @org.jetbrains.annotations.NotNull()
    private final java.text.SimpleDateFormat dbDateFormat = null;
    private final java.util.Calendar todayCal = null;
    
    public DashboardActivity() {
        super();
    }
    
    @java.lang.Override()
    protected void onCreate(@org.jetbrains.annotations.Nullable()
    android.os.Bundle savedInstanceState) {
    }
    
    @java.lang.Override()
    protected void onResume() {
    }
    
    private final void setupRecyclerViews() {
    }
    
    private final void loadDashboard() {
    }
    
    private final void handleCheckIn(long habitId, boolean isCompleted) {
    }
    
    private final void toggleTodo(long todoId, boolean completed) {
    }
}