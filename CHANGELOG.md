# Changelog

Versions are `versionName` (`versionCode`). Dates are when the build was cut.

## 3.30 (33) — 2026-10-07 (privacy fixes: Auto Backup, app-lock PIN hashing)

- A real privacy gap, flagged by an outside reviewer on the public repo: the manifest
  had `android:allowBackup="true"` with no exclusion rules, which let Android's system
  Auto Backup silently copy the whole app data directory (mood.db, journal entries,
  medications, PHQ-9/GAD-7 check-ins, even the app-lock PIN hash) to the user's Google
  account backup storage - contradicting PRIVACY.md's "none of it leaves the device"
  promise. Fixed by setting `allowBackup="false"`, matching the watch companion app
  (which already had it right) and the project's standing no-telemetry rule. This stops
  future backups; it does not retroactively delete anything a device may have already
  backed up before this fix - check Google Account > Backup if that matters to you.
- Full audit done at the same time, confirmed clean: no `INTERNET` permission on either
  the phone or watch module, no Firebase/Crashlytics/analytics SDK anywhere in the
  project, and the phone<->watch sync uses the Wearable Data Layer's local
  Bluetooth/Wi-Fi Direct link only, never a cloud server.
- **Second real find, same reviewer:** the app-lock PIN was a single SHA-256 round over
  a 6-digit keyspace (a million possibilities) - fast enough to brute-force completely
  in well under a second if the salt+hash ever got out. Replaced with PBKDF2-HMAC-SHA256
  at 210,000 rounds (same `newSalt`/`hashPin`/`verifyPin` interface, no other file
  changed). Raises the cost of a full brute force by roughly five orders of magnitude
  while staying fast enough for a real unlock screen. **One-time consequence: this
  invalidates any PIN already set under the old scheme** - if you'd set one, you'll need
  to set it again once.

## 3.29 (32) — 2026-09-28 (health summary lock, PHQ-9/GAD-7, medication reminders)

- The Care tab's "Summary" view can now open full-screen and locked - no back button,
  no swipe-away, no tapping out of it, closable only by an explicit X. For handing the
  phone to a doctor or nurse without worrying what else they might land on. Verified
  live: the system back button and a full app-switch away and back both left it locked.
- New "Check-ins" tab in Care: real PHQ-9 and GAD-7 screenings (standard wording,
  standard 0-3 scoring, standard severity bands), a running history with delete, and an
  optional weekly reminder (Sunday evening, off by default) in Settings. If the PHQ-9
  self-harm question comes back above "Not at all," the result screen shows the 988
  Suicide & Crisis Lifeline. DB v16→17. Verified live end to end on a real GAD-7 (scored
  1, "Minimal"), including delete.
- Medications can now have real reminder times (a "Remind me" toggle plus one or more
  times of day), not just the free-text schedule note - a real notification fires at
  each time, same alarm pattern as the other reminders in this app. Verified live: the
  toggle and add-a-time picker both work.

## 3.28 (31) — 2026-09-28 (nutrition + app lock, installed 2026-09-28 evening)

- New Nutrition tab (home-screen button, next to Shopping): search a bundled,
  offline USDA database (7,839 whole/generic foods, Foundation Foods + SR
  Legacy) or log a food manually (name + calories required, protein/fat/carbs
  optional). Shows today's running total against a daily calorie budget you
  set in Settings. Plain numbers, no judgment in the copy. DB v16.
- New Privacy section in Settings: an optional PIN lock over the whole app
  (salted SHA-256, PIN never stored in the clear), with an optional fingerprint
  unlock once a PIN exists. Re-locks whenever the app goes to the background
  and comes back.
- Built on a secondary machine 2026-09-28 evening (that copy of the repo was
  stale, so everything was written additive), then merged into the main
  project the same night.

## 3.27 (30) — 2026-09-21 (watch companion, built, not yet installed)

- New Wear OS companion app in the `wear/` module for the Galaxy Watch Ultra:
  mood logging only. Open it (or tap the hourly buzz), tap how you feel, feel a
  small buzz, and it closes itself. Same honey ramp as the phone.
- The watch runs its own hourly reminder so it still buzzes when the phone is in
  a locker or car. The phone stays the source of truth: quiet hours and the
  reminders on/off switch are still set on the phone and copied to the watch.
- Moods logged on the watch wait on the watch until the phone is in range, then
  arrive on their own and are recorded with the watch's timestamp. A mood logged
  on the watch from an hourly prompt also closes out the phone's own silent
  prompt for that hour, so a shift with the phone away doesn't read as misses.
- The phone's hourly reminder no longer mirrors to the watch (it would buzz twice).
- Still no INTERNET permission in either app. Both are signed with the same key,
  which the phone-to-watch link requires.
