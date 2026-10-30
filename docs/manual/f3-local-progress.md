# F3 local progress

Home offers **Kayıtlı oyuna devam** for the single saved game. It opens paused;
**Devam et** starts the foreground clock. Home/Back now offers **Kaydet ve dön**.
Starting another game requires confirmation before replacing the resumable game.
Replacement counts as abandonment; completed-mode records remain intact.

## Saved data and checkpoints

The versioned document contains settings/effects, a validated engine snapshot and
all mode statistics. The snapshot preserves occupied cells, used/remaining hand
slots, exact PRNG state, score, strikes and remaining move time. Restore never
advances time while the app was closed. Selection, drag coordinates, dialogs,
feedback animation and monotonic-clock baselines are not saved.

Checkpoints occur after moves, timeouts, pause/background, settings changes and
one-second foreground intervals (including Relaxed play time). Encoding and
platform writes run off the UI thread, with one in-flight write and the newest
pending document retained. A sudden kill may recover the last completed write;
a checkpoint still in flight is not guaranteed durable.

Android uses app-private SharedPreferences, replacing one JSON string with a
checked `commit()` on Dispatchers.IO. iOS uses NSUserDefaults on a background
dispatcher. Preferences, resume and statistics are one logical value; completing
an entire game updates its final statistics and removes resume together.
iOS's storage durability and lifecycle behavior still require device verification.

The UI keeps a retry warning after write failure, including a slow failure of an
older in-flight checkpoint. Only acknowledgement of the current document clears
it. Newer pending writes cannot be hidden by an older success. Startup read or
validation failures preserve the existing stored value: retry reading, or confirm
creating a new record. Unsupported versions are never silently treated as an
empty successful load. Saved UI modes are8×8/16×16; duplicate modes, inconsistent
counters, invalid board/hand/RNG/timer state and payloads over1,048,576 characters
are rejected.

## Statistics

Each exact size+difficulty+tempo combination stores games started, finished and
abandoned; moves; lines cleared; active foreground milliseconds; and its best
**final completed score**, including timeout penalties. Ongoing/abandoned games
contribute activity counters, but do not set a completed-score record. A resumed
game does not increment started. A completed result is counted once across fresh
controllers/activities. Local records are not trusted server score verification.

This is local storage, without an account or cloud synchronization. App data
removal/uninstall removes local progress. F4 will add the separate leaderboard.

## Verification and outstanding checks

Automated checks exercise engine round-trip/PRNG continuation, malformed saves,
writer ordering/conflation/failure/retry, stale acknowledgements, counters/records,
UI resume/replacement/errors and Android whole-document storage. Fresh Activity/
ViewModel instances read committed progress; malformed Android data stays untouched
on launch/cancel. These are Robolectric checks, not physical process-kill tests.

The existing macOS XCTest now includes save, terminate, relaunch and paused resume.
Apple compilation/simulator execution remain unrun in Linux. Before release,
verify actual Android force-stop/relaunch, iPhone/iPad storage, device termination
around an in-flight write, background memory pressure, storage-full retry, and the
TalkBack/VoiceOver/device matrix from the F2 notes.
