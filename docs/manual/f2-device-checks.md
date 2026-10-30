# F2 device validation

Historical F2 checkpoint; see [F3 notes](f3-local-progress.md) for current local
save/restore behavior and its validation.

Automated in the Linux workspace: 37 engine tests and 31 application tests
(controller/geometry/Robolectric UI), ktlint, Android lint and debug APK build.
The native-graphics Robolectric placement test captures home/game PNGs under
`composeApp/build/reports/ui-snapshots`. These are test-runtime renders, not
physical-device or iOS screenshots.

## Implemented play flow

- Setup: 8×8 or 16×16, three independent difficulty levels, four tempos.
- Select a piece then tap an origin cell, or drag it from the tray. Valid/invalid
  ghosts and text feedback share the engine's placement validation.
- Enlarge the board for 48dp cells and pan both axes; Fit returns to full-board
  view. The default board fits available space, so cells may be smaller.
- Completed lines flash briefly; accepted placements trigger platform haptic
  feedback. Setup switches disable animation/haptics.
- Manual/background pause freezes the timer and cancels gestures. Returning
  to foreground requires explicit Resume; foreground elapsed time is accounted
  before pause/moves. Third timeout or no remaining fit ends play.
- Android Back and the Home control ask before returning home. In F3 this saves
  the game; replacing it with another game requires separate confirmation.
  Android ViewModel retains a session across configuration recreation. Process
  death, disk save and persistence are F3, not provided by F2.

## Unrun device checks

These checks require an emulator/physical device or macOS. Do not mark them passed
based on the APK or Robolectric tests.

| Target | Checks |
| --- | --- |
| Android API24 and API35/36 phones | Install APK, tray-to-board drag, cancellation, system bars/insets, portrait/landscape, resize |
| Android tablet | Responsive two-column layout, 8×8/16×16 fit, enlarge/pan, large text |
| TalkBack | Row/column/occupancy descriptions, piece-offset descriptions, independent horizontal/vertical scrolling and placement actions |
| iOS simulator (macOS CI) | Native engine tests, SwiftUI/Compose app launch, start, pause/resume |
| Physical iPhone/iPad | Drag accuracy, VoiceOver, haptic support/toggle, background/foreground and safe areas |
| Both platforms | 200% text, animation disabled, timed penalty, third timeout, no-fit end, restart, exit cancel/confirm |

No remote CI execution, push, merge, publication, store upload or physical-device
verification was performed by the implementation task.
