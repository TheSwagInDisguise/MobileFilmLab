# Changelog

All notable changes to this project will be documented in this file.
The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## Unpublished 0.2.0-beta.5 HDR test candidate (14)

- Handheld Smart HDR becomes the Fold5 JPEG default once; Original remains selectable, existing other pipeline choices retained. Other devices keep their default. RAW/RAW+JPEG remain single-exposure native paths.
- Added a quality-first static-scene preset, metered safety-frame fallback, constant highlight-protecting short exposures, optional long shadow frames, actual metadata/capability/thermal/flash/manual guards and progress/result diagnostics.
- Original conventional JPEG-domain processing: sharp early reference, local fractional tile registration, ISO-aware robust RGB temporal merge, capped long-frame weights and restrained hue-preserving local tone. One output bitmap plus decoded strips bounds allocations; no Google code, learned model, RAW HDR+ parity or measured handset quality claim.
- Corrected fractional shutter EXIF writing for newly encoded JPEGs; empty/transparent region output cannot masquerade as HDR. Native fixture tests use an explicitly test-only codec adapter because Robolectric does not decode real regions; handset codec/camera validation remains pending.
- Optional metered Original companion for private A/B feedback; successful HDR avoids double Balanced filtering. Same package/key, test artifact only. Public beta.4/site/approval variables/tags stay unchanged. See [candidate/rollback guide](docs/HDR_TEST_CANDIDATE.md).

## [0.2.0-beta.4]

- Added Samsung SM-F946* compatibility policy: prefer actual, normal-mode ~12.6MP-budget JPEG streams; maximum advertised native size is opt-in within the 55MP ceiling. Bounded session fallbacks no longer retry larger buffers. Other devices retain their existing native preference.
- Added optional conventional balanced JPEG rendering: ISO-aware edge-preserving luma/chroma denoise, mild high-contrast tone balancing and restrained low-ISO detail. Original rendering stays the default; no forced vendor colour bias, model enhancement or invented detail.
- Kept Natural recipe IDs/catalogue/custom names; Fold5 uses a truthful Camera2 display alias. RAW-only disables JPEG rendering and RAW+JPEG affects only the JPEG; untouched sensor DNG is retained.
- Made all manual bracket frames use a frozen metered baseline and consistent bounded/physical-key request settings; compensation routes briefly await reported AE settling. Fusion requires actual exposure metadata, uses IEC sRGB transfer, retains highlight headroom until tone mapping and rejects RGB as well as luma motion.
- Reused the fusion middle bitmap, added scanline-bounded filtering and skipped full-image identity grading. These are allocation safeguards, not measured handset speed/battery claims.
- Added resize-aware centred portrait controls for a wide inner display, narrow/wide native illustrative UI renders and genuine numerical/policy/JPEG/HDR/motion/persistence regressions.
- Same package/key, versionCode 13; owner-authorised regular Latest after automated gates. Real Fold5 image quality, firmware/RAW and fold transitions remain unverified; no samples/report were supplied. Update/verify the APK and website before withdrawing beta.3 binaries; preserve tags/history.

## Documentation update — 2026-11-04

- Added a use-at-your-own-risk, warranty and liability disclaimer to the README, public testing documentation and website, explicitly covering bricked devices, other device damage and data loss, with applicable-law/non-excludable-rights qualifications.
- The GPL licence, beta.3 APK bytes, version/code, signing identity and release tags are unchanged. This is a documentation/website update, not a new app release or an in-app notice change.

## [0.2.0-beta.3] - 2026-10-30

- Fixed inherited black-on-dark text with a light theme content default and correctly scoped Settings Surface; brighter body/helper tokens and larger, wrapping settings labels.
- Settings now has a scrolling body with persistent title/Done access, including at increased font scales.
- Replaced repeated-tap Pro cycling with one-active-editor EV, ISO, shutter and white-balance sliders, explicit Auto/metered states and direct supported AF choices.
- EV snaps to reported steps; ISO/time use logarithmic scales and current-route bounds. Shutter obeys exposure/frame limits and a 30-second app cap, with a corresponding long-exposure deadline.
- Re-bound controls on camera changes; clearly distinguish a metered-and-held companion axis from genuine priority-auto exposure. Retained real RAW/DNG, dynamic exact-ID cameras, recipe catalogue and user-data compatibility.
- Added slider interaction, capability/Auto state, control mapping, camera-switch, contrast and large-font settings tests, plus an actual native Settings render. 116 Android tests pass locally; release lint and six approval tests are publication gates.
- Same package/key, versionCode 12. Owner-authorised regular Latest release; update/verify the replacement and website before withdrawing beta.2 binary downloads, preserving all tags/history.

