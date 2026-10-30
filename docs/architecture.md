# Architecture

- `engine`: active pure Kotlin Multiplatform domain module with immutable boards,
  deterministic generation, configurable rules and validated session snapshots.
- `composeApp`: shared Compose Android/iOS UI, foreground/lifecycle controller,
  local JSON progress, records/statistics and platform storage adapters.
- `iosApp`: SwiftUI host and Xcode simulator/UI-test project.
- Planned: Cloudflare Workers + D1 leaderboard; AdMob, Play Billing and StoreKit.

The original `game-engine/` prototype is preserved from the existing main branch.
It is not included in Gradle settings; the application uses `:engine` instead.
See README.md and engine/README.md for current setup and game rules.
