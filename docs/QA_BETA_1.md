# Beta.1 QA evidence and boundaries

**Build:** `0.2.0-beta.1` / code 10 / `com.mobilefilmlab.camera`.

## Local pre-publication checks — 2026-10-27

- **64 Android tests passed**, zero failures/errors/skips (14 test classes), including actual native UI rendering.
- **6 Python release-approval tests passed**; exact beta approval does not authorise another beta or stable tag.
- **Android release lint passed**; warnings are not a hardware-support or audit claim.
- Local documentation-link/image check passed; no missing gallery assets.
- Confirmed the camera engine, capture/processing pipeline and full built-in recipe catalogue are unchanged from published alpha.9.
- Public-beta APK assembly/signing remains the responsibility of the secret-isolated GitHub release job, not an unsigned local build.

## Required automated release evidence

The main-branch CI and signed-release workflow associated with this source/tag are the authoritative build records. Publication requires:

1. Source/package/version/release-notes identity, Python exact-tag approval tests and local documentation-link checks.
2. Android JVM/native-graphics regression suite, including image/EXIF/QR/database/crop/orientation/lifecycle tests, beta-channel update selection and actual UI documentation renders.
3. Android lint and a signed release build using the established secret-managed key.
4. Verification of APK package, versionName/code, zipalignment and certificate SHA-256; checksum generation.
5. A new immutable GitHub prerelease with signed APK, checksum and public testing guide. No replacement of an existing published release.
6. Downloaded-release checksum/signature/identity verification after publication.

[Continuous integration](https://github.com/TheSwagInDisguise/MobileFilmLab/actions/workflows/ci.yml) · [Signed-release workflow](https://github.com/TheSwagInDisguise/MobileFilmLab/actions/workflows/release.yml)

Passing these checks is **not hardware certification**. The UI renders exercise real components but use a generated demo scene/illustrative camera state, not physical-camera capture. [Provenance](SCREENSHOTS.md).

## Handset evidence

The owner reported alpha.9 working well and approved the first public beta. No detailed per-lens, per-mode, long-soak or thermal results were supplied as part of that feedback. Beta.1 retains that capture implementation rather than making unvalidated camera-engine changes during documentation polish.

Broad-device compatibility, measured per-lens calibration, experimental-pipeline quality, full stress testing and actual upgrade data-retention checks remain open. See [validation status](BETA_READINESS.md), [known limitations](../KNOWN_LIMITATIONS.md) and the [public test checklist](TESTING.md).