## [0.2.0-beta.2] - 2026-10-27

- Replaced four fixed rear lens slots with all exposed photo-capable rear Camera2 routes, exact-ID selection, detected focal labels and explicit logical Auto labels.
- Added capability-gated JPEG only, real sensor RAW DNG only, and timestamp-matched RAW + JPEG. RAW-only requests no still JPEG.
- RAW uses standard/native/uncropped 1× capture, with recipes/grain/calibration affecting JPEG only. No OEM/fusion/ZSL RAW emulation or JPEG-only fallback.
- Added retained RAW-image lifetime across session close, direct DNG streaming, same-stem pairs, pending rows/rollback, DNG gallery metadata and correct external-view/share MIME.
- Saved per-camera calibration/selection and format with legacy gain fallback; full recipe catalogue and user data preserved.
- New policy, frame-matching, lifecycle ownership, preference, storage and native UI tests. Real sensor/DNG/firmware validation remains open; see [testing](docs/TESTING.md).
- Same package/key, versionCode 11. Owner-authorised regular Latest release; retire prior binary downloads only after the new APK and website are verified, keeping source tags/history.

## Project publication update — 2026-10-27

- Promoted the existing `v0.2.0-beta.1` entry to a regular **Latest GitHub Release** by owner request. No app version/code, APK bytes, tag or signing key changed; no new stable-version build was created.
- Withdrew nine previous release entries/assets and nine old signed-APK workflow artifacts. Kept all version tags and source history as requested; external copies cannot be revoked.
- Added the GitHub Pages showcase with native UI renders, downloads, compatibility information, privacy and explicit processing/development disclosures.
- Prominent natural-capture selling point: no generative/ML enhancement or cloud processing added by the app. Clearly distinguish Camera2/ISP JPEG output from RAW/DNG and unknown proprietary firmware processing.

## [0.2.0-beta.1] - 2026-10-27

### First public-testing beta
- Owner-authorised beta following positive initial alpha.9 handset feedback; still a prerelease, not stable or universally hardware-validated.
- Primary focus remains **Oppo Find X9 Ultra**; invite beta testers to help expand device compatibility.
- Concise, general-feature README without camera/film-brand feature lists; upstream author thanks and complete attribution/licensing retained.
- Clear AI-assisted/generated-development disclaimer in repository and About; truthful Android/Compose UI renders with a labelled generated demo scene.
- Public test guide, device/bug issue forms, documentation index and clearly marked historical design/readiness records.
- Beta/stable manual update channel with semantic selection and exact official release/asset matching; no silent downloads.
- Exact-tag release approval, beta-aware immutable prerelease titles/assets, additional release-guard/update-channel/UI-render tests.
- Package and existing signing certificate unchanged; versionCode **10** supports updating code 9.

### Scope and limitations
- Retains alpha.9's working capture/processing behaviour; no new proprietary tuning or image-quality parity claim.
- Firmware-dependent lens/extension access, experimental fusion/ZSL, measured per-lens calibration, broad stress tests and other-device compatibility remain ongoing testing, not silently completed checks.
- See [public testing](docs/TESTING.md), [validation status](docs/BETA_READINESS.md), [known limitations](KNOWN_LIMITATIONS.md) and [AI disclosure](docs/AI_DISCLOSURE.md).

## [0.1.0-alpha.9] - 2026-10-27

### Added / changed
- Beta-roadmap implementation released deliberately as an **alpha test candidate**, with handset acceptance gates and an explicit beta-publication guard.
- Neutral **Oppo Natural (Camera2)** factory default, original-JPEG pixel preservation when unedited, and three Camera2 baselines alongside all 77 upstream recipes. Per-lens user RGB matching controls (identity defaults, not a measured factory calibration).
- Native output dimensions by default; labelled optional interpolation; corrected double-cropping and unnecessary zoom multiplication on selected physical lenses. Four crop levels per lens via long-press menu.
- Edge-adaptive ISO-aware luminance sharpening with bounded halos/row buffers; opt-in procedural film grain.
- Experimental three-JPEG bracket fusion with translation registration, exposure normalization and motion rejection; capability-gated OEM HDR/Night extension sessions and PRIVATE reprocessing ZSL with visible fallback.
- Tap AF/AE mapping, bounded 3A/capture deadlines, volume-key single-shot/repeat handling, sensor-driven level, optional composition grids, device-aware capture orientation and haptic settings.
- Shared live colour grading, refreshed recipe thumbnails, favourite quick-switch carousel and sampled split comparison; file/QR recipe exchange with validation and transactional imports.
- Swipe/zoom in-app gallery with actual metadata/dimensions/processing notes, sharing and confirmed deletion.
- Handler-confined camera lifecycle, timestamp-paired metadata, off-UI processing/storage, partial-save cleanup, thermal safeguards and errors instead of synthetic blank photos.
- Tests of production functions plus native-graphics EXIF/bitmap/QR/SQLite tests, lint gates, verified signing/package/version/checksums, explicit alpha/stable updater and F-Droid metadata starter.

