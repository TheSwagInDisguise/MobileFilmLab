# Privacy — alpha.9

- Camera: used for local preview and still photographs only; no audio/video recording.
- Storage: JPEGs published to `Pictures/MobileFilmLab/` via MediaStore. Android 9 needs legacy read/write permission; Android 10+ uses scoped access to the app's output. Gallery listing is limited to its folder (up to 500 images).
- Presets: customs/favourites and per-lens settings stored locally in the app's SQLite/preferences. Android system backup is enabled and may back up app data according to the user's Android backup configuration.
- Imports/exports: files/QR images selected through Android's picker; contents processed locally. QR PNG and recipe exports use user-chosen documents. Sharing a photo invokes an explicitly chosen Android share target.
- Network: Internet permission supports the explicit **Check alpha/stable updates** button. It queries the public GitHub releases API, without an account token. GitHub receives normal network information such as IP address and an app/version User-Agent. No background check, analytics, ads, crash telemetry or photo upload exists.
- Installation: release pages open in the browser; no silent APK download, package installation or beta promotion.
- Diagnostics: device model, Android version and public camera capabilities/settings. No device serial, account data, credentials or photo pixels are included. The owner decides whether to copy/share it.
