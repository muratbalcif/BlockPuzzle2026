# F2 — Playable shared Compose UI

Approved visual direction: light, simple, colored blocks and large controls.
Home selects 8×8/16×16, Easy/Medium/Hard and Relaxed/Slow/Medium/Fast. Game shows
board, three-piece hand, score, remaining time and timeout rights. Drag previews
valid/invalid placement; selecting a piece then tapping a cell is an equal input
path. F1 rules remain unchanged.

Architecture: shared Compose screens and a stateful presentation controller over
immutable GameSession. A monotonic injected clock accounts for foreground time
before actions; background/manual pause freezes the game, cancels selection/drag,
and requires explicit resume. ViewModel retains the session through Android
configuration changes; no disk persistence (F3).

Single shared UI avoids duplicate Android/iOS screens. Native wrappers supply
Android back handling and Compose's platform haptic service. Standard lifecycle
and ViewModel Compose integration handles configuration/lifecycle. No navigation
library is needed for two screens.

Visuals: warm off-white surfaces, teal board, distinct hand colors, visible
non-color validity text. Short cell color/clear feedback and placement haptic;
explicit animation/haptic toggles. Responsive portrait/landscape layout, scalable
text, scrollable setup, and board enlargement/panning for 16×16 or larger touch
targets. Cells expose row/column/occupancy descriptions and accessibility actions;
selected piece dimensions/cell offsets are described for nonvisual play.

Confirmation is required before abandoning a running game; cancel restores its
prior active/paused state. Finished games offer same-settings replay or home.
No gameplay UI, timer or haptic claim is treated as device verification without
running its corresponding check. Linux can run Robolectric Compose tests and APK
build; native Apple/device haptics, VoiceOver/TalkBack and manual device matrix
remain external checks.
