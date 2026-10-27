> **Historical alpha.9 planning record.** Superseded by the [current public-beta scope](../BETA_READINESS.md) and explicit owner approval. Unchecked hardware work remains pending; this file is not a current publication block.

# Beta-readiness tracker

**Test candidate:** `v0.1.0-alpha.9` / versionCode **9** / `com.mobilefilmlab.camera`  
**Release channel:** alpha prerelease only. No beta build/tag is authorised or created by this work.

“Implemented” below means there is a wired code path, not that a phone-dependent feature has been verified. **Handset results are still pending.** Exact stock-camera processing cannot be recreated from public Camera2 alone.

| # | Agreed roadmap area | Alpha implementation | Gate before beta |
|---|---|---|---|
| 1 | Stock Oppo default | Neutral Camera2/OEM original JPEG by default; no added film grade/grain; Natural available to reset | Owner accepts/compares this closest public-API baseline. Exact proprietary stock tuning is **not** implemented/claimed |
| 2 | Per-lens matching | Independent persistent RGB gain controls, identity defaults; detected lens metadata | Controlled chart/paired shots for all exposed lenses; no measured factory calibration yet |
| 3 | Sharpening | ISO-aware, edge-adaptive luminance high-pass with noise threshold, row buffers and halo clamp | Fine texture, edge halo, sky/skin and low-light A/B tests; improved acuity is not proven detail recovery |
| 4 | OEM HDR/Night | Advertised extensions queried; real extension sessions; visible standard fallback | Confirm support/unsupported fallback on this ColorOS build, capture/save/metadata, no lockups |
| 5 | Three-frame fusion | Bracket requests, timestamp matching, translation registration, exposure normalization and motion rejection; ~12MP cap | Static/handheld/moving-scene A/B; reject ghosting and exposure errors; tune or leave experimental/off |
| 6 | Tap focus/meter | Rotated/cropped sensor-coordinate mapping; supported AF/AE regions | Corners/centre and each crop on every exposed lens; close/far transitions |
| 7 | ZSL | Optional PRIVATE reprocess input/output ring; fresh/converged frame selection; normal capture fallback | Confirm supported combination, source/output dimensions, latency and memory/thermal cost; unsupported correctly reported is acceptable unless owner requires ZSL |
| 8 | Volume buttons | One shot per press, repeats suppressed; dialogs/gallery/editing excluded | Volume up/down/camera key press/hold and dialog regression tests |
| 9 | Grids & level | Off/thirds/golden/square guides; lifecycle-managed accelerometer level; device-oriented JPEG | Real tilt, flat/absent sensor, portrait/landscape capture orientation; square clearly guide-only |
| 10 | Favourite carousel | Persisted stars and up to six quick-switch favourites | Favourites survive update/relaunch, chips select correct look, no cramped overlap |
| 11 | Before/after | Shared colour transform + draggable sampled baseline split | Monochrome/warmth/tone on real viewfinder; baseline really ungraded, no frozen frame/rotation issue |
| 12 | Film grain | Explicit Off/Fine/Medium/Coarse luminance grain; off by default | No unintended default grain, reasonable texture across lenses/ISO, consistent exports |
| 13 | Recipe exchange | Typed JSON/.filmrecipe and QR PNG export; JSON/QR-image import, limits/validation/new IDs/transactions | Export/import on phone + another scanner/device; malformed input does not replace built-ins |
| 14 | In-app gallery | Folder-scoped swipe viewer, pinch/double-tap, higher-resolution decode, real EXIF/dimensions, share/delete | Zoom readable at native dimensions within heap; share permission, delete confirmation, permission denial/empty gallery |
| 15 | Haptics | Shutter/control/favourite feedback; switch to disable | Tactile response, disabled means no app-triggered vibration, Android 9 fallback |
| 16 | Stability, battery, thermal | Handler confinement, session epochs, bounded captures, worker processing, image cleanup, severe-thermal safeguards | 50-shot/all-lens run, 10-minute preview, 20 lifecycle cycles, low storage/permissions/camera conflict. Actual thermal/battery acceptance remains unmeasured; no app/device CTS certification claim |
| 17 | Release engineering | Tests + lint + signed APK/checksums/package/certificate/tag verification; prerelease flag; explicit updater; beta approval guard; F-Droid metadata starter | Verify install-over-alpha.8, data retention, release integrity and user sign-off. No automatic beta promotion or F-Droid publication |

## Mandatory cross-cutting fixes in this candidate

- Native pixels are the default; alpha.8's resize is no longer described as super-resolution.
- No second hardware/software crop, telephoto zoom multiplier on an already selected physical lens, or silently substituted lens.
- Capture/JPEG EXIF correlation, actual flash result, physical lens focal-length specification, signed EV rationals and APEX aperture conversion.
- No placeholder black photo when camera is unready; failures/timeouts clear busy state and report an error.
- Capture settings/recipe are snapshotted at shutter press; processing/storage run off the UI thread.
- Gallery no longer hardcodes 28.1MP, wide lens or a quality guarantee.
- Dynamic version labels, user-controlled network checks and accurate privacy/limitations documentation.

## Hard stop: no beta promotion until all of these hold

- [ ] Owner has tested the [alpha checklist](../ALPHA_9_TESTING.md) and returned the selected-camera report.
- [ ] Four rear lens slots are genuinely usable on the target phone, or the owner explicitly approves documented firmware limitations/re-scoping.
- [ ] Baseline quality/default behaviour is accepted; no false claim of exact Oppo stock/AI tuning remains.
- [ ] Flash, focus, crop geometry, orientation, EXIF and all 77 recipe workflows pass practical checks.
- [ ] Experimental paths either pass agreed quality/stability checks or remain visibly unavailable/off with an accepted fallback. No feature is labelled verified just because it compiled.
- [ ] Soak, storage/permission/lifecycle tests show no blocking defects, unacceptable heat, memory crashes or lost/corrupted photos.
- [ ] Signed upgrade preserves custom recipes/favourites and matches the established release certificate.
- [ ] Owner explicitly authorises the beta tag; repository approval guard is changed only after that authorisation.

A feature needing unavailable proprietary firmware access or calibration photos is a **declared limitation/pending gate**, not a silently completed requirement. Keep shipping alpha fixes until these gates are satisfied.
