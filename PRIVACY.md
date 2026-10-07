# Privacy Policy

Honeycomb does not collect, transmit, or share any personal data.

## What the app stores

Everything you enter (moods, notes, tags, journal text, voice reflections, water,
sleep, workouts, goals, habits, step counts, settings) is stored in a private
SQLite database and app files on your device. None of it leaves the device
except when **you** explicitly export a CSV or share a file through the Android
share sheet.

## Network

The app declares **no `INTERNET` permission**. It cannot make network requests,
and there is no analytics, crash reporting, advertising, or account system of any
kind.

## Optional device signals

Two features are off by default and read only what is described:

- **Sleep assist** — records the times your device is connected to and
  disconnected from a charger, to pre-fill a bedtime/wake guess. Turning it off
  discards this.
- **Step counter** — a one-shot read of the hardware step-counter sensor when you
  open the app. No background service. Turning it off discards this.

## Microphone

The microphone is used only while you are actively recording a voice reflection,
and the resulting audio file is stored only on your device.

## Permissions

- `POST_NOTIFICATIONS`, `USE_EXACT_ALARM`, `SCHEDULE_EXACT_ALARM` — the optional
  reminders and the named timer.
- `RECEIVE_BOOT_COMPLETED` — re-arm your reminders and timer after a reboot.
- `RECORD_AUDIO` — voice reflections (requested when you first record).
- `ACTIVITY_RECOGNITION` — the optional step counter (requested when you enable
  it).

## Contact

Open an issue on the project's source repository.
