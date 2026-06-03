package com.boox.atomic.habits.ui.setup;

/**
 * Guided 5-step onboarding wizard using handwriting input for everything.
 *
 * Step 0: "Who do you want to become?" — Write your identity goal name
 * Step 1: "What habits would [GOAL] do?" — Handwrite habits
 * Step 2: "Another identity?" — Loop or continue
 * Step 3: "Any to-do's?" — Handwrite todos
 * Step 4: Done → Dashboard
 */
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000r\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010 \n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010!\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\t\n\u0002\b\u0003\n\u0002\u0010\b\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\b\u0010\u0018\u00002\u00020\u0001:\u000289B\u0005\u00a2\u0006\u0002\u0010\u0002J\u0010\u0010$\u001a\u00020%2\u0006\u0010&\u001a\u00020\tH\u0002J\b\u0010\'\u001a\u00020%H\u0002J\u0012\u0010(\u001a\u00020%2\b\u0010)\u001a\u0004\u0018\u00010*H\u0014J\b\u0010+\u001a\u00020%H\u0002J\b\u0010,\u001a\u00020%H\u0002J\b\u0010-\u001a\u00020%H\u0002J\b\u0010.\u001a\u00020%H\u0002J\b\u0010/\u001a\u00020%H\u0002J\b\u00100\u001a\u00020%H\u0002J\b\u00101\u001a\u00020%H\u0002J\b\u00102\u001a\u00020%H\u0002J \u00103\u001a\u00020%2\u0006\u00104\u001a\u00020\u00052\u0006\u00105\u001a\u00020\u00052\u0006\u00106\u001a\u00020\u0005H\u0002J\b\u00107\u001a\u00020%H\u0002R\u0014\u0010\u0003\u001a\b\u0012\u0004\u0012\u00020\u00050\u0004X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0007X\u0082.\u00a2\u0006\u0002\n\u0000R\u000e\u0010\b\u001a\u00020\tX\u0082.\u00a2\u0006\u0002\n\u0000R\u0014\u0010\n\u001a\b\u0012\u0004\u0012\u00020\f0\u000bX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0012\u0010\r\u001a\u0004\u0018\u00010\u000eX\u0082\u000e\u00a2\u0006\u0004\n\u0002\u0010\u000fR\u000e\u0010\u0010\u001a\u00020\u0005X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0011\u001a\u00020\u0012X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0013\u001a\u00020\u0014X\u0082.\u00a2\u0006\u0002\n\u0000R\u0010\u0010\u0015\u001a\u0004\u0018\u00010\u0016X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0010\u0010\u0017\u001a\u0004\u0018\u00010\u0018X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0014\u0010\u0019\u001a\b\u0012\u0004\u0012\u00020\u001a0\u000bX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u001b\u001a\u00020\u0007X\u0082.\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u001c\u001a\u00020\u0005X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u001d\u001a\u00020\u0007X\u0082.\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u001e\u001a\u00020\u001fX\u0082.\u00a2\u0006\u0002\n\u0000R\u000e\u0010 \u001a\u00020\u001fX\u0082.\u00a2\u0006\u0002\n\u0000R\u000e\u0010!\u001a\u00020\u001fX\u0082.\u00a2\u0006\u0002\n\u0000R\u0014\u0010\"\u001a\b\u0012\u0004\u0012\u00020#0\u000bX\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006:"}, d2 = {"Lcom/boox/atomic/habits/ui/setup/SetupActivity;", "Lcom/boox/atomic/habits/ui/EInkActivity;", "()V", "ICONS", "", "", "backButton", "Landroid/widget/Button;", "contentArea", "Landroid/widget/LinearLayout;", "createdGoals", "", "Lcom/boox/atomic/habits/data/entity/IdentityGoal;", "currentGoalId", "", "Ljava/lang/Long;", "currentGoalName", "currentStep", "", "db", "Lcom/boox/atomic/habits/data/AppDatabase;", "goalNameField", "Lcom/boox/atomic/habits/ui/widget/HandwritingFieldView;", "goalStatementInput", "Landroid/widget/EditText;", "habitFieldStates", "Lcom/boox/atomic/habits/ui/setup/SetupActivity$HabitFieldState;", "nextButton", "selectedIcon", "skipButton", "stepDescription", "Landroid/widget/TextView;", "stepIndicator", "stepTitle", "todoFieldStates", "Lcom/boox/atomic/habits/ui/setup/SetupActivity$TodoFieldState;", "addHabitRow", "", "container", "onBack", "onCreate", "savedInstanceState", "Landroid/os/Bundle;", "onNext", "onSkip", "renderStep0", "renderStep1", "renderStep2", "renderStep3", "renderStep4", "renderWizardStep", "setHeader", "step", "title", "desc", "showManageView", "HabitFieldState", "TodoFieldState", "app_debug"})
public final class SetupActivity extends com.boox.atomic.habits.ui.EInkActivity {
    private com.boox.atomic.habits.data.AppDatabase db;
    private android.widget.TextView stepIndicator;
    private android.widget.TextView stepTitle;
    private android.widget.TextView stepDescription;
    private android.widget.LinearLayout contentArea;
    private android.widget.Button nextButton;
    private android.widget.Button backButton;
    private android.widget.Button skipButton;
    private int currentStep = 0;
    @org.jetbrains.annotations.Nullable()
    private java.lang.Long currentGoalId;
    @org.jetbrains.annotations.NotNull()
    private java.lang.String currentGoalName = "";
    @org.jetbrains.annotations.NotNull()
    private final java.util.List<com.boox.atomic.habits.data.entity.IdentityGoal> createdGoals = null;
    @org.jetbrains.annotations.Nullable()
    private com.boox.atomic.habits.ui.widget.HandwritingFieldView goalNameField;
    @org.jetbrains.annotations.Nullable()
    private android.widget.EditText goalStatementInput;
    @org.jetbrains.annotations.NotNull()
    private java.lang.String selectedIcon = "\ud83d\udcd6";
    @org.jetbrains.annotations.NotNull()
    private final java.util.List<com.boox.atomic.habits.ui.setup.SetupActivity.HabitFieldState> habitFieldStates = null;
    @org.jetbrains.annotations.NotNull()
    private final java.util.List<com.boox.atomic.habits.ui.setup.SetupActivity.TodoFieldState> todoFieldStates = null;
    @org.jetbrains.annotations.NotNull()
    private final java.util.List<java.lang.String> ICONS = null;
    
