# Honeycomb

A calm daily check-in app for the basics: mood, water, sleep, movement, and the
habits and goals you're working on. Built for someone who forgets things and
wants the app to do the remembering, without nagging.

Everything you log is a row in a local SQLite database on your phone and nowhere
else. **No account, no cloud, no analytics, no ads.** The app has **no `INTERNET`
permission**, so it cannot phone home even by accident.

> The project folder and package id are `mood-logger` / `com.mark.moodlogger`
> for historical reasons (existing installs keep their data). The app has been
> called **Honeycomb** since v3.

## Features

- **Mood log** — a quick 1–5 check-in with an optional note and tags. Optional
  hourly reminder; log any time.
- **Journal** — short written or voice reflections, grouped by day.
- **Water** — a honey-jar tracker with a daily goal and quick-add servings.
- **Sleep** — log last night by bedtime/wake, with an optional assist that reads
  *only* charger connect/disconnect events (off by default).
- **Workout** — a weekly muscle-group split drawn as a figure made of hexagons.
- **Goals & habits** — day-to-day goals (some auto-tracked from your other logs),
  long-horizon "major goals" with a home-screen reminder, and quit/started
  streak counters.
- **Nutrition** — search a bundled, fully offline USDA food database (7,839
  foods) or log something manually, and see today's running total against a
  daily calorie budget you set yourself. Plain numbers, no judgment in the copy.
- **Care** — care-team contacts, medications with optional reminders, PHQ-9 /
  GAD-7 check-ins trended over time, and a locked "hand this to a doctor"
  summary view that only closes via an explicit button.
- **Privacy lock** — an optional PIN (plus fingerprint, once a PIN is set) over
  the whole app. Re-locks automatically whenever the app goes to the background.
- **Shopping list** — a simple add / check-off / bought list with an escalating
  reminder if it sits too long.
- **Trends & history** — a honeycomb calendar of the last 30 days, per-day and
  per-hour averages, tag breakdowns, sleep and workout summaries, full browsable
  history with edit/delete.
- **Step counter** — optional, off by default, a one-shot sensor read with
  nothing running in the background.
- **Wear OS companion** — mood logging from the wrist (Galaxy Watch Ultra),
  syncs back to the phone once in range.
- **CSV export** of everything, any time.

## Privacy

The optional helpers (sleep guess from charger events, step count from the
motion sensor) are off until you turn them on, read only what is described, and
are forgotten the moment you turn them off. The app never reads app-usage,
location, contacts, or the microphone (except while you are recording a voice
reflection).

See [PRIVACY.md](PRIVACY.md).

## Build

Standard Android project. Needs JDK 17+ (the toolchain is pinned to
Temurin/OpenJDK **21** for the Gradle 8.13 / AGP 8.7.3 pair) and the Android SDK
(compileSdk 35, minSdk 26).

```sh
./gradlew assembleRelease      # unsigned release APK
./gradlew assembleDebug        # debug APK
```

Output: `app/build/outputs/apk/<type>/`.

There are no non-free dependencies — everything is AndroidX, Jetpack Compose,
Room and Kotlin, all Apache-2.0, resolved from `google()` and `mavenCentral()`.

## Project layout

```
app/src/main/java/com/mark/moodlogger/
  MoodLoggerApp.kt   Application; creates the notification channel
  MoodScale.kt       the 1..5 scale: labels, emoji, colours
  data/              Room entities, DAO, database, migrations, DataStore settings
  audio/             AudioRecorder / AudioPlayer for voice reflections
  alarm/             AlarmScheduler + BroadcastReceivers (reminder, timer, boot, power)
  sensor/            one-shot step-counter read
  notification/      builds and posts notifications
  ui/                MainActivity, the screens, the ViewModel
app/schemas/         Room exported schemas (one JSON per DB version)
wear/                Wear OS companion app (mood logging only)
```

## Data credit

The bundled nutrition reference database (`app/src/main/assets/nutrition.db`)
is built from [USDA FoodData Central](https://fdc.nal.usda.gov/) (Foundation
Foods + SR Legacy), U.S. government public-domain data.

## How this was built

I'm not a professional software developer. This app was built through heavy,
hands-on collaboration with an AI assistant (Claude, from Anthropic) — I
directed what to build, made every real decision about features and design,
and tested everything on my own phone, but a lot of the actual code is
AI-generated and AI-assisted under my direction. I'm saying that plainly
because it's the honest thing to do, not because I'm trying to hide it.

I'd love for someone who knows coding firsthand to take this and build it
even better than I could ever imagine.

## Releases

Tagged `vX.Y` on `main`. `versionCode` increments by 1 each release;
`versionName` tracks the tag.

## License

[MIT](LICENSE).

## History

Started as an hourly mood-logger in August 2026 and grew from there. See
[CHANGELOG.md](CHANGELOG.md).
