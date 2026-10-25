# Changelog

All notable changes to this project will be documented in this file.
The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

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
