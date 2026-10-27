# Privacy — public beta

- **Camera:** local preview and still photographs only; no audio/video recording.
- **Storage:** JPEGs published to `Pictures/MobileFilmLab/` via MediaStore. Android 9 needs legacy read/write permission; Android 10+ uses scoped access to the app's output. Gallery listing is limited to its folder, up to 500 images.
- **Presets:** custom recipes, favourites and per-lens settings are stored locally in SQLite/preferences. Android system backup is enabled and may back up app data according to the user's Android backup configuration.
- **Imports/exports:** files and QR images are selected through Android's picker and processed locally. Recipe/QR exports use user-chosen documents. Photo sharing invokes an explicitly selected Android share target; that other app's data handling is outside MobileFilmLab's control.
- **Network:** Internet permission supports the explicit **Check beta / stable updates** button. It queries the public GitHub releases API without an account token. GitHub receives normal network information such as IP address and an app/version User-Agent. There is no background check, analytics, advertising, crash telemetry or photo upload.
- **Installation:** release pages open in the browser; no silent APK download, package installation or stable promotion.
- **Diagnostics:** device model, Android version and public camera capabilities/settings; no device serial, account data, credentials or photo pixels. You decide whether to copy/share the report. Review it and remove any details you do not want public.
- **AI assistance:** development used AI assistance, but the app does not send your photographs or recipes to an AI service. See the [development disclosure](AI_DISCLOSURE.md).
- **Public testing reports:** GitHub issues and attachments are public. Share only material you intend to make public, and remove private images, precise location and identifying metadata first.
