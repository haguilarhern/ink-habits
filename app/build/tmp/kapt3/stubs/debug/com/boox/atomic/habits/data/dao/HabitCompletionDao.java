package com.boox.atomic.habits.data.dao;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u00002\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010 \n\u0002\u0010\u000e\n\u0000\n\u0002\u0010\t\n\u0002\b\n\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\b\n\u0002\b\u0002\n\u0002\u0010\u0002\n\u0000\bg\u0018\u00002\u00020\u0001J\u001c\u0010\u0002\u001a\b\u0012\u0004\u0012\u00020\u00040\u00032\u0006\u0010\u0005\u001a\u00020\u0006H\u00a7@\u00a2\u0006\u0002\u0010\u0007J,\u0010\b\u001a\b\u0012\u0004\u0012\u00020\u00040\u00032\u0006\u0010\u0005\u001a\u00020\u00062\u0006\u0010\t\u001a\u00020\u00042\u0006\u0010\n\u001a\u00020\u0004H\u00a7@\u00a2\u0006\u0002\u0010\u000bJ$\u0010\f\u001a\b\u0012\u0004\u0012\u00020\u00040\u00032\u0006\u0010\u0005\u001a\u00020\u00062\u0006\u0010\r\u001a\u00020\u0004H\u00a7@\u00a2\u0006\u0002\u0010\u000eJ\u0016\u0010\u000f\u001a\u00020\u00062\u0006\u0010\u0010\u001a\u00020\u0011H\u00a7@\u00a2\u0006\u0002\u0010\u0012J\u001e\u0010\u0013\u001a\u00020\u00142\u0006\u0010\u0005\u001a\u00020\u00062\u0006\u0010\u0015\u001a\u00020\u0004H\u00a7@\u00a2\u0006\u0002\u0010\u000eJ\u001e\u0010\u0016\u001a\u00020\u00172\u0006\u0010\u0005\u001a\u00020\u00062\u0006\u0010\u0015\u001a\u00020\u0004H\u00a7@\u00a2\u0006\u0002\u0010\u000e\u00a8\u0006\u0018"}, d2 = {"Lcom/boox/atomic/habits/data/dao/HabitCompletionDao;", "", "getCompletionDates", "", "", "habitId", "", "(JLkotlin/coroutines/Continuation;)Ljava/lang/Object;", "getCompletionsInRange", "startDate", "endDate", "(JLjava/lang/String;Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "getStreakWindow", "sinceDate", "(JLjava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "insert", "completion", "Lcom/boox/atomic/habits/data/entity/HabitCompletion;", "(Lcom/boox/atomic/habits/data/entity/HabitCompletion;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "isCompleted", "", "date", "removeCompletion", "", "app_debug"})
@androidx.room.Dao()
public abstract interface HabitCompletionDao {
    
    @androidx.room.Query(value = "SELECT date FROM habit_completions WHERE habitId = :habitId ORDER BY date DESC")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object getCompletionDates(long habitId, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.util.List<java.lang.String>> $completion);
    
    @androidx.room.Query(value = "SELECT COUNT(*) FROM habit_completions WHERE habitId = :habitId AND date = :date")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object isCompleted(long habitId, @org.jetbrains.annotations.NotNull()
    java.lang.String date, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.lang.Integer> $completion);
    
    @androidx.room.Insert(onConflict = 1)
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object insert(@org.jetbrains.annotations.NotNull()
    com.boox.atomic.habits.data.entity.HabitCompletion completion, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.lang.Long> $completion);
    
    @androidx.room.Query(value = "DELETE FROM habit_completions WHERE habitId = :habitId AND date = :date")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object removeCompletion(long habitId, @org.jetbrains.annotations.NotNull()
    java.lang.String date, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
    
    @androidx.room.Query(value = "SELECT date FROM habit_completions WHERE habitId = :habitId AND date >= :sinceDate ORDER BY date DESC")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object getStreakWindow(long habitId, @org.jetbrains.annotations.NotNull()
    java.lang.String sinceDate, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.util.List<java.lang.String>> $completion);
    
    @androidx.room.Query(value = "SELECT date FROM habit_completions WHERE habitId = :habitId AND date >= :startDate AND date <= :endDate ORDER BY date ASC")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object getCompletionsInRange(long habitId, @org.jetbrains.annotations.NotNull()
    java.lang.String startDate, @org.jetbrains.annotations.NotNull()
    java.lang.String endDate, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.util.List<java.lang.String>> $completion);
}