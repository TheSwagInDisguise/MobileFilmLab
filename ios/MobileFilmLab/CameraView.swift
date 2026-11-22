import SwiftUI
import AVFoundation
import FilmLabCore

private enum ProParameter: String, CaseIterable {
    case iso = "ISO", shutter = "Shutter", exposure = "EV", whiteBalance = "WB", focus = "Focus"
}

struct CameraView: View {
    @EnvironmentObject var store: FilmLabStore
    @EnvironmentObject var camera: CameraController
    @State private var recipes = false
    @State private var settings = false
    @State private var library = false
    @State private var styles = false
    @State private var pro = false
    @State private var parameter: ProParameter = .iso
    @State private var controls = ManualControls()
    @State private var flash: FlashChoice = .off

    var body: some View {
        GeometryReader { geometry in
            VStack(spacing: 12) {
                toolbar
                viewfinder.frame(maxWidth: .infinity, maxHeight: .infinity)
                if pro { proPanel }
                lookBar
                captureBar
                if store.processing {
                    Text(camera.busy ? camera.status : "Processing and saving…")
                        .font(.caption2).foregroundStyle(FilmTheme.muted).lineLimit(1)
                }
            }
            .padding(.horizontal, 16).padding(.vertical, 8)
            .frame(width: geometry.size.width, height: geometry.size.height)
        }
        .background(Color(red: 0.025, green: 0.035, blue: 0.035).ignoresSafeArea())
        .background(HardwareCaptureKeys(enabled: camera.ready && !store.processing && !recipes && !settings && !library && !styles) {
            Task { await store.shoot(camera) }
        })
        .sheet(isPresented: $recipes) { RecipeDrawer().presentationDetents([.medium, .large]).presentationDragIndicator(.visible) }
        .sheet(isPresented: $styles) { StylePad().presentationDetents([.height(440)]).presentationDragIndicator(.visible) }
        .sheet(isPresented: $settings) { SettingsView() }
        .sheet(isPresented: $library) { LibraryView() }
        .onChange(of: controls) { _, value in camera.setControls(value) }
        .onChange(of: camera.selectedID) { _, _ in
            controls = ManualControls(); controls.focus = camera.capabilities.focusModes.first ?? "AF-C"; flash = .off
        }
        .preferredColorScheme(.dark)
    }

