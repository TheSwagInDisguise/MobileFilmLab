# Public testing and validation status

**Feature release:** `v0.2.0-beta.3` / code **12** / `com.mobilefilmlab.camera`.

The owner explicitly authorised the readability/Pro-slider update, retaining dynamic Camera2 lens controls and RAW/DNG formats, a new regular Latest GitHub Release after checks, an updated website and withdrawal of the previous binary downloads while preserving tags. The beta-version app remains a public-testing build, not universal hardware certification.

## Implemented scope

- Readable high-contrast Settings with persistent Done access; hardware-bounded EV/ISO/shutter/WB sliders, explicit Auto/metered states and supported discrete AF choices. [Pro controls and limits](PRO_CONTROLS.md).

- Existing photos-only AUTO/PRO, full 77-recipe upstream catalogue plus three baselines, custom recipes/favourites, JPEG editing and local gallery/interchange.
- Dynamic rear Camera2 photo routes with exact-ID selection and reported focal labels. Logical Auto routes are not additional physical lenses; firmware-hidden IDs are not promised.
- Capability-gated JPEG / RAW DNG / RAW+JPEG; RAW-only has no still-JPEG target. Sensor images are timestamp-matched to metadata and streamed to DNG without app edits. RAW modes are standard/native/uncropped 1×, not OEM/fusion/ZSL emulations.
- Camera-ID calibration preferences with legacy fallback; pending-file/rollback handling, retained RAW reader/image ownership and DNG-aware sharing/editor access.
- Same package/key, increased code, regression tests/lint, signed-APK identity/integrity verification and exact-tag publication approval.

## Evidence boundaries

| Area | Status |
|---|---|
| Prior primary-handset feedback | Alpha.9 worked well according to the owner. This is not a detailed report for the new RAW/session paths. |
| Automated suite / native UI renders / lint | Required before publication; see [beta.3 QA](QA_BETA_3.md) and tagged workflow results. |
| Sensor RAW capture and DNG validity | Must be verified on actual hardware/firmware in a RAW-capable editor. JVM storage tests use controlled payloads, not a simulated sensor. |
| Physical/logical route access | Advertised/discovered via public Camera2; opening and stream combinations remain firmware-dependent. |
| Per-lens measured colour calibration | Not completed. Identity/user JPEG gains are not a proprietary factory profile; RAW pixels are not graded. |
| Exact stock processing, maximum sensor mode, AI-free firmware | Not claimed. No app-added ML/generative enhancement; device processing remains outside that guarantee. |
| Long RAW write / lifecycle / low storage / heat | New lease, timeout and rollback logic is tested as software behaviour; real device stress testing remains open. |
| Other devices | Unverified until reports arrive. Primary target remains **Oppo Find X9 Ultra**. |
| Upgrade/user-data retention | Same identity and increased code support in-place upgrade. Confirm custom recipes, favourites, camera selection and calibration on device. |
| Independent security audit | Not performed. AI-assisted development remains explicitly disclosed. |

The release is public to collect this evidence, not because every hardware combination is known to work. [Testing guide](TESTING.md) · [Dynamic cameras / RAW](DYNAMIC_CAMERAS_AND_RAW.md) · [Known limitations](../KNOWN_LIMITATIONS.md).

Historical records: [alpha.9 gates](archive/ALPHA_9_BETA_GATES.md), [beta.1 QA](QA_BETA_1.md).
