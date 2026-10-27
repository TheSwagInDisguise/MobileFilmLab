# Alpha.9 automated QA record

Candidate: `0.1.0-alpha.9`, versionCode `9`, release package `com.mobilefilmlab.camera`.

## Verified locally

- **54 tests passed, 0 failures**, covering the actual production selector/crop/orientation/alignment/pixel filters, all 80 recipe entries (77 upstream + 3 baselines), serialization validation, semantic version comparison, QR payloads and real QR-image round trips.
- Robolectric API 34 **native graphics** tests cover decoded-pixel identity of the default original-JPEG path, actual EXIF lens/EV/flash/APEX aperture, crop applied once, monochrome output, real three-frame fusion execution, per-lens settings persistence, safe custom-ID import and favourite persistence.
- Activity/Compose creation and lifecycle teardown smoke test passes without camera hardware. It exposed an unavailable-thermal-service startup case; service registration/removal is now guarded rather than crashing.
- Kotlin compilation succeeds with Java 17 / Android SDK 34.
- Source tag/package/release-note validation and workflow YAML parsing pass.

## Release pipeline gates

The published release workflow is the source of truth for tests, release lint, signed APK assembly, package/version/alignment/certificate verification and attached SHA-256. CI reports are uploaded separately from the APK. A release is not declared complete until that workflow succeeds and its assets are verified.

## Not verified by these tests

No physical Oppo device is connected to the build environment. Camera capture, real flash/AF, lens blocking/routing, live TextureView colour/rotation, OEM HDR/Night support, ZSL latency, real fusion quality/ghosting, controlled per-lens stock matching, power/thermal soak and handset upgrade retention require the [phone checklist](ALPHA_9_TESTING.md).

These tests do not constitute stock-camera parity, Android/device CTS certification, a guarantee of zero crashes/lag, or permission to publish a beta. The [beta gates](BETA_READINESS.md) remain pending.
