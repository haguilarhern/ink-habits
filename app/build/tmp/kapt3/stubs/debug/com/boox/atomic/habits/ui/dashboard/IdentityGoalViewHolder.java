package com.boox.atomic.habits.ui.dashboard;

/**
 * ViewHolder for an identity goal header row.
 *
 * Displays the goal's icon, name, and an expand/collapse arrow.
 * Clicking the row toggles the visibility of its child habits.
 */
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u00006\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0010\b\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u000b\n\u0002\u0010\u000b\n\u0000\u0018\u00002\u00020\u0001B0\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012!\u0010\u0004\u001a\u001d\u0012\u0013\u0012\u00110\u0006\u00a2\u0006\f\b\u0007\u0012\b\b\b\u0012\u0004\b\b(\t\u0012\u0004\u0012\u00020\n0\u0005\u00a2\u0006\u0002\u0010\u000bJ\u000e\u0010\u0017\u001a\u00020\n2\u0006\u0010\u0018\u001a\u00020\u0019R\u0011\u0010\f\u001a\u00020\r\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000e\u0010\u000fR\u0011\u0010\u0010\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0011\u0010\u0012R\u0011\u0010\u0013\u001a\u00020\r\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0014\u0010\u000fR\u0011\u0010\u0015\u001a\u00020\r\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0016\u0010\u000fR)\u0010\u0004\u001a\u001d\u0012\u0013\u0012\u00110\u0006\u00a2\u0006\f\b\u0007\u0012\b\b\b\u0012\u0004\b\b(\t\u0012\u0004\u0012\u00020\n0\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u001a"}, d2 = {"Lcom/boox/atomic/habits/ui/dashboard/IdentityGoalViewHolder;", "Landroidx/recyclerview/widget/RecyclerView$ViewHolder;", "itemView", "Landroid/view/View;", "onToggle", "Lkotlin/Function1;", "", "Lkotlin/ParameterName;", "name", "adapterPosition", "", "(Landroid/view/View;Lkotlin/jvm/functions/Function1;)V", "expandArrow", "Landroid/widget/TextView;", "getExpandArrow", "()Landroid/widget/TextView;", "habitsContainer", "getHabitsContainer", "()Landroid/view/View;", "iconTextView", "getIconTextView", "nameTextView", "getNameTextView", "bind", "isExpanded", "", "app_debug"})
public final class IdentityGoalViewHolder extends androidx.recyclerview.widget.RecyclerView.ViewHolder {
    @org.jetbrains.annotations.NotNull()
    private final kotlin.jvm.functions.Function1<java.lang.Integer, kotlin.Unit> onToggle = null;
    @org.jetbrains.annotations.NotNull()
    private final android.widget.TextView iconTextView = null;
    @org.jetbrains.annotations.NotNull()
    private final android.widget.TextView nameTextView = null;
    @org.jetbrains.annotations.NotNull()
    private final android.widget.TextView expandArrow = null;
    @org.jetbrains.annotations.NotNull()
    private final android.view.View habitsContainer = null;
    
    public IdentityGoalViewHolder(@org.jetbrains.annotations.NotNull()
    android.view.View itemView, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super java.lang.Integer, kotlin.Unit> onToggle) {
        super(null);
    }
    
    @org.jetbrains.annotations.NotNull()
    public final android.widget.TextView getIconTextView() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final android.widget.TextView getNameTextView() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final android.widget.TextView getExpandArrow() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final android.view.View getHabitsContainer() {
        return null;
    }
    
    public final void bind(boolean isExpanded) {
    }
}