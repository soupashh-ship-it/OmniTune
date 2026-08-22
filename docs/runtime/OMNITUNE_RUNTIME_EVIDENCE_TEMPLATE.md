# OmniTune runtime smoke evidence

Copy this file to a private run record or issue. Do not commit device serials, screenshots, backups, provider credentials, cookies, or personal media.

## Environment

| Field | Value |
| --- | --- |
| Run ID / date | |
| Device model | |
| Android version / SDK | |
| Debug package and version | `com.omnitune.app.debug` / |
| Git commit | |
| Network mode before/after | |
| Provider/account state | signed out / disposable account / unavailable |
| Dataset setup result | |
| Logcat path | |
| Screenshot directory | |
| Backup artifact path | |

## Results

| ID | Scenario and exact steps | Expected | Actual | Result | Reproduction rate | Evidence | Code path / issue |
| --- | --- | --- | --- | --- | --- | --- | --- |
| RT-01 | Reset debug profile; launch | First-run UI, no release data | | | | | |
| RT-02 | Relaunch seeded debug profile | Library/queue/settings persist | | | | | |
| RT-03 | Search query, rapid replace, filter change, empty query, pagination | Latest query/filter only; deterministic error offline | | | | | |
| RT-04 | Select result; play/pause/seek/previous/next | Player state and queue stay synchronized | | | | | |
| RT-05 | Like/unlike; create/edit/reorder local playlist/folder | Library relations persist without duplicates | | | | | |
| RT-06 | Open lyrics for seeded track | Lyrics state is truthful | | | | | |
| RT-07 | Download fixture track; restart app | Completion and progress survive restart | | | | | |
| RT-08 | Disable network; play/seek/skip completed download | No network dependency or false offline claim | | | | | |
| RT-09 | Delete completed download | Cache/index/Room/UI availability removed | | | | | |
| RT-10 | Change non-secret setting; restart | Setting is consumed and persists | | | | | |
| RT-11 | ProcessDeath helper while queue is active | Queue order, index, position, and source restore | | | | | |
| RT-12 | Notification play/pause/next/previous | Correct metadata and service state | | | | | |
| RT-13 | UPI action, cancel at resolver | Correct external intent; no payment approval | | | | | |
| RT-14 | Backup Merge with disposable archive | Merge validates and preserves relations | | | | | |
| RT-15 | Backup Replace then recovery check | Safety backup exists; replace/rollback truthful | | | | | |

## Failures

For each failure, create an issue with the test ID, exact reproduction rate, a minimal logcat excerpt location, and the first relevant code path. Do not attach secrets or full personal backups.
