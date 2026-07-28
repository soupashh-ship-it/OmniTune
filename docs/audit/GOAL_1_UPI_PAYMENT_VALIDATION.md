# Goal 1 — UPI payment contract validation

**Date:** 2026-07-28
**Scope:** deterministic UPI URI construction, honest Android launch semantics, and release-gate recovery.

## Implemented

- Replaced Android `Uri.Builder` use in the JVM-tested path with a pure Kotlin serializer.
- Added fixed-reference test injection and UUID-derived production transaction references.
- Defined strict, locale-independent positive INR amount parsing and two-decimal formatting.
- Percent-encode query values once, including Unicode and reserved characters.
- Reject invalid VPA/payee/reference and invalid or non-positive amounts deliberately.
- Treat only an accepted Android activity launch as a launch; never represent it as payment success or completion.
- Add no-handler/invalid-request results and a clipboard manual-pay fallback.
- State the external-payment limitation directly in the About UI.

## Automated verification

| Command | Result |
| --- | --- |
| `:app:testDebugUnitTest --tests com.omnitune.app.ui.screens.settings.AboutMetadataTest` | Passed |
| `:app:testDebugUnitTest :app:lintDebug :app:lintRelease :app:compileDebugAndroidTestKotlin :app:assembleDebug :app:assembleRelease --no-daemon --max-workers=1 --console=plain` | Passed (final run: 8m 05s) |

The UPI tests cover deterministic serialization, payee/note/VPA encoding, Unicode/reserved characters, decimal formatting, omitted amount behaviour, invalid/empty/non-positive amounts, invalid destinations/references, and missing-handler classification.

## Device evidence and limitation

- Device `138898743000055` (I2202) accepted a debug update through `adb install -r`; data was retained.
- The About support card shows the amount controls, payment action, manual-copy action, and an explicit statement that OmniTune cannot confirm payment completion. See `device-qa/goal1-upi-fallback.png`.
- The generated URI opened Android's UPI resolver, which listed Navi, WhatsApp, Amazon Pay UPI, GPay, BHIM, and FamApp.
- GPay requires the device's encrypted-app fingerprint gate. BHIM blocks UPI use while USB is connected. No authentication or payment confirmation was attempted.
- At the user's direction, no further USB/device testing was performed. A later non-USB validation must confirm the exact payee, VPA, amount, currency, note, and transaction reference in an enabled UPI app and exercise a physical no-handler fallback.

## Result

The CI-blocking UPI test failure is resolved, and the app's launch/cancellation messaging is truthful. Release signing and the remaining external-app-detail test stay outside this local validation because signing material is unavailable and USB-gated UPI apps cannot be safely inspected.