    private var toolbar: some View {
        HStack(spacing: 12) {
            Button {
                pro.toggle()
                if !pro { controls = ManualControls() }
            } label: {
                Text(pro ? "PRO" : "AUTO").font(.system(size: 12, weight: .bold))
                    .padding(.horizontal, 14).frame(height: 36)
                    .background(pro ? FilmTheme.accent : Color.white.opacity(0.10), in: Capsule())
                    .foregroundStyle(pro ? Color.black : Color.white)
            }.accessibilityLabel("Toggle Auto and Pro controls").accessibilityIdentifier("cameraMode")
                .disabled(store.processing || camera.busy)
            Spacer(minLength: 8)
            Menu {
                ForEach(CaptureFormat.allCases.filter { camera.capabilities.supports($0) }) { format in
                    Button(format.label) { store.preferences.format = format; store.normalize() }
                }
            } label: {
                HStack(spacing: 4) { Text(formatTitle).font(.system(size: 11, weight: .semibold)).lineLimit(1).minimumScaleFactor(0.8); Image(systemName: "chevron.down").font(.system(size: 8, weight: .bold)) }
                    .frame(minHeight: 44)
            }.accessibilityIdentifier("captureFormat").accessibilityLabel("Photo format").accessibilityValue(store.preferences.format.label)
                .disabled(store.processing || camera.busy)
            if camera.capabilities.flashChoices.count > 1 {
                Button {
                    let choices = camera.capabilities.flashChoices
                    flash = choices[((choices.firstIndex(of: flash) ?? 0) + 1) % choices.count]; camera.setFlash(flash)
                } label: {
                    Image(systemName: flash == .off ? "bolt.slash" : flash == .torch ? "flashlight.on.fill" : "bolt.fill")
                        .foregroundStyle(flash == .off ? Color.white : FilmTheme.accent).frame(width: 36, height: 44)
                }.accessibilityLabel("Flash \(flash.rawValue)").disabled(store.processing || camera.busy)
            }
            Button { store.timer = store.timer == 0 ? 3 : store.timer == 3 ? 10 : 0 } label: {
                Image(systemName: store.timer == 0 ? "timer" : "\(store.timer).circle").frame(width: 36, height: 44)
                    .foregroundStyle(store.timer == 0 ? Color.white : FilmTheme.accent)
            }.accessibilityLabel("Timer \(store.timer) seconds").disabled(store.processing || camera.busy)
            Button { settings = true } label: { Image(systemName: "gearshape").frame(width: 36, height: 44) }
                .accessibilityLabel("Settings").accessibilityIdentifier("openSettings")
        }.font(.system(size: 18, weight: .regular)).foregroundStyle(.white).frame(height: 44)
    }
    private var formatTitle: String {
        switch store.preferences.format {
        case .jpeg: return "JPEG"
        case .raw: return "RAW"
        case .rawJPEG: return "RAW+JPEG"
        case .proRAW: return "ProRAW"
        case .proRAWJPEG: return "ProRAW+JPEG"
        }
    }
    private var viewfinder: some View {
        GeometryReader { geometry in
            ZStack {
                RoundedRectangle(cornerRadius: 20).fill(FilmTheme.panel)
                if let image = camera.preview {
                    Image(uiImage: image).resizable().scaledToFit()
                        .overlay { if store.preferences.grid != "Off" { GridOverlay(mode: store.preferences.grid).stroke(.white.opacity(0.3), lineWidth: 0.6) } }
                        .overlay {
                            if store.preferences.level && camera.motionAvailable {
                                Rectangle().fill(abs(camera.roll) < 1.5 ? FilmTheme.accent : .white.opacity(0.65))
                                    .frame(width: 68, height: 1.5).rotationEffect(.degrees(-camera.roll))
                            }
                        }
                        .overlay {
                            if let point = camera.focusPoint {
                                Rectangle().stroke(FilmTheme.accent, lineWidth: 1.5).frame(width: 52, height: 52)
                                    .position(x: geometry.size.width * point.x, y: geometry.size.height * point.y)
                            }
                        }
                        .contentShape(Rectangle()).onTapGesture { point in
                            camera.focus(at: CGPoint(x: clamp(point.x / geometry.size.width, 0, 1), y: clamp(point.y / geometry.size.height, 0, 1)))
                        }
                } else { cameraUnavailable }
                VStack {
                    HStack(alignment: .top) {
                        if store.preferences.format.writesRaw || store.preferences.pipeline != .original {
                            Text(store.preferences.format.writesRaw ? (store.preferences.format.usesProRAW ? "ProRAW" : "RAW") : "HDR")
                                .font(.system(size: 10, weight: .semibold)).padding(.horizontal, 10).padding(.vertical, 7)
                                .background(.black.opacity(0.45), in: Capsule())
                        }
                        Spacer()
                        Image(systemName: "circle.lefthalf.filled").font(.system(size: 18)).frame(width: 38, height: 38)
                            .background(.black.opacity(0.35), in: Circle())
                            .onLongPressGesture(minimumDuration: 0.01, pressing: { camera.compareOriginal($0) }, perform: {})
                            .accessibilityLabel("Hold to compare ungraded camera preview")
                    }.padding(12)
                    Spacer()
                    if store.countdown > 0 { Text("\(store.countdown)").font(.system(size: 64, weight: .light)).shadow(radius: 10); Spacer() }
                    #if DEBUG && targetEnvironment(simulator)
                    if camera.isShowingLensFixture {
                        Text("SIMULATOR UI · SAMPLE LENS METADATA").font(.system(size: 8, weight: .medium, design: .monospaced))
                            .foregroundStyle(FilmTheme.muted).padding(5).background(.black.opacity(0.5), in: Capsule())
                    }
                    #endif
                    lensControls(width: max(0, geometry.size.width - 24)).padding(.bottom, 12)
                }.foregroundStyle(.white)
            }.clipShape(RoundedRectangle(cornerRadius: 20))
        }.aspectRatio(3 / 4, contentMode: .fit)
    }
    private var cameraUnavailable: some View {
        VStack(spacing: 14) {
            Image(systemName: "camera").font(.system(size: 34, weight: .ultraLight))
            Text(camera.status).font(.footnote).multilineTextAlignment(.center).lineLimit(4)
            #if DEBUG && targetEnvironment(simulator)
            if !camera.isShowingLensFixture { cameraAccessButton }
            #else
            cameraAccessButton
            #endif
        }.padding(32).foregroundStyle(FilmTheme.muted)
    }
    @ViewBuilder private var cameraAccessButton: some View {
        if AVCaptureDevice.authorizationStatus(for: .video) == .denied || AVCaptureDevice.authorizationStatus(for: .video) == .restricted {
            Button("Open iOS Settings") { if let url = URL(string: UIApplication.openSettingsURLString) { UIApplication.shared.open(url) } }
        } else { Button("Enable camera") { camera.start() } }
    }
    private func lensControls(width: CGFloat) -> some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 10) {
                if !camera.visibleLenses.isEmpty {
                    HStack(spacing: 3) {
                        ForEach(camera.visibleLenses) { route in
                            Button { camera.select(route.id) } label: {
                                Text(route.shortName).font(.system(size: 13, weight: .semibold)).monospacedDigit()
                                    .padding(.horizontal, 12).frame(minWidth: 44, minHeight: 40)
                                    .background(camera.selectedID == route.id ? FilmTheme.accent : Color.clear, in: Capsule())
                                    .foregroundStyle(camera.selectedID == route.id ? Color.black : Color.white)
                            }.accessibilityIdentifier("lens-" + route.id)
                                .accessibilityLabel(route.name + ", " + route.shortName)
                                .accessibilityValue(camera.selectedID == route.id ? "Selected" : "Not selected")
                                .disabled(store.processing || camera.busy)
                        }
                    }.padding(4).background(.ultraThinMaterial, in: Capsule()).accessibilityElement(children: .contain).accessibilityIdentifier("lensRow")
                }
                if !store.preferences.format.writesRaw { framingMenu }
            }.frame(minWidth: width, alignment: .center)
        }.padding(.horizontal, 12)
    }
    private var framingMenu: some View {
        Menu {
            ForEach([1.0, 1.5, 2.0, 3.0], id: \.self) { factor in
                Button(factor == 1 ? "1× · full frame" : "\(String(format: "%g", factor))× JPEG crop") { store.preferences.crop = factor }
            }
        } label: {
            HStack(spacing: 5) {
                Image(systemName: "crop").font(.system(size: 15))
                if store.preferences.crop > 1 { Text("\(String(format: "%g", store.preferences.crop))× crop").font(.system(size: 11, weight: .semibold)) }
            }.padding(.horizontal, 12).frame(minWidth: 44, minHeight: 44)
                .background(.ultraThinMaterial, in: Capsule())
        }.accessibilityIdentifier("jpegCrop").accessibilityLabel("JPEG framing")
            .accessibilityValue(store.preferences.crop == 1 ? "Full frame" : "\(String(format: "%g", store.preferences.crop))× JPEG crop")
            .disabled(store.processing || camera.busy)
    }
    private var lookBar: some View {
        HStack(spacing: 8) {
            Button { recipes = true } label: {
                HStack(spacing: 10) {
                    Image(systemName: "square.stack.3d.up").foregroundStyle(FilmTheme.accent)
                    Text(store.selected.displayName).font(.system(size: 14, weight: .medium)).lineLimit(1)
                    Spacer(minLength: 2)
                    Image(systemName: "chevron.down").font(.system(size: 10, weight: .semibold)).foregroundStyle(FilmTheme.muted)
                }.frame(minHeight: 46).contentShape(Rectangle())
            }.accessibilityIdentifier("openRecipes")
            Divider().frame(height: 22).overlay(Color.white.opacity(0.12))
            Button { styles = true } label: { Image(systemName: "slider.horizontal.3").frame(width: 44, height: 46) }
                .accessibilityLabel("Tone and colour pad")
        }.padding(.leading, 13).padding(.trailing, 2).background(FilmTheme.panel, in: RoundedRectangle(cornerRadius: 14)).foregroundStyle(.white)
    }
    private var captureBar: some View {
        HStack {
            Button { library = true } label: {
                ZStack {
                    RoundedRectangle(cornerRadius: 12).fill(Color.white.opacity(0.08))
                    if let latest = store.captures.first, let image = store.image(latest) {
                        Image(uiImage: image).resizable().scaledToFill().frame(width: 46, height: 46).clipped().clipShape(RoundedRectangle(cornerRadius: 12))
                    } else { Image(systemName: "photo.on.rectangle").font(.system(size: 22)) }
                }.frame(width: 46, height: 46)
            }.accessibilityLabel("Photo library")
            Spacer()
            Button { Task { await store.shoot(camera) } } label: {
                ZStack {
                    Circle().stroke(Color.white.opacity(0.8), lineWidth: 2).frame(width: 80, height: 80)
                    Circle().fill(store.processing ? Color.white.opacity(0.3) : Color.white).frame(width: 66, height: 66)
                    if store.processing { ProgressView().tint(.black) }
                }
            }.disabled(!camera.ready || store.processing || camera.busy || store.catalogue == nil)
                .accessibilityLabel("Take photo").accessibilityIdentifier("shutter")
            Spacer()
            Button { camera.flipCamera() } label: {
                Image(systemName: "arrow.triangle.2.circlepath.camera").font(.system(size: 24)).frame(width: 46, height: 46)
            }.accessibilityLabel("Switch front and rear cameras").accessibilityIdentifier("flipCamera")
                .disabled(!camera.canFlipCamera || store.processing || camera.busy)
        }.foregroundStyle(.white).frame(height: 84)
    }

    private var availableParameters: [ProParameter] {
        ProParameter.allCases.filter {
            switch $0 {
            case .iso, .shutter: return camera.capabilities.customExposure
            case .exposure: return camera.capabilities.maxEV > camera.capabilities.minEV
            case .whiteBalance: return camera.capabilities.manualWB
            case .focus: return !camera.capabilities.focusModes.isEmpty
            }
        }
    }
    private var activeParameter: ProParameter? { availableParameters.contains(parameter) ? parameter : availableParameters.first }
    private var proPanel: some View {
        VStack(spacing: 8) {
            HStack {
                Text(camera.liveISO > 0 ? "ISO \(Int(camera.liveISO)) · \(exposureLabel(camera.liveTime))" : "Manual controls")
                    .font(.system(size: 10, design: .monospaced)).foregroundStyle(FilmTheme.muted)
                Spacer()
                HistogramView(bins: camera.histogram).frame(width: 64, height: 16)
            }
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 6) {
                    ForEach(availableParameters, id: \.self) { item in
                        Button { parameter = item } label: {
                            Text(item.rawValue).font(.system(size: 11, weight: .semibold)).padding(.horizontal, 12).frame(height: 30)
                                .background(activeParameter == item ? FilmTheme.accent.opacity(0.18) : Color.clear, in: Capsule())
                                .foregroundStyle(activeParameter == item ? FilmTheme.accent : Color.white)
                        }.accessibilityLabel("Adjust " + item.rawValue)
                    }
                }
            }
            if let activeParameter { parameterSlider(activeParameter).disabled(!camera.ready) }
            else { Text("Manual controls are not exposed by this camera.").font(.caption).foregroundStyle(FilmTheme.muted) }
        }.padding(11).background(FilmTheme.panel, in: RoundedRectangle(cornerRadius: 14)).accessibilityElement(children: .contain).accessibilityIdentifier("proControls")
    }
    @ViewBuilder private func parameterSlider(_ item: ProParameter) -> some View {
        switch item {
        case .iso:
            ProSlider(label: "ISO", value: Binding(get: { Double(controls.iso ?? camera.liveISO) }, set: { controls.iso = Float($0) }),
                range: Double(camera.capabilities.minISO)...Double(max(camera.capabilities.minISO + 1, camera.capabilities.maxISO)),
                auto: !controls.isManual, display: controls.iso == nil && controls.isManual ? "MTR \(Int(camera.liveISO))" : nil, reset: { controls.iso = nil })
        case .shutter:
            ProSlider(label: "Shutter", value: Binding(get: { log2(max(camera.capabilities.minTime, controls.seconds ?? camera.liveTime)) }, set: { controls.seconds = pow(2, $0) }),
                range: log2(max(0.000001, camera.capabilities.minTime))...log2(max(0.000002, camera.capabilities.maxTime)),
                auto: !controls.isManual, display: (controls.seconds == nil && controls.isManual ? "MTR " : "") + exposureLabel(controls.seconds ?? camera.liveTime), reset: { controls.seconds = nil })
        case .exposure:
            ProSlider(label: "EV", value: Binding(get: { Double(controls.ev) }, set: { controls.ev = Float($0) }),
                range: Double(camera.capabilities.minEV)...Double(camera.capabilities.maxEV), auto: false,
                display: String(format: "%+0.1f", controls.ev), reset: { controls.ev = 0 }).disabled(controls.isManual)
        case .whiteBalance:
            ProSlider(label: "WB", value: Binding(get: { Double(controls.kelvin ?? 5600) }, set: { controls.kelvin = Float($0) }),
                range: 2500...9900, auto: controls.kelvin == nil, display: "\(Int(controls.kelvin ?? 5600))K", reset: { controls.kelvin = nil })
        case .focus:
            VStack(spacing: 6) {
                Picker("Focus", selection: $controls.focus) { ForEach(camera.capabilities.focusModes, id: \.self) { mode in Text(mode).tag(mode) } }.pickerStyle(.segmented)
                if controls.focus == "MF" { Slider(value: $controls.focusPosition, in: 0...1).accessibilityLabel("Manual focus distance") }
            }
        }
    }
}

