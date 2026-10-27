# Natural capture and the no-app-added-AI promise

MobileFilmLab aims to take natural photographs from the camera hardware rather than adding image-model enhancement. This describes the **app's own processing**, not a guarantee about every proprietary step inside the device.

## What MobileFilmLab does not add

- No generative image model, scene synthesis, object replacement or AI-created detail.
- No neural/ML photo-enhancement model bundled or invoked by the app.
- No cloud photo processing, photo upload, analytics or automatic background release check.

Optional JPEG recipe colour transforms, sharpening, procedural grain and JPEG-domain three-frame fusion use conventional, non-ML algorithms. They are creative/processing options, not a claim that editing is disabled in every JPEG mode.

## Natural JPEG by default

The compatibility default remains JPEG, Natural baseline, standard Camera2, native pixel dimensions, no grain and identity lens gains. At 1× with that unedited configuration, the app preserves the exposed original JPEG pixel data and adds capture/recipe EXIF. It does not mandate a film look or enlarge output to advertise a higher pixel count.

Selecting a recipe, crop, JPEG lens calibration, grain or a different JPEG processing mode changes the JPEG output. Edited images may require decoding/re-encoding and memory-bounded dimensions.

## Sensor RAW when exposed

Starting with **beta.2**, users can choose RAW DNG only or RAW+JPEG on routes that advertise suitable `RAW_SENSOR` capabilities. The DNG is written from the real timestamp-matched sensor image and capture metadata through Android `DngCreator`, without app colour grading, grain, crop or resampling. RAW-only requests no still JPEG; RAW+JPEG also provides an optionally processed JPEG from the matched request.

RAW formats use standard Camera2, native output and uncropped 1× framing. OEM HDR/Night, fusion, reprocessing ZSL and enlarged export are JPEG-only. RAW native dimensions and its editor rendering may differ from the JPEG. A RAW-capable editor may be needed.

## Hardware-first is not a firmware bypass

- Camera2 JPEGs still pass through the device ISP. Demosaicing, noise reduction, tone, sharpening and other processing can already be applied.
- Exposed RAW can also include hardware/firmware corrections. We cannot inspect, disable or rule out every proprietary algorithm or claim a completely untouched electrical sensor signal.
- Optional manufacturer HDR/Night JPEG extensions may use proprietary or AI-based processing.
- Native dimensions mean the exposed stream, not necessarily the maximum sensor mode. RAW candidates are normal-resolution native sizes bounded at 55MP; unsupported access is reported, not manufactured.
- The promise is **no AI enhancement added by MobileFilmLab**, not a verifiably AI-free hardware/firmware stack or exact stock-camera reproduction.

## Development and presentation are different

The application was developed substantially with AI assistance; that does not make its photo-processing pipeline an AI model. The website uses actual UI renders with a clearly labelled AI-generated demo scene and illustrative capability state—not photographs captured by the app or evidence of image quality.

[Dynamic cameras and RAW](DYNAMIC_CAMERAS_AND_RAW.md) · [Known limitations](../KNOWN_LIMITATIONS.md) · [AI-development disclosure](AI_DISCLOSURE.md) · [Screenshot provenance](SCREENSHOTS.md) · [Privacy](PRIVACY.md)
