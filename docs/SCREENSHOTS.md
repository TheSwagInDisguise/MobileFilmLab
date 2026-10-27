# Screenshots and presentation-asset provenance

## Published gallery

| File | What it shows |
|---|---|
| [auto.png](screenshots/auto.png) | Actual AUTO screen, neutral camera baseline, favourite chips and lens controls. |
| [pro.png](screenshots/pro.png) | Actual PRO screen with illustrative manual values and a saved custom look. |
| [recipes.png](screenshots/recipes.png) | Actual modal recipe drawer, colour-transformed demo thumbnails and Tone / Warmth pad. |
| [formats.png](screenshots/formats.png) | Actual JPEG / RAW / RAW+JPEG dialog with illustrative advertised RAW capability. |

These are **production Android/Jetpack Compose components rendered by Robolectric native graphics (API 34)** at **864 × 1920** pixels. The interface is not AI-invented, hand-redrawn in HTML or composited from design mockups. The same screen, drawer, controls, typography and colour-matrix implementation used by the app are exercised by `DocumentationScreenshotsTest`.

**Important:** no handset is connected to this rendering workflow. The camera scene is an **AI-generated coastal landscape**, not a camera capture or a verified photograph of a specific location. Demo camera availability, focal length, resolution, ISO, shutter and white balance are illustrative; they do not prove any device capability. The displayed demo resolution is derived from the source image, not an advertised sensor resolution. `Coastal Gold` and `Soft Monochrome` are test-created custom presets demonstrating the app's existing save workflow, not extra built-in catalogue entries. The generated scene says nothing about real-camera image quality, HDR, fusion or ZSL performance.

A `DEMO SCENE` badge appears in the viewfinder. Beta.2 fixtures show three illustrative rear routes labelled 14mm, 23mm and 83mm; these are test inputs, not a verified lens list for any phone. The format dialog uses explicitly simulated RAW capability and does not prove that a DNG was captured. Floating windows use their Android window gravity in the headless compositor; rendering can still differ from actual handset window metrics.

## Reproduce

With JDK 17 and Android SDK 34 configured:

```sh
./gradlew :app:testDebugUnitTest \
  --tests 'com.mobilefilmlab.camera.DocumentationScreenshotsTest' \
  --rerun-tasks --no-daemon
mkdir -p docs/screenshots
cp app/build/outputs/documentation-screenshots/*.png docs/screenshots/
python3 scripts/check_docs.py
```

The test renders Android windows, including the real modal bottom sheet, into PNGs and rejects unexpectedly blank output. It also runs in the ordinary unit-test suite; CI uploads its renders with QA reports. System fonts/rendering differences can change pixels across environments; these are documentation renders, not a claim of bit-for-bit handset screenshot parity.

The demo input is supplied through an inspection-only composition provider. It requires a **debug build AND `LocalInspectionMode`**, has no intent, user setting or release-mode entry point, and never replaces the real camera path in a signed release. The scene is in `docs/`, not Android assets, so it is not bundled in the APK.

## Website copies

The project site uses lossless WebP copies in `site/assets/app-*.webp`. Their decoded pixels were checked against these original PNG renders; the format conversion does not invent or alter the interface. The same demo-scene disclosures appear on the website.

## Assets

- `docs/assets/demo-scene.png`: AI-generated demonstration landscape, 896 × 1200 pixels; created for this documentation. No user photographs or vendor artwork used.
- `docs/assets/icon.png`: copy of the project's previously approved original film-canister branding asset in `design/logos/`; AI-assisted design under human direction.
- `docs/screenshots/*.png`: actual native UI renders described above, containing the generated demo scene.
- `design/`: historical design proposals, explicitly archived as mockups.

Please submit real handset screenshots through an issue only if you are comfortable making them public. Remove private images, location data and identifying details first. Do not relabel these demo renders as genuine device captures.
