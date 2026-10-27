# Known limitations — public beta

## No app-added AI does not mean no device processing

MobileFilmLab adds no generative or neural/ML photo enhancement and sends no photos to a cloud processor. Its own optional editing/fusion is conventional. The device camera/ISP and optional OEM extensions may still use proprietary or AI-based processing we cannot inspect or rule out. JPEG uses the device ISP. Beta.2 also exports actual RAW_SENSOR DNG where exposed, but that is still the firmware-provided sensor stream, not a guarantee of an untouched electrical signal. [Natural-capture scope](docs/NATURAL_CAPTURE.md).

## Proprietary stock processing is not replicated

Exact proprietary sensor/processing internals, frame counts, algorithms and SNR gains cannot be inferred from JPEG/MakerNote metadata alone. Earlier explanations overstated those details. Multi-frame processing, sharpening, denoising, exposure/focus and stream limits can all contribute to differences from the stock app.

**Oppo Natural (Camera2)** means the exposed ISP JPEG without an added film look, not certified stock-camera tuning or a Hasselblad colour profile. Per-lens gains default to identity and are user-adjustable; no measured calibration has been completed.

## Pixel counts and image quality

- Alpha.8 enlarged images using a bitmap resize. That was not genuine super-resolution, did not defeat HAL access restrictions, and did not create new captured detail.
- Since alpha.9, native pixels are the default. A 2× crop has a quarter of the captured pixels. The selected HAL stream is not necessarily the sensor's full pixel array.
- Optional enlarged export interpolates to a 6120-pixel long edge (about 28.1MP for 4:3), with a proportionally smaller target for crops. Other aspect ratios have different pixel counts. Higher-resolution native input is not downscaled just to match this target.
- JPEG quality 98/100 is still lossy encoding. The default unedited original-JPEG path avoids another pixel recompression; edited images must be encoded again.
- Sharpening boosts edge acuity; it does not prove recovered texture/detail. Do not judge quality solely by Laplacian variance, which also increases with noise/halos.
- Edited captures can be downsampled when the memory budget is insufficient. The saved EXIF processing note records this. No zero-OOM guarantee is made.

## Firmware-dependent lenses and sessions

Only public Camera2 capabilities are used, under the fixed package `com.mobilefilmlab.camera`. Physical IDs are routed through logical output configurations when needed. Opening/stream combinations may still be blocked by ColorOS. Unavailable slots are disabled; a different camera is not silently relabelled as the requested lens. The dynamic selector labels exact IDs with detected approximate equivalent focal lengths. Logical Auto routes are not additional physical lenses; unknown metadata is not replaced with guessed zoom labels.

High-resolution and maximum-pixel-mode streams can fail in combination with preview. The engine tries a bounded set of smaller advertised native streams and reports failure if none works. No synthetic black substitute photo is saved.

## Experimental processing

- **Fusion:** three JPEG captures, not RAW. Translation-only registration cannot fully correct rotation, parallax, rolling shutter or subject motion. Registration/pixel rejection can fall back, and ghosting, noise or tone shifts remain possible. No fixed sharpness/SNR gain is promised. Burst input is capped near 12.6MP and may use lower native sizes on limited devices.
- **OEM HDR/Night:** only when publicly advertised. Vendor extensions do not guarantee stock-app equivalence, physical-camera routing, preview parity or complete metadata. Unsupported sessions visibly fall back. OEM modes control exposure/flash; PRO/cropped framing returns to standard Camera2. A capture failure reports an error rather than pretending to succeed.
- **ZSL:** PRIVATE reprocessing capability and a valid preview+input+JPEG stream combination are required. Off by default. Ring frames must be recent and exposure/focus-converged; crop, manual exposure, flash or missing frames use regular capture. The output from a reprocessed input can have fewer effective source pixels than a larger JPEG buffer. Latency remains device-dependent.
- **Thermal:** severe thermal status disables/reconfigures a requested ZSL ring and makes burst capture single-frame; OEM HDR/Night is blocked until cooling/standard mode selection. Real battery/thermal performance still needs a handset soak test.

## Recipe rendering and live preview

All 77 upstream definitions remain present. Creative Style, monochrome filters, tint, colour/contrast, recipe EV offsets and the used Retro/High-key effects are mathematical approximations. Proprietary DRO, some picture-effect subparameters and negative sharpness do not have exact Android equivalents; preserving their values is not an exact hardware emulation. Recipe EV grading is not falsely reported as a hardware exposure change.

The live preview shares the colour matrix but not final grain, sharpening or every OEM still-processing step. Split comparison samples the baseline periodically; it is not frame-synchronized dual-sensor video. Live preview correctness, orientation and latency require physical-device checking. UI remains portrait; JPEG orientation follows device orientation.

Kelvin WB is an approximate manual gain model, not measured per-sensor colour science. Manual values are hardware-clamped. Setting just ISO/shutter freezes the companion from a recent result, not a full priority-mode AE algorithm. Unsupported AF modes can fall back to the device's supported mode. Flash operation must be verified on hardware; selecting On alone is never treated as proof the flash fired.

## Gallery, interchange and release checks

The gallery lists up to 500 readable images from the app's output folder. Higher-resolution zoom decoding is memory-bounded; the overlay reports the decoded dimensions rather than promising arbitrary full-resolution allocation. RAW/DNG capture/export is capability-gated in beta.2; a compatible RAW editor may be needed. No general-library gallery, live-camera QR scanner or video mode is implemented. QR import reads a user-selected image locally.

Updates are opened in the browser for user-controlled download/installation. No background updater, analytics or auto-promotion. F-Droid metadata is a starter only, not a distribution commitment.

## Public beta, not completed hardware validation

The owner accepted initial alpha.9 handset behaviour and authorised the first public beta. Passing JVM/native-graphics tests and Android Lint still does not verify physical camera hardware, broad firmware compatibility or an app/device certification. See [public-beta scope and ongoing validation](docs/BETA_READINESS.md) and the [tester guide](docs/TESTING.md). Exact stock-camera matching and four-lens access on every firmware are not claimed. Other-device support will expand with beta testers' reports.

## RAW/DNG — new hardware validation needed

RAW formats use standard Camera2, native 1× and normal-resolution RAW_SENSOR streams at or below 55MP. They do not use JPEG fusion, OEM extensions, PRIVATE ZSL, enlargement or software crops. Raw-only requests no still JPEG; paired mode binds RAW/JPEG/result by sensor timestamp. Missing physical capture metadata or mismatched frames cause failure rather than a fabricated DNG or JPEG-only substitute.

A RAW capability flag and advertised size do not guarantee that a preview/RAW/JPEG session works on every firmware. Native combinations are retried within a bounded budget; rejection remains possible. DNG and JPEG dimensions/active-area framing and editor rendering may differ. Raw previews use the device ISP, not a decoded sensor-RAW image.

DNGs can be large, and Android's decoder may not preview them; use the gallery's external RAW-editor action. Retained-image lifetime and pending-file cleanup are software-tested, but actual DNG validity, exposure/orientation, lifecycle during large writes and memory/thermal behaviour require handset checks. Android 9 has no pending-row support and can transiently expose an in-progress row. See [RAW details](docs/DYNAMIC_CAMERAS_AND_RAW.md).
