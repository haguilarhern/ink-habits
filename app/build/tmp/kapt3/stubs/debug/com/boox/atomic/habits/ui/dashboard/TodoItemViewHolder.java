package com.boox.atomic.habits.ui.dashboard;

/**
 * ViewHolder for a single to-do item row.
 *
 * Uses [HandwritingTodoWidget] to display handwritten to-do strokes
 * with a checkbox and strikethrough on completion.
 */
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000<\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0010\t\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u000b\n\u0000\n\u0002\u0010\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0000\u0018\u00002\u00020\u0001BE\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00126\u0010\u0004\u001a2\u0012\u0013\u0012\u00110\u0006\u00a2\u0006\f\b\u0007\u0012\b\b\b\u0012\u0004\b\b(\t\u0012\u0013\u0012\u00110\n\u00a2\u0006\f\b\u0007\u0012\b\b\b\u0012\u0004\b\b(\u000b\u0012\u0004\u0012\u00020\f0\u0005\u00a2\u0006\u0002\u0010\rJ\u001e\u0010\u0011\u001a\u00020\f2\u0006\u0010\t\u001a\u00020\u00062\u0006\u0010\u0012\u001a\u00020\u00132\u0006\u0010\u000b\u001a\u00020\nR\u000e\u0010\u000e\u001a\u00020\u0006X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u000f\u001a\u00020\u0010X\u0082\u0004\u00a2\u0006\u0002\n\u0000R>\u0010\u0004\u001a2\u0012\u0013\u0012\u00110\u0006\u00a2\u0006\f\b\u0007\u0012\b\b\b\u0012\u0004\b\b(\t\u0012\u0013\u0012\u00110\n\u00a2\u0006\f\b\u0007\u0012\b\b\b\u0012\u0004\b\b(\u000b\u0012\u0004\u0012\u00020\f0\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0014"}, d2 = {"Lcom/boox/atomic/habits/ui/dashboard/TodoItemViewHolder;", "Landroidx/recyclerview/widget/RecyclerView$ViewHolder;", "itemView", "Landroid/view/View;", "onToggle", "Lkotlin/Function2;", "", "Lkotlin/ParameterName;", "name", "todoId", "", "isCompleted", "", "(Landroid/view/View;Lkotlin/jvm/functions/Function2;)V", "currentTodoId", "handwritingTodoWidget", "Lcom/boox/atomic/habits/ui/widget/HandwritingTodoWidget;", "bind", "strokeData", "", "app_debug"})
public final class TodoItemViewHolder extends androidx.recyclerview.widget.RecyclerView.ViewHolder {
    @org.jetbrains.annotations.NotNull()
    private final kotlin.jvm.functions.Function2<java.lang.Long, java.lang.Boolean, kotlin.Unit> onToggle = null;
    @org.jetbrains.annotations.NotNull()
    private final com.boox.atomic.habits.ui.widget.HandwritingTodoWidget handwritingTodoWidget = null;
    private long currentTodoId = 0L;
    
    public TodoItemViewHolder(@org.jetbrains.annotations.NotNull()
    android.view.View itemView, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function2<? super java.lang.Long, ? super java.lang.Boolean, kotlin.Unit> onToggle) {
        super(null);
    }
    
    public final void bind(long todoId, @org.jetbrains.annotations.NotNull()
    java.lang.String strokeData, boolean isCompleted) {
    }
}