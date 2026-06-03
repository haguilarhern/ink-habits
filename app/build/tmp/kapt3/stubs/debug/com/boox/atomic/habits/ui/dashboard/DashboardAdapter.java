package com.boox.atomic.habits.ui.dashboard;

/**
 * Expandable adapter that displays identity goals as top-level items
 * with child habits revealed on expand/collapse.
 *
 * Data model: [DashboardItem] from the Room query joins identity goals
 * with their habits. The adapter groups them by goal ID.
 */
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000p\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010!\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0010\t\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u000b\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010%\n\u0000\n\u0002\u0010\b\n\u0002\b\u0006\n\u0002\u0018\u0002\n\u0002\b\t\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0002\b\u0003\u0018\u0000 .2\b\u0012\u0004\u0012\u00020\u00020\u0001:\u0002./B[\u0012\u000e\b\u0002\u0010\u0003\u001a\b\u0012\u0004\u0012\u00020\u00050\u0004\u00126\u0010\u0006\u001a2\u0012\u0013\u0012\u00110\b\u00a2\u0006\f\b\t\u0012\b\b\n\u0012\u0004\b\b(\u000b\u0012\u0013\u0012\u00110\f\u00a2\u0006\f\b\t\u0012\b\b\n\u0012\u0004\b\b(\r\u0012\u0004\u0012\u00020\u000e0\u0007\u0012\f\u0010\u000f\u001a\b\u0012\u0004\u0012\u00020\u000e0\u0010\u00a2\u0006\u0002\u0010\u0011J\b\u0010\u001a\u001a\u00020\u001bH\u0016J\u0010\u0010\u001c\u001a\u00020\u001b2\u0006\u0010\u001d\u001a\u00020\u001bH\u0016J\u0018\u0010\u001e\u001a\u00020\u000e2\u0006\u0010\u001f\u001a\u00020\u00022\u0006\u0010\u001d\u001a\u00020\u001bH\u0016J\u0018\u0010 \u001a\u00020\u00022\u0006\u0010!\u001a\u00020\"2\u0006\u0010#\u001a\u00020\u001bH\u0016J\b\u0010$\u001a\u00020\u000eH\u0002J\u001e\u0010%\u001a\u00020\u000e2\u0006\u0010&\u001a\u00020\u00152\u0006\u0010\'\u001a\u00020\u00172\u0006\u0010(\u001a\u00020\u0013J\u0010\u0010)\u001a\u00020\u000e2\u0006\u0010\u001d\u001a\u00020\u001bH\u0002J\u0014\u0010*\u001a\u00020\u000e2\f\u0010+\u001a\b\u0012\u0004\u0012\u00020-0,R\u0010\u0010\u0012\u001a\u0004\u0018\u00010\u0013X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0014\u001a\u00020\u0015X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0010\u0010\u0016\u001a\u0004\u0018\u00010\u0017X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u001a\u0010\u0018\u001a\u000e\u0012\u0004\u0012\u00020\b\u0012\u0004\u0012\u00020\f0\u0019X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010\u0003\u001a\b\u0012\u0004\u0012\u00020\u00050\u0004X\u0082\u000e\u00a2\u0006\u0002\n\u0000R>\u0010\u0006\u001a2\u0012\u0013\u0012\u00110\b\u00a2\u0006\f\b\t\u0012\b\b\n\u0012\u0004\b\b(\u000b\u0012\u0013\u0012\u00110\f\u00a2\u0006\f\b\t\u0012\b\b\n\u0012\u0004\b\b(\r\u0012\u0004\u0012\u00020\u000e0\u0007X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010\u000f\u001a\b\u0012\u0004\u0012\u00020\u000e0\u0010X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u00060"}, d2 = {"Lcom/boox/atomic/habits/ui/dashboard/DashboardAdapter;", "Landroidx/recyclerview/widget/RecyclerView$Adapter;", "Landroidx/recyclerview/widget/RecyclerView$ViewHolder;", "items", "", "Lcom/boox/atomic/habits/ui/dashboard/DashboardAdapter$ExpandableItem;", "onCheckIn", "Lkotlin/Function2;", "", "Lkotlin/ParameterName;", "name", "habitId", "", "isCompleted", "", "onRefreshNeeded", "Lkotlin/Function0;", "(Ljava/util/List;Lkotlin/jvm/functions/Function2;Lkotlin/jvm/functions/Function0;)V", "coroutineScope", "Lkotlinx/coroutines/CoroutineScope;", "currentDateStr", "", "dbRef", "Lcom/boox/atomic/habits/data/AppDatabase;", "expandedState", "", "getItemCount", "", "getItemViewType", "position", "onBindViewHolder", "holder", "onCreateViewHolder", "parent", "Landroid/view/ViewGroup;", "viewType", "rebuildFlatList", "setDataSource", "dateStr", "db", "scope", "toggleGoal", "updateData", "dashboardItems", "", "Lcom/boox/atomic/habits/data/dao/DashboardItem;", "Companion", "ExpandableItem", "app_debug"})
public final class DashboardAdapter extends androidx.recyclerview.widget.RecyclerView.Adapter<androidx.recyclerview.widget.RecyclerView.ViewHolder> {
    @org.jetbrains.annotations.NotNull()
    private java.util.List<com.boox.atomic.habits.ui.dashboard.DashboardAdapter.ExpandableItem> items;
    @org.jetbrains.annotations.NotNull()
    private final kotlin.jvm.functions.Function2<java.lang.Long, java.lang.Boolean, kotlin.Unit> onCheckIn = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlin.jvm.functions.Function0<kotlin.Unit> onRefreshNeeded = null;
    @org.jetbrains.annotations.NotNull()
    private java.lang.String currentDateStr = "";
    @org.jetbrains.annotations.Nullable()
    private com.boox.atomic.habits.data.AppDatabase dbRef;
    @org.jetbrains.annotations.Nullable()
    private kotlinx.coroutines.CoroutineScope coroutineScope;
    private static final int TYPE_GOAL = 0;
    private static final int TYPE_HABIT = 1;
    @org.jetbrains.annotations.NotNull()
    private final java.util.Map<java.lang.Long, java.lang.Boolean> expandedState = null;
    @org.jetbrains.annotations.NotNull()
    public static final com.boox.atomic.habits.ui.dashboard.DashboardAdapter.Companion Companion = null;
    
    public DashboardAdapter(@org.jetbrains.annotations.NotNull()
    java.util.List<com.boox.atomic.habits.ui.dashboard.DashboardAdapter.ExpandableItem> items, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function2<? super java.lang.Long, ? super java.lang.Boolean, kotlin.Unit> onCheckIn, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onRefreshNeeded) {
        super();
    }
    
    public final void setDataSource(@org.jetbrains.annotations.NotNull()
    java.lang.String dateStr, @org.jetbrains.annotations.NotNull()
    com.boox.atomic.habits.data.AppDatabase db, @org.jetbrains.annotations.NotNull()
    kotlinx.coroutines.CoroutineScope scope) {
    }
    
    @java.lang.Override()
    public int getItemViewType(int position) {
        return 0;
    }
    
    @java.lang.Override()
    public int getItemCount() {
        return 0;
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.NotNull()
    public androidx.recyclerview.widget.RecyclerView.ViewHolder onCreateViewHolder(@org.jetbrains.annotations.NotNull()
    android.view.ViewGroup parent, int viewType) {
        return null;
    }
    
    @java.lang.Override()
    public void onBindViewHolder(@org.jetbrains.annotations.NotNull()
    androidx.recyclerview.widget.RecyclerView.ViewHolder holder, int position) {
    }
    
    /**
     * Toggles the expand/collapse state of a goal header at the given adapter position.
     */
    private final void toggleGoal(int position) {
    }
    
    /**
     * Rebuilds the flat item list based on current grouped data and expanded state.
     */
    public final void updateData(@org.jetbrains.annotations.NotNull()
    java.util.List<com.boox.atomic.habits.data.dao.DashboardItem> dashboardItems) {
    }
    
    private final void rebuildFlatList() {
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0014\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\b\n\u0002\b\u0002\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0006"}, d2 = {"Lcom/boox/atomic/habits/ui/dashboard/DashboardAdapter$Companion;", "", "()V", "TYPE_GOAL", "", "TYPE_HABIT", "app_debug"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000&\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\b\n\u0000\n\u0002\u0010\t\n\u0000\n\u0002\u0010\u000e\n\u0002\b\t\n\u0002\u0010\u000b\n\u0002\b%\b\u0086\b\u0018\u00002\u00020\u0001B\u0085\u0001\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u0012\b\b\u0002\u0010\u0006\u001a\u00020\u0007\u0012\b\b\u0002\u0010\b\u001a\u00020\u0007\u0012\b\b\u0002\u0010\t\u001a\u00020\u0007\u0012\b\b\u0002\u0010\n\u001a\u00020\u0005\u0012\b\b\u0002\u0010\u000b\u001a\u00020\u0007\u0012\n\b\u0002\u0010\f\u001a\u0004\u0018\u00010\u0007\u0012\b\b\u0002\u0010\r\u001a\u00020\u0007\u0012\b\b\u0002\u0010\u000e\u001a\u00020\u0003\u0012\b\b\u0002\u0010\u000f\u001a\u00020\u0007\u0012\b\b\u0002\u0010\u0010\u001a\u00020\u0011\u0012\b\b\u0002\u0010\u0012\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0013J\t\u0010$\u001a\u00020\u0003H\u00c6\u0003J\t\u0010%\u001a\u00020\u0003H\u00c6\u0003J\t\u0010&\u001a\u00020\u0007H\u00c6\u0003J\t\u0010\'\u001a\u00020\u0011H\u00c6\u0003J\t\u0010(\u001a\u00020\u0003H\u00c6\u0003J\t\u0010)\u001a\u00020\u0005H\u00c6\u0003J\t\u0010*\u001a\u00020\u0007H\u00c6\u0003J\t\u0010+\u001a\u00020\u0007H\u00c6\u0003J\t\u0010,\u001a\u00020\u0007H\u00c6\u0003J\t\u0010-\u001a\u00020\u0005H\u00c6\u0003J\t\u0010.\u001a\u00020\u0007H\u00c6\u0003J\u000b\u0010/\u001a\u0004\u0018\u00010\u0007H\u00c6\u0003J\t\u00100\u001a\u00020\u0007H\u00c6\u0003J\u008d\u0001\u00101\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u00032\b\b\u0002\u0010\u0004\u001a\u00020\u00052\b\b\u0002\u0010\u0006\u001a\u00020\u00072\b\b\u0002\u0010\b\u001a\u00020\u00072\b\b\u0002\u0010\t\u001a\u00020\u00072\b\b\u0002\u0010\n\u001a\u00020\u00052\b\b\u0002\u0010\u000b\u001a\u00020\u00072\n\b\u0002\u0010\f\u001a\u0004\u0018\u00010\u00072\b\b\u0002\u0010\r\u001a\u00020\u00072\b\b\u0002\u0010\u000e\u001a\u00020\u00032\b\b\u0002\u0010\u000f\u001a\u00020\u00072\b\b\u0002\u0010\u0010\u001a\u00020\u00112\b\b\u0002\u0010\u0012\u001a\u00020\u0003H\u00c6\u0001J\u0013\u00102\u001a\u00020\u00112\b\u00103\u001a\u0004\u0018\u00010\u0001H\u00d6\u0003J\t\u00104\u001a\u00020\u0003H\u00d6\u0001J\t\u00105\u001a\u00020\u0007H\u00d6\u0001R\u0011\u0010\u000f\u001a\u00020\u0007\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0014\u0010\u0015R\u0011\u0010\r\u001a\u00020\u0007\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0016\u0010\u0015R\u0011\u0010\b\u001a\u00020\u0007\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0017\u0010\u0015R\u0011\u0010\u0004\u001a\u00020\u0005\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0018\u0010\u0019R\u0011\u0010\u0006\u001a\u00020\u0007\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001a\u0010\u0015R\u0011\u0010\t\u001a\u00020\u0007\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001b\u0010\u0015R\u0011\u0010\n\u001a\u00020\u0005\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001c\u0010\u0019R\u0011\u0010\u000b\u001a\u00020\u0007\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001d\u0010\u0015R\u0013\u0010\f\u001a\u0004\u0018\u00010\u0007\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001e\u0010\u0015R\u0011\u0010\u000e\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001f\u0010 R\u0011\u0010\u0010\u001a\u00020\u0011\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0010\u0010!R\u0011\u0010\u0012\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\"\u0010 R\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b#\u0010 \u00a8\u00066"}, d2 = {"Lcom/boox/atomic/habits/ui/dashboard/DashboardAdapter$ExpandableItem;", "", "type", "", "goalId", "", "goalName", "", "goalIcon", "goalStatement", "habitId", "habitName", "habitStrokeData", "frequencyType", "intervalDays", "daysOfWeek", "isCompletedToday", "", "streak", "(IJLjava/lang/String;Ljava/lang/String;Ljava/lang/String;JLjava/lang/String;Ljava/lang/String;Ljava/lang/String;ILjava/lang/String;ZI)V", "getDaysOfWeek", "()Ljava/lang/String;", "getFrequencyType", "getGoalIcon", "getGoalId", "()J", "getGoalName", "getGoalStatement", "getHabitId", "getHabitName", "getHabitStrokeData", "getIntervalDays", "()I", "()Z", "getStreak", "getType", "component1", "component10", "component11", "component12", "component13", "component2", "component3", "component4", "component5", "component6", "component7", "component8", "component9", "copy", "equals", "other", "hashCode", "toString", "app_debug"})
    public static final class ExpandableItem {
        private final int type = 0;
        private final long goalId = 0L;
        @org.jetbrains.annotations.NotNull()
        private final java.lang.String goalName = null;
        @org.jetbrains.annotations.NotNull()
        private final java.lang.String goalIcon = null;
        @org.jetbrains.annotations.NotNull()
        private final java.lang.String goalStatement = null;
        private final long habitId = 0L;
        @org.jetbrains.annotations.NotNull()
        private final java.lang.String habitName = null;
        @org.jetbrains.annotations.Nullable()
        private final java.lang.String habitStrokeData = null;
        @org.jetbrains.annotations.NotNull()
        private final java.lang.String frequencyType = null;
        private final int intervalDays = 0;
        @org.jetbrains.annotations.NotNull()
        private final java.lang.String daysOfWeek = null;
        private final boolean isCompletedToday = false;
        private final int streak = 0;
        
        public ExpandableItem(int type, long goalId, @org.jetbrains.annotations.NotNull()
        java.lang.String goalName, @org.jetbrains.annotations.NotNull()
        java.lang.String goalIcon, @org.jetbrains.annotations.NotNull()
        java.lang.String goalStatement, long habitId, @org.jetbrains.annotations.NotNull()
        java.lang.String habitName, @org.jetbrains.annotations.Nullable()
        java.lang.String habitStrokeData, @org.jetbrains.annotations.NotNull()
        java.lang.String frequencyType, int intervalDays, @org.jetbrains.annotations.NotNull()
        java.lang.String daysOfWeek, boolean isCompletedToday, int streak) {
            super();
        }
        
        public final int getType() {
            return 0;
        }
        
        public final long getGoalId() {
            return 0L;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String getGoalName() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String getGoalIcon() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String getGoalStatement() {
            return null;
        }
        
        public final long getHabitId() {
            return 0L;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String getHabitName() {
            return null;
        }
        
        @org.jetbrains.annotations.Nullable()
        public final java.lang.String getHabitStrokeData() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String getFrequencyType() {
            return null;
        }
        
        public final int getIntervalDays() {
            return 0;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String getDaysOfWeek() {
            return null;
        }
        
        public final boolean isCompletedToday() {
            return false;
        }
        
        public final int getStreak() {
            return 0;
        }
        
        public final int component1() {
            return 0;
        }
        
        public final int component10() {
            return 0;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String component11() {
            return null;
        }
        
        public final boolean component12() {
            return false;
        }
        
        public final int component13() {
            return 0;
        }
        
        public final long component2() {
            return 0L;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String component3() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String component4() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String component5() {
            return null;
        }
        
        public final long component6() {
            return 0L;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String component7() {
            return null;
        }
        
        @org.jetbrains.annotations.Nullable()
        public final java.lang.String component8() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String component9() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final com.boox.atomic.habits.ui.dashboard.DashboardAdapter.ExpandableItem copy(int type, long goalId, @org.jetbrains.annotations.NotNull()
        java.lang.String goalName, @org.jetbrains.annotations.NotNull()
        java.lang.String goalIcon, @org.jetbrains.annotations.NotNull()
        java.lang.String goalStatement, long habitId, @org.jetbrains.annotations.NotNull()
        java.lang.String habitName, @org.jetbrains.annotations.Nullable()
        java.lang.String habitStrokeData, @org.jetbrains.annotations.NotNull()
        java.lang.String frequencyType, int intervalDays, @org.jetbrains.annotations.NotNull()
        java.lang.String daysOfWeek, boolean isCompletedToday, int streak) {
            return null;
        }
        
        @java.lang.Override()
        public boolean equals(@org.jetbrains.annotations.Nullable()
        java.lang.Object other) {
            return false;
        }
        
        @java.lang.Override()
        public int hashCode() {
            return 0;
        }
        
        @java.lang.Override()
        @org.jetbrains.annotations.NotNull()
        public java.lang.String toString() {
            return null;
        }
    }
}