# iOS beta 3 — viewfinder performance

The owner reported a very laggy viewfinder in beta 2. Source inspection found an explicit `1/12`-second frame gate: the app could not display more than about 12 fresh frames per second even when the camera delivered faster. Each accepted frame was also synchronously rendered to a `CGImage`, wrapped in a `UIImage`, and published through the camera's SwiftUI `ObservableObject`. This describes the old implementation, **not a measured before/after phone benchmark**.

## New live-preview path

1. AVFoundation delivers a **preview-sized proxy** through `AVCaptureVideoDataOutput`. Supported bi-planar YUV is preferred, with supported/native format fallback. Late capture buffers may be discarded. The session stays in the photo preset; `AVCapturePhotoOutput` remains responsible for the full-resolution photograph.
2. The camera callback builds a lazy Core Image crop/colour graph and replaces **one pending immutable frame**. It does not perform bitmap readback, allocate a per-frame `UIImage`, dispatch a UI image update, or wait for analysis/thumbnail rendering.
3. A native `MTKView` consumes the **newest available** frame and renders through Core Image/Metal to its drawable. There are at most **two GPU renders in flight**; no FIFO of old frames is accumulated. Input buffers are retained through GPU completion.
4. The drawable is aspect-fit, upright and bounded to a **1440-pixel longest edge**. It is not a saved JPEG/DNG resize. Colour coefficients are cached when the look changes; an identity look avoids a needless colour filter. Press-and-hold Original and JPEG framing still apply to live view.
5. Histogram/HDR analysis runs at most four times per second on coalesced background work. Histogram and ISO/shutter UI updates are requested in PRO only. This is independent of live-frame delivery.
6. Ungraded thumbnail sources are produced at most once per second **only while the look drawer is open**. Visible look tiles render their colour previews off-main with stale requests replaced/cancelled. They observe a separate feed rather than causing camera-screen redraws.
7. Drawing pauses when the native surface is detached or the app becomes inactive. Environments without a Metal device retain a bounded native Core Image/UIImageView fallback; they do not restore per-frame SwiftUI image publication.

The surface requests a **60 Hz display cadence**, not a 60 FPS sensor guarantee. It neither forces an exposure duration nor changes the selected camera's sensor frame-rate limits. Input may be 30 FPS or lower, especially in low light, with a long manual shutter, during still/HDR capture or under thermal constraints.

## Photo quality is not the performance shortcut

This preview-only change deliberately preserves:

- the `.photo` session preset and existing requested still-photo dimensions/quality priority;
- JPEG/Bayer RAW/RAW+JPEG and separately labelled computational Apple ProRAW capability gating;
- the original still/HDR capture transaction, safety-original/fallback logic and DNG-byte handling;
- photo colour, detail, grain, HDR, enlargement, saving/Photos and recipe/interchange algorithms;
- all 80 recipes, the clean physical-lens picker, front/rear switch and separate honest JPEG crop controls.

`shared/ios-photo-baseline.json` and the source checker protect the beta 2 processing/save files and still/HDR transaction byte-for-byte. This does not promise that two separately captured photographs will have identical sensor/ISP output, or that the owner has already verified the new build on the handset.

## Diagnostics on the phone

Open **Settings → Advanced → Camera routing → All camera routes & diagnostics → Live preview**.

The panel shows the renderer, approximately one-second camera-input and completed-render counts, proxy/drawable dimensions, pending-slot count, cumulative stale-frame replacements and render errors. Completed GPU work is **not a laboratory measurement of physical display presentation or sensor-to-screen latency**. Replaced preview frames are not lost still photographs. Counters stay local; no diagnostics are uploaded.

For an initial A/B check use Main 1×, JPEG, Original, iPhone Natural and automatic exposure in daylight. Pan before taking a shot, then try another look/front camera and open/close the recipe library. Record the lens, look, capture format, light, manual shutter (if any) and counters if it remains choppy. Then independently confirm the dimensions/quality of a saved JPEG/DNG. Keep the system Camera available for important events.

## Software tests and image provenance

The added tests cover the single-slot mailbox, concurrent replacement, reset generations, background coalescing/cancellation, bounded drawable/aspect fit, cached look-matrix parity across all 80 recipes, origin/letterbox handling, available-backend texture orientation/colours, windowed diagnostic counters, active-only background thumbnails and the native viewfinder surface.

A **Debug + Simulator-only four-colour test pattern** exercises the actual crop/grade/mailbox/native-render path. Its on-screen label says **SIMULATOR · SYNTHETIC PREVIEW TEST**. It is not a photograph, AI-generated demo scene, camera feed or target-phone specification. Capture remains disabled. The device IPA independently excludes the fixture launch arguments/identifying strings. If the runner has no Metal device, tests exercise and explicitly report the native fallback instead of claiming GPU execution; the release QA report records the backend actually tested.

These tests establish software behaviour, not handset FPS, panning latency, camera/RAW/Photos compatibility, photo quality, temperature or signing-service acceptance. Exact build/test results accompany the delivered IPA. **No measured iPhone 18 Pro Max / public iOS 27 performance result is claimed until the owner checks it.**

AI assistance was used for development, tests and documentation. No app-added AI photo enhancement has been introduced. GPL-3.0-or-later and upstream attribution are retained; Android source, existing downloads and the public website remain unchanged.

[Install and phone-test checklist](TESTING.md) · [Feature scope/build](README.md) · [Camera UI](UI.md)
