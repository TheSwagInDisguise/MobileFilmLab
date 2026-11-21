import SwiftUI
import PhotosUI
import UniformTypeIdentifiers
import CoreImage
import FilmLabCore

struct RecipeDrawer:View {
    @EnvironmentObject var store:FilmLabStore
    @EnvironmentObject var camera:CameraController
    @Environment(\.dismiss) var dismiss
    @State private var group="ALL",search="",onlyFavourite=false,importing=false,editing=false
    @State private var qr:Recipe?,photo:PhotosPickerItem?,qrText="",showQRText=false
    var filtered:[Recipe] {store.allRecipes.filter {(group=="ALL" || $0.group==group) && (!onlyFavourite || store.favourites.contains($0.id)) && (search.isEmpty || $0.displayName.localizedCaseInsensitiveContains(search))}}
    var body:some View {
        NavigationStack {
            VStack(spacing:12) {
                ScrollView(.horizontal,showsIndicators:false) {HStack {chip("All \(store.allRecipes.count)",id:"ALL");ForEach(store.catalogue?.groups ?? []) {g in chip(g.displayName,id:g.id)}}.padding(.horizontal)}
                HStack {Label("Live colour previews",systemImage:"camera.aperture").font(.caption).foregroundStyle(.secondary);Spacer();Button {onlyFavourite.toggle()} label:{Image(systemName:onlyFavourite ? "star.fill" : "star")}.accessibilityLabel("Show favourites")}.padding(.horizontal)
                ScrollView {LazyVGrid(columns:[GridItem(.flexible()),GridItem(.flexible())],spacing:14) {ForEach(filtered) {r in
                    VStack(alignment:.leading,spacing:7) {
                        Button {store.select(r);dismiss()} label:{LookTile(recipe:r).environmentObject(camera).overlay(alignment:.bottomLeading) {Text(r.displayName).font(.caption.weight(.semibold)).lineLimit(2).foregroundStyle(.white).padding(8).frame(maxWidth:.infinity,alignment:.leading).background(LinearGradient(colors:[.clear,.black.opacity(0.85)],startPoint:.top,endPoint:.bottom))}.clipShape(RoundedRectangle(cornerRadius:13)).overlay {RoundedRectangle(cornerRadius:13).stroke(store.selected.id==r.id ? FilmTheme.accent : .clear,lineWidth:2)}}
                        HStack {Text(r.summary).font(.system(size:9)).lineLimit(1).foregroundStyle(.secondary);Spacer();Button {store.toggleFavourite(r)} label:{Image(systemName:store.favourites.contains(r.id) ? "star.fill" : "star").font(.caption).foregroundStyle(FilmTheme.accent)}}
                    }.contextMenu {Button("Use recipe") {store.select(r);dismiss()};Button("Export recipe") {store.exportRecipes([r])};Button("Show QR code") {qr=r};Button("Edit a copy") {store.selected=r;editing=true};if r.isCustom {Button("Delete custom recipe",role:.destructive) {store.deleteCustom(r)}}}
                }}.padding()}
                HStack {Button("Edit current") {editing=true};Spacer();Text("77 upstream looks + 3 natural baselines").font(.system(size:9)).foregroundStyle(.secondary)}.font(.caption).padding(.horizontal).padding(.bottom,8)
            }.navigationTitle("The look library").navigationBarTitleDisplayMode(.inline).searchable(text:$search,prompt:"Search all looks")
                .toolbar {ToolbarItem(placement:.topBarLeading) {Button("Done") {dismiss()}};ToolbarItem(placement:.topBarTrailing) {Menu {Button("Import .filmrecipe / JSON") {importing=true};Button("Import QR text") {showQRText=true};PhotosPicker(selection:$photo,matching:.images) {Label("Scan QR from image",systemImage:"qrcode.viewfinder")};Button("Export current recipe") {store.exportRecipes([store.selected])};Button("Show current QR") {qr=store.selected};Button("Export custom recipes") {store.exportRecipes(store.custom)}.disabled(store.custom.isEmpty);Button("Export favourites") {store.exportRecipes(store.allRecipes.filter {store.favourites.contains($0.id)})}.disabled(store.favourites.isEmpty)} label:{Image(systemName:"square.and.arrow.up")}.accessibilityLabel("Import or export recipes")}}
                .fileImporter(isPresented:$importing,allowedContentTypes:[.json,UTType(exportedAs:"com.mobilefilmlab.recipe",conformingTo:.json)],allowsMultipleSelection:false) {result in do {if let url=try result.get().first {store.importURL(url)}} catch {store.error=error.localizedDescription}}
                .sheet(item:$qr) {QRView(recipe:$0)}
                .sheet(isPresented:$editing) {RecipeEditor()}
                .sheet(isPresented:$showQRText) {NavigationStack {VStack {Text("Paste a FILMRECIPE:1: QR payload from MobileFilmLab.").font(.caption).foregroundStyle(.secondary);TextEditor(text:$qrText).font(.caption.monospaced());Button("Import QR recipe") {store.importQR(qrText.trimmingCharacters(in:.whitespacesAndNewlines));showQRText=false}.buttonStyle(.borderedProminent)}.padding().navigationTitle("Import QR text").toolbar {Button("Cancel") {showQRText=false}}}}
                .onChange(of:photo) {_,item in Task {if let data=try? await item?.loadTransferable(type:Data.self) {store.scanQRImage(data)}}}
        }.tint(FilmTheme.accent)
    }
    func chip(_ title:String,id:String)->some View {Button {group=id} label:{Text(title).font(.caption).padding(.horizontal,12).padding(.vertical,8).background(group==id ? FilmTheme.accent : Color.white.opacity(0.08),in:Capsule()).foregroundStyle(group==id ? .black : .white)}}
}
struct LookTile:View {
    @EnvironmentObject var camera:CameraController
    var recipe:Recipe
    @State private var image:UIImage?
    private static let context=CIContext(options:[.workingColorSpace:NSNull(),.cacheIntermediates:false])
    var body:some View {ZStack {Rectangle().fill(FilmTheme.panel);if let image {Image(uiImage:image).resizable().scaledToFill()} else {Image(systemName:"camera.aperture").font(.largeTitle).foregroundStyle(.white.opacity(0.25))}}.aspectRatio(1.05,contentMode:.fit).clipped().onReceive(camera.$lookSource) {source in guard let source,let cg=source.cgImage else {return};let input=CIImage(cgImage:cg),output=PhotoProcessor.matrix(input,recipe:recipe,preferences:CapturePreferences());if let rendered=Self.context.createCGImage(output,from:output.extent) {image=UIImage(cgImage:rendered)}}}
}
struct QRView:View {
    @EnvironmentObject var store:FilmLabStore
    @Environment(\.dismiss) var dismiss
    var recipe:Recipe
    @State private var qr:UIImage?
    var body:some View {NavigationStack {VStack(spacing:22) {Text(recipe.displayName).font(.title2.weight(.semibold));if let qr {Image(uiImage:qr).interpolation(.none).resizable().scaledToFit().padding(18).background(.white,in:RoundedRectangle(cornerRadius:12))};Text("Compatible with MobileFilmLab Android recipe QR codes. Use a recipe file for larger notes.").font(.footnote).foregroundStyle(.secondary).multilineTextAlignment(.center);Button("Export recipe file instead") {store.exportRecipes([recipe])}}.padding(28).navigationTitle("Recipe QR").navigationBarTitleDisplayMode(.inline).toolbar {Button("Done") {dismiss()}}.task {qr=store.qrImage(recipe)}}}
}
