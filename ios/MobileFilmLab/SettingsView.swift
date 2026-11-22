import SwiftUI
import UIKit
import FilmLabCore

private var appVersionLabel: String {
    let version = Bundle.main.object(forInfoDictionaryKey: "CFBundleShortVersionString") as? String ?? "0.1.0"
    let build = Bundle.main.object(forInfoDictionaryKey: "CFBundleVersion") as? String ?? "–"
    return "\(version) (\(build))"
}

struct SettingsView: View {
    @EnvironmentObject var store: FilmLabStore
    @EnvironmentObject var camera: CameraController
    @Environment(\.dismiss) var dismiss
    @State private var about = false
    @State private var diagnostics = false
    var body: some View {
        NavigationStack {
            Form {
                Section("Capture pipeline") {
                    Picker("JPEG mode", selection: $store.preferences.pipeline) {
                        ForEach(CapturePipeline.allCases) { mode in Text(mode.label).tag(mode) }
                    }.disabled(store.preferences.format.writesRaw)
                    Toggle("Balanced noise & tone", isOn: $store.preferences.balanced)
                    Toggle("Buffered capture", isOn: $store.preferences.responsive)
                        .disabled(!camera.capabilities.responsive || store.preferences.format.writesRaw || store.preferences.pipeline != .original)
                    DisclosureGroup("How these modes work") {
                        Text("Original uses the camera's quality-prioritised JPEG. Smart / Quality HDR add sequential, locally aligned JPEG exposures with motion rejection and a safety Original. This is not proprietary stock Night mode or a guaranteed quality improvement. RAW stays single-exposure.")
                        Text("Balanced adds conventional noise reduction and tone adjustment; it is skipped after a successful HDR merge. Buffered capture uses public iOS APIs where supported, not Android PRIVATE reprocessing. Only one app save transaction runs at a time.")
                    }.font(.footnote).foregroundStyle(.secondary)
                }
                Section("Photo output") {
                    Toggle("Maximum native size", isOn: $store.preferences.maximum)
                    Picker("Grain", selection: $store.preferences.grain) {
                        Text("Off").tag(0); Text("Fine").tag(1); Text("Medium").tag(2); Text("Strong").tag(3)
                    }
                    Toggle("Keep unedited JPEG", isOn: Binding(get: { store.preferences.saveOriginal || store.preferences.compare }, set: { store.preferences.saveOriginal = $0; store.preferences.compare = $0 }))
                    DisclosureGroup("Advanced output") {
                        Toggle("Interpolated ~28 MP enlargement", isOn: $store.preferences.enlarge28).disabled(!store.preferences.format.writesJPEG)
                        Text("Maximum size requests only dimensions actually exposed by the selected camera. HDR is capped near 12 MP. Enlargement adds pixels, not captured detail. DNG bytes are never graded, cropped or merged by the app; RAW+JPEG saves separate files.")
                            .font(.footnote).foregroundStyle(.secondary)
                    }
                }
                Section("Saving & composition") {
                    Toggle("Save to Apple Photos", isOn: $store.preferences.saveToPhotos)
                    Picker("Grid", selection: $store.preferences.grid) { ForEach(["Off", "Thirds", "Golden", "Centre"], id: \.self) { Text($0) } }
                    Toggle("Horizon level", isOn: $store.preferences.level)
                    Toggle("Shutter haptics", isOn: $store.preferences.haptics)
                    DisclosureGroup("Saving & backups") {
                        Text("Every capture is kept in the app library first, even if Photos access is denied. JPEG and DNG are separate Photos assets. Export photos and recipes before deleting/reinstalling the app or changing its signing ID.")
                            .font(.footnote).foregroundStyle(.secondary)
                    }
                }
                Section("Advanced") {
                    DisclosureGroup("Camera routing") {
                        if let automatic = camera.automaticRearRoute {
                            Button("Use automatic rear lenses") { camera.select(automatic.id) }
                                .disabled(store.processing || camera.busy)
                        }
                        Text("The viewfinder shows physical lenses for the current camera side only. Automatic multi-camera routes and alternate front cameras remain available in Diagnostics. A JPEG crop is never labelled as a lens.")
                            .font(.footnote).foregroundStyle(.secondary)
                        Button("All camera routes & diagnostics") { diagnostics = true }.accessibilityIdentifier("openDiagnostics")
                    }
                    DisclosureGroup("Lens colour calibration") {
                        calibration("Red", value: $store.preferences.red)
                        calibration("Green", value: $store.preferences.green)
                        calibration("Blue", value: $store.preferences.blue)
                        Button("Reset colour") { store.preferences.red = 1; store.preferences.green = 1; store.preferences.blue = 1 }
                        Text("Manual JPEG multipliers, not an automatic camera profile. Re-check when switching lenses.").font(.footnote).foregroundStyle(.secondary)
                    }
                    DisclosureGroup("Camera capabilities") {
                        LabeledContent("Active lens", value: camera.capabilities.name)
                        LabeledContent("Bayer RAW", value: camera.capabilities.bayer == nil ? "Not exposed" : "Available")
                        LabeledContent("Apple ProRAW", value: camera.capabilities.proRAW == nil ? "Not exposed" : "Available")
                        LabeledContent("Manual exposure", value: camera.capabilities.customExposure ? "Available" : "Not exposed")
                    }
                }
                Section {
                    Button("About, source & licence") { about = true }
                    LabeledContent("Version", value: appVersionLabel).foregroundStyle(.secondary)
                } footer: {
                    Text("Photos only. No app-added AI enhancement. Beta software: back up important photos and recipes.")
                }
                if let message = store.message { Section("Last save") { Text(message).font(.caption).textSelection(.enabled) } }
            }
            .navigationTitle("Settings")
            .toolbar { Button("Done") { store.normalize(); dismiss() } }
            .onChange(of: store.preferences.format) { _, _ in store.normalize() }
            .onChange(of: store.preferences.pipeline) { _, _ in store.normalize() }
            .sheet(isPresented: $about) { AboutView() }
            .sheet(isPresented: $diagnostics) { DiagnosticsView() }
        }.tint(FilmTheme.accent)
    }
    private func calibration(_ name: String, value: Binding<Double>) -> some View {
        HStack {
            Text(name).frame(width: 52, alignment: .leading)
            Slider(value: value, in: 0.8...1.2)
            Text(String(format: "%0.2f×", value.wrappedValue)).font(.caption.monospacedDigit()).foregroundStyle(.secondary)
        }
    }
}

