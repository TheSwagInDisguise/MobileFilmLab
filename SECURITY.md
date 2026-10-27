# Security and privacy

MobileFilmLab is an experimental public beta developed substantially with AI assistance. It has not undergone an independent security audit. Please report reproducible problems and do not rely on it as the only camera for irreplaceable photographs.

## Data handling

- No analytics SDKs, advertising, crash telemetry or cloud photo processing/upload.
- Camera preview, still capture, recipes, QR-image processing and gallery operations are local.
- The app **does declare `android.permission.INTERNET`**. It is used only for the user-initiated public GitHub release check; no account token is bundled, and no background checking or silent APK installation occurs.
- GitHub receives normal request information, such as the IP address and app/version User-Agent, during that check.
- Android system backup is enabled and follows the user's backup configuration. Explicit sharing sends a photo to the share target the user selects.
- See the [privacy notice](docs/PRIVACY.md) for permissions, storage and diagnostics.

## Release integrity

The release package and established RSA-4096 certificate remain unchanged. Keystore/passwords and repository credentials are secret-managed; they must never be committed, logged or attached to an issue. CI verifies source/tag/version, tests, lint, package, signature, certificate and alignment before publication. Exact-tag approval is required for non-alpha releases. Published APKs/tags are not replaced. See [release verification](docs/RELEASE_SIGNING.md).

Checksums identify the downloaded file; they are not a substitute for verifying the signing certificate or downloading from the official release page.

## Reporting a concern

For ordinary bugs, use the [bug form](https://github.com/TheSwagInDisguise/MobileFilmLab/issues/new?template=bug_report.yml). For a potentially sensitive security issue, first request a private reporting channel from the maintainer without publishing exploit details or personal data. This project does not promise a dedicated security team or response SLA.

Never post tokens, passwords, keystores, private photos or identifying diagnostics in a public issue. If you accidentally expose a credential, revoke it promptly; deleting a public post alone does not make it secret again.
