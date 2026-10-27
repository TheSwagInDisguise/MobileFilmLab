# Current public release and withdrawn binary downloads

The owner authorised **v0.2.0-beta.2 / code 11** as the next regular Latest GitHub Release, with dynamic camera selection and RAW/DNG formats. It retains the established package and signing key; its version is still beta-qualified, not a new v0.2.0 stable build or hardware certification.

[Current release](https://github.com/TheSwagInDisguise/MobileFilmLab/releases/latest) · [Project website](https://theswagindisguise.github.io/MobileFilmLab/) · [Testing guide](TESTING.md)

Publication order is deliberate: pass CI/lint, build/sign, verify the downloaded new APK, update and verify the website, **then** withdraw the previous release entry/assets and signed-APK workflow artifacts. Do not break the current download before its replacement is ready.

The nine alpha release downloads were previously withdrawn. Beta.1 is superseded when beta.2 and the website are verified. All source tags and commit history are preserved; removing binary downloads does not erase historical source or revoke copies already downloaded, redistributed or cached elsewhere.

APK bytes are immutable after publication. Bugs require a new code/version/tag, not replacement of a published APK or moving a tag. Exact owner-approved tags control publication and regular-release classification; this is not blanket permission to recreate withdrawn downloads.

Historical [beta.1 publication record](archive/PUBLIC_RELEASE_BETA_1.md) · [Signing](RELEASE_SIGNING.md) · [Validation status](BETA_READINESS.md)
