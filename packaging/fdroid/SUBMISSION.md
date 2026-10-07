# Getting Honeycomb into F-Droid

## 0. Prereqs (done in the repo)

- [x] GPL-3.0 `LICENSE` at repo root
- [x] `README.md`, `PRIVACY.md`, `CHANGELOG.md`
- [x] `fastlane/metadata/android/en-US/` store text + changelog
- [x] no non-free dependencies (all AndroidX / Compose / Room / Kotlin, Apache-2.0)
- [x] no `INTERNET` permission
- [ ] portrait phone screenshots in `fastlane/metadata/android/en-US/images/phoneScreenshots/`
- [ ] `git tag v3.17` pushed
- [ ] `./gradlew assembleRelease` verified from a clean checkout on Linux

## 1. Publish the source

Push `main` to a public host (GitHub / GitLab / Codeberg). F-Droid's build
server clones it publicly, so it cannot be behind Tailscale or a login.

Set the real git author identity first:

    git config user.name  "Your Name"
    git config user.email "you@example.com"
    git commit --amend --reset-author        # only the initial commit, before pushing

## 2. Tag the release

    git tag -a v3.17 -m "Honeycomb 3.17"
    git push origin main --tags

`versionCode` must go up by at least 1 every release; keep `versionName` == tag
minus the `v`.

## 3. Test the F-Droid build locally (Linux + Docker — the homelab works)

    pip install fdroidserver          # or: apt install fdroidserver
    git clone https://gitlab.com/fdroid/fdroiddata
    cd fdroiddata
    cp ../mood-logger/packaging/fdroid/com.mark.moodlogger.yml metadata/
    # edit metadata/com.mark.moodlogger.yml: fill in <REPO_URL>, AuthorName
    fdroid readmeta
    fdroid lint com.mark.moodlogger
    fdroid build -v -l com.mark.moodlogger

Fix anything the build or lint complains about (usually: a dependency it cannot
fetch, a Gradle flag, or `scanner` flagging a bundled binary — there should be
none here).

## 4. Open the merge request

Fork `fdroiddata` on GitLab, push the branch with `metadata/com.mark.moodlogger.yml`,
open an MR. Follow the MR template. Review typically takes a few weeks; expect
a round or two of small requested changes. When merged, F-Droid's server builds,
signs (with F-Droid's key), and publishes it.

## 5. Updates after that

Bump `versionCode` + `versionName`, add `fastlane/.../changelogs/<versionCode>.txt`,
tag `vX.Y`, push. With `UpdateCheckMode: Tags` F-Droid picks it up automatically;
no further MR needed unless the build recipe changes.

## Alternative: self-hosted F-Droid repo

Faster, no review queue, signed with your own key, but users must add the repo
URL by hand and it is not searchable in the F-Droid app. Tools:
`fdroid` (`fdroid init` / `fdroid update`) publishing to a static host, or a
GitHub Action such as `fdroid/fdroidserver` building from GitHub Releases.

## Signature note

The F-Droid build is signed with F-Droid's key, not the key used for any APK
you sideload yourself. Switching an existing install from a sideloaded build to
the F-Droid build needs one uninstall + reinstall. Back up first: export CSV
from Settings, and/or copy the app database.