### Corrections and limits
- Alpha.8's enlargement was a bitmap interpolation, **not super-resolution, a HAL restriction bypass, or newly recovered detail**. Alpha.9 does not claim guaranteed 28MP native access.
- Exact Oppo/Hasselblad stock tuning, sensor/AI pipeline internals, fixed SNR/sharpness improvements and zero-OOM/zero-lag guarantees were not established. JPEG quality 100 is not lossless. Earlier historical notes below must be read with these corrections.
- Hardware support/quality, real lens calibration, flash, preview correctness, ZSL latency and thermal/battery behaviour remain pending handset testing. Square is guide-only; QR import reads an image; RAW/DNG/video are not implemented.
- See `docs/ALPHA_9_TESTING.md`, `docs/BETA_READINESS.md` and `KNOWN_LIMITATIONS.md` before promotion.

## [0.1.0-alpha.8] - 2026-10-26

### Added
- **Fixed-size 28.1MP interpolated export (historical; corrected in alpha.9)**:
  - Overcomes the ColorOS Camera2 HAL package whitelist restriction (`camera.provider@2.x-service`) which limits non-whitelisted third-party applications to 12.5MP binned streams (`3072 × 4096`).
  - Implemented high-order super-resolution reconstruction in `ImageProcessor.kt` that processes the incoming frame and upscales/reconstructs it to the exact stock Oppo Find X9 Ultra resolution of **`4596 × 6120` (28.1 MP)**.
  - Generates 100% quality JPEGs (~10.6 MB) with 28.1MP pixel dimensions, matching the stock ColorOS camera output pixel-for-pixel.
  - Honest 2x crop toggle produces **`2298 × 3060` (7.0 MP)**, preserving genuine sensor crop geometry.
- **Complete Flash System Overhaul (`OFF` -> `AUTO` -> `ON` -> `TORCH`)**:
  - Added dedicated `FlashMode.ON` (Always Flash) to UI toggle, ViewModel, and CameraEngine.
  - Fixed conflict where `FLASH_MODE_OFF` previously suppressed hardware strobe triggers during Auto/Flash capture on Qualcomm HAL.
  - Implemented torch-assist pre-illumination sequence: when `FlashMode.ON` is active, the camera hardware turns on the torch LED to illuminate the subject, meters auto-exposure, executes still capture with `CONTROL_AE_MODE_ON_ALWAYS_FLASH` and `FLASH_MODE_SINGLE`, and resets cleanly after exposure.
  - Continuous `TORCH` mode immediately powers the flashlight in both live preview and capture.
- **Stock ColorOS / Google Photos Lens Metadata Matching**:
  - Populated standard EXIF `LensModel` (`TAG_LENS_MODEL`) with device-matched strings (e.g. `OPPO Find X9 Ultra wide camera 23mm f/1.5`).
  - Google Photos and ColorOS Photos now accurately recognize the lens and display `Wide-angle camera`, `Ultra wide-angle camera`, `Telephoto camera`, or `Periscope telephoto camera` instead of `No lens information`.
  - Added EXIF `LensMake` (`TAG_LENS_MAKE` = `OPPO`) and `LensSpecification` (`TAG_LENS_SPECIFICATION`).
  - Formatted `ExposureBiasValue` as a valid signed rational (`0/1` or `n/10`), enabling Google Photos to display `ev 0.0` in the shooting parameters pill.
  - Recorded accurate flash firing state in EXIF (`TAG_FLASH`).

## [0.1.0-alpha.7] - 2026-10-25