- Known gaps in this first cut: no note field or missed-prompt reasons on the
  watch, and moods from the watch aren't yet labelled as watch-logged in History.

## 3.23 (26) — 2026-09-10

- Care Team is now a top-level "Care" tab in the menu, with three sections:
  Team, Meds, Summary. Menu: Goals, Journal, Trends, Care, Settings.
- Providers now include Primary care / Specialist / Dentist (not just the two
  mental-health kinds), plus an optional phone number and an active/inactive flag.
- New Medications section: name, dose, how many per dose, when you take it,
  which provider prescribed it, an "as needed" flag, notes, active/inactive.
- New Health summary — a plain read-only screen with your current meds, drug
  allergies, and care team contacts. The one to hand to a doctor.
- Shopping list moved off the menu to a button on the home screen (shows the
  open-item count); its daily reminder still opens it directly. (DB v13.)

## 3.22 (25) — 2026-09-09

- Menu declutter: the top menu drops from seven items to five (Goals, Journal,
  Trends, Shopping, Settings).
- History is now the "List" view inside Trends — a Chart / List toggle at the
  top. The day-grouped entry list, with edit and delete, is unchanged.
- Workout is now a fourth tab inside Goals (Major / Goals / Habits / Workout).
  The weekly muscle-group tracker and the hexagon figure are unchanged.

## 3.21 (24) — 2026-09-09

- Shopping list: a new Shopping screen (cart icon in the menu). Add items fast,
  check them off when bought, collapse the bought pile out of the way or clear it.
- A daily reminder names what's still on the list. Anything that has sat unbought
  past a threshold you set (default 4 days) escalates: the reminder gets louder
  and fires twice a day until it's checked off. Time and threshold are set on the
  Shopping screen itself. Re-arms after reboot and updates (DB v12).

## 3.20 (23) — 2026-09-09

- Home screen: the "Log last night's sleep" / "Take a breath" actions are now a
  full-width row of two outlined buttons with icons, instead of two cramped text
  links in the middle.
- Home screen: the space below "Working toward" is a user-choice widget — tap it
  to cycle through Today's snapshot (mood average + the day's dots), Next
  appointment (from Care Team), Steps today, Workout this week, and a weekly
  insight line. The choice is remembered.

## 3.18–3.19 (21–22) — 2026-09-08

- Journal: explicit Save button (swipe/back auto-save kept), optional title
  field, search across title/body/date, entries collapsed into per-day stacks.
- Trends: month separators.
- Guided breathing exercise ("Take a breath") with four patterns.
- Care Team section in Settings: therapist / psychiatrist / other, recurring
  weekly slot, computes the next appointment (DB v11).
- On-schedule vs off-schedule is now decided by timing (a 5-minute window from
  when the reminder actually fired), not by entry point; existing entries
  re-tagged once.

## 3.17 (20) — 2026-09-07

- Journal moved to its own tab, between Workout and Trends.
- After logging a mood, the app can offer to open a reflection for that day
  (once per day, only if none exists yet).
- Workout history now appears in the History tab, and a Workout section was
  added to Trends (this-week groups, days trained in the last 30, per-region
  frequency).
- The system back button / back-gesture returns to the home screen instead of
  closing the app; from the journal editor it saves and returns to the list.

## 3.14–3.16 (17–19) — 2026-09-07

- Journal: text and voice reflections, grouped by day (new `journal_entries`
  table, DB v9). Recorded as `.m4a` in app-private storage.

## 3.13 (16) — 2026-09-02

- Major goals: long-horizon goals in their own tab plus a tap-to-rotate
  home-screen reminder stack (DB v8).

## 3.12 (15) — 2026-09-02

- Goals feature (manual and auto-tracked), opt-in step counter, About screen
  (DB v7). Sleep tracker and honey-theme fixes from 3.11 shipped in this bundle
  (DB v6).

## 3.3–3.10 — 2026-09-01

- Movable menu position; side-menu alignment; swipe navigation; water tracker
  reskinned as a honey jar; two-column main screen; weekly workout section with
  the hexagon figure (DB v4–v5).

## 3.0–3.2 — 2026-08-31

- Full "Honeycomb" visual reskin: hexagons, honey mood ramp, honeycomb calendar.
- Habit tracker (quit/started streaks, DB v3).
- Named countdown timer with an alarm notification.

## 2.0 (2) — 2026-08-31

- Keyword tags, entry-source tracking, History screen, edit/delete, more Trends,
  CSV gains `source` and `keywords` columns (DB v2, existing data migrated).

## 1.0 (1) — 2026-08-30

- Hourly reminder to log a 1–5 mood with a note; local storage; today list;
  basic Trends; quiet hours; CSV export; re-arm after reboot/update.
