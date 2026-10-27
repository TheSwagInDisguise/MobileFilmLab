# Release identity and signing

- Package: **`com.mobilefilmlab.camera`**, unchanged across release APKs.
- Current feature release: **`0.2.0-beta.2`**, versionCode **11**; upgrades beta.1/code 10 with the same signing key.
- Established RSA-4096 signing certificate SHA-256:

```text
8D:CD:B6:C6:5B:DA:C8:0A:48:8D:06:3D:70:26:B3:D4:5E:AA:5B:4B:C0:02:E2:D4:C8:4A:CB:C9:73:51:F4:1A
```

The public certificate fingerprint is not a secret. The keystore, passwords and repository credentials are never source files or release assets. Release CI decodes the existing secret-managed key into a temporary ignored directory, builds and verifies the package/version/certificate/alignment, uploads only approved deliverables, and removes key material afterwards.

## Exact-tag approval and release classification

- Source version, version-specific release notes, tag and tagged commit must agree.
- `APPROVED_RELEASE_TAG` must equal the exact intended tag **before checking out historical release source**. The identity helper also blocks unapproved non-alpha publication. Do not recreate withdrawn downloads.
- The owner explicitly authorised **v0.2.0-beta.2** as the next regular Latest release. `REGULAR_RELEASE_TAG` also names that exact tag, so this beta-version build publishes as a regular GitHub Release. Future prerelease-version tags still default to prereleases unless separately authorised and named.
- A beta suffix remains part of the app version. GitHub's regular-release category does not create a `v0.2.0` stable build or certify camera compatibility.
- The workflow refuses to overwrite an existing release. Fixes need a new version/code/tag; published APKs/tags are not replaced or moved.
- Older binary downloads and their signed-APK workflow artifacts are withdrawn only after a replacement APK and project website are verified. Tags/source history remain public as requested. [Publication policy](PUBLIC_RELEASE.md).

## Verify a download

Download the APK and `checksums.txt` from the same [official release](https://github.com/TheSwagInDisguise/MobileFilmLab/releases/latest). With Android build-tools:

```sh
sha256sum -c checksums.txt
apksigner verify --verbose --print-certs MobileFilmLab-v0.2.0-beta.2-signed.apk
zipalign -c 4 MobileFilmLab-v0.2.0-beta.2-signed.apk
```

Compare the certificate SHA-256 to the value above. Per-file checksums change with each build; use that release's attached checksum. Direct Android updates require the same package/key and a higher code. Install over the existing release and confirm user-data retention; debug builds are separate apps.
