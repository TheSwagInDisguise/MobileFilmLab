# Contributing to MobileFilmLab

Code review, reproducible bug reports and careful handset testing are welcome. Please be respectful and constructive.

## Device testing

The primary target is **Oppo Find X9 Ultra**. We plan to expand device compatibility with beta testers' help. Use the [public test checklist](docs/TESTING.md), [bug form](https://github.com/TheSwagInDisguise/MobileFilmLab/issues/new?template=bug_report.yml) or [device report](https://github.com/TheSwagInDisguise/MobileFilmLab/issues/new?template=device_support.yml). Include app/Android/firmware versions and only non-private samples. Public reports are not a place for tokens, keystores or sensitive photos.

## Development

Use JDK 17, Android SDK/build-tools 34 and the included Gradle wrapper (8.7). Android Studio Iguana or newer can open the project. Configure your SDK through Android Studio or an ignored `local.properties`.

```sh
git clone https://github.com/TheSwagInDisguise/MobileFilmLab.git
cd MobileFilmLab
python3 -m unittest discover -s scripts -p 'test_*.py'
python3 scripts/verify_release.py
python3 scripts/check_docs.py
./gradlew testDebugUnitTest --no-daemon
./gradlew lintRelease --no-daemon
./gradlew assembleDebug --no-daemon
```

The native-graphics suite includes actual UI documentation rendering. See [screenshot reproduction](docs/SCREENSHOTS.md); do not present mockups or simulated scenes as handset captures.

## Project website

The static project site lives in `site/` and deploys through GitHub Pages. Run `python3 scripts/check_site.py`; see [website development](docs/WEBSITE.md). Preserve the distinction between no app-added AI enhancement, device ISP processing and AI-assisted development.

## Pull requests

- Keep changes focused and add tests for behaviour, failure paths and migration/format changes.
- Preserve the complete upstream catalogue, credits and licence notices.
- Do not claim stock-camera parity, extra captured detail, full lens access or hardware validation without evidence.
- Do not add analytics, advertising, cloud photo processing, private-API workarounds or video modes.
- Never commit credentials, keys, user photos, local SDK paths or build outputs.
- AI-assisted contributions are welcome with disclosure, human review and reproducible testing. See [AI-development disclosure](docs/AI_DISCLOSURE.md).
- Contributions must be compatible with **GPL-3.0-or-later**. See [upstream attribution](UPSTREAM_ATTRIBUTION.md) and [third-party notices](THIRD_PARTY_NOTICES.md).

Release publication uses exact-tag owner approval and existing signing secrets. Do not move published tags, replace released APKs or change the package/signing identity. See [release signing](docs/RELEASE_SIGNING.md).
