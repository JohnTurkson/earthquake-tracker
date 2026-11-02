---
name: test-emulator
description: Set up USGS Tracker's shared Earthquake Project emulator, delegate availability waiting, acquire it through ADB, and return it to verified idle home after device tests, mockups, screenshots, or validation.
---

# Test Emulator

Read the [testing guide](../../../docs/testing.md#shared-test-emulator) and
[Android CLI workflow](../../../docs/android-cli.md). Use the single shared
**Earthquake Project** AVD (`Earthquake_Project`): **Resizable**, **API 37**,
**Google Play**. It is exclusively for project validation across all agents and
worktrees on this host. Leave other emulators stopped. Use direct CLI commands
and agent tool calls, without creating or requiring a shell/Python helper script.

## Set up once

Use Android Studio's SDK and the standard `~/.android/avd` directory. Check
`android emulator list` first; preserve a matching existing AVD. Assign setup to
one agent if missing; other agents continue host work and wait for setup to finish.
Never create per-agent copies, use `--force`, or overwrite an incompatible AVD.

Inspect `android emulator create --help` and `android sdk list --all`. Choose a
stable API 37 **Google Play** image for the host ABI, such as
`system-images/android-37.0/google_apis_playstore/arm64-v8a` on Apple Silicon
(`x86_64` on Intel). Install a missing image with `android sdk install PACKAGE`.
Do not accept new SDK licenses on the user's behalf.

If Android CLI cannot select the exact Resizable profile and AVD name, use its
SDK command-line tool fallback (answer `no` to a custom hardware profile):

```sh
"$ANDROID_HOME/cmdline-tools/latest/bin/avdmanager" create avd \
  --name Earthquake_Project --device resizable \
  --package 'system-images;android-37.0;google_apis_playstore;arm64-v8a'
```

With the new AVD stopped, verify its `config.ini` under
`~/.android/avd/Earthquake_Project.avd/`. Use a text editor to set the display
name and enable Play Store/GPU if necessary:

```ini
avd.ini.displayname=Earthquake Project
hw.device.name=resizable
PlayStore.enabled=yes
hw.ramSize=4G
hw.gpu.enabled=yes
hw.gpu.mode=auto
```

Verify `hw.resizable.configs` exists and `image.sysdir.1` names the chosen API 37
Google Play image. Do not substitute a fixed-size or Google APIs-only image.
See Google's [Resizable setup](https://developer.android.com/studio/run/resizable-emulator).

Start with `android emulator start Earthquake_Project --headless`. Discover its
serial with `adb devices -l`, confirming `adb -s SERIAL emu avd name` reports
`Earthquake_Project`; never assume a port. The setup agent takes the reservation
below before first-boot interaction, skips account sign-in, completes only local
onboarding, and sets `adb -s SERIAL shell settings put global stay_on_while_plugged_in 7`.
Finish with the cleanup and release steps below. Subsequent starts preserve data.
If another agent is already starting the same AVD, wait and rediscover its serial;
do not launch a second instance, wipe data, cold boot, or restart it to obtain access.

## Wait in a subagent; keep implementing

When device work is approaching, delegate **one read-only waiter per task** to a
subagent. Give it this skill's path, the AVD ID and the parent task identity.
The parent continues implementation, review, builds, local tests, lint and host
screenshot tests. Do not block the parent on a long shell command or leave the
emulator reserved while doing unrelated work.

The waiter performs direct Android CLI/ADB queries, using agent wait tools
between checks (about 15 seconds, not a shell polling script). First resolve the
dedicated AVD's current serial. If it is stopped, report that to the parent;
coordinate one start, then resume waiting. If disconnected, offline, booting or
a command fails, report unavailable and retry without changing the device.

```sh
adb -s "$ANDROID_SERIAL" shell ls -ld /data/local/tmp/earthquake-project-reservation
adb -s "$ANDROID_SERIAL" shell getprop sys.boot_completed
adb -s "$ANDROID_SERIAL" shell getprop ro.build.version.sdk
adb -s "$ANDROID_SERIAL" shell cmd package resolve-activity --brief \
  -a android.intent.action.MAIN -c android.intent.category.HOME
adb -s "$ANDROID_SERIAL" shell dumpsys window
adb -s "$ANDROID_SERIAL" shell dumpsys window policy
adb -s "$ANDROID_SERIAL" shell dumpsys power
adb -s "$ANDROID_SERIAL" shell pidof com.johnturkson.sandbox
```

A reservation directory means **busy**, even at Home. Skip further UI checks
while reserved; inspect its owner with `adb -s SERIAL shell ls -1 DIRECTORY` only
when useful for coordination. Only an explicit `No such file or directory`
means absent; connection/permission errors never mean free.
`pidof` returning no PID with exit code 1 simply means the app is not running;
that expected absence is not a device connection failure.

When unreserved, require boot `1`, SDK `37`, `mWakefulness=Awake`, keyguard
`showing=false`, and `mCurrentFocus` matching the resolved HOME activity.
Expand a relative component name when comparing. The app's main screen is busy;
a background app process alone neither grants nor prevents availability.
Require two matching observations at least three seconds apart. Never press
Home, wake/unlock, stop an app or change settings to make acquisition succeed.

Notify the parent with the AVD/serial and observed readiness; **do not reserve it
for a parent that is still implementing**. A notification is only a snapshot.
If busy for ten minutes, report the owner/state once and continue waiting in the
subagent while the parent works. Do not fail the whole task or create another
emulator merely because it is occupied. If subagents are unavailable, use short
read-only checks between independent work instead of blocking on a long wait.

## Acquire atomically when ready to test

The parent exports a unique shell-safe `EMULATOR_OWNER`, such as its chat UUID
plus an attempt number, and records it in chat. Export `ANDROID_SERIAL` from the
confirmed AVD identity; shell variables must be set in each new tool shell.
Use the actual serial on every command. After the waiter reports ready, attempt
this **single atomic command**:

```sh
adb -s "$ANDROID_SERIAL" shell mkdir /data/local/tmp/earthquake-project-reservation
```

Only confirmed success grants the reservation. **Do not use `mkdir -p`.** If
another agent won, return to the waiter and continue independent work. If the
result is uncertain, inspect and coordinate; do not assume ownership. After a
successful claim, label it with a directory containing only your unique token:

```sh
adb -s "$ANDROID_SERIAL" shell mkdir "/data/local/tmp/earthquake-project-reservation/$EMULATOR_OWNER"
```

Retain the reservation across all tool calls and any testing subagents; only
the parent releases it. While owning it, verify your token instead of expecting
reservation absence. Recheck boot, unlock and HOME focus, then run
`android layout --device="$ANDROID_SERIAL" --full` without `--no-idle` and
confirm a valid hierarchy. Recheck HOME focus afterward. This CLI command waits
for UI idle; do not run instrumentation/layout inspection as a read-only waiter
against another owner's work. A transient CLI failure means retry under your
reservation, not permission to skip the idle check. If readiness changed before
testing starts, release only your reservation and resume waiting, without
navigating the current activity.

Before testing, record the original display mode/overrides, density, font scale,
night mode, rotation, and any scenario-specific settings. Run device tests with
the exported serial, for example `ANDROID_SERIAL=SERIAL ./gradlew :app:connectedDebugAndroidTest`.
Do not start detached work that can outlive the reservation.

## Clean up, verify, then release

Agents **MUST** perform this cleanup after success, failure, cancellation or
interruption. Stop/join all their device work and restore the exact original
settings, including display mode/overrides, permissions, network or location
changes made for a test. Use `wm size reset` / `wm density reset` only when the
recorded values had no overrides; otherwise restore the recorded overrides.
Then:

```sh
adb -s "$ANDROID_SERIAL" shell am force-stop com.johnturkson.sandbox
adb -s "$ANDROID_SERIAL" shell input keyevent KEYCODE_WAKEUP
adb -s "$ANDROID_SERIAL" shell wm dismiss-keyguard
android input --device="$ANDROID_SERIAL" key home
```

Repeat the readiness checks and valid `android layout` idle check, observing
stable unlocked HOME focus twice. If settings or idle home cannot be verified,
**keep the reservation**, report incomplete cleanup and recover it before
release. A crash leaves the directory behind intentionally; no timeout or TTL
allows another agent to steal it. Coordinate with its recorded owner and confirm
all prior device work has ended before recovery. An unlabelled reservation is
also busy; ask for a specific recovery decision if ownership is unclear.

Verify the directory contains your exact owner token, then remove only those
empty directories, checking each command succeeds before the next:

```sh
adb -s "$ANDROID_SERIAL" shell rmdir "/data/local/tmp/earthquake-project-reservation/$EMULATOR_OWNER"
adb -s "$ANDROID_SERIAL" shell rmdir /data/local/tmp/earthquake-project-reservation
```

Never use recursive deletion, remove another owner's token, or release before
cleanup. Leave **Earthquake Project running at verified idle home** for the next
agent. Report cleanup/release separately from the test result; no local scripts,
lock files, session logs or run reports belong in the repository.
