import SwiftUI
import FilmLabCore

@main struct MobileFilmLabApp:App {
    @StateObject private var store=FilmLabStore()
    @StateObject private var camera=CameraController()
    @Environment(\.scenePhase) private var phase
    var body:some Scene {WindowGroup {
        CameraView().environmentObject(store).environmentObject(camera).preferredColorScheme(.dark)
            .task {camera.updateLook(store.selected,store.preferences);camera.start()}
            .onChange(of:phase) {_,phase in camera.setForeground(phase == .active)}
            .onChange(of:store.selected) {_,r in camera.updateLook(r,store.preferences)}
            .onChange(of:store.preferences) {_,p in camera.updateLook(store.selected,p)}
            .onChange(of:camera.selectedID) {_,_ in
                if !camera.capabilities.supports(store.preferences.format) {store.preferences.format = .jpeg}
                store.normalize()
            }
            .onOpenURL {store.importURL($0)}
            .alert("MobileFilmLab",isPresented:Binding(get:{store.error != nil},set:{if !$0 {store.error=nil}})) {Button("OK",role:.cancel) {store.error=nil}} message:{Text(store.error ?? "")}
            .sheet(item:$store.share) {ShareView(urls:$0.urls)}
    }}
}
struct ShareView:UIViewControllerRepresentable {
    var urls:[URL]
    func makeUIViewController(context:Context)->UIActivityViewController {UIActivityViewController(activityItems:urls,applicationActivities:nil)}
    func updateUIViewController(_ uiViewController:UIActivityViewController,context:Context) {}
}
enum FilmTheme {static let accent=Color(red:0.85,green:0.97,blue:0.63);static let panel=Color(red:0.07,green:0.09,blue:0.09);static let muted=Color(white:0.72)}
