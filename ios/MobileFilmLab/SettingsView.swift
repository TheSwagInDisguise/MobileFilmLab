import SwiftUI
import FilmLabCore

struct SettingsView:View {
    @EnvironmentObject var store:FilmLabStore
    @EnvironmentObject var camera:CameraController
    @Environment(\.dismiss) var dismiss
    @State private var about=false,diagnostics=false
    var body:some View {NavigationStack {Form {
        Section {Label("Unpublished iOS personal test",systemImage:"testtube.2").foregroundStyle(FilmTheme.accent);Text("iPhone 18 Pro Max / iOS 27 is the target. This build needs installation and camera checks on your device; simulator success is not handset validation.").font(.footnote).foregroundStyle(.secondary)}
        Section("Capture pipeline") {
            Picker("JPEG pipeline",selection:$store.preferences.pipeline) {ForEach(CapturePipeline.allCases) {p in Text(p.label).tag(p)}}.disabled(store.preferences.format.writesRaw)
            Text("Original uses AVFoundation quality prioritisation. Smart / Quality HDR take a safety original plus sequential, locally aligned JPEG helpers, with motion rejection and guarded fallback. JPEGs already contain Apple's ISP processing. This is not Google's RAW HDR+, stock Night mode or a guaranteed quality improvement.").font(.caption).foregroundStyle(.secondary)
            Toggle("Balanced JPEG NR / tone",isOn:$store.preferences.balanced)
            Text("Conventional noise reduction and mild shadow adjustment; skipped after a successful HDR merge to avoid double-processing.").font(.caption).foregroundStyle(.secondary)
            Toggle("Buffered / responsive capture",isOn:$store.preferences.responsive).disabled(!camera.capabilities.responsive || store.preferences.format.writesRaw || store.preferences.pipeline != .original)
            Text("Uses exposed Apple buffering and responsive APIs where supported. This app still saves one transaction at a time; no overlapping burst UI. Disabled for custom HDR and RAW. Not an Android ZSL equivalence claim.").font(.caption).foregroundStyle(.secondary)
        }
        Section("Output") {
            Toggle("Maximum exposed photo dimensions",isOn:$store.preferences.maximum)
            Text("Requests the largest dimensions exposed by the selected route, not an invented sensor resolution. Custom HDR is capped at approximately 12 MP. Apple may deliver a different size. RAW dimensions remain system-defined.").font(.caption).foregroundStyle(.secondary)
            Toggle("Interpolated ~28 MP enlargement",isOn:$store.preferences.enlarge28).disabled(!store.preferences.format.writesJPEG)
            Text("Enlarges only when smaller than a 6120-pixel long edge; crop scales that target down. Interpolation adds pixels, not captured detail.").font(.caption).foregroundStyle(.secondary)
            Picker("Synthetic grain",selection:$store.preferences.grain) {Text("Off").tag(0);Text("Fine").tag(1);Text("Medium").tag(2);Text("Strong").tag(3)}
            Toggle("Keep Original JPEG companion",isOn:$store.preferences.saveOriginal)
            Toggle("Keep Original for comparison",isOn:$store.preferences.compare)
            Text("Companion is the ungraded safety image. RAW / ProRAW DNG bytes are never graded or merged by the app. RAW+JPEG exports a DNG and a separate, looked JPEG.").font(.caption).foregroundStyle(.secondary)
        }
        Section("Saving & interface") {
            Toggle("Also save to Apple Photos",isOn:$store.preferences.saveToPhotos)
            Text("Every shot is saved in the app library first. If Photos access is denied, use Share. RAW and JPEG are imported as separate Photos assets. Deleting this app deletes its private library: export photos and custom recipes before reinstalling or changing signing IDs.").font(.caption).foregroundStyle(.secondary)
            Picker("Grid",selection:$store.preferences.grid) {ForEach(["Off","Thirds","Golden","Centre"],id:\.self) {Text($0)}}
            Toggle("Horizon level",isOn:$store.preferences.level);Toggle("Capture haptics",isOn:$store.preferences.haptics)
        }
        Section("Lens colour calibration · JPEG only") {calibration("Red",value:$store.preferences.red);calibration("Green",value:$store.preferences.green);calibration("Blue",value:$store.preferences.blue);Button("Reset colour multipliers") {store.preferences.red=1;store.preferences.green=1;store.preferences.blue=1};Text("Global manual multipliers, not an automatic camera profile. Re-check when switching lenses.").font(.caption).foregroundStyle(.secondary)}
        Section("Exposed capabilities") {LabeledContent("Camera",value:camera.capabilities.name);LabeledContent("Bayer RAW",value:camera.capabilities.bayer==nil ? "Not exposed" : "Available");LabeledContent("Apple ProRAW",value:camera.capabilities.proRAW==nil ? "Not exposed" : "Available");LabeledContent("Manual exposure",value:camera.capabilities.customExposure ? "Available" : "Not exposed");Button("View diagnostics") {diagnostics=true}}
        Section {Button("About, source & GPL licence") {about=true};Text("No video modes. No microphone, location, tracking or network permission. No app-added AI enhancement. Apple firmware / ISP and computational ProRAW are outside that claim.").font(.caption).foregroundStyle(.secondary)}
        if let message=store.message {Section("Last save") {Text(message).font(.caption).textSelection(.enabled)}}
    }.navigationTitle("Settings").toolbar {Button("Done") {store.normalize();dismiss()}}.onChange(of:store.preferences.format) {_,_ in store.normalize()}.onChange(of:store.preferences.pipeline) {_,_ in store.normalize()}.sheet(isPresented:$about) {AboutView()}.sheet(isPresented:$diagnostics) {DiagnosticsView()}}.tint(FilmTheme.accent)}
    func calibration(_ name:String,value:Binding<Double>)->some View {HStack {Text(name).frame(width:52,alignment:.leading);Slider(value:value,in:0.8..0.1.2);Text(String(format:"%0.2f×",value.wrappedValue)).font(.caption.monospacedDigit()).foregroundStyle(.secondary)}}
}
struct AboutView:View {
    @Environment(\.dismiss) var dismiss
    @State private var licence=false
    let attribution=(Bundle.main.url(forResource:"Attribution",withExtension:"txt").flatMap {try? String(contentsOf:$0,encoding:.utf8)}) ?? "Attribution missing — consult the matching source tree."
    var body:some View {NavigationStack {ScrollView {VStack(alignment:.leading,spacing:20) {
        Image(systemName:"camera.aperture").font(.system(size:48,weight:.ultraLight)).foregroundStyle(FilmTheme.accent)
        Text("MobileFilmLab").font(.largeTitle.weight(.semibold));Text("Native iOS companion · personal test 0.1.0 (1)").foregroundStyle(.secondary)
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
struct DiagnosticsView:View {
    @EnvironmentObject var camera:CameraController
    @EnvironmentObject var store:FilmLabStore
    @Environment(\.dismiss) var dismiss
    var body:some View {NavigationStack {List {Section("Device / exposed APIs only") {Text("\(UIDevice.current.model) · iOS \(UIDevice.current.systemVersion)");Text("Routes: \(camera.routes.count)");ForEach(camera.routes) {r in VStack(alignment:.leading) {Text(r.name);Text(r.detail+(r.virtual ? " · virtual / automatic route" : " · physical route")).font(.caption).foregroundStyle(.secondary)}}};Section("Selected format bounds") {Text("ISO \(Int(camera.capabilities.minISO))–\(Int(camera.capabilities.maxISO))");Text("Shutter \(exposureLabel(camera.capabilities.minTime))–\(exposureLabel(camera.capabilities.maxTime))");ForEach(Array(camera.capabilities.dimensions.enumerated()),id:\.offset) {_,d in Text("\(d.width) × \(d.height)")};Text("A capability being listed does not prove request honouring or image quality. Test capture and DNG opening on your phone.").font(.caption)};Section("Last result") {Text(store.message ?? "No capture yet")}}.navigationTitle("Diagnostics").toolbar {Button("Done") {dismiss()}}}}
}
