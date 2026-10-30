# F3 Local Progress Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement task-by-task.

**Goal:** Persist and safely resume local games with mode-specific records/statistics.
**Architecture:** Validated domain snapshots, versioned JSON document, serialized async platform writer, presentation integration.
**Tech Stack:** Kotlin/Compose, kotlinx.serialization1.9.0, coroutines, SharedPreferences/NSUserDefaults.
**Spec:** docs/superpowers/specs/2026-10-31-f3-local-progress-design.md

## Global Constraints
- Preserve existing work; no worktree, push or publication.
- Restore unfinished games paused; never count offline time.
- One resume slot and one logical document for stats+resume consistency.
- Final completed score determines record for its exact mode.

## Review Focus
- PRNG resumes at its state, not reseeded; used hand slots stay used.
- Corrupt/unsupported snapshots cannot create invalid engine states.
- Terminal result cannot be counted twice across process recreation.
- Async writes cannot reorder or let stale success hide latest failure.
- Foreground play time includes relaxed mode but excludes pause/background/offline time.

### Task1: Domain snapshots
Files: engine/GameSnapshot.kt, GameSession/PieceGenerator restore APIs, common tests.
- [x] Test round-trip continuation/hand refill, paused restoration and malformed snapshots.
- [x] Observe RED, implement validated snapshots and verify full engine tests.

### Task2: Document and platform persistence
Files: composeApp persistence model/codec/store/writer, platform actuals, codec/writer/platform tests.
- [x] Test JSON round trips, corruption/version limits and serialized failure/retry writes.
- [x] Implement typed JSON, Android/iOS stores and ViewModel loading/writer integration.

### Task3: Controller and UI
Files: GameController, home/game/stats screens and tests.
- [x] Test checkpoint, resume, replacement confirmation, counting/records/time and failures.
- [x] Implement autosave, resume UI, records/stats and save status/retry.
- [x] Update F2 exit tests for Save and Return behavior; add fresh-controller storage tests.

### Task4: Verification
- [x] Independent review and important fixes with regression tests.
- [x] Full engine+app suite, ktlint, lint and debug build; native checks reported separately.
- [x] Documentation, saved startup instructions and actual validation counts.

## Verification — 2026-10-31

- Engine snapshot tests observed6 failures before implementation, then passed;
  additional import/export defensive-copy coverage included. Total engine44.
- Codec8 tests initially RED; writer2 and controller9 initially RED. Added stale
  slow-failure regression (observed RED→GREEN), missing-mode rejection, platform
  store and fresh Activity/ViewModel integration. Total application61.
- Full command: `./gradlew ktlintCheck :engine:jvmTest :composeApp:testDebugUnitTest
  :composeApp:lintDebug :composeApp:assembleDebug`: exit0.105 tests, zero failures,
  errors or skips. Android lint reports no issues; debug APK produced.
- Scoped UseKtx lint exception on Android store.write: the KTX edit helper returns
  Unit and would hide the checked commit failure Boolean. Global warning/error
  gates remain enabled; code comment explains the exception.
- Independent review found slow stale write failures could be invisible while
  newer checkpoints queued. Regression now proves failure remains visible until
  latest success; stale success/failure cannot undo a current saved state.
- Startup asynchronous loading could leave home scrolled away from its title;
  post-load scroll reset fixed the existing title regression. UI statistics tests
  scroll to the button before invoking it rather than tapping a clipped control.
- Android actual preference adapter and newly created Activity/ViewModel instances
  read the same committed session; unsupported/corrupt data survives launch/cancel.
- iOS XCTest extended for save/terminate/relaunch/paused resume. Apple compilation,
  actual native storage/device process kill/haptics remain UNRUN on Linux.
- F0/F1/F2 work preserved. No push, merge, commit, remote CI or publication.
