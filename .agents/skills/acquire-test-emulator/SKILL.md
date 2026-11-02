---
name: acquire-test-emulator
description: Check Earthquake Tracker's shared Earthquake Project emulator on demand, coordinate agents with one ignored project lock file, and verify idle-home cleanup after tests, mockups, screenshots, or validation.
---

# Acquire Test Emulator

Read the [testing guide](../../../docs/testing.md#shared-test-emulator) and
[Android CLI workflow](../../../docs/android-cli.md). Use the single shared
**Earthquake Project** AVD (`Earthquake_Project`): **Resizable**, **API 37**,
**Google Play**. It is exclusively for project validation across all agents and
worktrees on this host. Leave other emulators stopped. Use direct CLI commands
and agent tool calls, without creating or requiring a shell/Python helper script.

## One lock across worktrees

The lock is `.earthquake-emulator.lock` in the **primary checkout**, ignored by
the project's `.gitignore`. Every parent and subagent resolves that same path,
even when working in a linked worktree:

```sh
EMULATOR_PROJECT_ROOT="$(git worktree list --porcelain | sed -n '1s/^worktree //p')"
EMULATOR_LOCK="$EMULATOR_PROJECT_ROOT/.earthquake-emulator.lock"
```

Git lists the primary checkout first. Confirm the resolved root is nonempty and
exists; never fall back to a lock in the current worktree. Export/pass this exact
path and the owner token when delegating device work. Reset shell variables in
each new tool shell; do not assume they persist between commands.

A lock contains one unique parent-chat/attempt token. An existing lock, including
an empty or incomplete one, means **busy**. Read it with `cat "$EMULATOR_LOCK"`;
read/permission errors never mean available. Never overwrite or append to it.

## Set up once

Use Android Studio's SDK and the standard `~/.android/avd` directory. Check
`android emulator list` first; preserve a matching existing AVD. Assign setup to
one agent if missing; it must claim the project lock below before setup or start.
Other agents continue host work and wait for setup to finish.
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
`Earthquake_Project`; never assume a port. The setup agent retains its project
lock through first-boot interaction, skips account sign-in, completes only local
onboarding, and sets `adb -s SERIAL shell settings put global stay_on_while_plugged_in 7`.
Finish with the cleanup and release steps below. Subsequent starts preserve data.
If another agent is already starting the same AVD, wait and rediscover its serial;
do not launch a second instance, wipe data, cold boot, or restart it to obtain access.

## Check on demand; keep implementing

Agents check availability themselves when device work is approaching. If busy,
continue implementation, review, builds, local tests, lint and host screenshot
tests, then check again when useful. For a longer wait, optionally delegate one
read-only waiter per parent task with this skill, the exact lock path and the
parent identity. Do not reserve the emulator while doing unrelated work.

Check the project lock first. If it exists, leave the device untouched and
resume independent work; a waiter can check again about every 15 seconds using
agent wait tools. Do not use a blocking shell loop or a local polling script.
When the lock is absent, resolve the dedicated AVD's serial through ADB. If it
is stopped, the parent must acquire the project lock before starting it. If it
is offline, booting or a query fails, treat it as unavailable and retry later.

```sh
adb -s "$ANDROID_SERIAL" shell getprop sys.boot_completed
adb -s "$ANDROID_SERIAL" shell getprop ro.build.version.sdk
adb -s "$ANDROID_SERIAL" shell cmd package resolve-activity --brief \
  -a android.intent.action.MAIN -c android.intent.category.HOME
adb -s "$ANDROID_SERIAL" shell dumpsys window | rg 'mCurrentFocus='
adb -s "$ANDROID_SERIAL" shell dumpsys window policy | rg 'showing=|screenState='
adb -s "$ANDROID_SERIAL" shell dumpsys power | rg 'mWakefulness='
adb -s "$ANDROID_SERIAL" shell pidof com.johnturkson.sandbox
```

`pidof` returning no PID with exit code 1 simply means the app is not running;
that expected absence is not a device connection failure.

When unreserved, require boot `1`, SDK `37`, `mWakefulness=Awake`, keyguard
`showing=false`, and `mCurrentFocus` matching the resolved HOME activity.
Expand a relative component name when comparing. The app's main screen is busy;
a background app process alone neither grants nor prevents availability.
Require two matching observations at least three seconds apart. Never press
Home, wake/unlock, stop an app or change settings to make acquisition succeed.

A waiter reports the AVD/serial and readiness to its parent; it never acquires
or releases the lock. A notification is only a snapshot. If busy for ten minutes,
report the owner/state once and continue independent work or waiting. Do not fail
the whole task or create another emulator merely because it is occupied.

## Acquire atomically when ready to test

The parent sets a unique shell-safe `EMULATOR_OWNER`, such as its chat UUID plus
an attempt number, and records it in chat. With the shared path resolved above,
claim it using exclusive creation in a subshell:

```sh
(set -C; printf '%s\n' "$EMULATOR_OWNER" > "$EMULATOR_LOCK")
```

Both variables must be nonempty before running the claim.
`set -C` enables shell noclobber: the create fails if the file already exists.
Checking absence then using ordinary `>` or `touch` is not an atomic claim.
Never use `>|`, `set +C`, or delete an existing lock to win. Only confirmed exit
code 0 and your exact token in the file grant ownership. If another agent wins,
keep working and retry later. An uncertain/failed write leaves the lock busy.

Retain the reservation across all tool calls and any testing subagents; only
the parent releases it. Children use the parent's token rather than claiming
again, never remove the lock, and report completion back. The parent serializes
all its children's device operations; a shared token is not permission to run
tests or UI actions concurrently. Do not hand ownership to a child that can
outlive the parent without an explicit coordinated handoff.

Export `ANDROID_SERIAL` from the confirmed AVD identity. Before every device
mutation or instrumentation command, verify the lock still contains your token.
While owning it, recheck boot, unlock and HOME focus, then run
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
release. A crash leaves the file behind intentionally; no timeout or TTL
allows another agent to steal it. Coordinate with its recorded owner and confirm
all prior device work and children have ended before recovery. An empty lock is
also busy; ask for a specific recovery decision if ownership is unclear.

Only after cleanup and all child work have completed, release conditionally:

```sh
test "$(cat "$EMULATOR_LOCK")" = "$EMULATOR_OWNER" && rm -- "$EMULATOR_LOCK"
```

A token mismatch or failed read must leave the lock untouched. This check/remove
sequence relies on all agents honoring exclusive creation and no forced takeover;
it is cooperative coordination, not an OS-enforced security boundary or FIFO
queue. Leave **Earthquake Project running at verified idle home** for the next
agent. Report cleanup/release separately from test results. Never commit the
ignored lock, local helper scripts, session logs or run reports.
