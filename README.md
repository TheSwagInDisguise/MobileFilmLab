# MobileFilmLab

Native Android camera application for the **Oppo Find X9 Ultra**, porting the complete photographic preset experience and 77-recipe catalogue of the **Sony Recipe Lab** project to modern Android camera hardware.

[![CI Status](https://github.com/TheSwagInDisguise/MobileFilmLab/actions/workflows/ci.yml/badge.svg)](https://github.com/TheSwagInDisguise/MobileFilmLab/actions)
[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](LICENSE)

---

## Highlights

* **Oppo Find X9 Ultra Quad-Optics:** Dynamic Camera2 discovery and intelligent lens switching across all 4 rear cameras:
  * **Ultra-wide** (15mm, 0.6×)
  * **Wide / Primary 1-inch** (23mm LYT-900, 1.0×)
  * **Telephoto** (70mm, 3.0×)
  * **Periscope Super-telephoto** (135mm, 6.0× / 10.0×)
* **Honest Crop Framing:** Translucent lens selector with an instant `2×` crop toggle and honest sensor telemetry (`Full sensor`, `Native-pixel crop / no-resample`, `Reduced-resolution crop`, or `Resampled digital crop`).
* **Complete Recipe Lab Catalogue (77 Presets):**
  * **Sony (8):** Factory (ST), PT Portrait, NT Neutral, VV Vivid, VV2, FL Film-like, IN Instant, SH Soft High-key
  * **Fuji Sim (16):** Provia, Velvia, Astia, Classic Chrome, Classic Negative, Nostalgic Neg, Reala Ace, Pro Neg Std, Pro Neg Hi, Eterna, Bleach Bypass, Acros, Acros +Ye, Acros +R, Acros +G, Sepia
  * **Fuji Film (5):** Pro 400H, Fortia 50, Superia 400, C200, Natura 1600
  * **Kodak (14):** Portra 160, Portra 400, Portra 800, Gold 200, Ultra Max 400, Color Plus 200, Ektar 100, Ektachrome E100, Kodachrome 64, Vision3 500T, Vision 200T Asteroid City, Tri-X 400, T-Max, Tri-X 1600
  * **Cine (4):** Cinestill 50D, Cinestill 800T, Classic Cinema, Rec709 Video
  * **Ricoh GR (8):** Positive Film, Negative Film, Bleach Bypass, Retro, Cross Process, Hi-Contrast B&W, Hard Monotone, Soft Monotone
  * **Leica (4):** Contemporary, Classic, Eternal, Monochrom
  * **Hasselblad (1):** HNCS Natural
  * **Canon / Nikon (5):** Canon Standard, Canon Portrait, Canon Faithful, Nikon Flat, Nikon Vivid
  * **Pana / Olympus (4):** L.Monochrome D, L.ClassicNeo, Pop Art, Pale & Light
  * **Other Stocks (3):** Agfa Vista 200, Agfa Ultra 100, Polaroid / Instax
  * **Ilford (5):** HP5, FP4, Delta 100, Delta 3200, Pan F 50
* **2D Photographic Styles Square Pad:** Real-time tactile modulation of tone contrast and white balance warmth/tint.
* **Auto & Pro Modes:** Switch with one tap between an uncluttered point-and-shoot interface and a manual control strip (EV, ISO, Shutter, Kelvin, Focus Peaking).
* **Pure Photo Focus:** Designed exclusively for still photography.
* **Zero Telemetry:** No analytics, no cloud uploads, no internet permission required.

---

## Upstream Attribution & Licensing

MobileFilmLab is an independent derivative work based on the preset engine of **Recipe Lab (Sony PMCA)** by voxivoid.
* Upstream Repository: [https://github.com/voxivoid/recipe-lab-sony-pmca](https://github.com/voxivoid/recipe-lab-sony-pmca)
* Upstream Commit SHA: `aab4b5703a2b77362b83682801c8e50145ddc6ee`
* Licence: **GNU General Public License v3.0 or later (GPL-3.0-or-later)**.
* Detailed attribution: See [UPSTREAM_ATTRIBUTION.md](UPSTREAM_ATTRIBUTION.md).

---

## Building from Source

Requires **JDK 17 or JDK 21** and the Android SDK (API 34).

```bash
# Build debug APK
./gradlew assembleDebug

# Run unit tests
./gradlew test

# Build release APK
./gradlew assembleRelease
```
