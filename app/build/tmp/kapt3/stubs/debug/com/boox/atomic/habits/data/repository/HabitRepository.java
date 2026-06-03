package com.boox.atomic.habits.data.repository;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000X\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\t\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\b\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\u0004\n\u0002\u0010\u000b\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0002\n\u0002\b\u0002\u0018\u00002\u00020\u0001B\u0015\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u00a2\u0006\u0002\u0010\u0006J\u0016\u0010\u0007\u001a\u00020\b2\u0006\u0010\t\u001a\u00020\nH\u0086@\u00a2\u0006\u0002\u0010\u000bJ\u0012\u0010\f\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u000f0\u000e0\rJ2\u0010\u0010\u001a\u00020\u00112\u0006\u0010\u0012\u001a\u00020\b2\u0006\u0010\u0013\u001a\u00020\u00142\b\b\u0002\u0010\u0015\u001a\u00020\u00112\b\b\u0002\u0010\u0016\u001a\u00020\u0014H\u0086@\u00a2\u0006\u0002\u0010\u0017J(\u0010\u0018\u001a\u00020\u00192\u0006\u0010\u001a\u001a\u00020\u001b2\u0006\u0010\u0013\u001a\u00020\u00142\u0006\u0010\u0015\u001a\u00020\u00112\u0006\u0010\u0016\u001a\u00020\u0014H\u0002J\u0016\u0010\u001c\u001a\u00020\u001d2\u0006\u0010\u0012\u001a\u00020\bH\u0086@\u00a2\u0006\u0002\u0010\u001eR\u000e\u0010\u0004\u001a\u00020\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u001f"}, d2 = {"Lcom/boox/atomic/habits/data/repository/HabitRepository;", "", "habitDao", "Lcom/boox/atomic/habits/data/dao/HabitDao;", "habitCompletionDao", "Lcom/boox/atomic/habits/data/dao/HabitCompletionDao;", "(Lcom/boox/atomic/habits/data/dao/HabitDao;Lcom/boox/atomic/habits/data/dao/HabitCompletionDao;)V", "createHabit", "", "habit", "Lcom/boox/atomic/habits/data/entity/Habit;", "(Lcom/boox/atomic/habits/data/entity/Habit;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "getFullDashboard", "Lkotlinx/coroutines/flow/Flow;", "", "Lcom/boox/atomic/habits/data/dao/DashboardItem;", "getStreak", "", "habitId", "frequencyType", "", "intervalDays", "daysOfWeek", "(JLjava/lang/String;ILjava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "isScheduledDay", "", "date", "Ljava/time/LocalDate;", "toggleCompletion", "", "(JLkotlin/coroutines/Continuation;)Ljava/lang/Object;", "app_debug"})
public final class HabitRepository {
    @org.jetbrains.annotations.NotNull()
    private final com.boox.atomic.habits.data.dao.HabitDao habitDao = null;
    @org.jetbrains.annotations.NotNull()
    private final com.boox.atomic.habits.data.dao.HabitCompletionDao habitCompletionDao = null;
    
    public HabitRepository(@org.jetbrains.annotations.NotNull()
    com.boox.atomic.habits.data.dao.HabitDao habitDao, @org.jetbrains.annotations.NotNull()
    com.boox.atomic.habits.data.dao.HabitCompletionDao habitCompletionDao) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.Flow<java.util.List<com.boox.atomic.habits.data.dao.DashboardItem>> getFullDashboard() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object createHabit(@org.jetbrains.annotations.NotNull()
    com.boox.atomic.habits.data.entity.Habit habit, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.lang.Long> $completion) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object toggleCompletion(long habitId, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object getStreak(long habitId, @org.jetbrains.annotations.NotNull()
    java.lang.String frequencyType, int intervalDays, @org.jetbrains.annotations.NotNull()
    java.lang.String daysOfWeek, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.lang.Integer> $completion) {
        return null;
    }
    
    private final boolean isScheduledDay(java.time.LocalDate date, java.lang.String frequencyType, int intervalDays, java.lang.String daysOfWeek) {
        return false;
    }
}