### Added
- **28.1MP High-Resolution Stream Discovery**: Added comprehensive inspection of `StreamConfigurationMap.getHighResolutionOutputSizes(ImageFormat.JPEG)`. Android CTS requires all frame sizes that cannot sustain 20 fps burst rates (such as 28MP and 50MP) to be reported via `getHighResolutionOutputSizes()`, which prevented earlier versions from discovering the 6120×4596 stream.
- **Stock-Matched 28MP (4596×6120) Portrait Output**: Calibrated ImageReader allocation and sensor orientation rotation matrix to match Oppo Find X9 Ultra's stock camera output pixel-for-pixel (4596×6120 @ 100% quality, ~10.6 MB).
- **SessionConfiguration & OutputConfiguration Pipeline**: Upgraded capture session lifecycle to `android.hardware.camera2.params.SessionConfiguration` with explicit `OutputConfiguration` targets, supporting `addSensorPixelModeUsed(SENSOR_PIXEL_MODE_MAXIMUM_RESOLUTION)` without session stalling or fallback.
- **Complete Hardware EXIF Metadata**: Fixed missing lens and exposure indicators in system gallery viewers (ColorOS Photos / Google Photos). The captured JPEG now records:
  - Lens model name matching stock convention (`Wide-angle camera`, `Ultra wide-angle camera`, `Telephoto camera`, `Periscope telephoto camera`).
  - Hardware ISO sensitivity (`TAG_PHOTOGRAPHIC_SENSITIVITY`).
  - Exact exposure duration in seconds (`TAG_EXPOSURE_TIME`, e.g. `1/35 s`).
  - True lens aperture (`TAG_F_NUMBER`, e.g. `f/1.5`).
  - 35mm equivalent focal length (`TAG_FOCAL_LENGTH_IN_35MM_FILM`, e.g. `23 mm` or `46 mm` on 2x crop).
  - Exposure compensation value (`TAG_EXPOSURE_BIAS_VALUE`).
  - Standardized EXIF capture timestamp (`TAG_DATETIME_ORIGINAL`).
- **Memory Optimization for 28MP / 50MP Processing**: Added `android:largeHeap="true"` to `AndroidManifest.xml` and converted the photographic unsharp mask convolution to in-place scanline row caching, cutting memory consumption in half and ensuring zero OOM crashes on full-resolution 28MP/50MP captures.

## [0.1.0-alpha.6] - 2026-10-25

### Added
- **Full-Resolution Sensor Output**: Enabled `SCALER_STREAM_CONFIGURATION_MAP_MAXIMUM_RESOLUTION` and `SENSOR_PIXEL_MODE_MAXIMUM_RESOLUTION` on Android 12+ (Oppo Find X9 Ultra) to capture full Quad-Bayer remosaic sensor resolution (28MP / 50MP) instead of 12.5MP binned mode.
- **Hardware ISP High-Quality Pipeline**: Configured Camera2 capture requests with `EDGE_MODE_HIGH_QUALITY`, `NOISE_REDUCTION_MODE_HIGH_QUALITY`, `COLOR_CORRECTION_ABERRATION_MODE_HIGH_QUALITY`, `SHADING_MODE_HIGH_QUALITY`, and `DISTORTION_CORRECTION_MODE_HIGH_QUALITY`.
- **Photographic Unsharp Mask Engine**: Implemented fast photographic unsharp mask convolution in `ImageProcessor.kt` directly wired to the recipe's sharpness parameter (`recipe.sharp`), restoring crisp edge definition and tile/texture micro-contrast.
- **Quality-100 JPEG encoding** (still lossy; the earlier lossless description was incorrect).

## [0.1.0-alpha.5] - 2026-10-25

### Added
- **Full Interactive Pro Controls**: Added interactive parameter chips in `ManualControlStrip.kt` allowing users to tap and cycle EV compensation, ISO, Shutter Speed, White Balance Kelvin, and Autofocus mode (AF-C, AF-S, Macro) in real time.
- **Interactive Flash & Torch Mode**: Added multi-state flash toggle (Off / Auto / Torch) in `MainCameraScreen.kt` commanding Camera2 HAL.
- **Real Photo Gallery Thumbnail**: Integrated dynamic thumbnail preview in bottom-left capture deck that updates with the latest captured photo and launches the system gallery viewer on tap.
- **Live Viewfinder Film Simulation**: Added real-time non-destructive tone and warmth grading overlay in `CameraViewfinder.kt` matching active recipe parameters.
- **Tactile Shutter Flash**: Added visual shutter pulse feedback animation upon photo capture.
- **Stationary Ergonomic Lens Bar**: Positioned `UW`, `W`, `3x`, `10x` in fixed stationary slots with a dedicated `2×` toggle pill to prevent button jumping.
- **Favourites Category & Custom Name Dialog**: Added `★ Favourites` filter category and a custom name input dialog when saving recipes in `RecipeBottomSheet.kt`.

