# Architecture — alpha.9

## Data flow

Compose controls → `CameraViewModel` state/snapshot → `CameraEngine` (Camera2 handler) → timestamp-correlated JPEG + capture result → worker-thread `ImageProcessor` → atomic MediaStore publication.

### Camera ownership

A dedicated handler owns open/close, sessions, requests, readers, reprocessing images and continuation matching. Generation/session epochs discard stale callbacks after lens or lifecycle changes. Activity pause closes hardware and cancels pending captures; no blocking semaphore/join runs on the UI thread. Missing camera resources return failure, not a dummy photo. Capture timeouts release shutter state. The native preview surface is retained from the alpha viewfinder path.

`CameraDiscovery` enumerates direct IDs and physical IDs behind logical cameras, derives approximate 35mm equivalent from focal length and sensor diagonal, and records capability flags. Physical output configurations use a logical open ID where direct opening is unavailable. Restricted lenses are not replaced without telling the user. The UI slot names are separate from measured/detected focal metadata.

Advertised normal, high-resolution and maximum-resolution JPEG sizes feed `ResolutionSelector`. Maximum mode is registered on its capture output/request, with a bounded smaller-stream fallback if the combination fails. Preview crop is not repeated in the still request: capture is full-stream, followed by one native software crop.

### Capture paths

1. **Standard:** supported ISP quality modes, AF/AE wait/precapture with deadlines, full-stream JPEG; image and result correlate by sensor timestamp. EXIF never invents an ISO, exposure, camera sensor model or flash firing event.
2. **OEM extension:** advertised HDR/Night `CameraExtensionSession`, independent supported stream sizes and bounded session fallback. The OEM owns exposure/flash. Extensions are not a promise of stock-app parity.
3. **Fusion:** three independently correlated −1/0/+1 requests. Uses actual metadata when available for exposure normalization. JPEG decoding is capped near 12MP; normalized thumbnail translation alignment rejects low-confidence frames; linear-light weighted rows reject moving pixels against the middle reference. No RAW/neural/super-resolution claim.
4. **PRIVATE ZSL:** capability/combination-gated input reader and ring of up to three Image/result pairs. Eligible frames are queued to `ImageWriter` and reprocessed to JPEG. The ring is cleared and images closed on reconfiguration. Flash/manual/crop/stale/unconverged cases use ordinary capture. Severe thermal state disables the ring.

### Rendering and memory

`Oppo Natural (Camera2)` with no edits/crop/grain/calibration preserves original compressed pixel data and adds EXIF only. All other captures decode honouring EXIF orientation, crop once, apply the shared recipe/calibration colour matrix, bounded ISO-aware luminance sharpening and opt-in grain, then encode at JPEG quality 98. Optional resizing is explicitly ordinary interpolation. Row buffers avoid full-frame integer arrays for sharpening/grain; editing decode size respects the app heap budget. Gallery hardware is closed before higher-resolution photo decoding.

The TextureView's layer paint applies the shared colour matrix; recipe thumbnail samples use that matrix too. A sampled ungraded preview supports the split comparison. Preview does not claim to simulate final sharpening/grain or every OEM tone operation. Grid is composition-only; accelerometer level and orientation listeners are lifecycle-managed.

### Persistence and interchange

The original 77 recipes remain in `RecipeCatalog`, plus three baseline profiles. SQLite stores customs and favourites; imports validate first, assign new custom IDs, then save transactionally. JSON has a typed/versioned envelope and legacy array support. QR payloads are bounded compressed JSON with decompression limits; the image codec uses ZXing. SAF handles document/QR PNG exports and user-selected imports. No externally exported file provider or broad modern photo-library access is needed.

### Privacy and releases

Scoped MediaStore writes are pending until complete; failures remove partial rows. The app's output gallery derives EXIF and dimensions from real saved files. Only an explicit release-check button contacts GitHub. No photo network path, analytics or automatic installation exists. The alpha updater excludes beta/RC. CI runs production-function tests, Robolectric native-graphics tests and lint. Signing secrets are isolated in release jobs; version/package/signature checks precede prerelease publishing. Beta promotion has a separate approval guard and uncompleted handset gates.
