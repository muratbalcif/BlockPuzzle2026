# Architecture

- UI: Compose Multiplatform (Android/iOS), unidirectional state flow
- Domain: pure Kotlin game-engine with immutable board state
- Infrastructure (planned): local persistence, Cloudflare Workers + D1 leaderboard
- Platform integrations (planned): AdMob, Play Billing, StoreKit 2

The game-engine is independent of UI, network, and platform SDKs.
Next step: generate the Gradle KMP application shell and connect the engine as a shared module.
