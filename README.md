# MobileFilmLab

**Photos-only Android Camera2 app for the Oppo Find X9 Ultra.** Original 35mm film-canister branding, spacious AUTO/PRO interface and all **77 upstream Recipe Lab presets**, plus three independent Camera2 baseline profiles. GPL-3.0-or-later.

[![CI](https://github.com/TheSwagInDisguise/MobileFilmLab/actions/workflows/ci.yml/badge.svg)](https://github.com/TheSwagInDisguise/MobileFilmLab/actions)

## Current test release: `v0.1.0-alpha.9`

[Download the signed alpha](https://github.com/TheSwagInDisguise/MobileFilmLab/releases/tag/v0.1.0-alpha.9) · [Testing checklist](docs/ALPHA_9_TESTING.md) · [Beta readiness](docs/BETA_READINESS.md) · [Known limitations](KNOWN_LIMITATIONS.md)

**This is deliberately an alpha, not a beta.** Device testing and the owner's approval are required before promotion. Compiling successfully does not establish image-quality parity or confirm firmware-dependent features.

### Natural by default, honest about the stock camera

The factory default is **Oppo Natural (Camera2)**: auto exposure/white balance, no added film colour grade or grain. At 1× with default settings, the exposed Camera2/OEM JPEG is kept without decoding/recompressing its pixel data; capture/recipe EXIF is added. This is the closest unmodified ISP baseline available to this app, **not a copy of Oppo's proprietary stock-app or Hasselblad processing**. No guessed saturation/contrast offsets are presented as calibrated stock tuning.

Alpha.8's fixed-size enlargement was ordinary interpolation, **not super-resolution or recovered native sensor detail**. Alpha.9 defaults to the actual captured pixel dimensions. Optional enlargement is explicitly labelled. Native output may be smaller than the stock camera's output; no package spoofing or private-API bypass is used.

## Features available for alpha testing

- **Dynamic lens discovery/routing:** directly exposed rear IDs and physical output IDs behind logical cameras. Fixed translucent `UW`, `W`, `3x`, `10x` slots, with unavailable slots disabled. Slot names are navigation labels, **not proof of optical magnification**; the badge shows detected approximate 35mm-equivalent focal length.
- **Four framing levels per lens:** short-tap `2×` toggles 1×/2×; hold it for 1×, 1.5×, 2× or 3×. Preview zoom is not applied a second time to the saved photo. Native crop dimensions are preserved.
- **AUTO / PRO:** hardware-bounded ISO, shutter, EV, approximate Kelvin WB and AF modes where exposed. AUTO clears manual exposure/WB. When only ISO or shutter is specified, its companion is taken from the most recent result, not a promised shutter/ISO-priority algorithm.
- **Flash:** Off → Auto → On → Torch, supported modes only, bounded AE precapture metering. EXIF comes from the result/JPEG, never from the selected icon. Firmware behaviour needs physical testing.
- **Tap focus/metering:** touch coordinates account for sensor rotation, centre-fill and the active crop; only supported region types are requested.
- **Live colour grading:** one shared matrix for the native viewfinder layer, periodically refreshed recipe tiles and saved JPEG grading. Split before/after drag comparison. Final sharpening, grain, OEM tone processing and recipe approximations can differ from the live colour preview.
- **77 preserved upstream recipes across 12 original groups**, plus Natural/Vivid/B&W Camera2 baselines. Up to six starred quick-switch favourites, Tone/Warmth square, persistent custom presets, favourite and deletion controls.
- **Safer image processing:** bounded, ISO-aware luminance sharpening with halo limiting and row buffers. Optional fine/medium/coarse procedural luminance grain, off by default. No neural enhancement or measured detail-recovery claim.
- **Experimental 3-frame fusion:** −1/0/+1 EV requests, timestamp-matched JPEG/results, translation registration, exposure normalization and per-pixel motion rejection. Approximately 12MP input cap; poor registration uses the middle frame. Not RAW fusion, a neural HDR engine, or a guaranteed noise/detail improvement.
- **OEM HDR/Night:** real `CameraExtensionSession` only when advertised for the openable camera. Unsupported/failed extension sessions report a standard Camera2 fallback. These modes own exposure/flash and do not claim the same processing as the stock app.
- **Experimental reprocessing ZSL:** full-resolution PRIVATE input/output ring and `ImageWriter` reprocess request where the firmware supports the stream combination. Off by default. Only fresh, converged frames are considered; otherwise normal still capture is used. No guaranteed zero lag.
- **Composition tools:** sensor-driven level with lifecycle cleanup, thirds/golden-ratio/square guides and Off. Square is a framing guide, not a saved 1:1 crop. Capture rotation follows physical device orientation even though the UI remains portrait-oriented.
- **Gallery:** the app's saved photos, swiping, pinch/double-tap zoom, memory-bounded higher-resolution decoding, actual dimensions/EXIF/processing notes, Android sharing and confirmed deletion. No fabricated 28.1MP labels.
- **Interchange:** versioned `.filmrecipe` JSON and QR PNG export; JSON and QR-image import through Android's document picker. Bounded input sizes, parameter validation, new custom IDs and transactional imports protect built-ins.
- **Hardware shutter and haptics:** volume/camera keys trigger once, not on held-key repeats, and not while editing presets/in a dialog/gallery. Haptics can be disabled.
- **Release checks:** explicit user request only, alpha/stable channel, semantic version comparison, no silent downloads/installation. Beta/RC are intentionally excluded from the alpha updater.

## Privacy & storage

No analytics, advertising, crash telemetry or photo uploads. Camera permission is needed for capture; vibration is optional. Internet permission is used **only for an explicit GitHub release check**. Android 9 additionally requests legacy storage access; Android 10+ uses scoped MediaStore storage in `Pictures/MobileFilmLab/` without broad photo-library permission. User-selected recipe documents and QR images are read locally. See [privacy details](docs/PRIVACY.md).

## Build & verify

JDK 17 (recommended), Android SDK/platform/build tools 34, Gradle wrapper 8.7. Minimum Android 9/API 28. Direct APK distribution; no Play Store compatibility/certification claim.

```sh
./gradlew testDebugUnitTest --no-daemon
./gradlew lintRelease --no-daemon
./gradlew assembleDebug --no-daemon
./gradlew assembleRelease --no-daemon
```

Release signing uses repository secrets, never committed key material. The public certificate fingerprint is documented in [release signing](docs/RELEASE_SIGNING.md). The workflow checks tag/version, tests, lint, package ID, alignment and signature, then publishes APK + SHA-256 + test checklist as a GitHub **prerelease**. Non-alpha publication is guarded pending explicit approval. An F-Droid metadata starter is included; this is **not** an F-Droid submission or availability claim.

## Attribution

The 77 recipe definitions are derived from [voxivoid/recipe-lab-sony-pmca](https://github.com/voxivoid/recipe-lab-sony-pmca), commit `aab4b5703a2b77362b83682801c8e50145ddc6ee`. Parameter definitions are preserved; Android rendering is an independent approximation, not access to those cameras' colour registers. See [attribution](UPSTREAM_ATTRIBUTION.md), [third-party notices](THIRD_PARTY_NOTICES.md) and [GPL-3.0-or-later licence](LICENSE).

Not affiliated with or endorsed by Oppo, Sony, Fujifilm, Kodak, Leica, Ricoh or Hasselblad. No vendor camera art or copied upstream icons.
