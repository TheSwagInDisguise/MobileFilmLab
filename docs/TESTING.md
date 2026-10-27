# MobileFilmLab public beta testing

**Release:** `v0.2.0-beta.1` · versionCode **10** · package `com.mobilefilmlab.camera`
**Primary target:** **Oppo Find X9 Ultra** · Android 9 / API 28 minimum

Thank you for helping test the first public beta. Initial owner feedback on alpha.9 was that it was working well; the broader compatibility, image-quality and stress matrix below is still open. Other-device support will expand with beta testers' reports, not an assumption that every Android camera behaves alike.

## Install safely

1. Download `MobileFilmLab-v0.2.0-beta.1-signed.apk` from the [official release](https://github.com/TheSwagInDisguise/MobileFilmLab/releases/tag/v0.2.0-beta.1).
2. Optionally verify it against `checksums.txt` and the [established signing certificate](RELEASE_SIGNING.md).
3. Install **over the existing release** to retain custom recipes/favourites. The package and signing key are unchanged; code 10 is higher than alpha.9's code 9. Do not uninstall unless you intend to erase app data. A debug build has a separate package.
4. Allow camera permission. Android 9 also needs legacy storage permission. Android 10+ writes to `Pictures/MobileFilmLab/` through scoped storage.
5. Use another camera for important photos while testing. Back up custom recipe exports and any photos you cannot replace.

## Quick smoke checklist

- [ ] App opens; permission denial is explained and granting permission/retrying recovers.
- [ ] AUTO preview is correctly oriented; a portrait and a landscape-held capture save with correct orientation and dimensions.
- [ ] The default neutral baseline works without unexpected grade, grain or mandatory enlargement.
- [ ] Every **enabled** rear lens slot selects the expected view. Unavailable slots stay disabled; report which slots your firmware exposes.
- [ ] 1× / 1.5× / 2× / 3× crops match the saved framing. Short-tap `2×`; hold it for all levels. Do not assume slot names prove optical magnification.
- [ ] Tap focus/metering, supported flash states, volume/camera shutter keys and return from background work without stuck capture state.
- [ ] PRO controls change when supported; switching to AUTO clears manual exposure/WB. Unsupported controls explain their limits.
- [ ] Browse built-in looks, move the Tone/Warmth puck, use before/after, star a favourite and save a custom recipe.
- [ ] Recipe file and QR-image export/import work. Imported presets do not replace built-ins.
- [ ] Open the gallery, swipe, zoom, inspect dimensions/EXIF, share and confirm a test-photo deletion.
- [ ] Saved custom recipes and favourites survive a restart and an install-over-alpha upgrade.
- [ ] A manual update check uses the beta/stable channel; nothing downloads or installs silently.

## Extended testing

Try daylight, indoor mixed light, low light, high-contrast scenes, fine textures and moving subjects. Compare the same scene/framing before judging a look. The default is the exposed Camera2 ISP JPEG, not a reproduction of proprietary stock-camera processing.

Optional OEM HDR/Night, JPEG-domain three-frame fusion and PRIVATE reprocessing ZSL are **experimental and capability-dependent**. Test one option at a time. Unsupported operation should explain the fallback rather than hang or manufacture an output claim. Report the selected pipeline, visible active/fallback message and actual saved dimensions. Leave optional features disabled if they cause problems.

For reliability, try 50 captures across exposed lenses, a 10-minute preview session, repeated background/resume cycles, screen locking, camera-in-use conflicts and low storage. Stop if the phone becomes uncomfortably hot. Record failures; do not treat one successful photo as a completed soak or thermal test.

## Report an issue or a device

Use the [bug report](https://github.com/TheSwagInDisguise/MobileFilmLab/issues/new?template=bug_report.yml) or [device compatibility](https://github.com/TheSwagInDisguise/MobileFilmLab/issues/new?template=device_support.yml) form. Include:

- App version, phone model, Android version and firmware/build version.
- Lens/crop, AUTO/PRO, recipe and processing settings.
- Minimal steps, expected behaviour, actual result and whether it repeats.
- The **Camera capability report** from Settings, after reviewing/redacting private details.
- If useful, a non-private screenshot or paired sample with dimensions/EXIF intact. Public issue attachments are public; never include credentials, personal photographs or precise location without intending to share them.

See [known limitations](../KNOWN_LIMITATIONS.md), [validation status](BETA_READINESS.md), [privacy](PRIVACY.md) and the [AI-development disclosure](AI_DISCLOSURE.md). A beta label invites testing; it is not a certification of camera access, image quality or compatibility.