struct AboutView:View {
    @Environment(\.dismiss) var dismiss
    @State private var licence=false
    let attribution=(Bundle.main.url(forResource:"Attribution",withExtension:"txt").flatMap {try? String(contentsOf:$0,encoding:.utf8)}) ?? "Attribution missing — consult the matching source tree."
    var body:some View {NavigationStack {ScrollView {VStack(alignment:.leading,spacing:20) {
        Image(systemName:"camera.aperture").font(.system(size:48,weight:.ultraLight)).foregroundStyle(FilmTheme.accent)
        Text("MobileFilmLab").font(.largeTitle.weight(.semibold));Text("Native iOS · " + appVersionLabel).foregroundStyle(.secondary)
        Text("Beta software for iPhone 18 Pro Max / public iOS 27. Installation, real camera behaviour and photo quality still need checks on your device; simulator success is not handset validation.").font(.footnote).foregroundStyle(.secondary)
        Text("AI-development disclosure").font(.headline);Text("This application and some design / documentation assets were developed with AI assistance. Any software-test screenshots are labelled as simulator screenshots, not photographs taken with the target phone. Camera capture and processing in the app do not use added AI models or generative enhancement.")
        Text("Original branding and interface. Photos only. Conventional recipe approximations and HDR are not affiliated with camera, film or phone manufacturers. Apple ProRAW may contain Apple's computational processing; firmware internals are outside the no-app-added-AI claim.")
        Text("GPL-3.0-or-later").font(.headline);Text("A derivative of the credited open-source recipe work. You receive source and licence rights; do not remove attribution when sharing modified copies. Signing / sideloading is your choice and may be affected by certificate expiry or revocation. No App Store publication or licensing review is implied.")
        Link("Matching project source",destination:URL(string:"https://github.com/TheSwagInDisguise/MobileFilmLab")!)
        Text("Source commit: \(Bundle.main.object(forInfoDictionaryKey:"MFLSourceSHA") as? String ?? "local development")").font(.caption.monospaced()).textSelection(.enabled)
        Button("Read full GPL licence") {licence=true}
        Text("No warranty").font(.headline);Text("Experimental software supplied without warranty, to the extent permitted by law. You use it at your own risk. The authors are not responsible for bricked devices, lost photos / data, overheating, installation problems or other damage caused by using or sideloading it, except where liability cannot lawfully be excluded. Keep backups; this is not your only camera for irreplaceable events.")
        Divider();Text(attribution).font(.footnote).textSelection(.enabled)
    }.padding(24)}.navigationTitle("About").navigationBarTitleDisplayMode(.inline).toolbar {Button("Done") {dismiss()}}.sheet(isPresented:$licence) {NavigationStack {ScrollView {Text((Bundle.main.url(forResource:"GPL-3.0-or-later",withExtension:"txt").flatMap {try? String(contentsOf:$0,encoding:.utf8)}) ?? "See project LICENSE").font(.caption.monospaced()).padding().textSelection(.enabled)}.navigationTitle("GPL-3.0-or-later").toolbar {Button("Done") {licence=false}}}}}}
}

