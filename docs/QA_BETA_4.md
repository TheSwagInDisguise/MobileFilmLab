# Beta.4 QA scope and release gates

Version `0.2.0-beta.4`, code `13`, package `com.mobilefilmlab.camera`. This is the owner-authorised Fold5/processing beta, not a stable release or handset certification. The established release signing key must be reused.

## Automated coverage

The suite exercises production code with controlled/synthetic data:

- Samsung Fold5 regional model detection; other manufacturers/models retain the generic policy.
- Real advertised native stream selection, normal/high-resolution preference, opt-in maximum size, memory ceiling, bounded fallback order and fusion/ZSL restrictions; no invented camera IDs or resolutions.
- Rendering/native-size preference persistence and safe migration defaults; factory IDs, custom names, favourite state and the complete catalogue survive the Fold5 display alias.
- Conventional noise filtering on deterministic synthetic noise, exact flat-colour preservation, brightness/equal-luma colour-edge protection, monotonic/bounded adaptive tone and conservative high-ISO sharpening.
- IEC sRGB round trips, real metadata exposure ratios, rejection of invalid/missing values and recovered-highlight display mapping with an 18% anchor.
- Real native JPEG decode/encode/EXIF, original pixel preservation, balanced opt-in and crop dimensions; actual JPEG fusion registration, missing-metadata fallback, same-luma/different-colour motion and clipped-reference highlight recovery from a controlled darker exposure.
- Genuine RAW policy/storage/ownership/matching regression tests; these validate software paths, **not a physical sensor or actual DNG image validity**.
- Existing Pro controls/readability, dark-theme, format, recipe/interchange, framing/orientation, lifecycle and update-check regression coverage.
- Native Android/Compose documentation renders for standard portrait, narrow 336dp cover and wide 700dp inner-display layouts. They use a labelled generated scene and injected demo camera metadata—not a Fold5 emulator, device capture, folding event or quality measurement.

Release gates also include Python approval-guard tests, documentation/site checks, Android lint, CI, a newly versioned immutable tag, signed APK package/version/alignment/established-certificate checks, independent public download/hash verification and a separately verified website update. Previous binaries are withdrawn only after those gates; source tags/history are retained.

## Local results

- **147 Android tests / 28 classes**, zero failures/errors/skips, including eleven native illustrative UI renders.
- Six Python publication-approval tests and documentation link/source-version checks pass.
- Release Kotlin compilation and `lintRelease` pass locally. Remote CI/signing and independently downloaded APK/site verification remain further publication gates, recorded with the verified build after success.

## Explicitly not established

No phone is attached to this environment. The user reported working Fold5 capture with poor quality but did not supply firmware, a capability report or example originals. Therefore there is no measured Fold5 colour/noise/detail/HDR improvement, shutter-time/battery benchmark, full stock-camera parity, real fold/unfold session test, sensor DNG/editor validation, hidden-camera access guarantee or independent security audit. The Fold5 policy is **not measured factory calibration**.

Noise/tone/detail trade-offs can soften texture or alter brightness. JPEG fusion remains experimental, translation-only and vulnerable to movement/parallax and opaque ISP tone/WB changes. Optional OEM extensions remain hardware/firmware-dependent. Original rendering stays the default; RAW pixels are not app-processed. No app-added generative/ML enhancement or cloud photo processing is introduced; proprietary firmware internals remain outside that promise.

## Reproduce

JDK 17, Android SDK 34, one Gradle worker/in-process Kotlin are configured by the project:

```sh
./gradlew testDebugUnitTest --no-daemon
./gradlew lintRelease --no-daemon
python3 -m unittest discover -s scripts -p 'test_*.py'
python3 scripts/verify_release.py
python3 scripts/check_docs.py
python3 scripts/check_site.py
```

`check_site.py` is run after the verified release digest/size is written to the site; a source-only preparation commit may temporarily retain the previous live-site release metadata. Very small build environments should compile and run tests in separate invocations with a smaller Gradle heap for the test step; do not run overlapping compiler/test/Chromium processes. If `/tmp` is tmpfs, set `TMPDIR` and Java’s `java.io.tmpdir` to disk-backed scratch so extracted native fonts do not consume the RAM budget. Publication CI runs the complete suite without local task exclusions.

[Fold5 setup and hardware checklist](GALAXY_Z_FOLD5.md) · [Testing](TESTING.md) · [Signing](RELEASE_SIGNING.md) · [Disclaimer](DISCLAIMER.md)