## [0.1.0-alpha.4] - 2026-10-25

### Fixed
- **2D Photographic Style Square Pad Touch Tracking**: Fixed touch/drag handling in `PhotographicStylesSquarePad.kt` by migrating from relative `detectDragGestures` to absolute pointer coordinate tracking with `awaitEachGesture`.
- **Persistent Puck Positioning**: Resolved issue where the tuning puck would snap back to the center upon release; the puck now tracks finger touch directly anywhere on the 2D coordinate plane and maintains its position persistently.
- **Direct Tap-to-Position**: Added instant tap-to-reposition support allowing users to tap anywhere on the Tone/Warmth plane to position the puck immediately.

## [0.1.0-alpha.3] - 2026-10-25

### Fixed
- **Viewfinder Orientation Calibration**: Corrected `configureTransform()` matrix in `CameraViewfinder.kt` by removing erroneous 90° clockwise sensor orientation rotation in standard portrait mode (`Surface.ROTATION_0`), ensuring the viewfinder is naturally upright and aligned with gravity.
- **Dynamic Display Rotation Handling**: Integrated display-relative transform math to support seamless portrait, landscape-left, landscape-right, and reverse portrait orientations.

## [0.1.0-alpha.2] - 2026-10-25

### Fixed
- **Camera2 Live Viewfinder Stream**: Connected `TextureView` with `SurfaceTextureListener` and `setDefaultBufferSize` in `CameraViewfinder.kt` so real-time sensor frames stream to the screen immediately upon permission grant.
- **Hardware Camera Session Lifecycle**: Implemented complete Camera2 `openCamera`, `createCaptureSession`, and repeating preview request loop in `CameraEngine.kt`.
- **Dynamic Multi-Camera Zoom Mapping**: Added seamless physical/logical camera switching with `CONTROL_ZOOM_RATIO` fallback for ColorOS and Oppo Find X9 Ultra multi-camera subsystems.
- **Native Still Capture**: Connected `ImageReader` in `CameraEngine` for full-resolution sensor JPEG capture with EXIF metadata generation.
- **Upright Viewfinder Matrix**: Applied aspect-ratio preserving center-crop transformation and sensor-orientation rotation matrix to `TextureView`.

## [0.1.0-alpha.1] - 2026-10-25

### Added
- **Core Architecture:**
  - Modern Kotlin & Jetpack Compose camera architecture targeting Oppo Find X9 Ultra.
  - Camera2 dynamic multi-camera discovery supporting all 4 rear optics (UW, Wide, 3x Tele, 10x Periscope).
  - Clean Auto Mode and Pro Mode operational paradigms with expansive, breathable visual hierarchy.
  - Camera Diagnostics screen providing full hardware capability reports.
- **Recipe Lab Preset System:**
  - Complete 77-preset Recipe Lab catalogue ported across 12 categories: Sony, Fuji Sim, Fuji Film, Kodak, Cine, Ricoh GR, Leica, Hasselblad, Canon / Nikon, Pana / Olympus, Other Stocks, and Ilford.
  - Honest computational color approximation pipeline: tone curves, saturation, Kelvin WB shift, amber/green tinting, DRO shadow boost, and monochrome optical filters.
  - 2D Photographic Styles square pad for tactile real-time color and contrast modulation.
  - Custom recipe creation, editing, favoriting, and persistent SQLite storage.
  - Full compatibility with upstream Recipe Lab YAML Format 1 custom recipe files and modern JSON export.
- **Framing & Honest Crop Engine:**
  - Translucent floating lens controls (`UW`, `W`, `3x`, `10x`) with adjacent `2x` crop toggle.
  - Honest status telemetry badges (`Full sensor`, `Native-pixel crop / no-resample`, `Reduced-resolution crop`, `Resampled digital crop`).
- **Brand Identity:**
  - Refined 35mm Analog Film Canister logo with brass spool top, perforated leader, and adaptive icon support.
- **Release Automation:**
  - Automated GitHub Actions CI workflow (formatting, lint, unit tests).
  - Automated release workflow with RSA 4096-bit keystore signing, APK verification, and checksum generation.