    public SetupActivity() {
        super();
    }
    
    @java.lang.Override()
    protected void onCreate(@org.jetbrains.annotations.Nullable()
    android.os.Bundle savedInstanceState) {
    }
    
    private final void renderWizardStep() {
    }
    
    private final void setHeader(java.lang.String step, java.lang.String title, java.lang.String desc) {
    }
    
    private final void renderStep0() {
    }
    
    private final void renderStep1() {
    }
    
    private final void addHabitRow(android.widget.LinearLayout container) {
    }
    
    private final void renderStep2() {
    }
    
    private final void renderStep3() {
    }
    
    private final void renderStep4() {
    }
    
    private final void onNext() {
    }
    
    private final void onBack() {
    }
    
    private final void onSkip() {
    }
    
    private final void showManageView() {
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u00002\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\f\n\u0002\u0010\u000b\n\u0002\b\u0002\n\u0002\u0010\b\n\u0000\n\u0002\u0010\u000e\n\u0000\b\u0086\b\u0018\u00002\u00020\u0001B\u001d\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u0012\u0006\u0010\u0006\u001a\u00020\u0007\u00a2\u0006\u0002\u0010\bJ\t\u0010\u000f\u001a\u00020\u0003H\u00c6\u0003J\t\u0010\u0010\u001a\u00020\u0005H\u00c6\u0003J\t\u0010\u0011\u001a\u00020\u0007H\u00c6\u0003J\'\u0010\u0012\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u00032\b\b\u0002\u0010\u0004\u001a\u00020\u00052\b\b\u0002\u0010\u0006\u001a\u00020\u0007H\u00c6\u0001J\u0013\u0010\u0013\u001a\u00020\u00142\b\u0010\u0015\u001a\u0004\u0018\u00010\u0001H\u00d6\u0003J\t\u0010\u0016\u001a\u00020\u0017H\u00d6\u0001J\t\u0010\u0018\u001a\u00020\u0019H\u00d6\u0001R\u0011\u0010\u0004\u001a\u00020\u0005\u00a2\u0006\b\n\u0000\u001a\u0004\b\t\u0010\nR\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000b\u0010\fR\u0011\u0010\u0006\u001a\u00020\u0007\u00a2\u0006\b\n\u0000\u001a\u0004\b\r\u0010\u000e\u00a8\u0006\u001a"}, d2 = {"Lcom/boox/atomic/habits/ui/setup/SetupActivity$HabitFieldState;", "", "nameField", "Lcom/boox/atomic/habits/ui/widget/HandwritingFieldView;", "freqGroup", "Landroid/widget/RadioGroup;", "root", "Landroid/view/View;", "(Lcom/boox/atomic/habits/ui/widget/HandwritingFieldView;Landroid/widget/RadioGroup;Landroid/view/View;)V", "getFreqGroup", "()Landroid/widget/RadioGroup;", "getNameField", "()Lcom/boox/atomic/habits/ui/widget/HandwritingFieldView;", "getRoot", "()Landroid/view/View;", "component1", "component2", "component3", "copy", "equals", "", "other", "hashCode", "", "toString", "", "app_debug"})
    public static final class HabitFieldState {
        @org.jetbrains.annotations.NotNull()
        private final com.boox.atomic.habits.ui.widget.HandwritingFieldView nameField = null;
        @org.jetbrains.annotations.NotNull()
        private final android.widget.RadioGroup freqGroup = null;
        @org.jetbrains.annotations.NotNull()
        private final android.view.View root = null;
        
        public HabitFieldState(@org.jetbrains.annotations.NotNull()
        com.boox.atomic.habits.ui.widget.HandwritingFieldView nameField, @org.jetbrains.annotations.NotNull()
        android.widget.RadioGroup freqGroup, @org.jetbrains.annotations.NotNull()
        android.view.View root) {
            super();
        }
        
        @org.jetbrains.annotations.NotNull()
        public final com.boox.atomic.habits.ui.widget.HandwritingFieldView getNameField() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final android.widget.RadioGroup getFreqGroup() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final android.view.View getRoot() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final com.boox.atomic.habits.ui.widget.HandwritingFieldView component1() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final android.widget.RadioGroup component2() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final android.view.View component3() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final com.boox.atomic.habits.ui.setup.SetupActivity.HabitFieldState copy(@org.jetbrains.annotations.NotNull()
        com.boox.atomic.habits.ui.widget.HandwritingFieldView nameField, @org.jetbrains.annotations.NotNull()
        android.widget.RadioGroup freqGroup, @org.jetbrains.annotations.NotNull()
        android.view.View root) {
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
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000,\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\t\n\u0002\u0010\u000b\n\u0002\b\u0002\n\u0002\u0010\b\n\u0000\n\u0002\u0010\u000e\n\u0000\b\u0086\b\u0018\u00002\u00020\u0001B\u0015\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u00a2\u0006\u0002\u0010\u0006J\t\u0010\u000b\u001a\u00020\u0003H\u00c6\u0003J\t\u0010\f\u001a\u00020\u0005H\u00c6\u0003J\u001d\u0010\r\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u00032\b\b\u0002\u0010\u0004\u001a\u00020\u0005H\u00c6\u0001J\u0013\u0010\u000e\u001a\u00020\u000f2\b\u0010\u0010\u001a\u0004\u0018\u00010\u0001H\u00d6\u0003J\t\u0010\u0011\u001a\u00020\u0012H\u00d6\u0001J\t\u0010\u0013\u001a\u00020\u0014H\u00d6\u0001R\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0007\u0010\bR\u0011\u0010\u0004\u001a\u00020\u0005\u00a2\u0006\b\n\u0000\u001a\u0004\b\t\u0010\n\u00a8\u0006\u0015"}, d2 = {"Lcom/boox/atomic/habits/ui/setup/SetupActivity$TodoFieldState;", "", "field", "Lcom/boox/atomic/habits/ui/widget/HandwritingFieldView;", "root", "Landroid/view/View;", "(Lcom/boox/atomic/habits/ui/widget/HandwritingFieldView;Landroid/view/View;)V", "getField", "()Lcom/boox/atomic/habits/ui/widget/HandwritingFieldView;", "getRoot", "()Landroid/view/View;", "component1", "component2", "copy", "equals", "", "other", "hashCode", "", "toString", "", "app_debug"})
    public static final class TodoFieldState {
        @org.jetbrains.annotations.NotNull()
        private final com.boox.atomic.habits.ui.widget.HandwritingFieldView field = null;
        @org.jetbrains.annotations.NotNull()
        private final android.view.View root = null;
        
        public TodoFieldState(@org.jetbrains.annotations.NotNull()
        com.boox.atomic.habits.ui.widget.HandwritingFieldView field, @org.jetbrains.annotations.NotNull()
        android.view.View root) {
            super();
        }
        
        @org.jetbrains.annotations.NotNull()
        public final com.boox.atomic.habits.ui.widget.HandwritingFieldView getField() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final android.view.View getRoot() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final com.boox.atomic.habits.ui.widget.HandwritingFieldView component1() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final android.view.View component2() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final com.boox.atomic.habits.ui.setup.SetupActivity.TodoFieldState copy(@org.jetbrains.annotations.NotNull()
        com.boox.atomic.habits.ui.widget.HandwritingFieldView field, @org.jetbrains.annotations.NotNull()
        android.view.View root) {
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