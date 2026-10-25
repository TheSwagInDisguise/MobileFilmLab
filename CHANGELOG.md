# Changelog

All notable changes to this project will be documented in this file.
The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

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
- **Lossless Quality 100 JPEG Encoding**: Disabled secondary lossy Skia chroma quantization tables, preserving fine high-frequency sensor detail.

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
