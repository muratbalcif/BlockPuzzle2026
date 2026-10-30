# F0 Foundation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Build a reproducible Android/iOS KMP foundation with quality gates.
**Architecture:** Pure Kotlin engine, shared Compose UI, thin Android/SwiftUI hosts.
**Tech Stack:** Kotlin 2.2.20, Compose 1.9.1, AGP 8.11.1, Gradle 8.14.3.
**Spec:** docs/superpowers/specs/2026-10-30-f0-design.md

## Global Constraints
- Android min 24, compile/target 36; iOS 15.0.
- Keep F1–F6 functionality out of the scaffold.
- Use existing isolated cloud checkout; no additional worktree required.
- No push/publish; leave reviewable changes on a feature branch.

## Review Focus
- Engine builds/tests without Android SDK (configure-on-demand command).
- Wrapper verifies the Gradle distribution checksum.
- iOS framework architecture and simulator destination agree.
- CI quality commands execute assertions and preserve failures.
- Local paths and secrets stay out of tracked files.

### Task 1: Build and module configuration
Files: root Gradle scripts, version catalog, wrapper, engine/build.gradle.kts,
composeApp/build.gradle.kts, .gitignore, .editorconfig.
Produces: engine JVM/iOS targets, Compose Android/iOS framework targets.
- [x] Add pinned toolchains and module definitions, generate official wrapper.
- [x] Verify Gradle configuration and engine test task with JDK 21.

### Task 2: Platform hosts
Files: composeApp/src/{commonMain,androidMain,iosMain}, iosApp Xcode project.
Consumes: engine project dependency and Compose framework named ComposeApp.
Produces: Android launcher, SwiftUI iOS launcher, placeholder app UI.
- [x] Add minimal placeholder app and platform launchers; engine has no fake logic.
- [x] Add Compose UI smoke test asserting the app title is rendered.
- [x] Build Android debug APK; run smoke test where a supported runner exists.
- [x] Configure unsigned iOS simulator debug build and simulator tests on macOS.

### Task 3: Quality gates and developer workflow
Files: .github/workflows/ci.yml, README.md.
Consumes: module tasks and native hosts from tasks 1–2.
- [x] Configure ktlint, Android lint, debug build, engine tests, UI test and iOS CI.
- [x] Document exact setup/build/test commands and platform limits.
- [x] Run available checks; record blockers and inspect final diff.

## Verification — 2026-10-30

- Full saved cloud setup script executed successfully, including checksum checks
  and `ktlintCheck :engine:jvmTest :composeApp:testDebugUnitTest
  :composeApp:lintDebug :composeApp:assembleDebug` (exit 0).
- Android UI smoke: 1 executed, 0 failures, 0 skipped. Verified RED→GREEN.
- Android lint: "No issues found." A stale incremental lint report required
  `:composeApp:lintDebug --rerun-tasks --no-daemon`; a subsequent normal full
  setup run also passed.
- Engine independent configuration verified with ANDROID_HOME/ANDROID_SDK_ROOT
  unset. Engine tests remain NO-SOURCE; F1 adds game tests.
- JVM target mismatch found by independent review: assertion failed at 21,
  passed after setting engine bytecode target 17. JDK toolchain remains 21.
- Xcode project/plist/scheme references and workflow/shell syntax parsed on Linux.
- iOS debug build and XCTest are configured, **not executed**: macOS required.
- Remote GitHub Actions runs are **not executed**; no push, PR or publication.
- Cloud `install_script` and `start_skill` saved as a draft; publication is a
  separate user action in environment settings.