struct ProSlider: View {
    var label: String
    @Binding var value: Double
    var range: ClosedRange<Double>
    var auto: Bool
    var display: String? = nil
    var reset: () -> Void
    var body: some View {
        HStack(spacing: 8) {
            Text(label).frame(width: 43, alignment: .leading)
            Slider(value: $value, in: range).accessibilityLabel(label)
            Text(auto ? "AUTO" : (display ?? String(Int(value)))).monospacedDigit().frame(width: 67, alignment: .trailing)
            Button(action: reset) { Image(systemName: "arrow.uturn.backward").frame(width: 30, height: 30) }.accessibilityLabel("Reset \(label) to auto")
        }.font(.system(size: 10)).foregroundStyle(.white).tint(FilmTheme.accent)
    }
}
func exposureLabel(_ time: Double) -> String {
    guard time > 0 else { return "AUTO" }
    return time < 0.5 ? "1/\(Int((1 / time).rounded()))s" : String(format: "%0.2fs", time)
}
struct GridOverlay: Shape {
    var mode: String
    func path(in rect: CGRect) -> Path {
        var path = Path()
        let positions: [CGFloat] = mode == "Golden" ? [0.382, 0.618] : mode == "Centre" ? [0.5] : [1 / 3, 2 / 3]
        for value in positions {
            path.move(to: CGPoint(x: rect.width * value, y: 0)); path.addLine(to: CGPoint(x: rect.width * value, y: rect.height))
            path.move(to: CGPoint(x: 0, y: rect.height * value)); path.addLine(to: CGPoint(x: rect.width, y: rect.height * value))
        }
        return path
    }
}
struct HistogramView: View {
    var bins: [Double]
    var body: some View {
        GeometryReader { geometry in
            HStack(alignment: .bottom, spacing: 1) {
                ForEach(bins.indices, id: \.self) { index in
                    Rectangle().fill(.white.opacity(0.65)).frame(width: max(1, (geometry.size.width - 31) / 32), height: max(1, geometry.size.height * bins[index]))
                }
            }
        }.accessibilityLabel("Live ungraded luminance histogram")
    }
}
