# MobileFilmLab beta.3 public-release testing

**Version:** `0.2.0-beta.3` · code **12** · package `com.mobilefilmlab.camera`
**Primary target:** **Oppo Find X9 Ultra** · Android 9 / API 28 minimum

This beta-version build is published as a regular Latest GitHub Release at the owner's request. That classification is not broad-device certification. The new RAW/session paths need real handset testing; there is no connected camera in the JVM/CI test environment.

## Install safely

1. Download `MobileFilmLab-v0.2.0-beta.3-signed.apk` from the [current release](https://github.com/TheSwagInDisguise/MobileFilmLab/releases/latest).
2. Verify the attached checksum and optionally the [established signing certificate](RELEASE_SIGNING.md).
3. Install **over** beta.2 / code 11 to retain app data; do not uninstall. Debug builds are separate apps. Back up important photos and export custom recipes.
4. Grant camera permission; Android 9 also needs legacy storage permission. Photos go to `Pictures/MobileFilmLab/`.

## Readability and Pro sliders

- [ ] Open Settings. Read the output-size choices, composition switches, helper text and RGB gain labels: no black text on dark panels. The gold Done button correctly uses black text.
- [ ] Increase Android font size/display scaling. Scroll the settings body; the title and Done action should remain reachable. Check long labels wrap rather than overlap switches.
- [ ] Enter PRO. Tap EV / ISO / SEC / WB / FOCUS: this opens an editor without changing its value.
- [ ] Drag EV in Auto exposure. Confirm it snaps to the route's reported Camera2 steps and reaches both displayed endpoints. Reset returns to zero.
- [ ] Turn ISO Auto off and drag its logarithmic slider. Confirm the displayed value and saved exposure metadata respond within this lens's bounds; switch back to Auto explicitly.
- [ ] Repeat for SEC, including a supported longer exposure on a tripod. Wait for the exposure to finish before assuming a stalled shutter; preview may also slow. The app caps the range at 30 seconds and the sensor/frame-duration limit.
- [ ] When either ISO or shutter is manual, EV is inactive with a reason. An unspecified companion is labelled Metered and held, not continuously priority-auto controlled. Set both to Auto to restore AE/EV.
- [ ] Adjust WB with its Auto switch and slider; values are approximate gain-model Kelvin, not a calibrated temperature reading. Choose a supported AF mode directly, without cycling.
- [ ] Switch routes. Values must re-bound to the new sensor, and unsupported controls show a reason. During capture, Pro controls are disabled.
- [ ] Repeat basic manual settings with JPEG, RAW and RAW+JPEG where advertised; validate actual ISO/exposure in a compatible editor.

[Control details and limitations](PRO_CONTROLS.md) · [Automated QA scope](QA_BETA_3.md)

## Dynamic-camera checks

- [ ] Compare Settings → Camera capability report with the visible rear camera chips. The list is derived from exposed photo-capable Camera2 routes, not fixed UW/W/3x/10x slots.
- [ ] Tap every reported route. Confirm the exact ID/view changes; hold a chip for details. Distinct cameras with similar focal lengths should remain selectable.
- [ ] Logical multi-camera routes say **Auto**. Do not count a logical route as an extra physical lens or infer hidden lenses from product specifications.
- [ ] Labels show approximate detected equivalent millimetres, physical millimetres marked `*`, or an ID when metadata is missing—not guessed zoom ratios.
- [ ] Camera-ID-specific JPEG gains persist, and legacy calibration/favourites/custom recipes survive the upgrade.

## JPEG regression

- [ ] AUTO/PRO preview, portrait/landscape-held capture orientation, tap focus, supported flash and manual controls work.
- [ ] The default Natural 1× unedited output retains camera JPEG pixel data without mandatory grain or enlargement.
- [ ] Short-tap/hold the framing control; 1× / 1.5× / 2× / 3× saved framing matches preview without a second crop.
- [ ] Recipes, Tone/Warmth, favourites, custom saving, local file/QR-image interchange and gallery share/delete still work.
- [ ] Background/resume, hardware shutter and disabled controls during dialogs/capture remain correct.

## RAW (DNG) only — on each capable route

- [ ] Open the format badge or Settings → Photo file format. RAW choices are disabled with a capability reason where unsupported.
- [ ] Choose RAW only. Framing becomes uncropped **1×**, output native, pipeline standard Camera2, with no OEM HDR/Night/fusion/ZSL/enlargement.
- [ ] Shoot. Exactly one `.dng` is saved, not a renamed JPEG or a `.jpg` companion.
- [ ] Open the file in a **RAW-capable editor**, not just the app's preview. Confirm it recognises sensor RAW/DNG, dimensions/active area, orientation, ISO and exposure, and renders without a corrupt/unsupported-file error.
- [ ] RAW is not graded by the selected recipe, grain, RGB gains or digital crop. Its camera preview is not a decoded-RAW quality reference; editing/WB interpretation can differ.
- [ ] If Android cannot preview DNG, the gallery's Open in compatible editor and Share actions work with the DNG MIME type.

## RAW + JPEG

- [ ] Shoot with RAW+JPEG. Two files share a stem: `.dng` and `.jpg`.
- [ ] The JPEG reflects the selected look; the DNG remains sensor data. Native dimensions/active-area framing and rendering can differ.
- [ ] Try a different recipe/optional JPEG grain. Verify the JPEG changes, not the underlying DNG processing.
- [ ] Try switching to a JPEG-only route while RAW is selected. A format-change confirmation appears; cancellation retains the existing camera/format.
- [ ] An unsupported or failed RAW session reports an error instead of silently saving a JPEG-only replacement.
- [ ] Low-storage/write failure does not leave a reported successful but incomplete pair. Modern rows remain pending until writes finish; API 28 has no pending-row visibility guarantee.

## Lifecycle and stress

- [ ] Capture several DNGs/pairs, then close/reopen the camera; no stuck shutter or exhausted image queue.
- [ ] Background/lock during a RAW write and resume. Verify either complete files or a clear failure, not invalid-buffer crashes or orphaned pending rows.
- [ ] Try 50 mixed captures and a 10-minute preview session. Stop if the device becomes uncomfortably hot. Record time, native dimensions, memory/heat symptoms and any stream rejection.
- [ ] Switch back to JPEG and verify all previous pipelines and framing controls remain available as supported.

## Report useful, non-private evidence

Use the [bug/device forms](https://github.com/TheSwagInDisguise/MobileFilmLab/issues/new/choose). Include app version/code, model, Android/firmware, camera ID/route, chosen format, pipeline/status message, saved dimensions and reproducible steps. Review/redact the capability report. Only share sample DNG/JPEGs and metadata you intend to make public; remove private scenes, identifying data and precise location first.

[RAW implementation/limitations](DYNAMIC_CAMERAS_AND_RAW.md) · [Known limitations](../KNOWN_LIMITATIONS.md) · [Privacy](PRIVACY.md) · [AI-development disclosure](AI_DISCLOSURE.md)
