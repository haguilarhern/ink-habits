# Backup — 2026-06-27

Snapshot of the app after a session of To-Do / gamification / focus-timer work.
Branch: `backup/2026-06-27-tasks-pomodoro-kanban`. Built, installed, and verified on the
Boox NoteAir3C with existing data preserved (DB migrated 7 → 10 via real migrations).

## Database
- Versions advanced **7 → 10** with non-destructive migrations (no data loss; dry-run
  verified against the live DB each step).
  - v8: `economy`, `streak_freezes`, `task_lists` tables; `todos` gains
    `listId`, `dueEpochDay`, `priority`, `recurType`, `recurInterval`, `recurDaysOfWeek`.
  - v9: `task_stages` table; `todos.stageId`.
  - v10: `task_stages.role` (fixed To Do / Done vs custom).

## Features added this session
1. **Aura economy + protective totems (streak freezes)**
   - Aura earned purely from history (+2 / completion, +8 / perfect day); balance = earned − spent.
   - Shop in the Rewards tab: buy Habit Totem (25) / Identity Totem (60).
   - Overnight reconcile auto-consumes a totem to freeze a streak broken by a miss
     (midnight alarm + app start). Frozen days count as completed everywhere; shown ❄ in Records.

2. **Task lists, due dates, Eisenhower priority, recurrence**
   - Classification lists (filter chips), due dates, importance, daily/every-N-days/weekday recurrence.
   - Recurring tasks auto-regenerate the next occurrence on completion.

3. **To-Do screen redesign**
   - `+` button → handwrite, then a single configure popup (classification / due / importance / repeat).
   - Visible **edit (pencil) icon** per row (replaced the unreliable long-press).
   - Importance labels use urgency/importance wording (no more P1–P4).

4. **Pomodoro mode (e-ink optimized)**
   - Timer icon in the To-Do header → dedicated PomodoroActivity.
   - Countdown animates in **whole minutes** with a once-per-minute clean refresh (avoids ghosting).
   - Configurable focus / short / long / rounds. Pick to-do tasks for the session, listed below the
     timer with checkboxes; completing there feeds the year counter + widgets + recurrence.

5. **Views dropdown + Kanban**
   - One **View ▾** selector: List / Kanban / Matrix / Completed.
   - Real 2×2 Eisenhower matrix.
   - Kanban board with custom stages; **To Do** and **Done** are fixed (un-movable / un-deletable),
     custom stages can be added, renamed, deleted, and reordered (header arrows).
   - Moving a card into **Done** marks it done → feeds the "done this year" counter (reopen reverses it).

6. **Focused editor** — the task editor only shows fields relevant to the current view
   (List: classification/due/repeat · Matrix: importance · Kanban: stage · Completed: all).

## Commits (newest first)
- 92fbf8c Focus task editor fields by view
- d43a5e1 Kanban: Done feeds the counter, fixed To Do/Done stages, reorderable customs
- 3afe071 Task views dropdown + Kanban board with custom stages
- a927892 Pomodoro: pick to-do tasks for the session, list below timer, check off
- 645327c Pomodoro timer (e-ink, minute-stepped) + edit icon + Eisenhower labels
- 87ebb21 Redesign To-Do: + button create flow, configure popup, 2x2 matrix
- c25fc8b Task lists, due dates, Eisenhower priority & recurrence
- aab46da Aura economy + protective totems (streak freezes)
- afb11de Records: check off past days; widgets auto-refresh at midnight (already on main)

## Restore points
- Branches: `backup/v8-tasks-base` (pre-task work), this branch (current).
- Tags: `backup-2026-06-27` (pre-feature main).
- `backups/` (gitignored): device DB snapshots + `rollback-main-app-debug.apk`; `backups/revert.sh`
  reinstalls the old app and restores the v7 DB.
