# MobileFilmLab — Technical Architecture

## 1. System Overview
MobileFilmLab is a native Android photography application built in **Kotlin** and **Jetpack Compose**, targeting modern multi-camera hardware (specifically the **Oppo Find X9 Ultra**).

The application bridges two distinct domains:
1. **The Recipe Lab Preset Engine:** A port of the 77 built-in photographic recipes from the upstream Sony PMCA project, adapted into a computational color grading pipeline.
2. **Multi-Camera Hardware Abstraction:** Dynamic discovery of the target device's physical camera optics (Ultra-wide 15mm, Primary 1" Wide 23mm, Telephoto 70mm, Periscope 135mm), coupled with honest digital crop framing.

```
┌────────────────────────────────────────────────────────┐
│                   Jetpack Compose UI                   │
│  [Auto / Pro]   [UW | W | 3x | 10x]   [2x Crop]        │
│  [2D Photographic Styles Pad]   [Recipe Sheet]         │
└──────────────────────────┬─────────────────────────────┘
                           │ StateFlow / Events
┌──────────────────────────▼─────────────────────────────┐
│                    CameraViewModel                     │
└──────────────┬───────────────────────────┬─────────────┘
               │                           │
┌──────────────▼─────────────┐   ┌─────────▼─────────────┐
│      Camera2 Engine        │   │   Recipe Repository   │
│  - Dynamic Discovery       │   │  - 77 Catalog Recipes │
│  - Session Management      │   │  - SQLite Database    │
│  - Optical / Zoom Routing  │   │  - YAML / JSON Import │
└──────────────┬─────────────┘   └─────────┬─────────────┘
               │                           │
               │ Raw Capture Frame         │ Active Recipe
┌──────────────▼───────────────────────────▼─────────────┐
│             Computational Image Pipeline               │
│  - Matrix Color Transform & Tone S-Curve               │
│  - Kelvin WB & Amber/Green Tint Multiplier             │
│  - Optical Monochrome Filter Simulation                │
│  - Non-destructive Sensor Crop & Resampling Check      │
│  - EXIF Metadata Tagging (ExifInterface)               │
└──────────────────────────┬─────────────────────────────┘
                           │ Scoped Storage (Atomic)
┌──────────────────────────▼─────────────────────────────┐
│            Android MediaStore (Pictures/)              │
└────────────────────────────────────────────────────────┘
```

## 2. Dynamic Camera Discovery (`CameraDiscovery.kt`)
The app does **not** hardcode camera IDs. It queries Android's `CameraManager` for all available characteristics:
- Hardware level (`LEGACY`, `LIMITED`, `FULL`, `LEVEL_3`)
- Facing direction (`LENS_FACING_BACK`, `LENS_FACING_FRONT`)
- Logical multi-camera status (`REQUEST_AVAILABLE_CAPABILITIES_LOGICAL_MULTI_CAMERA`) and underlying physical camera IDs
- Physical focal lengths ($mm$), equivalent 35mm focal lengths, aperture ($f$-number)
- Active array size and pixel dimensions
- Supported output formats (`JPEG`, `RAW_SENSOR`, `YUV_420_888`)
- Manual sensor control capabilities (Exposure time, ISO, Manual focus distance, AWB lock)

## 3. Honest Crop & Framing Engine (`CropEngine.kt`)
The four physical lenses map to:
- `UW`: Ultra-wide (~15mm, 0.6x)
- `W`: Primary Wide 1-inch (~23mm, 1.0x)
- `3x`: Telephoto (~70mm, 3.0x)
- `10x`: Periscope Super-telephoto (~135mm, 6.0x–10.0x)

Each physical optic provides a direct `2x` crop toggle. The app classifies and displays the honest nature of every frame:
1. `FULL_SENSOR`: Entire available sensor area at native resolution.
2. `NATIVE_NO_RESAMPLE`: A cropped window output at native pixel resolution without interpolation.
3. `REDUCED_RESOLUTION`: Cropped frame output at actual cropped pixel dimensions.
4. `DIGITALLY_RESAMPLED`: Digital zoom enlargement interpolated to output size.

## 4. Recipe Processing Pipeline (`ImageProcessor.kt`)
Because Android devices do not have Sony's internal BIONZ X creative style registers, recipes are translated into an honest mathematical approximation:
- **Tone Curve:** S-curves calculated from recipe contrast ($-3 \dots +3$).
- **Color Matrix:** Scaled saturation and vibrance.
- **Color Temperature & Tint:** Kelvin values translated to CIE daylight chromaticity coordinates; amber-blue and green-magenta offsets applied as linear RGB scaling matrices.
- **Dynamic Range Optimization (DRO):** Shadow-lifting gamma curve approximating Sony DRO levels 1 through 5.
- **Monochrome Optics:** B&W conversion weighted by simulated optical glass filter transmittance (Neutral, Yellow, Red, Green).
- **2D Control Pad (Photographic Styles):** Real-time modulation along Tone ($Y$-axis) and Warmth ($X$-axis).

## 5. Storage & Privacy
- Zero internet permissions, zero analytics, zero crash telemetry, zero advertising.
- All photos saved directly to `Pictures/MobileFilmLab/` using Android Scoped Storage (`MediaStore.Images.Media`).
- Non-destructive processing preserves recipe metadata in standard EXIF tags (`ExifInterface.TAG_USER_COMMENT`).
