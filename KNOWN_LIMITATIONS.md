# Known Limitations & Hardware Caveats

### 1. Proprietary Hardware Pipelines (Sony vs. Android / ColorOS)
* **Sony-Specific Registrations:** The upstream Recipe Lab project writes directly into Sony camera body settings stores (CameraEx API on Sony PMCA bodies like the A6000). Android devices running ColorOS do not expose Sony Creative Style, Sony Picture Effects, or DRO hardware registers.
* **Approximation Status:** All recipe simulations are computational approximations developed for Android Camera2. They do not claim exact parity with Sony, Fuji, Kodak, or Leica proprietary color science.

### 2. Third-Party Access to Multi-Camera Arrays on ColorOS
* **Physical Camera Isolation:** On certain Oppo and OnePlus ColorOS builds, third-party applications querying `CameraManager.getCameraIdList()` are presented with a primary logical camera, while direct access to individual physical periscope lenses may be restricted or merged through logical zoom streams.
* **Graceful Degradation:** MobileFilmLab dynamically inspects physical camera characteristics. If individual physical camera opening is blocked by device firmware, the app falls back transparently to logical sensor zoom and reports the hardware status in the **Camera Diagnostics** screen.

### 3. RAW / DNG Support
* **Sensor RAW Exposure:** RAW (`RAW_SENSOR`) output is supported only when the active camera reports RAW capability via Camera2 characteristics.
* **Recipe Application:** Per photographic conventions, RAW/DNG files contain unprocessed sensor data; recipe color science is baked into the companion high-resolution JPEG, with recipe metadata preserved in sidecar/EXIF records.

### 4. Photographic Focus (Photos Only)
* MobileFilmLab is exclusively designed for still photography. Video capture and high-speed video recording are intentionally not implemented to keep the interface focused, lightweight, and responsive.
