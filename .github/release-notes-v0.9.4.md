# OmniTune v0.9.4

Hotfix release addressing crashes and broken options found in v0.9.3.

### Fixes
* **Queue crash fixed:** Tapping the Queue button (three lines) in the fullscreen player no longer closes the app. The `queue` navigation route was missing from the nav graph — now properly registered.
* **Player options bottom sheet wired:** The three-dot (⋮) button in the fullscreen player now correctly opens the options sheet (Start radio, Add to playlist, Copy link, Listen Together). Previously the sheet existed but was never triggered.
* **Listen Together: duplicate options removed:** The "Allow guests to control playback" and "Require host approval to join" toggles were duplicated in Settings → Listen Together. The broken stub copies (which did not call `updateSettings`) have been removed.
* **Listen Together: double toast fixed:** Joining a session no longer fires the "Connecting to session..." toast twice.
* **Listen Together: Copy link wired:** Tapping "Copy link" in the player options now correctly copies the YouTube Music URL for the currently playing track to clipboard.
* **Listen Together navigation wired:** Tapping "Listen Together" in the player options now correctly navigates to the Listen Together settings screen.
