# MobileFilmLab — native iOS companion (beta 2 · cleaner camera UI)

Native SwiftUI + AVFoundation + Core Image, alongside the existing native Android app. Target handset: **iPhone 18 Pro Max, public iOS 27**. Deployment floor is iOS 18 because the implementation uses established public APIs; that is not a tested-device list. Photos only. No app-added AI models, generative enhancement, cloud processing, analytics or video/audio recording.

**Development focus:** iOS is now the active platform; the Android app is retained unchanged. The owner has authorised iOS test builds in GitHub Releases. [iOS beta 2 release](https://github.com/TheSwagInDisguise/MobileFilmLab/releases/tag/v0.1.0-ios.beta.2).

**Distribution:** publish the verified, unencrypted, unsigned device IPA in GitHub Releases, then let the owner re-sign/install it with Signulous. Signulous signs compiled IPAs; it does not build this source. No Apple ID, service password, developer certificate or provisioning profile belongs in this repository. No App Store / TestFlight publication is attempted. Signing-service acceptance and installation must be checked by the owner.

## Cleaner camera controls in beta 2

- **One compact toolbar:** AUTO/PRO, photo format, supported flash, timer and Settings. The redundant title/banner and always-visible technical text are gone.
- **A physical rear lens row:** Main is 1×; other numeric ratios come from validated AVFoundation constituent/crossover metadata relative to Main. If that mapping is unavailable, **Ultra / Tele** are used instead of guessed numbers. No hard-coded telephoto power or fixed slot count.
- **A separate front/rear switch** next to the shutter remembers the selected lens on each side. Front cameras and duplicate automatic routes no longer fill the rear lens row.
- **JPEG framing is a separate crop menu:** tap the crop icon for full frame, 1.5×, 2× or 3×. An active crop says **“× crop”**, not a lens name. RAW hides the crop control and remains uncropped.
- **Quieter AUTO; focused PRO:** ISO/shutter readout and histogram appear only in PRO, with one parameter slider at a time. The look name and Tone/Colour shortcut share a single bar.
- **Advanced routes remain available:** Settings → Advanced → Camera routing → All camera routes & diagnostics. Choosing an automatic route adds a clearly labelled **Auto** choice beside the physical lenses so it can be changed back. Detailed camera IDs stay in Diagnostics.
- Settings explanations and less common controls are in expandable groups, not removed. RAW/ProRAW/HDR, the complete look library, sharing, gallery and other delivered features remain available.

The release includes genuine native simulator UI screenshots. Screens showing lens controls use **explicit sample metadata**, with a visible simulator label, no camera feed and capture disabled. Their example ratios are **not measurements of the target handset**. See [UI changes and screenshot provenance](UI.md).

## Feature scope

This is the advanced-feature candidate, not a core-only placeholder with RAW/HDR promised later:

| Android function | Native iOS implementation / explicit difference |
| --- | --- |
| Photos-only AUTO / PRO | SwiftUI camera, supported exposure / ISO / shutter / EV / focus / WB controls. Disabled or absent when not exposed. No video UI. |
| Dynamic camera routes | Clean physical rear lens row, separate front/rear switch; all enumerated physical/virtual routes retained in Diagnostics. Runtime metadata supplies numeric labels; readable names are the fallback. A route is not a guarantee of RAW support. |
| Native JPEG and crop | Requested JPEG quality priority; largest exposed dimensions optional. **1× / 1.5× / 2× / 3× JPEG framing** (tap the separate crop icon for the menu); never described as optical lenses. |
| RAW / RAW+JPEG | Genuine, capability-gated Bayer DNG output; 1× and single exposure. RAW bytes unmodified. Companion JPEG is graded separately. |
| Computational RAW | Separately labelled, capability-gated **Apple ProRAW / ProRAW+JPEG**; not called unprocessed Bayer sensor data. |
| HDR and fusion | Original + Smart HDR + Quality HDR + experimental three-frame fusion. Safety Original first, sequential exposure-verified JPEG helpers, local alignment, conservative motion rejection, sRGB-linear merge and restrained hue-preserving highlight compression. ~12 MP cap. Not the Google RAW HDR+ implementation and not a stock Camera Night/HDR switch. |
| Buffered capture | Exposed Apple zero-shutter-lag / responsive APIs, capability checked; disabled for RAW, manual exposure and custom HDR. One app capture/save transaction at a time. Not an identical Android ZSL pipeline. |
| Looks | All **77 upstream recipes + 3 independent baselines**, stable IDs preserved. iOS baseline names are mapped for display rather than claiming to use another phone's Camera2 pipeline. Shared colour coefficients are tested in both native implementations. |
| Recipe workflows | Live colour thumbnails, favourites, custom-copy editor, tone/colour pad, compatible bounded JSON / `.filmrecipe`, compressed QR display, QR-text and image import. No required Internet connection. |
| Processing / output | Optional conventional Balanced NR/tone, sharpening, synthetic luminance grain, calibration multipliers, native-pixel crop and explicitly labelled interpolated enlargement. Untouched Original JPEG bytes preserved when no processing is selected. |
| Composition / saving | Timer, flash/torch where exposed, grids, sensor-driven level, PRO ungraded histogram, hardware capture events, private gallery with comparison/share/delete, optional Photos add-only import. |

### Honest rendering limits

- Proprietary stock Camera processing, Night mode, all physical lenses, exposure ranges, capture dimensions, Bayer RAW and ProRAW availability are runtime capabilities, not guaranteed by the model name.
- Many upstream effect / DRO flags are retained for file compatibility, as in the Android approximation, not falsely presented as equivalent proprietary filters. The shared grade implements style/saturation/contrast, mono filters, Kelvin tint approximation, colour tint, recipe exposure offset, sepia, retro and high-key. Negative detail metadata is preserved, not synthesised as blur.
- Looked JPEG processing is SDR sRGB; capture opts out of automatic wide-colour switching and requests sRGB on formats that expose it. This is not an HDR-display / Display P3 export pipeline. Bayer RAW requires AVFoundation speed priority even for a paired JPEG; that companion can differ from JPEG-only quality priority.
- Matrix previews do not simulate capture exposure, Balanced NR, grain, final sharpening, HDR, computational ProRAW or enlargement. Hardware WB is separate from recipe Kelvin tint.
- Apple JPEGs already incorporate device ISP processing; JPEG burst merging cannot recover already-clipped RAW information. There is **no app-added AI**, but no claim that opaque device firmware / Apple ProRAW is free of computational or learned processing.
- Sequential custom HDR is experimental and can be slower or worse than Original, particularly for people, foliage and low light. Keep Original companions and compare static crops; no handset speed/quality claim is established by CI.
- iOS interface is portrait; capture orientation is derived from device orientation. Front preview is mirrored; saved-photo behaviour follows the capture connection, not a fake rear-camera crop.

## Source and build

Requires Xcode with iOS 18+ SDK, Swift and [XcodeGen](https://github.com/yonaskolb/XcodeGen) on macOS. The manual GitHub workflow uses `macos-26` and records the actual Xcode/SDK, rather than assuming an installed iOS 27 SDK. An SDK older than the phone OS can compile these established APIs; installation and OS behaviour still need phone testing.

```sh
python3 scripts/export_catalogue.py --check
python3 scripts/check_ios.py
swift test --package-path ios/Packages/FilmLabCore
cd ios
xcodegen generate
bash scripts/build.sh
```

The workflow checks an exact source SHA, runs shared Swift tests, native simulator image tests / UI smoke tests, builds **arm64 device code**, packages `Payload/MobileFilmLab.app`, and inspects the IPA's Mach-O, encryption flags, plist, full catalogue and GPL resources. The build workflow is iOS-only and produces IPA/QA artifacts without signing credentials. Verified builds may then be published to GitHub Releases under the owner's standing approval. No Android build, retuning or public website update is performed. Generated projects/build output are excluded from version control; `project.yml` is the reproducible project specification.

[Install, backup and handset test checklist](TESTING.md) · [Shared schema and vectors](../shared/README.md) · [Licence](../LICENSE) · [Attribution](../UPSTREAM_ATTRIBUTION.md)

## Test evidence and disclosure

The source, tests, UI and documentation were developed with AI assistance. Tests use **synthetic pixels**; simulator screenshots depict native app UI with **no handset camera feed** (some use visibly labelled sample lens metadata), not iPhone photographs or camera-quality evidence. Build results are recorded beside a delivered IPA, not predeclared here. A successful build/test/package inspection is not proof that a third-party service can sign it or that RAW/HDR/Photos work on the user's phone.

## Licence / warranty

GPL-3.0-or-later, with full upstream attribution, original approved artwork and source rights retained. App Store licensing/terms review has not been performed; no unilateral relicensing is implied. Supplied as-is without warranty, to the extent permitted by law. Back up irreplaceable photographs and recipes. See the project's [disclaimer](../docs/DISCLAIMER.md); it does not override non-excludable legal rights.
