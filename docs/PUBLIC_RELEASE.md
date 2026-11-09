# Current public release and withdrawn binary downloads

The owner authorised **v0.2.0-beta.4 / code 13** as the next regular Latest GitHub Release for Fold5 compatibility and optional conventional JPEG/HDR processing. Oppo Find X9 Ultra remains the primary target. Original JPEG, real RAW/DNG, full recipes/custom data and the established package/signing key are preserved. The app is beta-qualified, not handset-certified.

[Current release](https://github.com/TheSwagInDisguise/MobileFilmLab/releases/latest) · [Website](https://theswagindisguise.github.io/MobileFilmLab/) · [Fold5 setup](GALAXY_Z_FOLD5.md) · [Testing](TESTING.md)

Publication order is deliberate: pass CI/lint, build/sign a new immutable tag, independently verify the downloaded APK, update and verify the website, **then** withdraw the superseded release entry/assets and signed-APK workflow artifacts. Do not break the current download before its replacement is ready. Exact-tag approval is not permission to recreate withdrawn historical downloads.

The nine alpha, beta.1 and beta.2 binary releases were previously withdrawn. Beta.3 is superseded only after beta.4 and the website pass verification. All source tags and history are retained; previously downloaded/cached/redistributed copies cannot be revoked. Published APK bytes and tags are never overwritten or moved to fix a bug.

## Evidence boundaries

[Beta.4 QA scope and publication gates](QA_BETA_4.md) covers numerical/policy, synthetic native JPEG/HDR/motion, RAW/persistence, existing regressions and narrow/wide illustrative UI renders. Release CI also checks lint, signing identity, package/version and alignment; independent download/site verification is required before old-binary withdrawal.

A physical camera is not connected here. The reporting Fold5 already captures but has poor image quality; no firmware, capability report or original samples were provided. No real-world quality gain, stock-camera parity, speed/battery benchmark, fold transition or DNG/editor validation is claimed. Native renders use a labelled generated scene and illustrative state, not handset images.

Historical records: [beta.3](archive/PUBLIC_RELEASE_BETA_3.md) · [beta.2](archive/PUBLIC_RELEASE_BETA_2.md) · [beta.1](archive/PUBLIC_RELEASE_BETA_1.md). [Signing](RELEASE_SIGNING.md) · [Validation status](BETA_READINESS.md) · [Disclaimer](DISCLAIMER.md).

## Verified beta.4 app build

- Immutable source/tag: `1a33ea3bfb24341aa674fa97285f21a67a901aa1` / `v0.2.0-beta.4`.
- [Main CI](https://github.com/TheSwagInDisguise/MobileFilmLab/actions/runs/42617697895) and [signed release](https://github.com/TheSwagInDisguise/MobileFilmLab/actions/runs/42618636353) succeeded; the downloaded signed-workflow QA artifact independently confirms **147 Android tests / 28 classes**, zero failures/errors/skips and eleven native illustrative renders.
- Six Python guard tests, Android lint and APK package/version/alignment/established-certificate checks passed.
- The unauthenticated public APK download matched the published digest: **40,976,138 bytes**, SHA-256 `337b2fffe31afef405cfacd945ade6ada14f224ca08d5b71e9537c009cef031a`.
- VersionCode **13**, package `com.mobilefilmlab.camera`. Signing certificate SHA-256: `8D:CD:B6:C6:5B:DA:C8:0A:48:8D:06:3D:70:26:B3:D4:5E:AA:5B:4B:C0:02:E2:D4:C8:4A:CB:C9:73:51:F4:1A`. No new signing key was generated.
- These checks do not establish real Fold5 quality, folding, thermal behaviour or sensor DNG/editor validity. Website deployment and previous-binary withdrawal are verified separately, without rebuilding/replacing this APK.
