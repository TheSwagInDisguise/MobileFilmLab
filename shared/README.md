# Cross-platform recipe and colour contract

- `catalogue.json`: all 80 Android recipe definitions (77 upstream + 3 independent baselines), with stable IDs, 14 groups including Custom, and every interchange field. Deterministically exported, not a reduced hand-maintained iOS sample.
- `colour-vectors.json`: 160 affine sRGB matrix cases, one neutral puck and one adjusted puck for each look. Python generates independent expected coefficients; Swift and Android native matrix builders compare against these vectors. This checks colour mathematics, not proprietary rendering or identical ISP output.
- `interchange-fixtures.json`: a version-1 recipe document and zlib-wrapped, URL-safe base64 QR produced with Java `DeflaterOutputStream`, the protocol used on Android. Both platforms decode the fixture; their own roundtrips are also tested.
- `android-site-baseline.json`: protected Android production source / version and website content hashes from the public beta.4-based branch. Adding iOS must not silently replace the public Android build, retune Android capture or update website download links. The separate unpublished beta.5 HDR branch/candidate remains intact.

Run `python3 scripts/export_catalogue.py --check` from the repository root. When intentionally changing Android catalogue definitions, run it without `--check`, inspect the diff and update compatibility tests in both languages. Do not casually change stable IDs. iOS uses display-name overrides for the three baselines; serialized fields and source provenance remain intact.

Recipe-file envelope: `{"type":"mobilefilmlab.recipe","version":1,"recipes":[...]}`. Legacy array-only files remain readable. Imports are bounded to 256 KiB / 1–100 recipes, with validated enum/numeric/string ranges. QR prefix: `FILMRECIPE:1:` followed by zlib data encoded as URL-safe base64 without padding, maximum 2200 characters. Decompression is capped at 256 KiB. Imported recipes become independent custom copies; the app does not silently replace built-in data.

The catalogue remains GPL-3.0-or-later with [upstream attribution](../UPSTREAM_ATTRIBUTION.md). Colour test vectors do not imply an official manufacturer simulation. Camera engines are intentionally separate: Camera2 on Android; AVFoundation on iOS. Synthetic test cases are not camera-quality evidence.
