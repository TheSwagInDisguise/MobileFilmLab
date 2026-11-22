# iOS beta 2 — simpler camera interface

This refresh addresses a crowded camera screen and confusing device names. It changes presentation, not the sensor pipeline or the meaning of RAW.

## On the camera screen

- A single toolbar holds **AUTO / PRO**, photo format, available flash, timer and Settings.
- The translucent rear strip contains **physical lenses only**. **Main is 1×**; other numeric labels are derived from validated system constituent/crossover metadata relative to Main. When that mapping is unavailable, **Ultra / Tele** are used rather than guessed powers. Different physical devices are not silently collapsed when their types or labels match.
- The button to the right of the shutter switches **front / rear** and remembers the previous camera on each side. Front routes do not fill the rear lens strip.
- The **separate crop icon** opens full-frame / 1.5× / 2× / 3× JPEG framing. Crops always say **crop** and are never advertised as optical lenses. RAW hides this control and normalises to an uncropped single exposure.
- The compact look bar opens all 80 looks; its slider icon opens the Tone/Colour pad.
- **AUTO** omits the live exposure text and histogram. **PRO** displays them and expands only one supported control at a time: ISO, shutter, EV, WB or focus.
- The half-circle icon in the viewfinder retains press-and-hold ungraded preview comparison.

## Advanced features are still there

Settings explanations, output enlargement and colour calibration are in expandable groups. Automatic rear routing and every discovered alternate camera remain accessible at:

**Settings → Advanced → Camera routing → All camera routes & diagnostics.**

An explicitly selected automatic rear route appears as **Auto** beside the physical lenses, so you can switch back without reopening Settings. Diagnostics retains system camera names, IDs, route type, formats and exposure limits; those details no longer overwhelm the ordinary picker.

JPEG / Bayer RAW / RAW+JPEG, separately labelled computational Apple ProRAW, Original / Smart HDR / Quality HDR / experimental fusion, recipe editing and interchange, favourites, local gallery, comparison/share, optional Photos saving and hardware capture events are retained and runtime-capability-gated. This is not a claim of proprietary stock-camera or photographic-quality parity.

## Screenshots and software-test provenance

The release includes native simulator screenshots from its exact-source UI test run:

- **SIMULATOR-sample-lenses-AUTO.png** — clean physical rear picker.
- **SIMULATOR-sample-lenses-PRO.png** — a single expanded PRO parameter.
- **SIMULATOR-settings.png** — shorter settings with expandable explanations.
- **SIMULATOR-four-JPEG-crop-choices.png** — explicitly separate digital framing choices.

The lens-layout screenshots use a **Debug + Simulator-only sample-metadata fixture**, not data read from an iPhone 18 Pro Max. A visible **SIMULATOR UI · SAMPLE LENS METADATA** label is overlaid. There is **no camera image or generated substitute**, and capture remains disabled. Example 0.5× / 1× / 5× labels exercise runtime-ratio code; they do not assert the target handset's lens specifications. The fixture and its sentinel capability values are excluded from the device IPA; packaging independently checks that its identifying strings are absent.

The tests exercise rear-only grouping, front switching, remembered rear selection, advanced-route access, JPEG crop choices, RAW crop removal and JPEG fallback when a selected route lacks RAW. Synthetic metadata/pixels and simulator tests cannot prove real camera capture, exact lens ratios on the handset, DNG validity, Photos behaviour, signing-service acceptance, latency, thermals or image quality. Those checks still need the owner’s phone.

AI assistance was used to develop the software, tests and documentation. No app-added AI image enhancement has been introduced. The Android app, existing downloads and public website are unchanged.

[Source and feature scope](README.md) · [Installation and handset checklist](TESTING.md)
