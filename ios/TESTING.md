# Install and test MobileFilmLab iOS — beta 3 · native preview performance

**Target:** iPhone 18 Pro Max, public iOS 27. **App:** MobileFilmLab, `com.mobilefilmlab.camera`, 0.1.0 (4). This is an experimental iOS release, not an App Store release or a certified camera-quality upgrade.

## 1. Before installing

- Keep the system Camera available; do not trust an untested app as the only camera for an important event.
- Back up important photos and exported recipe files. Future re-signs / bundle-ID changes / certificate expiry may require reinstalling. **Deleting the app deletes its private library, favourites and custom recipes.** Export files outside the app first.
- Check the delivered IPA's SHA-256 against `checksums.txt`. It is an unencrypted device IPA **without an Apple developer signature/provisioning profile**, not a directly installable signed application.
- The accompanying `BUILD-INFO.json` records the exact source commit and software-only checks. Its `handset_tested` remains false until the owner reports actual handset results.

## 2. Sign / install with Signulous

1. Register the intended device with your signing service using its official instructions, if not already registered. Service fees, device registration and certificate validity are outside this project.
2. Upload **`MobileFilmLab-iOS-test-unsigned.ipa`** using the service's custom-IPA upload facility.
3. Let the service re-sign it for your registered device and install using its instructions. Keep the original bundle ID where the service permits; avoid enabling unrelated injected tweaks or permissions for the first test.
4. Follow only the iOS trust / Developer Mode prompts actually required by that signing method. No need to send the assistant your Apple ID, password, service login, device UDID or private signing material.
5. If signing or installation fails, capture the **exact error text** and whether failure occurred at upload, signing, installation or launch. Do not assume that a correct IPA bypasses service restrictions, revoked certificates or device-registration delays.

