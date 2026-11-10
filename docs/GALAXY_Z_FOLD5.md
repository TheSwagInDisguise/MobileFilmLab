# Galaxy Z Fold5: beta.4 compatibility and JPEG processing

> This page describes **public beta.4**. The candidate source branch adds unpublished Smart/Quality HDR; see the separate [HDR test guide](HDR_TEST_CANDIDATE.md) for defaults, limits, backups and downgrade cautions. No public beta.5 is approved.

**Status: capture already works according to the reporting user; image quality was reported as poor. These changes are capability-driven improvements, not a handset-calibrated profile.** No Fold5 sample photograph, capability report, Android/One UI version or attached phone was available during development. Automated/synthetic checks do not establish real-world image quality or stock-camera parity. Oppo Find X9 Ultra remains the primary development target.

## What changes on a Fold5

- Samsung `SM-F946*` build-model variants use a Fold5 compatibility policy. Unknown models keep the standard policy. This does not unlock hidden cameras or assume particular camera IDs.
- **Device-balanced native capture** prefers advertised normal-mode JPEG streams within a ~12.6MP budget, favouring ordinary streams over high-resolution/stalling ones. If none fit, the smallest usable advertised native stream is used; the app never invents a 12MP buffer. Fallbacks do not jump up to more expensive buffers. Fusion/ZSL keep the same small-stream safety policy on every device.
- **Maximum advertised native pixels** is an explicit alternative, within the existing 55MP safety ceiling and actual HAL/session support. Large *edited* images may still be downsampled for memory safety. Interpolated export remains a separate, honestly labelled option—not detail recovery.
- The existing Natural recipe ID is retained for database/favourite/import compatibility, but its display label is **Fold5 Natural (Camera2)**. It uses the selected camera's exposed ISP JPEG, not Oppo tuning and not the proprietary Samsung stock-app pipeline. The complete 77 upstream recipes plus three baselines and all custom names remain available.
- The portrait UI can resize with the window. Controls are centred and capped at 600dp on wide inner displays; Settings scrolls independently with Done always accessible. Native illustrative renders cover narrow and wide layouts. A real fold/unfold camera-session transition has **not** been tested.

## Choose the rendering you want

Open **Settings → Device & JPEG rendering**:

| Choice | Saved JPEG behaviour |
| --- | --- |
| **Camera original / recipe only · fastest** (default) | No additional balanced filtering. With the unedited Natural look, 1×, no grain, identity calibration and native export, the camera JPEG pixel data is preserved without recompression. Selected recipe/crop/grain edits still apply when used. |
| **Balanced detail + noise · optional** | Conventional ISO-aware, edge-preserving luma/chroma noise reduction; mild histogram-adaptive shadow/highlight balancing; restrained low-ISO detail sharpening. No global saturation/white-balance bias or neural detail synthesis is added. Unknown ISO uses a conservative fallback without automatic detail sharpening. It decodes/re-encodes the JPEG and is not an untouched-original mode. |

Balanced rendering applies **only to saved JPEGs**. Preview/live recipe thumbnails show recipe colour, not final noise filtering, fusion or adaptive tone. The luma/chroma filter protects strong brightness and colour boundaries; it can still soften fine texture. Tone balancing leaves black/white anchored, avoids aggressive high-ISO shadow lifting, and cannot reconstruct detail already clipped in its only input. It is not a measured Samsung colour correction.

**RAW is unchanged.** RAW-only disables JPEG rendering; RAW+JPEG may process only the paired JPEG. DNG comes from actual `RAW_SENSOR` with capture metadata, without app denoising, colour grading, cropping, interpolation or fusion. Capability gates and standard/native 1× RAW restrictions remain in force.

## HDR / low light is a separate, optional decision

Keep **Camera2 natural** for everyday motion and speed. For a static high-contrast scene, try **3-frame fusion · experimental**, flash Off and both ISO/shutter Auto:

- Manual-capable routes meter a baseline once and use the same bounded/physical-key-aware request path for all three exposures, including the middle frame. Other routes wait briefly for reported AE settling rather than assuming a fixed delay is sufficient.
- Fusion requires valid actual exposure metadata; missing/invalid values use the middle frame instead of guessing stops.
- Registered JPEGs are exposure-normalised with the IEC sRGB transfer. Highlight headroom is retained until display mapping; only suitable registered darker-frame samples establish a recovered white point.
- Translation registration and RGB-plus-luma motion rejection reduce obvious colour ghosts. Unreliable alignment/low texture falls back to the middle frame. Translation-only fusion **cannot** guarantee ghost-free moving subjects, compensate parallax or reproduce a proprietary multi-frame RAW pipeline.
- Three decoded inputs reuse the middle output bitmap rather than allocating a fourth full-size image. Balanced JPEG filtering uses scanline scratch; identity grading avoids another full-size bitmap. These are allocation safeguards, **not measured handset speed/battery claims**.

OEM HDR/Night remains available only if the selected route/firmware advertises a usable Camera2 extension. The app does not promise that a Fold5 exposes those extensions or its complete stock-camera feature set. No app-added generative/ML enhancement or cloud photo processing is introduced; proprietary ISP/firmware internals remain opaque.

## Suggested first test

1. Install beta.4 over the existing release, without uninstalling, to keep recipes/favourites. Leave native size **Device-balanced** and export **Native pixels**.
2. Start with the main exposed rear route, unedited **Fold5 Natural (Camera2)**, 1×, grain 0, calibration gains 1.0, ISO/shutter/WB Auto, flash Off and **Camera2 natural**.
3. Photograph a daylight textured scene, skin/neutral colours, a bright-window interior and a low-light static scene. Compare **Original** and **Balanced** with the same framing/exposure where possible. Keep original files/EXIF; avoid social-media recompression and digital zoom for the baseline comparison.
4. Try the exposed ultrawide/tele routes independently—do not assume a fixed count or RAW support. Try experimental fusion on a static scene separately from single-frame quality.
5. Test folded and unfolded, reopening/foregrounding the app and changing format/route. Check framing, touch focus, JPEG orientation, shutter responsiveness, thermal behaviour and repeated captures. Validate DNG in a compatible RAW editor if offered.
6. Share Android/One UI version, exact build model, the in-app capability report, chosen route/settings and representative original files privately or in a report you explicitly authorise for publication. Do not include private photographs or identifiers unintentionally.

See [beta.4 QA boundaries](QA_BETA_4.md), [testing](TESTING.md), [natural-capture promise](NATURAL_CAPTURE.md), [RAW details](DYNAMIC_CAMERAS_AND_RAW.md) and [use-at-your-own-risk disclaimer](DISCLAIMER.md).
