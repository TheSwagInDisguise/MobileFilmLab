# Beta.3 validation scope

**Candidate:** `v0.2.0-beta.3` / code **12**, same package and signing key. Owner-authorised for publication as the next regular Latest release after checks, with the website updated before withdrawing superseded binaries. Source tags and history remain immutable.

## Local execution

116 Android tests in 24 classes pass with zero failures/errors/skips. Release lint passes. The native screenshot test produces five actual UI states; slider interaction and compact-screen 1.5× font-scale Settings checks run against production components. Remote publication/integrity gates below still apply.

## Automated gates

- Existing recipe, colour, crop, orientation, metadata, storage, RAW frame matching/lifetimes, preference migration, lifecycle and update-channel regressions.
- Logarithmic ISO/time mapping, endpoints, monotonic round trips and fixed/malformed range handling.
- EV step quantisation, bounds and manual-exposure lockout.
- Exposure-time / frame-duration / app-cap constraints, plus long-exposure capture deadlines.
- Explicit Auto/manual setters, capture lockout, route-change clamping and supported focus selection.
- Production Compose slider interactions: selecting a tab does not cycle a value; Auto/metered switches, slider semantics, white-balance increments and direct focus choices.
- Correct light default theme content and body/helper text token contrast of at least 4.5:1 on opaque dark app surfaces; black-on-gold selection contrast.
- Compact-screen Settings scrolling at 1.5× text scale, including persistent Done access.
- Actual Android/Compose AUTO, PRO slider, recipes, formats and Settings documentation renders.
- Python exact-tag approval tests, local documentation links, release lint, signing/package/version/alignment verification and downloaded-APK checksum verification before final delivery.

See the immutable tag's CI/release run for the final execution results. This document describes the checks and their boundaries; it is not a hardware-certification claim.

## Still requires a handset

No physical camera is attached to the automated environment. Verify readable text at the phone's actual display/font settings, continuous slider response per lens, camera switching, AF behaviour, supported manual ISO/shutter/WB, flash interaction, long-exposure preview/capture completion and existing JPEG / RAW / RAW+JPEG output. An advertised capability does not guarantee a working firmware/session combination.

RAW sensor buffers and DNG rendering must still be validated in a compatible editor on actual hardware. The storage suite uses controlled payloads and UI fixtures use illustrative metadata; neither proves a sensor DNG was captured. No new measured image-quality or stock-pipeline equivalence claim is made.

[Pro controls / limits](PRO_CONTROLS.md) · [Public checklist](TESTING.md) · [RAW implementation boundaries](QA_BETA_2.md) · [Provenance](SCREENSHOTS.md)
