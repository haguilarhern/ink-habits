package com.boox.atomic.habits.ui.widget;

/**
 * A custom compound view that renders a single handwritten todo item.
 *
 * Layout (horizontal LinearLayout):
 * - [StylusCheckableView] checkbox on the left (40dp)
 * - [HandwritingFieldView] (read-only, confirmed) in the center showing handwritten strokes
 *
 * When checked, a strikethrough line is drawn over the handwriting area via
 * [StrokeRenderer.drawStrikethrough].
 */
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000T\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\b\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u000b\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\b\u0003\u0018\u00002\u00020\u0001B%\b\u0007\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\n\b\u0002\u0010\u0004\u001a\u0004\u0018\u00010\u0005\u0012\b\b\u0002\u0010\u0006\u001a\u00020\u0007\u00a2\u0006\u0002\u0010\bJ\u0010\u0010\u0011\u001a\u00020\u00122\u0006\u0010\u0013\u001a\u00020\u0014H\u0014J\u0010\u0010\u0015\u001a\u00020\u00072\u0006\u0010\u0016\u001a\u00020\u0007H\u0002J\u0006\u0010\u0017\u001a\u00020\u0018J\u000e\u0010\u0019\u001a\u00020\u00122\u0006\u0010\u001a\u001a\u00020\u0018J\u001a\u0010\u001b\u001a\u00020\u00122\u0012\u0010\u001c\u001a\u000e\u0012\u0004\u0012\u00020\u0018\u0012\u0004\u0012\u00020\u00120\u001dJ\u000e\u0010\u001e\u001a\u00020\u00122\u0006\u0010\u001f\u001a\u00020\u0010R\u000e\u0010\t\u001a\u00020\nX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u000b\u001a\u00020\fX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\r\u001a\u00020\u000eX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u000f\u001a\u00020\u0010X\u0082\u000e\u00a2\u0006\u0002\n\u0000\u00a8\u0006 "}, d2 = {"Lcom/boox/atomic/habits/ui/widget/HandwritingTodoWidget;", "Landroid/widget/LinearLayout;", "context", "Landroid/content/Context;", "attrs", "Landroid/util/AttributeSet;", "defStyleAttr", "", "(Landroid/content/Context;Landroid/util/AttributeSet;I)V", "checkbox", "Lcom/boox/atomic/habits/ui/widget/StylusCheckableView;", "handwritingField", "Lcom/boox/atomic/habits/ui/widget/HandwritingFieldView;", "strikethroughPaint", "Landroid/graphics/Paint;", "strokeData", "", "dispatchDraw", "", "canvas", "Landroid/graphics/Canvas;", "dpToPx", "dp", "isChecked", "", "setChecked", "checked", "setOnCheckedChangeListener", "listener", "Lkotlin/Function1;", "setStrokeData", "data", "app_debug"})
public final class HandwritingTodoWidget extends android.widget.LinearLayout {
    @org.jetbrains.annotations.NotNull()
    private final com.boox.atomic.habits.ui.widget.StylusCheckableView checkbox = null;
    @org.jetbrains.annotations.NotNull()
    private final com.boox.atomic.habits.ui.widget.HandwritingFieldView handwritingField = null;
    
    /**
     * Serialised stroke data, cached so we can pass it to the strikethrough renderer.
     */
    @org.jetbrains.annotations.NotNull()
    private java.lang.String strokeData = "";
    
    /**
     * Paint used for the strikethrough line drawn in [dispatchDraw].
     */
    @org.jetbrains.annotations.NotNull()
    private final android.graphics.Paint strikethroughPaint = null;
    
    @kotlin.jvm.JvmOverloads()
    public HandwritingTodoWidget(@org.jetbrains.annotations.NotNull()
    android.content.Context context, @org.jetbrains.annotations.Nullable()
    android.util.AttributeSet attrs, int defStyleAttr) {
        super(null);
    }
    
    /**
     * Load handwritten stroke data into the handwriting field.
     */
    public final void setStrokeData(@org.jetbrains.annotations.NotNull()
    java.lang.String data) {
    }
    
    /**
     * Set the checked (completed) state.
     * When checked, the strikethrough overlay is drawn over the handwriting.
     */
    public final void setChecked(boolean checked) {
    }
    
    /**
     * Return whether this todo is checked.
     */
    public final boolean isChecked() {
        return false;
    }
    
    /**
     * Register a callback invoked when the checkbox state changes.
     */
    public final void setOnCheckedChangeListener(@org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super java.lang.Boolean, kotlin.Unit> listener) {
    }
    
    @java.lang.Override()
    protected void dispatchDraw(@org.jetbrains.annotations.NotNull()
    android.graphics.Canvas canvas) {
    }
    
    private final int dpToPx(int dp) {
        return 0;
    }
    
    @kotlin.jvm.JvmOverloads()
    public HandwritingTodoWidget(@org.jetbrains.annotations.NotNull()
    android.content.Context context) {
        super(null);
    }
    
    @kotlin.jvm.JvmOverloads()
    public HandwritingTodoWidget(@org.jetbrains.annotations.NotNull()
    android.content.Context context, @org.jetbrains.annotations.Nullable()
    android.util.AttributeSet attrs) {
        super(null);
    }
}