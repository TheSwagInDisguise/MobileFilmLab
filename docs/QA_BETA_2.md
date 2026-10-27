# Beta.2 validation boundaries

**Version:** `0.2.0-beta.2` / code 11 / `com.mobilefilmlab.camera`.

## Local pre-publication evidence — 2026-10-27

- **95 Android tests passed**, zero failures/errors/skips across 20 classes.
- Android release lint passed.
- Four actual native UI renders generated: AUTO, PRO, recipes and format selection; simulated sensor capabilities remain explicitly disclosed.
- Robolectric file-descriptor provider tests use a JDK 17 test-worker-only java.io module opening; no runtime Android permission or app module access is changed.
- Signed build and downloaded-APK identity/signature verification remain mandatory in the isolated release workflow before publication.

## Automated checks

The release requires source/tag identity, exact owner approval, unit/native-graphics tests, release lint, signed build, package/version/certificate/alignment verification and a downloaded-APK checksum check. Existing recipe, colour/crop/orientation, metadata, QR, database, lifecycle and update-channel regression tests remain in the suite.

New tests exercise production logic for:

- Dynamic rear route enumeration policy, exact IDs, logical labels, same-category cameras and missing focal metadata.
- JPEG/RAW/paired file choices, capability compatibility, standard/native RAW restrictions and crop gating.
- Camera-ID preference separation, legacy calibration fallback and saved format/ID.
- RAW/result/optional-JPEG timestamp matching rather than arrival-order pairing.
- Retained-image/reader lifetime, duplicate close and concurrent session retirement/writer release.
- DNG-only output plan, same-stem paired files, pending flags, and cleanup after JPEG processing/output/publication failures.
- Actual Android/Compose AUTO, PRO, recipe and format-dialog renders with explicitly illustrative camera capabilities.

The MediaStore tests use controlled byte payloads to test storage semantics; **they do not encode or validate a real sensor DNG**. UI fixtures similarly do not test a physical camera. Do not treat their success as proof of RAW support on a particular firmware.

## Handset checks still required

No real handset/sensor is connected to this development environment. DNG creation from actual `RAW_SENSOR` buffers, physical capture-result completeness, valid rendering in RAW editors, orientation, flash/manual operation in RAW sessions, memory pressure, lifecycle during large writes and per-lens stream combinations need actual-device reports.

A Camera2 capability flag means advertised support, not an app-certified working combination. Rejected RAW requests must report failure, not manufacture a substitute. JPEG-only remains the compatibility default.

[Public checklist](TESTING.md) · [Dynamic cameras and RAW](DYNAMIC_CAMERAS_AND_RAW.md) · [Known limitations](../KNOWN_LIMITATIONS.md)
