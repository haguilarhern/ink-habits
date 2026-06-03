package com.boox.atomic.habits.data.repository;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u00000\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0004\u0018\u00002\u00020\u0001B\u0015\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u00a2\u0006\u0002\u0010\u0006JN\u0010\u0007\u001a \u0012\u001c\u0012\u001a\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u000b0\n\u0012\n\u0012\b\u0012\u0004\u0012\u00020\f0\n0\t0\b2\u0012\u0010\r\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u000b0\n0\b2\u0012\u0010\u000e\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\f0\n0\bH\u0002J$\u0010\u000f\u001a \u0012\u001c\u0012\u001a\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u000b0\n\u0012\n\u0012\b\u0012\u0004\u0012\u00020\f0\n0\t0\bR\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0004\u001a\u00020\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0010"}, d2 = {"Lcom/boox/atomic/habits/data/repository/DailyRepository;", "", "habitRepository", "Lcom/boox/atomic/habits/data/repository/HabitRepository;", "todoRepository", "Lcom/boox/atomic/habits/data/repository/TodoRepository;", "(Lcom/boox/atomic/habits/data/repository/HabitRepository;Lcom/boox/atomic/habits/data/repository/TodoRepository;)V", "combineFlows", "Lkotlinx/coroutines/flow/Flow;", "Lkotlin/Pair;", "", "Lcom/boox/atomic/habits/data/dao/DashboardItem;", "Lcom/boox/atomic/habits/data/entity/ToDo;", "dashboardFlow", "todoFlow", "getFullDashboard", "app_debug"})
public final class DailyRepository {
    @org.jetbrains.annotations.NotNull()
    private final com.boox.atomic.habits.data.repository.HabitRepository habitRepository = null;
    @org.jetbrains.annotations.NotNull()
    private final com.boox.atomic.habits.data.repository.TodoRepository todoRepository = null;
    
    public DailyRepository(@org.jetbrains.annotations.NotNull()
    com.boox.atomic.habits.data.repository.HabitRepository habitRepository, @org.jetbrains.annotations.NotNull()
    com.boox.atomic.habits.data.repository.TodoRepository todoRepository) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.Flow<kotlin.Pair<java.util.List<com.boox.atomic.habits.data.dao.DashboardItem>, java.util.List<com.boox.atomic.habits.data.entity.ToDo>>> getFullDashboard() {
        return null;
    }
    
    private final kotlinx.coroutines.flow.Flow<kotlin.Pair<java.util.List<com.boox.atomic.habits.data.dao.DashboardItem>, java.util.List<com.boox.atomic.habits.data.entity.ToDo>>> combineFlows(kotlinx.coroutines.flow.Flow<? extends java.util.List<com.boox.atomic.habits.data.dao.DashboardItem>> dashboardFlow, kotlinx.coroutines.flow.Flow<? extends java.util.List<com.boox.atomic.habits.data.entity.ToDo>> todoFlow) {
        return null;
    }
}