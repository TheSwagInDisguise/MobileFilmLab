# Project website / GitHub Pages

**Live project site:** https://theswagindisguise.github.io/MobileFilmLab/

A static, mobile-friendly showcase of MobileFilmLab's natural-capture philosophy, actual interface, optional features, primary device target, limitations and signed APK download. GitHub Pages is configured for **Actions deployment**, not a separate website repository or paid service.

## Structure

- `site/index.html` — semantic single-page site with natural-capture selling points and honest limitations.
- `site/styles.css` — self-contained responsive styling; operating-system fonts, no external font/CDN dependencies.
- `site/app.js` — local navigation, accessible screenshot dialog and checksum copying; no network calls or analytics.
- `site/assets/` — existing approved icon, deterministic social preview and pixel-identical lossless WebP copies of the actual UI renders.
- `site/release.json` — pinned official release/download identity, checksum and minimum Android version.
- `scripts/check_site.py` — dependency-free checks for local links/assets, anchors, release consistency and required disclosures.
- `.github/workflows/pages.yml` — validate, upload and deploy `site/` to the `github-pages` environment.

The screenshots use a labelled AI-generated scene and illustrative camera/control state. They are not handset captures or app-photo quality examples. [Provenance](SCREENSHOTS.md).

## Local development

```sh
python3 scripts/check_site.py
python3 -m http.server 4173 --bind 0.0.0.0 --directory site
```

Open the server in a browser. All styles, scripts and images are local. A static file viewer that blocks relative assets is not equivalent to an HTTP server.

Changes to `site/`, the site checker or the Pages workflow on `main` trigger deployment. Manual dispatch is also available. Site changes do not rebuild or replace a published APK. Android CI continues separately for main-branch changes.

## Updating the download

Keep `site/release.json` and the visible HTML version, APK links, checksum, file size and code in sync with the actual verified release. The checker compares the release version/code with Gradle and rejects stale or unexpected download CTAs. Do not link withdrawn historical APKs.

The current app remains **v0.2.0-beta.1 / code 10**, promoted to a regular Latest GitHub Release without changing its APK/tag/signing identity. Classification is not a new stable-version build or universal hardware validation. See [publication policy](PUBLIC_RELEASE.md).

## Privacy and honest wording

No advertising, analytics, third-party JavaScript, remote fonts or cookies are added by this site. GitHub Pages receives normal hosting requests; following GitHub links or downloading the APK contacts GitHub. The website does not process camera images.

The central promise is **no AI enhancement added by MobileFilmLab**. Camera firmware/ISP and optional manufacturer modes may still use proprietary processing we cannot inspect. JPEG capture is not unprocessed RAW/DNG. Preserve those distinctions when editing the marketing copy. See [natural capture](NATURAL_CAPTURE.md).