Official service information: [Signulous](https://www.signulous.com/). The assistant has not logged into or operated your signing account. There is no signing-service connector integration in this app.

## 3. First launch / safe baseline

- Allow **Camera** when asked. No microphone or location permission should appear. Motion powers the level / HDR guard. Photos add-only permission is requested only when saving there.
- Start on the physical **Main (1×)** lens, **JPEG**, **Original**, **iPhone Natural (AVFoundation)**, no crop, no grain, Balanced off, enlargement off. Take a daylight still scene.
- Confirm the app library shows a new capture; Share it to Files and open it independently. With optional Photos saving enabled, check the Photos copy. Deny Photos once and verify the app copy remains shareable.
- Record Settings → Advanced → Camera routing → All camera routes & diagnostics: routes, formats, exposure bounds and dimensions. A listed capability is not proof of request honouring.

## Viewfinder performance check

- First try **Main 1× / JPEG / Original / iPhone Natural**, in good daylight with a normal automatic shutter. Pan slowly across a textured static scene before taking a photo. The app no longer applies the old 12 FPS limiter or publishes frame bitmaps through SwiftUI.
- Try another look, the front camera, then open/close the look library and Tone/Colour pad. Check that the preview stays current rather than playing back old frames. Live looks and JPEG crop must still match the chosen framing.
- Settings → Advanced → Camera routing → All camera routes & diagnostics → **Live preview** shows the backend, approximate one-second input/completed-render counts, proxy/drawable sizes, one pending slot and replaced-frame/error counters. These are diagnostic counters, not a laboratory FPS/latency benchmark. Do not interpret replaced stale frames as lost still photographs.
- Record the lens, look, JPEG/RAW format, light level, manual shutter (if any), pipeline and the displayed counters if it still stutters. Do not send private photos unless you choose to. No diagnostics are uploaded by the app.
- Separately check a saved JPEG/DNG and its dimensions. Preview sizing must not reduce the still output. Very low light, a deliberately long manual shutter and active still/HDR capture can legitimately reduce or interrupt the sensor feed. This release does not claim to eliminate those platform limits or promise 60 FPS.

## 4. Capability and workflow checks

- Check that the normal rear row contains physical lenses only: **Main (1×)** and runtime-reported ratios, or **Ultra / Tele** if no valid ratio mapping is exposed. No camera IDs or duplicate Auto routes should fill that row. Do not expect fixed UW/W/3×/10× slots or a particular telephoto count.
- Tap the camera-switch icon beside the shutter. Only the active front camera should remain in the lens row. Switch back and verify that the last rear lens is restored. Front preview may mirror while capture orientation differs.
- Open Settings → Advanced → Camera routing → All camera routes & diagnostics. Select each automatic / alternate front route there, then close Settings. An explicitly selected rear automatic route should appear as **Auto**, not a numeric physical lens. Check you can return to a physical lens.
- AUTO / PRO: technical exposure readouts and the histogram should be hidden in AUTO. In PRO, select ISO / Shutter / EV / WB / Focus; only the selected control is expanded. Move the supported slider, reset to Auto, tap focus and try AF-S / AF-C / manual focus where exposed. Confirm exposure visibly responds; no claim of camera-vendor-equivalent ranges.
- Tap the separate **crop icon** for **1× / 1.5× / 2× / 3×** framing, matching the Android choices. An active selection must say **“× crop”**. These are JPEG centre crops, not lenses. Compare dimensions; RAW must remain 1×. Maximum exposed dimensions and interpolated enlargement are separate settings.
- Open all 80 looks, favourite a look, adjust Tone/Colour, save a custom copy, quit/reopen and confirm persistence.
- Export / import `.filmrecipe` to Android and back. Show QR on one device and import its image / payload on the other. Check name, tint and tone survive. Imported recipes become independent customs rather than overwriting built-ins.
- Try timer, supported flash/torch, grids, level, histogram, hardware shutter events, live Original comparison, gallery sharing/deletion and saved-original comparison.

## 5. Genuine RAW / ProRAW checks

- Select **RAW (Bayer DNG)** only when offered. If missing, try a physical rear route; do not substitute a renamed JPEG.
- Capture RAW, then RAW+JPEG. Share the DNG to a real RAW editor and inspect dimensions, ISO, exposure and orientation. A hex/signature check or successful encode does not prove sensor fidelity.
- Test **Apple ProRAW** separately if offered. This is computational, demosaiced/partially processed RAW, not identical to Bayer RAW.
- Verify DNGs are ungraded / uncropped; only a JPEG companion receives the selected recipe. There is no custom multi-frame RAW merge.
- Apple Photos imports DNG/JPEG as separate assets. Some editors / viewers may not recognise new-device DNGs even when correctly written.

## 6. HDR A/B tests — do not start with people

1. Enable **Keep unedited JPEG** and choose a static daylight scene with a bright window / sky and dark interior. Hold still.
2. Capture Original, then **Smart HDR**, then **Quality HDR**. Smart normally uses 3 or 5 short helpers; Quality uses 7 plus up to 2 longer helpers only when measured motion is quiet. A safety Original is taken first.
3. Compare exported full-size crops, not preview thumbnails. Look for colour casts, clipped highlights, shadow noise, misalignment / repeated edges, painterly textures, soft detail, rotation errors or striped seams.
4. Then test foliage / walking subjects / indoor low light. Some motion is deliberately rejected; HDR can fall back to Original. Read each capture's processing note to distinguish a merge from fallback.
5. Custom HDR requires JPEG, automatic exposure, flash/torch off, trustworthy helper exposure metadata, a ~12 MP stream, enough aligned frames and acceptable memory/temperature. No override is provided to force unsafe RAW/bracket requests.
6. Report shutter-to-save time, temperature, selected pipeline, route, dimensions and processing note. If it freezes, overheats or loses captures, switch to Original and report the details.

The app uses conventional local alignment / robust JPEG merging. It cannot reproduce Google's Bayer RAW HDR+, stock Camera Night mode, proprietary ISP internals, or restore highlights already clipped before JPEG encoding. No app-added AI is used; opaque Apple firmware / computational ProRAW are not claimed AI-free.

## 7. What has and has not been checked

Software evidence may include complete-catalogue / colour-vector / JSON+QR tests, synthetic pixel tests, native simulator UI screenshots, arm64 compilation and IPA structure/encryption inspection. **None proves Signulous acceptance, installation, real camera request honouring, DNG editor compatibility, photo quality, latency, thermals, or saving on your phone.** Those boxes require your handset results.

Software/UI/documentation were developed with AI assistance. Simulator UI captures have no handset camera feed; lens-layout checks use visibly labelled **sample metadata**, with capture disabled and no sensor access. Example numeric ratios are not target-phone measurements. Synthetic test pixels are never described as actual photographs.

GPL-3.0-or-later and upstream notices remain in the app and matching source. Provided as-is without warranty to the extent permitted by law; non-excludable rights remain. Source: <https://github.com/TheSwagInDisguise/MobileFilmLab>.
