# Unpublished Fold5 Smart HDR test candidate

**App version: 0.2.0-beta.5 (14). Test artifact only — not a public beta.5 release.**

The regular download, website, Latest release, publication approvals and existing source tags remain on **[v0.2.0-beta.4](https://github.com/TheSwagInDisguise/MobileFilmLab/releases/tag/v0.2.0-beta.4)**. This candidate uses the same `com.mobilefilmlab.camera` package and established signing certificate. Installing it is an opt-in test, not a stable-quality promise.

## Install and rollback first

1. Export custom recipes and save a copy of any settings/calibration you need before testing. Photos are normally in `Pictures/MobileFilmLab/`; back them up independently.
2. Install the signed `MobileFilmLab-v0.2.0-beta.5-HDR-test-signed.apk` supplied with this guide. It should update the existing app in place; **do not uninstall first just to test it**. Android still requires your permission to install.
3. Version code **14** is higher than public beta.4's **13**. Android normally blocks an ordinary downgrade. Returning to beta.4 may require uninstalling the candidate and installing the public APK, which removes private app settings/custom recipes. Do not rely on `adb install -d` for a release-build downgrade. Export/backup first.
4. Do not install an APK with a different certificate or package to work around an error. Please report the exact install message instead.

The certificate SHA-256 must be:

```text
8D:CD:B6:C6:5B:DA:C8:0A:48:8D:06:3D:70:26:B3:D4:5E:AA:5B:4B:C0:02:E2:D4:C8:4A:CB:C9:73:51:F4:1A
```

## What changes

In **Settings → Capture processing**:

- **Smart HDR · handheld** — Fold5 JPEG default. A metered Original safety exposure is taken first, followed by **3–5 constant-exposure highlight-protecting short frames**. The count/highlight bias responds to a small ungraded preview histogram, measured ISO and a simple motion estimate. Short helper shutters are limited where sensor sensitivity/exposure limits allow it; no pre-shutter RAW ring or zero-shutter-lag promise.
- **Quality HDR · still scenes / slower** — first the same safety exposure, then **7 short frames**, plus **up to 2 longer shadow frames when recent preview samples indicate a quiet scene**. A compressed-data memory limit can trim helpers once at least three shorts exist, or use Original if that minimum cannot fit. Long helpers are limited to 250 ms where supported, and need extra alignment/sharpness/colour agreement before contributing. This is for a stationary phone/scene, not walking subjects or a substitute for a tripod night mode.
- **Original · Camera2 natural** remains selectable. It uses one ordinary exposure. With the default neutral recipe, Original JPEG rendering, native output and 1× framing, the existing no-recompression pixel-preservation path remains available. A chosen recipe/Balanced/crop/grain still intentionally changes JPEG output.
- Existing **legacy 3-frame fusion** and firmware **OEM HDR/Night** remain separate choices, with their existing limitations.
- **Save metered Original with HDR** is optional and off by default. After a successful HDR save, it writes an additional ungraded first exposure in the same folder, with `HDRTestOriginal` in its filename. It uses the same framing, no recipe, no grain, and native output; a 2× crop necessarily needs JPEG re-encoding. It is a comparison from the same capture sequence, **not the same instant as the short reference**. A rejected HDR burst saves just the safety exposure, not a duplicate companion.

### Existing preferences

On a Fold5, the first candidate launch upgrades a missing/legacy `STANDARD` processing preference to Smart HDR **once**. Beta.4 did not distinguish an explicit Standard selection from the default persisted while changing other settings, so both legacy cases migrate. Existing non-Standard pipeline choices are retained. Select Original again to opt out; subsequent launches respect that selection. Other devices keep their previous default but may opt into the new JPEG modes.

RAW and RAW + JPEG still force **one standard native exposure**, no stack, no ZSL, uncropped 1× and genuine RAW_SENSOR/DNG handling. Changing to RAW does not create a merged or synthetic DNG. Returning to JPEG after RAW can leave Original selected; choose Smart HDR again if wanted.

## What processing actually does

This is original, conventional **JPEG-domain** burst processing, informed by published burst-HDR principles. It is **not a GCam port, Google's proprietary HDR+, a RAW-domain HDR+ reimplementation, super-resolution or a learned enhancement model**.

- Uses actual per-frame ISO × exposure time × available post-RAW sensitivity boost, with gain, duplicate-frame, focal-route and consistency checks, not just requested EV labels.
- Chooses a relatively sharp early short reference, performs coarse-to-fine overlapping local translation-tile registration with fractional refinement, and rejects unreliable tiles.
- Merges agreeing linearised sRGB samples with a conservative ISO-dependent JPEG noise proxy, RGB motion/outlier rejection and capped long-exposure weights. It does not have a measured RAW sensor noise profile.
- Applies a restrained, hue-preserving highlight curve and edge-aware low-resolution local shadow lift. No automatic saturation boost, invented sensor calibration or app-added AI. The selected photographic recipe/calibration is still applied afterwards.
- Keeps one full output bitmap plus bounded decoded strips/thumbnails rather than N full native RGB bitmaps. Burst capture prefers actual advertised normal-mode streams up to ~12.6 MP, even if Maximum was requested. Low memory or an unusually large smallest stream can require sampled output; the result note records this. Native resolution is not guaranteed on every route.
- Skips the optional Balanced post-filter after a successful Smart/Quality merge to avoid double tone/noise processing. It remains available for single-exposure fallback and legacy paths.
- Handles JPEG EXIF orientation before applying the existing crop once. Composite EXIF ISO/shutter describe the selected **short reference**, not an imaginary summed exposure. The user comment records capture/merge details and measured merge time.

Every input has already passed through the device ISP. Clipping, denoising, sharpening, changing tone curves, lens switching and colour decisions made there cannot necessarily be undone. No claim that vendor firmware/ISP is AI-free is possible. Matching GCam/stock quality, ghost-free output, texture recovery, capture speed and battery/thermal behaviour **have not been demonstrated on a Fold5**.

## Safety fallback and status

A failed helper request/deadline, unsupported manual sensor/physical exposure controls, inconsistent or missing exposure metadata, duplicated sensor frames, unreliable alignment or too little agreeing image area uses the **metered first Original** instead of an underexposed helper or incomplete stack. Flash/torch, manual exposure and severe thermal load also skip multi-frame HDR. White balance and converged focus are requested to remain fixed only where public controls allow it; the device must honour those controls.

Capture progress shows safety/frame counts, then registration/merge/save stages. The save notice distinguishes HDR from Original fallback. The capability report includes the last JPEG processing note and recent scene-meter values. A timeout before even the safety frame completes still means no saved photo. An OS kill, fatal camera/service error or storage failure cannot be made recoverable by an in-memory safety frame.

## First Fold5 A/B pass

Please start with **JPEG only, native output, 1×, flash off, AUTO, neutral Fold5 Natural recipe, zero grain and gains 1.00**. Enable **Save metered Original with HDR** for comparisons. Keep your original full-size files; screenshots or messaging-app recompression hide important differences.

Take each scene with Original, Smart HDR and Quality HDR where appropriate, then optionally the stock camera/GCam with the same lens/framing. These latter apps are independent references, not expected pixel-identical matches.

1. **Daylight backlight:** shaded subject/room and a bright window/sky. Check highlight texture, natural midtones/skin, halos and hue changes.
2. **Indoor static shadows:** fine fabric, printed text, neutral wall/grey object. Check chroma noise, small texture and excessive smoothing.
3. **Low light, static phone:** compare dark texture, blur and motion trails; time shutter-to-save. Use Quality only when the scene is still.
4. **Moving subject:** a person moving a hand or walking in a backlit scene. Use Smart, check both subject edges and background for duplicates/colour ghosts. Do not use a moving person as the first Quality-mode test.
5. **Each exposed rear camera and 2× JPEG crop:** record exact camera ID, dimensions and whether it merged or fell back. No promise of hidden-camera access.
6. **Cover/inner screen:** repeat a short session, fold/unfold between captures, background/resume, then a few spaced captures. Stop if the phone becomes uncomfortably hot. Do not stress-test while charging in heat.
7. **Regression:** Original neutral JPEG, a favourite/custom colour recipe, RAW and RAW + JPEG on a supported route; inspect the DNG in an actual RAW editor. Verify no duplicate/missing gallery entries or unexpected RAW processing.

Send, privately unless you separately authorise publication:

- Model code, Android/One UI/firmware version, and copied **Camera capability report** after the relevant capture (it contains device/camera metadata, not uploaded photos).
- The full-size HDR and optional Original files, mode/lens/framing, visible fallback notice, rough capture + processing time, and what looks worse/better. Remove location/other personal metadata yourself if desired; shared samples may contain faces/private surroundings.
- Whether installing/updating, folding, resume, saving and opening DNGs work. Any crash/timeout and approximate thermal state.

Automated synthetic tests/native UI renders establish software behaviour only, **not real-camera image-quality approval**. Robolectric’s region-decoder stub does not decode image content, so the HDR fixture suite uses a test-only native-JPEG decode/crop adapter. Actual Android region decoding, its memory/latency and Camera2 integration still require handset checks; the adapter and synthetic fixtures are not shipped in the APK. Public beta.5 remains unapproved until this test feedback and further fixes justify it.

## Research references / attribution

These are design-reading references, **not incorporated Google code/models/data**, and imply no Google endorsement:

- Hasinoff et al., *Burst photography for high dynamic range and low-light imaging on mobile cameras*, SIGGRAPH Asia 2016: https://research.google/pubs/burst-photography-for-high-dynamic-range-and-low-light-imaging-on-mobile-cameras/
- Google Research, *HDR+ with Bracketing on Pixel Phones* (2021): https://research.google/blog/hdr-with-bracketing-on-pixel-phones/
- Liba et al., *Handheld Mobile Photography in Very Low Light* (2019): https://arxiv.org/abs/1910.11336

The project remains GPL-3.0-or-later. See [disclaimer](DISCLAIMER.md), [Fold5 compatibility notes](GALAXY_Z_FOLD5.md) and [general testing guidance](TESTING.md). All older screenshot/demo disclosures continue to apply; no new device-photo evidence is claimed here.
