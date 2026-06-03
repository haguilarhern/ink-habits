package com.boox.atomic.habits.data.dao;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u00006\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0010\t\n\u0002\b\u0003\bg\u0018\u00002\u00020\u0001J\u0016\u0010\u0002\u001a\u00020\u00032\u0006\u0010\u0004\u001a\u00020\u0005H\u00a7@\u00a2\u0006\u0002\u0010\u0006J\u001c\u0010\u0007\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\n0\t0\b2\u0006\u0010\u000b\u001a\u00020\fH\'J\u001c\u0010\r\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u00050\t0\b2\u0006\u0010\u000e\u001a\u00020\u000fH\'J\u0016\u0010\u0010\u001a\u00020\u000f2\u0006\u0010\u0004\u001a\u00020\u0005H\u00a7@\u00a2\u0006\u0002\u0010\u0006J\u0016\u0010\u0011\u001a\u00020\u00032\u0006\u0010\u0004\u001a\u00020\u0005H\u00a7@\u00a2\u0006\u0002\u0010\u0006\u00a8\u0006\u0012"}, d2 = {"Lcom/boox/atomic/habits/data/dao/HabitDao;", "", "delete", "", "habit", "Lcom/boox/atomic/habits/data/entity/Habit;", "(Lcom/boox/atomic/habits/data/entity/Habit;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "getFullDashboard", "Lkotlinx/coroutines/flow/Flow;", "", "Lcom/boox/atomic/habits/data/dao/DashboardItem;", "today", "", "getHabitsForGoal", "goalId", "", "insert", "update", "app_debug"})
@androidx.room.Dao()
public abstract interface HabitDao {
    
    @androidx.room.Query(value = "SELECT * FROM habits WHERE identityGoalId = :goalId AND isActive = 1 ORDER BY sortOrder")
    @org.jetbrains.annotations.NotNull()
    public abstract kotlinx.coroutines.flow.Flow<java.util.List<com.boox.atomic.habits.data.entity.Habit>> getHabitsForGoal(long goalId);
    
    @androidx.room.Query(value = "\n        SELECT g.id, g.name, g.identityStatement, g.icon,\n               g.sortOrder as goalSortOrder,\n               h.id as habitId, h.name as habitName,\n               h.strokeData as habitStrokeData,\n               h.frequencyType, h.intervalDays, h.daysOfWeek,\n               h.sortOrder as habitSortOrder,\n               CASE WHEN hc.id IS NOT NULL THEN 1 ELSE 0 END as isCompletedToday\n        FROM identity_goals g\n        LEFT JOIN habits h ON h.identityGoalId = g.id AND h.isActive = 1\n        LEFT JOIN habit_completions hc ON hc.habitId = h.id AND hc.date = :today\n        ORDER BY g.sortOrder, h.sortOrder\n    ")
    @org.jetbrains.annotations.NotNull()
    public abstract kotlinx.coroutines.flow.Flow<java.util.List<com.boox.atomic.habits.data.dao.DashboardItem>> getFullDashboard(@org.jetbrains.annotations.NotNull()
    java.lang.String today);
    
    @androidx.room.Insert(onConflict = 1)
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object insert(@org.jetbrains.annotations.NotNull()
    com.boox.atomic.habits.data.entity.Habit habit, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.lang.Long> $completion);
    
    @androidx.room.Update()
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object update(@org.jetbrains.annotations.NotNull()
    com.boox.atomic.habits.data.entity.Habit habit, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
    
    @androidx.room.Delete()
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object delete(@org.jetbrains.annotations.NotNull()
    com.boox.atomic.habits.data.entity.Habit habit, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
}