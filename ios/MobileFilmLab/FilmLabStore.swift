import Foundation
import Combine
import UIKit
import Photos
import Vision
import CoreImage
import ImageIO
import FilmLabCore

@MainActor final class FilmLabStore:ObservableObject {
    let catalogue:Catalogue?
    @Published var selected:Recipe {didSet {persistState()}}
    @Published var preferences=CapturePreferences() {didSet {persistState()}}
    @Published var favourites=Set<String>() {didSet {persistState()}}
    @Published var custom:[Recipe]=[] {didSet {persistState()}}
    @Published var captures:[SavedCapture]=[]
    @Published var error:String?
    @Published var message:String?
    @Published var processing=false
    @Published var countdown=0
    @Published var share:ExportDocument?
    @Published var timer=0
    @Published var focusValue:Float=0.5
    let files:URL
    private var loading=true
    private let thumbnailCache=NSCache<NSString,UIImage>()
    private let worker=DispatchQueue(label:"com.mobilefilmlab.processing",qos:.userInitiated)
    private struct State:Codable {var selected:Recipe;var preferences:CapturePreferences;var favourites:Set<String>;var custom:[Recipe]}
    init() {
        let fm=FileManager.default
        files=(fm.urls(for:.documentDirectory,in:.userDomainMask).first ?? fm.temporaryDirectory).appendingPathComponent("FilmLab",isDirectory:true)
        var initial=Recipe(id:"stock_oppo_natural",name:"Natural",group:"OPPO")
        let c:Catalogue?
        do {c=try Catalogue.load();if let first=c?.recipes.first {initial=first}} catch {c=nil}
        catalogue=c;selected=initial
        do {
            try fm.createDirectory(at:files,withIntermediateDirectories:true)
            var resource=URLResourceValues();resource.isExcludedFromBackup=false;var folder=files;try folder.setResourceValues(resource)
            let stateURL=files.appendingPathComponent("recipes-and-settings.json")
            if fm.fileExists(atPath:stateURL.path) {
                let state=try JSONDecoder().decode(State.self,from:Data(contentsOf:stateURL));try state.selected.validate();try state.custom.forEach {try $0.validate()}
                selected=state.selected;preferences=state.preferences;preferences.normalize();favourites=state.favourites;custom=state.custom
            }
            let index=files.appendingPathComponent("library.json")
            if fm.fileExists(atPath:index.path) {captures=try JSONDecoder().decode([SavedCapture].self,from:Data(contentsOf:index))}
        } catch {self.error="Local library could not be fully read. Existing files were left in place. \(error.localizedDescription)"}
        if c==nil {error="Full recipe catalogue is missing or invalid. Reinstall from a verified build."}
        thumbnailCache.totalCostLimit=32*1024*1024
        loading=false
    }
    var allRecipes:[Recipe] {(catalogue?.recipes ?? [])+custom}
    func select(_ recipe:Recipe) {selected=recipe}
    func toggleFavourite(_ r:Recipe) {if favourites.contains(r.id) {favourites.remove(r.id)} else {favourites.insert(r.id)}}
    func normalize() {var p=preferences;p.normalize();if p != preferences {preferences=p}}
    func saveCustom(name:String) {
        do {var r=selected;r.id="custom_"+String(UUID().uuidString.prefix(8));r.name=name.trimmingCharacters(in:.whitespacesAndNewlines);r.group="CUSTOM";r.isCustom=true;r.isFavourite=false;try r.validate();custom.append(r);selected=r;message="Saved custom recipe"} catch {self.error=error.localizedDescription}
    }
    func deleteCustom(_ r:Recipe) {guard r.isCustom else {return};custom.removeAll {$0.id==r.id};favourites.remove(r.id);if selected.id==r.id,let first=catalogue?.recipes.first {selected=first}}
    private func persistState() {
        guard !loading else {return}
        do {let state=State(selected:selected,preferences:preferences,favourites:favourites,custom:custom);try JSONEncoder().encode(state).write(to:files.appendingPathComponent("recipes-and-settings.json"),options:.atomic)} catch {self.error="Settings could not be saved: \(error.localizedDescription)"}
    }
    func importData(_ data:Data) {
        do {
            let imported=try Interchange.decode(data)
            for var r in imported {r.id="custom_"+String(UUID().uuidString.prefix(8));r.group="CUSTOM";r.isCustom=true;r.isFavourite=false;custom.append(r)}
            if let last=custom.last {selected=last};message="Imported \(imported.count) recipe(s); existing recipes were not overwritten"
        } catch {self.error=error.localizedDescription}
    }
    func importURL(_ url:URL) {
        let access=url.startAccessingSecurityScopedResource();defer {if access {url.stopAccessingSecurityScopedResource()}}
        do {let values=try url.resourceValues(forKeys:[.fileSizeKey]);guard (values.fileSize ?? 0)<=Interchange.limit else {throw FilmError.invalid("Recipe file exceeds 256KB")};importData(try Data(contentsOf:url,options:.mappedIfSafe))} catch {self.error=error.localizedDescription}
    }
    func importQR(_ text:String) {do {importData(try Interchange.encode(Interchange.decodeQR(text)))} catch {self.error=error.localizedDescription}}
    func scanQRImage(_ data:Data) {
        worker.async {
            do {let request=VNDetectBarcodesRequest();request.symbologies=[.qr];let handler=VNImageRequestHandler(data:data,options:[:]);try handler.perform([request]);guard let payload=request.results?.compactMap(\.payloadStringValue).first(where:{$0.hasPrefix(Interchange.prefix)}) else {throw FilmError.invalid("No readable MobileFilmLab recipe QR in this image")};Task {@MainActor in self.importQR(payload)}}
            catch {Task {@MainActor in self.error=error.localizedDescription}}
        }
    }
    func exportRecipes(_ recipes:[Recipe]) {
        do {let data=try Interchange.encode(recipes);let url=FileManager.default.temporaryDirectory.appendingPathComponent("MobileFilmLab-\(recipes.count==1 ? "recipe" : "recipes").filmrecipe");try data.write(to:url,options:.atomic);share=ExportDocument(urls:[url])} catch {self.error=error.localizedDescription}
    }
    func qrImage(_ r:Recipe)->UIImage? {
        do {let text=try Interchange.qrPayload(r);guard let filter=CIFilter(name:"CIQRCodeGenerator") else {return nil};filter.setValue(Data(text.utf8),forKey:"inputMessage");filter.setValue("M",forKey:"inputCorrectionLevel");guard let ci=filter.outputImage?.transformed(by:CGAffineTransform(scaleX:8,y:8)),let cg=CIContext().createCGImage(ci,from:ci.extent) else {return nil};return UIImage(cgImage:cg)} catch {self.error=error.localizedDescription;return nil}
    }
    func shoot(_ camera:CameraController) async {
        guard !processing,!camera.busy,camera.ready,catalogue != nil else {return}
        processing=true;defer {processing=false;countdown=0}
        let seconds=timer
        if seconds>0 {for remaining in stride(from:seconds,through:1,by:-1) {countdown=remaining;do {try await Task.sleep(nanoseconds:1000000000)} catch {return};if UIApplication.shared.applicationState != .active {return}}};countdown=0
        var p=preferences;p.normalize();let recipe=selected
        let angle:CGFloat
        switch UIDevice.current.orientation {case .landscapeLeft:angle=0;case .landscapeRight:angle=180;case .portraitUpsideDown:angle=270;default:angle=90}
        do {
            let burst=try await camera.capture(p,angle:angle)
            if p.haptics {UIImpactFeedbackGenerator(style:.light).impactOccurred()}
            let result:ProcessedPhoto=try await withCheckedThrowingContinuation {continuation in worker.async {
                do {let processor=PhotoProcessor();let result=try autoreleasepool {try processor.process(burst,recipe:recipe,preferences:p)};continuation.resume(returning:result)}
                catch {
                    // A failed grade must not discard a successfully delivered JPEG / DNG.
                    if let first=burst.frames.first,first.jpeg != nil || first.raw != nil {
                        let image=first.jpeg.flatMap {PhotoProcessor.upright($0)}
                        continuation.resume(returning:ProcessedPhoto(data:p.format.writesJPEG ? first.jpeg : nil,raw:first.raw,original:nil,width:Int(image?.extent.width ?? 0),height:Int(image?.extent.height ?? 0),note:"Processing failed; captured original retained without requested grade/crop: \(error.localizedDescription)"))
                    } else {continuation.resume(throwing:error)}
                }
            }}
            let capture=try store(result,recipe:recipe)
            message="Saved to app library · \(result.note)"
            if p.saveToPhotos {await addToPhotos(capture)}
        } catch {self.error=error.localizedDescription}
    }
    private func store(_ result:ProcessedPhoto,recipe:Recipe) throws -> SavedCapture {
        let id="MFL_"+ISO8601DateFormatter().string(from:Date()).replacingOccurrences(of:":",with:"")+"_"+String(UUID().uuidString.prefix(6))
        let jpg=result.data != nil ? id+".jpg" : nil,raw=result.raw != nil ? id+".dng" : nil
        // Durable originals/exports first; a Photos denial must never discard this local copy.
        if let bytes=result.data,let jpg {try bytes.write(to:files.appendingPathComponent(jpg),options:.atomic)}
        if let bytes=result.raw,let raw {try bytes.write(to:files.appendingPathComponent(raw),options:.atomic)}
        if let bytes=result.original {try bytes.write(to:files.appendingPathComponent(id+"_Original.jpg"),options:.atomic)}
        let capture=SavedCapture(id:id,date:Date(),title:recipe.displayName,jpeg:jpg,raw:raw,note:result.note,width:result.width,height:result.height)
        captures.insert(capture,at:0);try JSONEncoder().encode(captures).write(to:files.appendingPathComponent("library.json"),options:.atomic)
        return capture
    }
    private func addToPhotos(_ capture:SavedCapture) async {
        var status=PHPhotoLibrary.authorizationStatus(for:.addOnly)
        if status == .notDetermined {status=await PHPhotoLibrary.requestAuthorization(for:.addOnly)}
        guard status == .authorized || status == .limited else {message="Saved to app library. Photos permission denied; use Share to export safely.";return}
        let urls=[capture.jpeg,capture.raw].compactMap {$0}.map {files.appendingPathComponent($0)}
        do {try await PHPhotoLibrary.shared().performChanges {
            // Separate JPEG and DNG assets preserve native bytes and avoid unsupported re-encoded RAW pairings.
            for url in urls {let request=PHAssetCreationRequest.forAsset();request.addResource(with:.photo,fileURL:url,options:nil)}
        }} catch {self.error="Saved in app library, but Photos could not import it: \(error.localizedDescription)"}
    }
    func fileURL(_ name:String)->URL {files.appendingPathComponent(name)}
    func image(_ capture:SavedCapture,original:Bool=false)->UIImage? {
        let name=original ? capture.id+"_Original.jpg" : capture.jpeg
        if let name,let cached=thumbnailCache.object(forKey:name as NSString) {return cached}
        guard let name,let source=CGImageSourceCreateWithURL(fileURL(name) as CFURL,nil),let cg=CGImageSourceCreateThumbnailAtIndex(source,0,[kCGImageSourceCreateThumbnailFromImageAlways:true,kCGImageSourceCreateThumbnailWithTransform:true,kCGImageSourceThumbnailMaxPixelSize:1600] as CFDictionary) else {return nil};let result=UIImage(cgImage:cg);thumbnailCache.setObject(result,forKey:name as NSString,cost:cg.bytesPerRow*cg.height);return result
    }
    func shareCapture(_ capture:SavedCapture) {let urls=[capture.jpeg,capture.raw].compactMap {$0}.map(fileURL);if !urls.isEmpty {share=ExportDocument(urls:urls)}}
    func delete(_ capture:SavedCapture) {
        do {for name in [capture.jpeg,capture.raw,capture.id+"_Original.jpg"].compactMap({$0}) {let url=fileURL(name);if FileManager.default.fileExists(atPath:url.path) {try FileManager.default.removeItem(at:url)}};captures.removeAll {$0.id==capture.id};try JSONEncoder().encode(captures).write(to:files.appendingPathComponent("library.json"),options:.atomic)} catch {self.error=error.localizedDescription}
    }
}
