# Google Android CLI content review

Reviewed on 2026-10-31. Use the [Android CLI workflow](android-cli.md) for the
project's working commands. This review records what Google's recent material
adds to that workflow and which capabilities still need a project decision.

## Sources and evidence

The review checked Google's official Android Developers channel listings and the
published descriptions and chapter lists of the 17 videos below, together with
Google's accompanying announcements and current CLI help. English caption tracks
were advertised, but their transcript endpoints returned empty responses. This
is a review of Google's published text, not a claim that every video was watched
or its spoken transcript verified. Dates are the official publication dates in
Pacific time; chapter links select the relevant starting point.

| Published | Official video | Relevant published content |
| --- | --- | --- |
| 2026-10-30 | [Agentic coding workflows with small models](https://www.youtube.com/watch?v=sBWgIjWU6PI) | Task routing, model evaluation, and offline coding workflows; no new required CLI command. |
| 2026-10-30 | [Agentic coding: A roundtable with industry leaders](https://www.youtube.com/watch?v=megAzvXTdrc) | Review, deployment, CI, and cost tradeoffs; not a project configuration specification. |
| 2026-10-29 | [Agentic engineering with Claude Code and Android CLI](https://www.youtube.com/watch?v=Mb9H43ia0kE&t=310s) | ANR diagnosis at 5:10; repository instructions at 12:00; skills at 16:24; performance at 20:22; verification loops at 22:33. |
| 2026-10-29 | [Trace me if you can: Automated instrumentation and field profiling](https://www.youtube.com/watch?v=eZtQgeIrmAo&t=633s) | Coroutine trace context, Compose, instrumentation, and benchmark validation; additional infrastructure would need agreement. |
| 2026-10-29 | [Declarative, agent-ready Android builds with Lightbuild](https://www.youtube.com/watch?v=0mYQtt2RfoQ&t=1015s) | A different build configuration approach and CLI demo; this repository continues using its Gradle wrapper. |
| 2026-10-28 | [The latest in Android dev tools from Android Dev Summit 2026](https://www.youtube.com/watch?v=w-auFZP5gJo&t=52s) | CLI overview at 0:52; crash diagnosis at 5:47; agent plugins at 13:23; Android Studio agent integration at 21:02. |
| 2026-10-28 | [Android Dev Summit 2026 keynote](https://www.youtube.com/watch?v=8Lmv7LGgL4E&t=119s) | Developer tooling starts at 1:59; later sections cover app intelligence, other devices, and platform APIs. |
| 2026-10-28 | [Give your agents access to tools and skills through Android CLI](https://www.youtube.com/watch?v=RrP7uWvCiSU) | Official skills and tooling for tasks including memory investigation and physical-device testing. |
| 2026-10-28 | [Use any coding agent directly in Android Studio](https://www.youtube.com/watch?v=1rwOVPybnDc) | ACP integration inside Android Studio; this is distinct from running CLI commands in this checkout. |
| 2026-10-28 | [Bring your Android app to Googlebook with the Adaptive skill](https://www.youtube.com/watch?v=JMX42rwY3fQ) | Adaptive layouts and keyboard/pointer input; does not authorize new form-factor requirements here. |
| 2026-10-02 | [Android CLI and skills to improve your development](https://www.youtube.com/watch?v=JMPd2jbQEqo) | Targeted Android skills, policy/performance tasks, and remote device access. |
| 2026-10-02 | [Test on real hardware through Android CLI](https://www.youtube.com/watch?v=gQswwgjX6n8) | Connect remote physical hardware, deploy, inspect logs, and capture screenshots. |
| 2026-10-02 | [Build better Wear OS apps with the Wear Compose Material 3 Android skill](https://www.youtube.com/watch?v=lBzZQmaC_2w) | Wear-specific components and guidance; no wearable feature is introduced in this project. |
| 2026-08-05 | [Android AI Tools: Vibe coding, sub-agents, and the Android CLI](https://www.youtube.com/watch?v=BEJ-RSgThuo&t=208s) | CLI at 3:28; Studio subagents at 11:35; Journeys at 35:08; skills at 43:36. |
| 2026-06-09 | [What are Android skills and how to use them with AI tools?](https://www.youtube.com/watch?v=qZLQVlLgTDU&t=331s) | Skill selection and application, with an XML-to-Compose example; this project already uses Compose. |
| 2026-04-17 | [Build Android apps using any AI agent with Android CLI](https://www.youtube.com/watch?v=d-EMhQ8opdY) | SDK setup, project creation, deployment, and UI interaction. |
| 2026-04-16 | [Build Android apps using any AI agent with Android CLI and skills](https://www.youtube.com/watch?v=AhrXPjk22OE) | The original CLI, skills, and Knowledge Base workflow. |

The principal accompanying sources are Google's
[October 28 tools and performance announcement](https://developer.android.com/blog/posts/android-dev-summit-26-developer-tools-performance),
[October 28 developer overview](https://developer.android.com/blog/posts/build-it-your-way-a-next-era-of-android-development),
[October 2 device and skills announcement](https://developer.android.com/blog/posts/android-cli-device-streaming-and-skills),
and [current command reference](https://developer.android.com/tools/agents/android-cli/commands).

## Instructions applicable to this project now

1. Check `android --version` and the relevant subcommand's `--help` before using
   syntax from a video. CLI 1.1.16717390 exposes native build, preview, input,
   profiling, artifact analysis, and Crashlytics commands. An announcement alone
   does not establish that a command exists in the installed version.
2. For an unfamiliar or changing Android API, search and fetch the official
   Knowledge Base before implementation. The following selection was exercised
   successfully and directly relates to the existing Navigation 3 result bus:

   ```sh
   android docs search 'Navigation 3 Hilt returning results'
   android docs fetch 'kb://android/guide/navigation/navigation-3/return-results'
   ```

   Compare the fetched API with the actual dependency versions and
   [architecture guide](architecture.md). Keep the project's Hilt factories,
   entry decorators, ViewModel state ownership, and Ktor boundaries. Resolve
   mismatches against the owning API or implementation rather than silently
   replacing an established convention with a tutorial's alternative.
3. During UI development, use the [inspect, interact, and verify workflow](android-cli.md#inspect-the-ui-while-developing).
   Select the device explicitly, inspect the live hierarchy, target a unique
   element, act, and inspect the resulting state. Use PNGs to verify appearance
   and content the hierarchy cannot describe. Record observed outcomes rather
   than treating successful command execution as behavior coverage.
4. Once a component has a deterministic preview, use `android preview list` to
   discover its real FQN and `android preview render --component-tree` to inspect
   its image and structure. The app currently has no active previews; its
   commented main preview is not a working fixture. Follow the existing
   read-only state pattern when previews are introduced. Preview images remain
   separate from screenshot regression tests and their baseline decisions.
5. Use focused checks and repeat them after a relevant fix, as described in the
   [testing guide](testing.md). If a check fails, investigate or report the
   blocker; do not stop at compilation, weaken assertions, or loop indefinitely
   until an unrelated failure disappears. Google's verification-loop examples
   do not replace this project's passing criteria or source-control checkpoints.
6. When Android Studio is already running, use `android studio check` to identify
   the actual project before trying semantic inspection. Commands such as
   `android studio analyze-file --project="$PWD" app/src/main/kotlin/com/johnturkson/sandbox/ui/MainContent.kt`
   can supplement source inspection when that exact worktree is open. A different
   project or a missing bridge is not evidence about this checkout. This review's
   probe found no running Studio instance, so bridge operations remain untested.
   See Google's
   [Studio command reference](https://developer.android.com/tools/agents/android-cli/commands/studio).

For specialized work, discover relevant official skills with `android skills
list` or `android skills find 'Navigation 3'`. Candidates matching this codebase
include `navigation-3`, `jetpack-compose-performance`, `edge-to-edge`, and
`android-profiler`. Read a selected skill and reconcile it with project rules
before applying it. If installing one, the verified command shape is:

```sh
android skills add navigation-3 --project="$PWD"
```

Use `--agent` to target the intended supported agent when needed, then verify
where the skill was installed and that the agent can discover it. An installation
reported for `android-studio` alone does not establish Codex discovery. Review
new repository files before staging. The October video describes `android skills
add all`; current help uses `android skills add --all`. Broad skill installation
is optional and is unnecessary for ordinary build and device interaction.

## Additional workflows for the user to choose

These are available capabilities or adoption candidates, not new project
requirements. Check current help before trying them and establish any new app,
test, or service conventions with the user.

| Candidate | Specific project use and prerequisite |
| --- | --- |
| Device Hub | `android device hub` provides a human-visible device window; useful for observing the same local or remote device while an agent interacts. |
| Remote physical devices | `android device remote projects`, then `models`, reservation, and connection workflows can validate hardware-specific behavior. Requires a selected Google Cloud/Firebase project, authentication, device choice, and review of service usage. Local emulator access does not prove remote access. |
| Journeys | Reusable app scenarios could cover list-to-detail, Back, refresh failure, offline data, and denied location as those features exist. Agree on format, fixtures, storage, runner, and failure reporting; there is no `android journey` command in the verified CLI help. |
| Perfetto and heap dumps | `android profiler systemtrace start` and `android profiler heapdump capture` can investigate scroll jank, startup, ANRs, and retained screens. Define a reproducible scenario and compare before/after; heap capture requires API 29+. Emulator timing alone does not establish production performance. |
| APK and R8 analysis | `android analyze app/build/outputs/apk/debug/app-debug.apk --analysis=basic --format=json` can inspect the built artifact. Optimization claims need a representative optimized release build and agreed release configuration. |
| Crashlytics signals | `android signals` can investigate field crashes and collected heap dumps. The app currently has no Crashlytics integration; this requires a separate service and data-handling decision. |
| Official agent plugin | Google's agent plugin can package CLI instructions and tools. Choose the target agent and installation scope; standalone CLI workflows already work without this extra installation. |
| Focused platform checks | Location denial, offline refresh, font scaling, rotation, process restoration, and accessibility scenarios are relevant to USGS Tracker. Add concrete scenarios as features and testing conventions are introduced. |

The October announcement describes LeakCanary integration while also saying its
CLI subcommand is coming soon. Installed `android profiler --help` exposes
`systemtrace`, `heapdump`, and `session`, but no `leakcanary` command. Do not
document leak detection as an executable or verified workflow until current help
and an actual run establish support. A heap dump by itself is not a leak finding.

Lightbuild migration, IDE-only ACP/model routing, Wear OS/XR expansion, and
in-app AI or AppFunctions are separate choices. The videos do not change the
Android-only product goals or authorize those additions to the app.
