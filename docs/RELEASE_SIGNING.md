# Release identity and signing

- Package: **`com.mobilefilmlab.camera`**, unchanged across release APKs.
- First public beta: **`0.2.0-beta.1`**, versionCode **10**; upgrades alpha.9/code 9 without a new signing key.
- Established RSA-4096 signing certificate SHA-256:

```text
8D:CD:B6:C6:5B:DA:C8:0A:48:8D:06:3D:70:26:B3:D4:5E:AA:5B:4B:C0:02:E2:D4:C8:4A:CB:C9:73:51:F4:1A
```

The certificate fingerprint is public; the keystore, passwords and repository credentials are not. They are never source files or release assets. Release CI decodes the existing keystore into a temporary ignored directory, builds with secret-provided credentials, verifies the signature/certificate, package, version and alignment, uploads only approved deliverables, and removes the key material afterwards.

## Approval and immutable releases

- The source version, version-specific release notes, tag and tagged commit must agree.
- The current publication workflow requires **`APPROVED_RELEASE_TAG` to equal the exact tag before checking out any historical release source**. The Python identity helper also blocks unapproved non-alpha publication. A beta approval cannot approve another beta or a stable tag; withdrawn old downloads must not be recreated.
- The owner explicitly authorised **`v0.2.0-beta.1`** for public testing. Set the repository variable to that tag only after authorisation; no broad stable-release approval is implied.
- New prerelease-version tags default to GitHub prereleases. By explicit owner request, the existing **v0.2.0-beta.1** entry was promoted to a regular **Latest** release without changing its APK, tag or app version. This is not a new stable-version build. See [publication status](PUBLIC_RELEASE.md).
- The workflow refuses to overwrite an existing release. Fixes require a new version/code/tag, not replacement of published APKs or moving a tag.
- Main-branch CI verifies source identity, release-guard tests, documentation links, Android tests, lint and debug assembly. Signed-release CI repeats release checks, tests, lint, build and APK verification.

## Verify a download

Download the APK and `checksums.txt` from the same official release. With Android build-tools available:

```sh
sha256sum -c checksums.txt
apksigner verify --verbose --print-certs MobileFilmLab-v0.2.0-beta.1-signed.apk
zipalign -c 4 MobileFilmLab-v0.2.0-beta.1-signed.apk
```

Compare the reported certificate SHA-256 to the value above. Per-file checksums change with each build. In-place Android updates require the same package/key and a greater versionCode; installation/data-retention behaviour must still be tested on actual devices. Debug builds use a separate package and are not a signed-release replacement.
