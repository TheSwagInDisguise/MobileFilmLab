> Historical record for beta.2. Current install instructions are in [the current publication page](../PUBLIC_RELEASE.md); this is not permission to recreate old binaries.

# Historical beta.2 publication record

The owner authorised **v0.2.0-beta.2 / code 11** as the next regular Latest GitHub Release, with dynamic camera selection and RAW/DNG formats. It retains the established package and signing key; its version is still beta-qualified, not a new v0.2.0 stable build or hardware certification.

[Current release](https://github.com/TheSwagInDisguise/MobileFilmLab/releases/latest) · [Project website](https://theswagindisguise.github.io/MobileFilmLab/) · [Testing guide](../TESTING.md)

Publication order is deliberate: pass CI/lint, build/sign, verify the downloaded new APK, update and verify the website, **then** withdraw the previous release entry/assets and signed-APK workflow artifacts. Do not break the current download before its replacement is ready.

The nine alpha release downloads were previously withdrawn. Beta.1 is superseded when beta.2 and the website are verified. All source tags and commit history are preserved; removing binary downloads does not erase historical source or revoke copies already downloaded, redistributed or cached elsewhere.

APK bytes are immutable after publication. Bugs require a new code/version/tag, not replacement of a published APK or moving a tag. Exact owner-approved tags control publication and regular-release classification; this is not blanket permission to recreate withdrawn downloads.

Historical [beta.1 publication record](PUBLIC_RELEASE_BETA_1.md) · [Signing](../RELEASE_SIGNING.md) · [Validation status](../BETA_READINESS.md)

## Verified beta.2 build

- Source tag commit: `b4a6e1d1c0f8eae74531dc40ed8794c75db493be`.
- [Main CI](https://github.com/TheSwagInDisguise/MobileFilmLab/actions/runs/40474427508) and [signed release](https://github.com/TheSwagInDisguise/MobileFilmLab/actions/runs/40475125278) succeeded.
- 95 Android tests and six publication-approval tests passed; release lint and downloaded APK package/version/alignment/certificate checks passed.
- APK SHA-256: `b85473ac25aeea56c52a44de731b3712835c8db715ed39ba391bfb203bdd210b`.
- Real handset RAW/DNG validation remains pending; automated checks are not sensor certification.
