# Ink Habits — Project Status

An identity-based habit tracker for the **Onyx Boox Note Air 3C** (Kaleido 3 color
e-ink, Android 12). You build habits by choosing an identity ("I am a Reader")
and adding the habits that person does. Habit names can be **handwritten** with
the stylus (zero-lag Onyx raw drawing) or typed.

Package: `com.inkhabits` · Project dir: `ink-habits/`

---

## How to build & install

```bash
cd ink-habits
./gradlew assembleDebug
# IMPORTANT: this Boox needs a PUSH install — streamed installs register the
# package but NOT its activities ("Activity does not exist").
adb -s <serial> install -r --no-streaming app/build/outputs/apk/debug/app-debug.apk
```

## Tech stack

- Kotlin, Android Views + Material 3 (flat; no elevation/animation — better on e-ink).
- `minSdk 28`, `compileSdk/targetSdk 34`, AGP 8.2.0, Gradle 8.14.3, Kotlin 1.9.22.
- Room (Flow + coroutines) for persistence.
- WorkManager for the daily "never miss twice" check.
- **Onyx SDK pairing (critical):** `onyxsdk-device:1.2.30` + `onyxsdk-pen:1.4.11`.
  These are the boox-rapid-draw versions with matching method signatures. Newer
  device (1.3.x) changed `BaseDevice.setEraserRawDrawingEnabled` to a 2-arg form
  and crashes pen 1.4.11 with `NoSuchMethodError`.
- **HiddenApiBypass** (`org.lsposed.hiddenapibypass:4.3`) — exempts the hidden
  `android.onyx.*` firmware APIs the pen SDK reflects into; without it raw drawing
  hangs/denies.

## Architecture

- `data/` — Room entities (`IdentityGoal`, `Habit`, `HabitCompletion`, `ToDo`),
  DAOs, `AppDatabase`.
- `util/` — `Schedule` (due-date logic), `Streaks` (current/best/perfect-day,
  never-miss-twice risk), `Quotes`, `Ink` (stroke serialize + crop-to-content render).
- `eink/` — `EInkUtils` (EPD update modes), `EInkActivity` (base).
- `ui/` — `dashboard`, `onboarding`, `history`, `todo`, `writing`, `widget`.
- `widget/` — home-screen habits + to-do widgets (RemoteViews + RemoteViewsService).
- `notify/` — `NeverMissTwiceWorker`, `NotificationHelper`, `ReminderScheduler`.

## Habit scheduling

Per-habit frequency, evaluated against scheduled occurrences (not calendar days):
- Daily · Specific days of week · Every N days · N× per week.
- Streaks count consecutive completed scheduled occurrences.
- "Never miss twice" fires when the previous scheduled occurrence was missed and
  the next is due.

## Features implemented

- Onboarding: identity (icon + name) → add its habits (one at a time) → loop → dashboard.
- Dashboard (planner style, liked by user): STREAK header (perfect-day streak),
  quote card, identity sections with timeline rail, rounded habit pills, two-way
  check-off (tap box OR strike with pen), bottom nav (Records / Home / To-Do).
- Inline handwriting: write directly in the field box (Onyx raw drawing, zero-lag),
  single active writer at a time, Clear button, crop-to-content rendering so saved
  ink is large/readable on dashboard + widgets.
- History: per-habit current/best streak + 5-week completion heatmap.
- To-do add-on (separate list).
- Home-screen widgets: habits (quote + tap-to-check) and to-dos.
- "Never miss twice" daily notification (7pm, WorkManager).

## Design decisions

- Palette: black on **white** background; single accent **deep brick red `#8C1D1D`**
  (survives Kaleido 3 desaturation; pastels wash out). Handwriting font: Patrick Hand.
- Headline streak = perfect-day streak (all due habits completed that day).
- Habits grouped under identity section headers (with timeline rail).

## Known device quirks

- **Streamed install** registers the package but not activities → always use
  `--no-streaming`.
- **Screenshots come back stale** over adb (e-ink framebuffer) and the device drops
  to idle, swallowing injected taps — on-device verification needs a human + pen.

---

## Changes to implement (current backlog)

1. ~~Background → pure white~~. **Done** (`#FFFFFF`).
2. ~~Onboarding button placement~~. **Done**: primary CTA now sits in the content
   flow right below each step; Skip is a small top-right link; Back is a subtle
   top-left arrow; the bottom bar is gone. Habits added one-at-a-time via
   "✓ Add this habit".
3. ~~Time of day per habit~~. **Done**: optional time picker; stored as
   `reminderMinutes`; shown in the schedule label ("Daily · 7:00 AM"). Not yet
   wired to a per-habit reminder *notification*.
4. ~~Anchor habit (habit stacking)~~. **Done**: optional small "after what?" cue
   stored as `anchor`; shown under the habit name as "after <cue>". Not tracked.
5. **Per-habit reminder notifications** at the chosen time. [proposed]
6. **Edit / delete habits on the dashboard** (today you can only add). [proposed]
7. Remember last input mode (Type/Write) between habits. [proposed]
8. Per-habit icons (today pills reuse the identity icon). [proposed]

## Install / launch gotchas (Boox-specific)

- Always **uninstall, then `install --no-streaming`**. A streamed install — and even
  `install -r` over a just-uninstalled package — registers the package but NOT its
  activities (`resolve-activity` → "No activity found"; launch → "does not exist").
- Onyx **auto-freezes** newly installed apps (snowflake on the icon). If it won't
  open, **long-press the icon → Unfreeze**. This also blocks adb from foregrounding it.
- adb screenshots are stale (e-ink) and the device drops to idle; verify by hand.

## Verified working on device

- Installs + launches (via `--no-streaming`); no crashes.
- Dashboard renders in the planner style with the handwriting font + accent.
- Inline pen writing works and is fast; Clear works.

## Not yet hardware-verified

- The new onboarding flow (add-this-habit + repositioned buttons) and widgets /
  notifications need a hands-on pass on the device.
