# Release identity

- Package: `com.mobilefilmlab.camera` (unchanged across release APKs).
- Current alpha candidate: `0.1.0-alpha.9`, versionCode `9`.
- Established RSA-4096 signing certificate SHA-256:

```text
8D:CD:B6:C6:5B:DA:C8:0A:48:8D:06:3D:70:26:B3:D4:5E:AA:5B:4B:C0:02:E2:D4:C8:4A:CB:C9:73:51:F4:1A
```

The keystore and passwords are repository secrets, not source/artifacts. Release CI decodes the keystore into a temporary ignored folder, builds using environment-provided signing credentials, checks the signature/certificate, verifies the package/version/alignment, uploads only the APK/checksums/test checklist, and removes key material afterwards.

All `-alpha.*` tags publish as **prereleases**. A non-alpha tag is refused unless the repository approval variable `BETA_RELEASE_APPROVED` is explicitly `true`; it must not be enabled before the owner approves promotion. Tag name must exactly match Gradle's version. The workflow never creates/moves a beta tag automatically.

Verify the downloaded APK with Android build-tools:

```sh
sha256sum -c checksums.txt
apksigner verify --verbose --print-certs MobileFilmLab-v0.1.0-alpha.9-signed.apk
```

The certificate above is public, not a secret. Per-file SHA-256 changes with each build; use the checksum attached to that release. Direct APK updates require the same package/key and a greater versionCode.
