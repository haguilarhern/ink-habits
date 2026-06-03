package com.boox.atomic.habits.notifications;

/**
 * Schedules daily reminders for habit check-ins using AlarmManager.
 *
 * Handles setting, cancelling, and rescheduling alarms for the
 * Boox Ink Habits app. The reminder fires a broadcast that
 * the app's notification system handles to display a status bar
 * notification.
 */
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000(\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\b\n\u0002\b\u0003\b\u00c6\u0002\u0018\u00002\u00020\u0001:\u0001\rB\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002J\u000e\u0010\u0005\u001a\u00020\u00062\u0006\u0010\u0007\u001a\u00020\bJ\u001e\u0010\t\u001a\u00020\u00062\u0006\u0010\u0007\u001a\u00020\b2\u0006\u0010\n\u001a\u00020\u000b2\u0006\u0010\f\u001a\u00020\u000bR\u000e\u0010\u0003\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u000e"}, d2 = {"Lcom/boox/atomic/habits/notifications/ReminderScheduler;", "", "()V", "ACTION_REMINDER", "", "cancelReminder", "", "context", "Landroid/content/Context;", "scheduleDailyReminder", "hour", "", "minute", "ReminderReceiver", "app_debug"})
public final class ReminderScheduler {
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String ACTION_REMINDER = "com.boox.atomic.habits.REMINDER";
    @org.jetbrains.annotations.NotNull()
    public static final com.boox.atomic.habits.notifications.ReminderScheduler INSTANCE = null;
    
    private ReminderScheduler() {
        super();
    }
    
    /**
     * Schedules a repeating daily reminder at the given hour and minute.
     *
     * @param context application context
     * @param hour    hour of day (0-23)
     * @param minute  minute of hour (0-59)
     */
    public final void scheduleDailyReminder(@org.jetbrains.annotations.NotNull()
    android.content.Context context, int hour, int minute) {
    }
    
    /**
     * Cancels any previously scheduled daily reminder.
     *
     * @param context application context
     */
    public final void cancelReminder(@org.jetbrains.annotations.NotNull()
    android.content.Context context) {
    }
    
    /**
     * Broadcast receiver for the daily reminder alarm.
     *
     * Registers with the manifest to handle the REMINDER action.
     */
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000 \n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\u0018\u00002\u00020\u0001B\u0005\u00a2\u0006\u0002\u0010\u0002J\u0018\u0010\u0003\u001a\u00020\u00042\u0006\u0010\u0005\u001a\u00020\u00062\u0006\u0010\u0007\u001a\u00020\bH\u0016J\u0010\u0010\t\u001a\u00020\u00042\u0006\u0010\u0005\u001a\u00020\u0006H\u0002\u00a8\u0006\n"}, d2 = {"Lcom/boox/atomic/habits/notifications/ReminderScheduler$ReminderReceiver;", "Landroid/content/BroadcastReceiver;", "()V", "onReceive", "", "context", "Landroid/content/Context;", "intent", "Landroid/content/Intent;", "showReminderNotification", "app_debug"})
    public static final class ReminderReceiver extends android.content.BroadcastReceiver {
        
        public ReminderReceiver() {
            super();
        }
        
        @java.lang.Override()
        public void onReceive(@org.jetbrains.annotations.NotNull()
        android.content.Context context, @org.jetbrains.annotations.NotNull()
        android.content.Intent intent) {
        }
        
        private final void showReminderNotification(android.content.Context context) {
        }
    }
}