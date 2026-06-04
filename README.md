# Ink Habits

An identity-based habit tracker built for the **Onyx Boox Note Air 3C** (Kaleido 3 color e‑ink tablet).

Grounded in the *Atomic Habits* philosophy: instead of setting goals, you choose an identity ("I am a Reader") and build the habits that person would do.

## Features

- **Handwriting-first** — write habit names, anchors, and quotes with the Boox stylus via the Onyx raw-drawing SDK (zero-lag, firmware-rendered ink). Typed input also supported.
- **Identity-based grouping** — habits belong to an identity. Every identity has a perfect‑day streak (consecutive days where every due habit was completed).
- **Flexible scheduling** — daily, specific days of week, every N days, or N times per week. Optional time‑of‑day (Morning/Afternoon/Evening).
- **"Never miss twice"** — detects habits at risk and nudges you before you break the chain twice in a row.
- **Schedule‑aware streaks** — evaluated against scheduled occurrences, not raw calendar days.
- **Gamified rewards** — define your own rewards tied to streak targets. Automatic unlock with notification.
- **Handwriting recognition** — Google ML Kit digital‑ink recognition converts handwritten anchors to typed text.
- **E‑ink optimized** — flat Material 3 UI (no elevation/animation), custom EPD update modes (fast DU for scroll, quality GU at rest, periodic ghosting clears).
- **Home‑screen widgets** — habits and to‑do widgets with tap‑to‑check.
- **Daily quotes** — deterministic Atomic Habits quotes from the app's built‑in collection.

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin |
| UI | Android Views + Material 3 |
| Persistence | Room (Flow + coroutines) |
| Async | Kotlin Coroutines |
| Background | WorkManager |
| Handwriting | Onyx SDK (`onyxsdk-pen`, `onyxsdk-device`) |
| Recognition | Google ML Kit Digital Ink |
| Min SDK | 28 (Android 9) |
| Target SDK | 34 |

## Downloads

Pre-built APKs are available in the [`apk/`](apk/) directory:

| File | Version | Type |
|---|---|---|
| [ink-habits-v1.0.0-debug.apk](apk/ink-habits-v1.0.0-debug.apk) | 1.0.0 | Debug |

Install directly on your Boox device:

```
adb install --no-streaming apk/ink-habits-v1.0.0-debug.apk
```

## Getting Started

1. Open the project in Android Studio.
2. Sync Gradle (the Boox Maven repo is already configured in `build.gradle`).
3. Build and install on your Boox device:
   ```
   adb install --no-streaming app/build/outputs/apk/debug/app-debug.apk
   ```
   > `--no-streaming` is required due to a Boox firmware quirk.

## License

MIT License — see [LICENSE](LICENSE).
