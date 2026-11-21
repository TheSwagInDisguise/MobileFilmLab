import SwiftUI

struct LibraryView:View {
    @EnvironmentObject var store:FilmLabStore
    @Environment(\.dismiss) var dismiss
    var body:some View {
        NavigationStack {
            Group {
                if store.captures.isEmpty {
                    ContentUnavailableView("Your photographs",systemImage:"photo.on.rectangle",description:Text("Photos and unmodified DNGs are saved here before any Apple Photos import. No demo captures are included."))
                } else {
                    ScrollView {
                        LazyVGrid(columns:[GridItem(.flexible()),GridItem(.flexible())],spacing:12) {
                            ForEach(store.captures) {capture in
                                NavigationLink {CaptureDetail(capture:capture)} label:{LibraryTile(capture:capture)}
                            }
                        }.padding()
                    }
                }
            }.navigationTitle("App photo library").navigationBarTitleDisplayMode(.inline).toolbar {Button("Done") {dismiss()}}
        }.tint(FilmTheme.accent)
    }
}
struct LibraryTile:View {
    @EnvironmentObject var store:FilmLabStore
    var capture:SavedCapture
    var body:some View {
        VStack(alignment:.leading,spacing:6) {
            ZStack {
                Rectangle().fill(FilmTheme.panel)
                if let image=store.image(capture) {Image(uiImage:image).resizable().scaledToFill()}
                else {Image(systemName:"camera.badge.ellipsis").font(.largeTitle)}
            }.aspectRatio(1,contentMode:.fit).clipped().clipShape(RoundedRectangle(cornerRadius:12))
            Text(capture.title).font(.caption).lineLimit(1)
            Text(capture.raw != nil ? (capture.jpeg != nil ? "DNG + JPEG" : "DNG") : "JPEG").font(.caption2).foregroundStyle(.secondary)
        }
    }
}
struct CaptureDetail:View {
    @EnvironmentObject var store:FilmLabStore
    @Environment(\.dismiss) var dismiss
    @State var capture:SavedCapture
    @State private var original=false,deleting=false
    @State private var zoom:CGFloat=1
    var body:some View {ScrollView {VStack(alignment:.leading,spacing:16) {
        if let image=store.image(capture,original:original) {Image(uiImage:image).resizable().scaledToFit().scaleEffect(zoom).gesture(MagnifyGesture().onChanged {value in zoom=min(5,max(1,value.magnification))}).onTapGesture(count:2) {zoom=zoom>1 ? 1 : 2}.clipShape(RoundedRectangle(cornerRadius:16))} else {ContentUnavailableView("Unmodified DNG",systemImage:"camera.aperture",description:Text("Use Share to open this RAW / ProRAW in a DNG-compatible editor. There is no app-added look baked into the DNG."))}
        if FileManager.default.fileExists(atPath:store.fileURL(capture.id+"_Original.jpg").path) {Toggle("Show ungraded safety original",isOn:$original)}
        Text(capture.title).font(.title2.weight(.semibold));Text(capture.date.formatted()).font(.caption).foregroundStyle(.secondary)
        if capture.width>0 {Text("\(capture.width) × \(capture.height) · \(String(format:"%0.1f",Double(capture.width*capture.height)/1000000)) MP").font(.caption.monospaced()).foregroundStyle(.secondary)}
        Text(capture.note).font(.footnote).textSelection(.enabled)
        Button {store.shareCapture(capture)} label:{Label("Share original files",systemImage:"square.and.arrow.up").frame(maxWidth:.infinity)}.buttonStyle(.borderedProminent)
        Text("Keep exports outside the app before deleting or reinstalling. Removing this local copy does not delete an Apple Photos copy.").font(.caption).foregroundStyle(.secondary)
        Button("Delete local capture",role:.destructive) {deleting=true}
    }.padding()}.simultaneousGesture(DragGesture(minimumDistance:60).onEnded {value in if zoom==1,abs(value.translation.width)>abs(value.translation.height) {move(value.translation.width<0 ? 1 : -1)}}).navigationTitle("Photograph").navigationBarTitleDisplayMode(.inline).toolbar {ToolbarItemGroup(placement:.topBarTrailing) {Button {move(-1)} label:{Image(systemName:"chevron.left")}.accessibilityLabel("Newer photo");Button {move(1)} label:{Image(systemName:"chevron.right")}.accessibilityLabel("Older photo")}}.confirmationDialog("Delete the local photo and DNG?",isPresented:$deleting,titleVisibility:.visible) {Button("Delete",role:.destructive) {store.delete(capture);dismiss()};Button("Cancel",role:.cancel) {}}}
    func move(_ delta:Int) {guard let index=store.captures.firstIndex(where:{$0.id==capture.id}),store.captures.indices.contains(index+delta) else {return};capture=store.captures[index+delta];original=false;zoom=1}

}