struct DiagnosticsView: View {
    @EnvironmentObject var camera: CameraController
    @EnvironmentObject var store: FilmLabStore
    @Environment(\.dismiss) var dismiss
    var body: some View {
        NavigationStack {
            List {
                Section("Live preview") {
                    PreviewDiagnosticsRows(frames: camera.previewFrames, performance: camera.previewPerformance)
                }
                Section("Device / exposed APIs only") {
                    Text("\(UIDevice.current.model) · iOS \(UIDevice.current.systemVersion)")
                    Text("\(camera.routes.count) camera routes · \(camera.visibleLenses.count) in the active lens row")
                    ForEach(camera.routes) { route in
                        VStack(alignment: .leading, spacing: 5) {
                            HStack {
                                Text(route.name + " · " + route.shortName)
                                Spacer()
                                if route.id == camera.selectedID { Image(systemName: "checkmark").foregroundStyle(FilmTheme.accent) }
                            }
                            Text(route.detail + (route.virtual ? " · automatic / virtual" : " · physical"))
                                .font(.caption).foregroundStyle(.secondary)
                            Text(route.id).font(.system(size: 9, design: .monospaced)).foregroundStyle(.secondary).textSelection(.enabled)
                            if route.id != camera.selectedID {
                                Button("Use this route") { camera.select(route.id) }.font(.caption).disabled(store.processing || camera.busy)
                                    .accessibilityIdentifier("useRoute-" + route.id)
                            }
                        }.padding(.vertical, 4)
                    }
                }
                Section("Selected format bounds") {
                    Text("ISO \(Int(camera.capabilities.minISO))–\(Int(camera.capabilities.maxISO))")
                    Text("Shutter \(exposureLabel(camera.capabilities.minTime))–\(exposureLabel(camera.capabilities.maxTime))")
                    ForEach(Array(camera.capabilities.dimensions.enumerated()), id: \.offset) { _, size in Text("\(size.width) × \(size.height)") }
                    Text("Numeric lens labels are derived from the system's constituent/crossover metadata relative to Main. If that mapping is unavailable, simple lens names are used instead of guessed zoom numbers. A listed capability does not prove real capture or quality.")
                        .font(.caption).foregroundStyle(.secondary)
                }
                Section("Last result") { Text(store.message ?? "No capture yet") }
            }.navigationTitle("Diagnostics").toolbar { Button("Done") { dismiss() } }
        }
    }
}

private struct PreviewDiagnosticsRows: View {
    let frames: LatestFrameMailbox<CameraPreviewFrame>
    let performance: PreviewPerformance
    @State private var snapshot = PreviewPerformance.Snapshot()
    @State private var pending = 0
    @State private var replaced: UInt64 = 0
    var body: some View {
        VStack(alignment: .leading, spacing: 9) {
            LabeledContent("Renderer", value: snapshot.renderer)
            LabeledContent("Input / rendered", value: "~\(snapshot.inputFPS) / ~\(snapshot.presentedFPS) per second")
            LabeledContent("Input proxy", value: snapshot.inputSize)
            LabeledContent("Display drawable", value: snapshot.drawableSize)
            LabeledContent("Pending frames", value: "\(pending) / 1")
            LabeledContent("Stale frames replaced", value: String(replaced))
            LabeledContent("Render errors", value: String(snapshot.renderErrors))
            Text("Approximate one-second input/completed-render counters, not a photo or physical-display FPS benchmark. One pending frame and at most two GPU renders are allowed. Low light, long manual exposures, thermal limits and still/HDR capture can reduce live input. No diagnostic data is uploaded.")
                .font(.caption).foregroundStyle(.secondary)
        }.task {
            while !Task.isCancelled {
                snapshot = performance.snapshot()
                let statistics = frames.statistics; pending = statistics.pendingCount; replaced = statistics.replaced
                do { try await Task.sleep(nanoseconds: 1_000_000_000) } catch { break }
            }
        }
    }
}
