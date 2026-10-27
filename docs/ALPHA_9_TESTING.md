# MobileFilmLab alpha.9 — phone test checklist

**This is an alpha candidate for testing the beta roadmap, not a beta release.**

Install the signed `v0.1.0-alpha.9` APK over alpha.8 (same package/signing key). Do not uninstall if you want to retain custom recipes and favourites. Export valuable custom presets before any destructive testing.

## 1. Baseline and image-quality comparison (start here)

1. Open the app: **Oppo Natural (Camera2)** should be selected. Set capture processing to Camera2 natural, native pixels, grain Off, calibration 1.00/1.00/1.00, crop 1×.
2. Take paired MobileFilmLab/stock Oppo shots of the same well-lit static scene. Include text, foliage/fabric, skin or a neutral wall, highlights and shadows. Keep focal length, framing, focus point and lighting as close as possible; a tripod helps.
3. Repeat indoors and at night. Keep original JPEGs. Compare at **matched display size**, not just each file's 100% zoom when resolutions differ.
4. Check real resolution/ISO/shutter/aperture/EV/flash/lens in gallery. Native resolution can be smaller than Oppo's stock output. This is intentional: interpolation is not recovered detail.
5. Try Vivid/B&W and film looks. Return to Natural to confirm the extra grade disappears. Stock-camera visual matching is a test goal, **not an existing calibrated guarantee**.

## 2. All rear lenses and four crops

- Try `UW`, `W`, `3x`, `10x`. Cover each physical lens briefly to verify which optic is used. Check detected millimetres in the badge/report; slot text alone is not proof of optical zoom.
- Any blocked lens must be disabled or report failure, not silently take a wide photo with a telephoto label.
- On each available lens take 1× and 2×. Hold the crop button for 1× / 1.5× / 2× / 3×. Frame should match the saved result; 2× must not turn into 4×.
- Native 2× output should be about half the full stream's width/height (quarter of pixels). Optional enlarged export must be labelled interpolation and not called native/super-resolution.
- Tap centre and each corner to focus/meter at 1×/2×; try a near object and a distant object.

## 3. Flash, AUTO/PRO and orientation

- Cycle Off → Auto → On → Torch in a dark room, then in good light. Check both visible LED behaviour and saved photo/EXIF. **An On icon is not proof the LED fired.** Test every lens that exposes flash.
- In PRO change ISO, shutter, EV, WB and focus. Check results, hardware clamping and reasonable exposure. With manual exposure, EV and auto/forced flash have restrictions shown by the app. Kelvin is approximate.
- Return to AUTO: ISO/shutter/WB should be automatic, without stale manual settings.
- Capture upright, phone-left, phone-right and upside down. UI is portrait, but saved images should open upright according to device orientation. Test share to another viewer too.
- Short-press volume up/down once and hold them: no held-key burst or duplicate on key-up. In recipe naming/gallery/settings, volume buttons must not take pictures.

## 4. Recipe workflow and live previews

- All **77 original recipes + 3 Camera2 baselines** should remain selectable.
- Test B&W (actual grayscale preview/save), saturated colour, warm/cool looks and Tone/Warmth extremes. Drag, lift, reopen and reset the square pad; no snap-back/coordinate jump.
- Star several recipes. Up to six should appear as scrollable quick-switch favourites and survive relaunch/update.
- Enable live before/after in settings; drag its divider. Baseline must be ungraded. Preview is colour-only; final grain/sharpness/OEM processing can differ.
- Save a custom look, export `.filmrecipe`, import it, show/save its QR PNG and import that QR image. Tuning should survive; imports become new customs without replacing built-ins.
- Delete a custom recipe only after confirming. Built-ins remain intact. Try an invalid JSON file/ordinary non-QR image: useful error, no crash or data loss.
- Try grain Off/Fine/Medium/Coarse; Off must never add grain secretly. Disable haptics and verify control/shutter feedback stops.

## 5. Experimental paths (keep separate from baseline tests)

- **3-frame fusion:** use a static detailed scene first, then handheld and a moving subject. Compare noise, small text, ghosting and exposure against single-frame at matched size. Record the saved processing note. A registration fallback is expected for unsuitable scenes; no fixed quality gain is promised.
- **OEM HDR/Night:** check settings/report for actual support. If unsupported or session-rejected, standard fallback must be visible. If active, capture a high-contrast/night scene and check save/metadata. These modes control exposure/flash; PRO/crop returns to Camera2.
- **ZSL:** enable reprocessing ZSL at 1×/AUTO/flash Off. Active status should explicitly say PRIVATE reprocessing, or state fallback. Compare shutter timing against a clock/moving object. Check detail/source dimensions and battery/heat; a fallback is not zero-lag proof.
- Severe thermal conditions should stop/reconfigure a ZSL ring and use single-frame burst fallback; OEM HDR/Night is blocked until cooling/standard mode. Do not deliberately overheat the phone.

## 6. Gallery and reliability

- Open the thumbnail, swipe, pinch/double-tap, inspect real EXIF/dimensions and decoded-resolution overlay, share, cancel deletion, then delete a disposable image.
- Return from gallery/another app/lockscreen repeatedly. Capture another photo: live preview and controls should recover with no stale image/torch.
- Take **50 photos** across lenses and modes, including quick repeated presses. No corrupt/blank files, indefinite busy state, crashes or missing saves.
- Run a **10-minute preview** and **20 background/foreground/lock cycles**. Record approximate battery use, warmth and any failures; avoid claiming precise power improvements without measurement.
- Check camera permission denial/re-enable, storage denial on Android 9, low free storage and another app using the camera. Errors should be clear; partial MediaStore entries must not accumulate.
- Verify existing alpha.8 custom recipes/favourites still exist after upgrade. The new Natural baseline is intentionally the factory startup default.

## What to send back

Use **Settings → Camera capability report → Copy report**, ideally for each problematic selected lens. Include:

```text
Build: 0.1.0-alpha.9
Phone model + Android/ColorOS build:
Lens slot + detected camera ID/mm:
Mode + flash + crop + processing + output size:
Recipe + Tone/Warmth + grain + calibration:
Steps / expected / actual:
Reproducibility (e.g. 3 of 5):
Original sample JPEGs and matching stock-camera JPEG:
Screenshot/report and any saved processing note:
Battery/heat/lifecycle observations:
```

Original files matter more than screenshots for EXIF and quality analysis. This testing does **not** authorise a beta release automatically; explicitly approve promotion only after the outstanding [readiness gates](BETA_READINESS.md) are resolved.
