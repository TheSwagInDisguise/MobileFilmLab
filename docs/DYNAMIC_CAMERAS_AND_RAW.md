# Dynamic Camera2 cameras and RAW capture — beta.2

## Camera buttons come from the device

The app enumerates public `CameraManager.cameraIdList` IDs and physical IDs advertised behind logical cameras. It reads focal lengths, sensor dimensions, facing, stream sizes and capabilities. The rear selector now shows **all discovered photo-capable rear routes**, selected by exact camera ID—not the first match in four hardcoded categories.

- Labels use approximate 35mm-equivalent focal length calculated from reported focal length and sensor dimensions.
- If equivalent metadata is missing, a physical focal length marked `*` or camera ID is shown instead of an invented zoom label.
- Logical multi-camera routes say **Auto**. A logical route is not an additional physical lens; its presence can make the route count larger than the physical-lens count.
- Distinct IDs with the same focal length/category remain individually selectable. Hold a chip for its ID, route and RAW capability; use Settings → Camera capability report for the full list.
- The list scrolls if needed; the digital-framing control remains separate. JPEG offers 1× / 1.5× / 2× / 3× digital framing.
- Firmware can hide IDs or reject physical stream routing. The app cannot enumerate undisclosed hardware, bypass package restrictions or promise the device's advertised total lens count. Front/external switching is not added by this change; the photo selector remains rear-focused.
- Per-camera JPEG RGB gains are stored by camera ID, with legacy category gains as a compatibility fallback. Custom recipes and favourites are unchanged.

## Three saved-file modes

Use the **JPEG / RAW / RAW+JPEG badge** beneath the viewfinder, or Settings → Photo file format.

| Choice | Camera outputs requested | Files saved |
|---|---|---|
| JPEG only | Preview + JPEG (existing optional JPEG pipelines remain available) | `.jpg` |
| RAW (DNG) only | Normal preview + `RAW_SENSOR`; **no still-JPEG reader/target** | `.dng` |
| RAW + JPEG | Preview + `RAW_SENSOR` + JPEG in one still request | Matching-stem `.dng` and `.jpg` |

RAW options require the selected physical/logical route to advertise RAW capability and a usable normal-resolution `RAW_SENSOR` size. JPEG+RAW also needs a JPEG output. A successful advertised capability check is not proof that every firmware stream combination works: rejected combinations are retried with a bounded set of advertised native sizes and then reported as an error. **The app does not silently save JPEG instead of requested RAW.** Selecting a camera incompatible with the current file format asks for confirmation before changing format.

## What is—and is not—in the DNG

`DngCreator` writes the actual acquired `RAW_SENSOR` image with matching capture-result metadata and the selected camera's characteristics. Image timestamps—not callback arrival order—bind RAW, metadata and the optional JPEG. Physical output routes require their physical capture metadata; a parent profile is not substituted when it is missing.

DNG sensor data receives **no app recipe, grain, RGB calibration, digital crop, interpolation or AI/ML enhancement**. Optional looks apply only to JPEG. RAW-only uses the ungraded Camera2 preview, not a decoded sensor-RAW preview. A RAW editor can render the DNG differently from that preview or a processed JPEG.

RAW modes use standard Camera2, native output and **uncropped 1× framing**. OEM HDR/Night, JPEG fusion, PRIVATE ZSL and enlargement are not used in RAW sessions. Both paired files originate from the same timestamp-matched request, but their native dimensions, aspect framing/active-area treatment and rendering may differ. DNG orientation is recorded as metadata rather than rotating/reconstructing Bayer pixels.

RAW uses normal-resolution advertised sizes, not a claim of maximum-pixel-mode sensor access. Candidate streams are capped at **55 million pixels** for memory safety and are never resized into fake RAW. DNG writing streams to storage rather than allocating a second full RAW byte array. An acquired image retains its reader until writing finishes, even when the camera session closes.

The device can still apply hardware/firmware corrections before exposing RAW, and its preview/JPEG ISP remains proprietary. This is not a promise of a completely unprocessed sensor signal or an AI-free firmware stack. [Natural-capture scope](NATURAL_CAPTURE.md).

## Storage and gallery

- Files go to `Pictures/MobileFilmLab/` with the correct JPEG/DNG MIME types.
- RAW+JPEG files share a filename stem. Android 10+ keeps rows pending until both byte streams are complete; failures attempt to remove every created row/file. Publishing two rows is not a universal filesystem-transaction guarantee.
- Android 9 lacks pending-row support; rollback still removes failed outputs, but another gallery could briefly see an in-progress row.
- The gallery lists both types, shares with the correct MIME type and offers **Open DNG in compatible editor**. DNG preview/zoom depends on Android's decoder; an external RAW editor may be necessary. A preview failure does not turn the file into JPEG or alter the sensor data.
- DNGs can be large and slow to write. Do not assume RAW is faster, smaller, denoised or better-looking without editing.

## Validation boundary

Automated tests cover discovery-list policy, format compatibility, preference migration, frame matching, retained-image ownership and pending-file/rollback behaviour. Native UI renders demonstrate the actual controls with **illustrative capabilities**, not physical camera validation. JVM tests do not emulate a real sensor or prove that a DNG from every handset opens correctly. Test all three formats on the actual device, inspect with a RAW-capable editor and report dimensions, orientation, exposure and any rejected stream combination.
