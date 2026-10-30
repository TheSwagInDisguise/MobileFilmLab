# Pro sliders and readable settings

Introduced in **v0.2.0-beta.3 / code 12** in response to handset feedback about low-contrast settings and repeated-tap manual controls.

## Interaction

Select **PRO**, then tap a parameter tab to open its editor. Selecting a tab does not alter the camera setting. Only one editor is displayed at a time. Drag its labelled slider; ISO, shutter and white balance have explicit Auto/metered switches. Unsupported controls are disabled with an explanation. Adjustments are locked during capture, and a haptic tick is issued at completion rather than on every slider movement.

| Control | Scale / constraints |
|---|---|
| EV | Linear Camera2 compensation-step indices; snapped and clamped to the selected route's reported range. Reset returns to zero. Available only while ISO and shutter are both Auto. |
| ISO | Logarithmic sensitivity, using reported minimum/maximum ISO. Auto must be turned off to drag. |
| SEC | Logarithmic exposure time in nanoseconds, with reciprocal/seconds labels. Bounded by reported exposure time, maximum frame duration and a 30-second app safety cap. |
| WB | Existing approximate Camera2 RGB gains, 2500–9900 K in 100 K increments, or Auto. Not calibrated Kelvin or a guarantee of stock-camera colour. |
| FOCUS | Direct choices among exposed continuous, single and macro AF modes. These are discrete modes, not a manual lens-distance slider. |

**Exposure is not a priority-auto implementation.** Camera2 AE is disabled when either ISO or shutter is manual. An unspecified counterpart uses the last metered value and is then held; the UI calls that state **Metered**, rather than claiming continuous auto ISO or shutter. Return both ISO and SEC to Auto to restore full AE and EV compensation. A retained EV value is inactive in manual exposure.

The same range helper is used by the UI, view model and native request clamping. Camera changes re-bound values or clear unsupported manual settings. Long-exposure still deadlines allow an in-flight preview plus the requested still exposure, with a 15-second baseline and 60-second baseline for manufacturer extensions. Hardware can still reject a request or take longer; device testing is required.

## Readability fix

`MaterialTheme` provides colours but does not itself establish `LocalContentColor` for every custom background. Previously some settings text inherited Compose's default black. The app theme now establishes light default content, and Settings explicitly uses a Material Surface with the matching light content colour. Material controls can still correctly override this—for example, black text on gold buttons.

Body, secondary and muted text tokens are tested to at least **4.5:1** contrast on the opaque dark app surfaces. This is a token-level regression check, not a claim of complete WCAG/Android accessibility certification or a guarantee over photographic/translucent backgrounds. Settings uses larger labels/explanations, wrapping rows and a scrolling body with a persistent Done footer. Native interaction tests also exercise 1.5× text scaling on a compact screen.

[Testing](TESTING.md) · [Beta.3 QA](QA_BETA_3.md) · [Native UI render provenance](SCREENSHOTS.md)
