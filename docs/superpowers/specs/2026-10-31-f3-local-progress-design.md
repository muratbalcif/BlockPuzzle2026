# F3 — Local progress

Scope from the approved roadmap: automatic game saves, records/statistics,
pause/resume and lifecycle integration. F2 already supplies lifecycle pause.

Defaults: one resumable game, restored paused with no offline clock advance.
Saving includes board, remaining/used hand slots, PRNG state, score, strikes and
remaining move time. Checkpoint after accepted moves, timeouts, pause/background,
settings changes and at most one second of active timer/play time. Writes are
serialized and conflated on background dispatchers; the UI does not block on disk.
A failed write stays visibly unsaved and can be retried. A crash may recover the
last completed checkpoint, never reconstructed wall-clock time.

Keep resume state, settings and per-mode statistics in one versioned JSON value.
Android uses app-private SharedPreferences; iOS uses NSUserDefaults. One logical
whole-document replacement prevents statistics and resume being saved separately.
Malformed/unsupported saves are rejected with a visible warning and not overwritten
just by opening the app. Validate engine snapshot invariants before construction.

Home offers Continue and New Game. Returning home saves progress; starting a new
game while a resumable game exists requires replacement confirmation and counts
one abandonment. Finished games are removed from resume in the same update that
records their final result, preventing duplicate completion after restart.

Statistics are separate for each board size+difficulty+tempo: games started,
finished, abandoned, moves, cleared lines, active foreground milliseconds and best
FINAL completed score (penalties included). Counters include ongoing/abandoned play;
only completed scores become records. Settings/effects are restored across launches.
No account, backend, cloud synchronization or reset-all UI in F3.

Engine snapshots have a rules version independent of document format. Restore
validates board/hand/config/RNG/score/strike/timer/end-state consistency and always
pauses unfinished games. Saved UI modes support 8 and16; engine tests may use
smaller fixtures. UI transient selection, gestures/dialogs and wall-clock origins
are never persisted. Existing F0/F1/F2 work stays intact.

Alternatives: separate keys risk partial logical updates; a database is unnecessary
for one small local document. Chosen: typed JSON snapshot, one value, serial writer.
Apple compile/device storage remains a macOS/device verification prerequisite.
