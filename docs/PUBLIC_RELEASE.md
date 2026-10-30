# Current public release and withdrawn binary downloads

The owner authorised **v0.2.0-beta.3 / code 12** as the next regular Latest GitHub Release, improving Settings readability and replacing cycling Pro controls with slider editors. Dynamic cameras and real RAW/DNG formats are retained. The established package and signing key are unchanged; the app remains beta-qualified, not universal hardware certification.

[Current release](https://github.com/TheSwagInDisguise/MobileFilmLab/releases/latest) · [Project website](https://theswagindisguise.github.io/MobileFilmLab/) · [Testing guide](TESTING.md) · [Pro controls](PRO_CONTROLS.md)

Publication order is deliberate: pass CI/lint, build/sign, verify the downloaded new APK, update and verify the website, **then** withdraw the previous release entry/assets and signed-APK workflow artifacts. Do not break the current download before its replacement is ready.

The nine alpha and beta.1 binary releases were previously withdrawn. Beta.2 is superseded only once beta.3 and the website are verified. All source tags and commit history are preserved; removing binary downloads does not erase historical source or revoke copies already downloaded, redistributed or cached elsewhere.

APK bytes are immutable after publication. Bugs require a new code/version/tag, not replacement of a published APK or moving a tag. Exact owner-approved tags control publication and regular-release classification; this is not blanket permission to recreate withdrawn downloads.

## Checks and evidence boundaries

The local Android suite passes **116 tests in 24 classes**, including production Compose slider interactions, larger-text Settings scrolling and five native documentation renders. Release lint passes. The exact tag also passed remote CI, signed-release checks and independent downloaded-APK verification. [Beta.3 QA scope](QA_BETA_3.md).

A physical camera is not connected here. Real handset slider response, long exposures and actual sensor DNG validity still require device/editor testing. Native UI renders contain a labelled generated scene and illustrative state, not handset captures.

Historical [beta.2 publication record](archive/PUBLIC_RELEASE_BETA_2.md) · [beta.1 publication record](archive/PUBLIC_RELEASE_BETA_1.md) · [Signing](RELEASE_SIGNING.md) · [Validation status](BETA_READINESS.md)

## Verified beta.3 build

- Immutable app source tag: `8746d4094b6d4c2e2f203886d9fd9002f0431bb7`.
- [Main CI](https://github.com/TheSwagInDisguise/MobileFilmLab/actions/runs/41060051050) and [signed release](https://github.com/TheSwagInDisguise/MobileFilmLab/actions/runs/41060737213) succeeded.
- 116 Android tests / 24 classes and six approval tests passed; release lint and downloaded APK package/version/alignment/established-certificate checks passed.
- APK: 40,947,054 bytes; SHA-256 `5f1dfb769e66b73218d4102c77ff5038c045f5542356ea89394ceade3af0893d`.
- The site is updated separately without rebuilding/replacing these immutable APK bytes. Older binaries are retired only after hosted-site verification.
