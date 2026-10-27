# Natural capture and the no-app-added-AI promise

MobileFilmLab aims to take natural photographs from the camera hardware rather than adding image-model enhancement. This describes the **app's own processing**, not a guarantee about every proprietary step inside the device.

## What MobileFilmLab does not add

- No generative image model, scene synthesis, object replacement or AI-created detail.
- No neural/ML photo-enhancement model bundled or invoked by the app.
- No cloud photo processing, photo upload, analytics or automatic background release check.

Optional recipe colour transforms, sharpening, procedural grain and JPEG-domain three-frame fusion use conventional, non-ML algorithms. They are creative/processing options, not a claim that all editing is disabled in every mode.

## Natural by default

The default settings use the Natural baseline, standard Camera2 pipeline, native pixel dimensions, no grain and identity lens gains. At 1× with that unedited configuration, the app preserves the exposed original JPEG pixel data and adds capture/recipe EXIF. It does not mandate a film look or enlarge the output to advertise a higher pixel count.

Selecting a recipe, crop, lens calibration, grain or a different processing mode changes the output. Edited images may require decoding and JPEG re-encoding. Memory limits can require reduced editing dimensions. See [known limitations](../KNOWN_LIMITATIONS.md).

## Hardware-first is not RAW or an ISP bypass

- This release captures **JPEG**, not RAW/DNG. Reported RAW capability in diagnostics does not mean RAW capture/export is implemented.
- Camera2 exposes a device camera/ISP pipeline, not an unprocessed sensor dump. Demosaicing, exposure/white balance, noise reduction, sharpening and other processing may already be applied by the device.
- We cannot inspect, disable or rule out every proprietary firmware/ISP algorithm. Optional manufacturer HDR/Night extensions may use proprietary or AI-based processing.
- The app therefore promises **no AI enhancement added by MobileFilmLab**, not that the entire hardware/firmware stack is verifiably AI-free or identical to a stock camera application.
- Native dimensions refer to the exposed capture stream, not necessarily the sensor's maximum mode. Enlarged export is interpolation, not recovered captured detail.

## Development and presentation are different

The application was developed substantially with AI assistance; that does not make its photo-processing pipeline an AI model. The website uses actual UI renders with a clearly labelled AI-generated demo scene. That landscape is a documentation input, not a photograph captured by the app or evidence of image quality.

[AI-development disclosure](AI_DISCLOSURE.md) · [Screenshot provenance](SCREENSHOTS.md) · [Privacy](PRIVACY.md)
