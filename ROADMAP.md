# Ink Habits — Roadmap

Future ideas not yet implemented. Captured so we don't lose them.

## Reminder notifications (planned)
Let the app send daily reminder notifications with motivational quotes / calls to action.

Requirements:
- **Daily motivational notification** — a quote or call-to-action, once per day.
- **User-configurable time** — the user picks when the daily reminder fires (e.g. 8:00 AM).
- **Choose what to be reminded about** — let the user opt in/out per category:
  - Daily motivational quote / call to action.
  - Specific habits (per-habit reminder at the habit's own time — `Habit.reminderMinutes` already exists, just not wired to notifications).
  - "Never miss twice" nudges when a habit/identity streak is at risk (logic already exists in `Streaks.atRiskOfMissingTwice`; `NeverMissTwiceWorker` is scaffolded).
  - To-do items still open.
  - Reward about to unlock / just unlocked (reward-unlock notifications already exist via `NotificationHelper.showRewardUnlocked`).
- **Quiet hours / global mute** and an easy on/off master switch.

Implementation notes:
- Schedule with `WorkManager` (periodic) or `AlarmManager` (exact daily time). `ReminderScheduler.kt` and `NeverMissTwiceWorker.kt` are already present — extend them.
- Add a dedicated "Reminders" notification channel (separate from the reward-unlock channel).
- Add a Settings screen to hold the time picker + per-category toggles (new prefs).
- Pull the daily quote from `Quotes`/`QuotePrefs` (reuse the user's custom quote when set).

## Deadlines for to-dos & repeatable tasks (planned)
Let items carry a due date / deadline, and support tasks that repeat.

Requirements:
- **Deadline on a to-do** — optional due date (and maybe time); show it on the
  to-do list and widget, and surface overdue items (e.g. red marker / sort to top).
- **Repeatable tasks** — a to-do that regenerates on a schedule (daily / weekly /
  every N days), reusing the existing `Schedule`/`Frequency` model from habits.
- **Reminders for deadlines** — reuse the per-habit reminder plumbing
  (`HabitReminderScheduler`) to notify before/at a to-do's deadline.

Implementation notes:
- Add `dueEpochDay` (and optional `dueMinutes`) + repeat fields to the `ToDo` entity
  (Room migration); on completion of a repeatable task, spawn the next occurrence.
- Reuse `Schedule` for recurrence and `HabitReminderScheduler`/AlarmManager for the
  deadline notification.

## Performance / polish (candidates)
- Flatten `item_habit` into a single `ConstraintLayout` and remove the rotated rail label (reduces per-row measure/draw during home-list scroll).
- Consider single-Activity + Fragments navigation to make tab switching fully instant (currently separate Activities).

## Notes
- Goal-streak progress (per-habit + per-identity, with inheritance) — being implemented; see the goal model in the app.
