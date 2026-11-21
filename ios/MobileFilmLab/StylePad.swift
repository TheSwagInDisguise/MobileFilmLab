import SwiftUI
import FilmLabCore

struct StylePad:View {
    @EnvironmentObject var store:FilmLabStore
    @Environment(\.dismiss) var dismiss
    var body:some View {VStack(spacing:16) {
        HStack {VStack(alignment:.leading,spacing:4) {Text("Make it yours").font(.title2.weight(.semibold));Text("Tone / warmth–colour · JPEG + live preview").font(.caption).foregroundStyle(.secondary)};Spacer();Button("Done") {dismiss()}}
        GeometryReader {g in ZStack {
            RoundedRectangle(cornerRadius:22).fill(LinearGradient(colors:[Color(red:0.20,green:0.29,blue:0.33),Color(red:0.43,green:0.35,blue:0.25)],startPoint:.leading,endPoint:.trailing))
            RoundedRectangle(cornerRadius:22).fill(LinearGradient(colors:[.white.opacity(0.08),.black.opacity(0.55)],startPoint:.top,endPoint:.bottom))
            Path {p in p.move(to:CGPoint(x:g.size.width/2,y:14));p.addLine(to:CGPoint(x:g.size.width/2,y:g.size.height-14));p.move(to:CGPoint(x:14,y:g.size.height/2));p.addLine(to:CGPoint(x:g.size.width-14,y:g.size.height/2))}.stroke(.white.opacity(0.2),style:StrokeStyle(lineWidth:1,dash:[3,5]))
            VStack {Text("CONTRAST +");Spacer();HStack {Text("COOL");Spacer();Text("WARM")};Spacer();Text("MUTED −")}.font(.system(size:9,weight:.semibold,design:.monospaced)).foregroundStyle(.white.opacity(0.7)).padding(15)
            Circle().fill(.white).frame(width:25,height:25).shadow(color:.black.opacity(0.3),radius:7,y:3).overlay {Circle().stroke(FilmTheme.accent,lineWidth:3)}.position(x:(store.selected.puckColor+100)/200*g.size.width,y:(100-store.selected.puckTone)/200*g.size.height)
        }.contentShape(Rectangle()).gesture(DragGesture(minimumDistance:0).onChanged {v in store.selected.puckColor=clamp(v.location.x/g.size.width*200-100,-100,100);store.selected.puckTone=clamp(100-v.location.y/g.size.height*200,-100,100)}).accessibilityElement().accessibilityLabel("Tone and colour adjustment pad. Use sliders below for precise values.")}
        HStack {Text("Tone").font(.caption);Slider(value:$store.selected.puckTone,in:-100...100);Text("\(Int(store.selected.puckTone))").font(.caption.monospacedDigit()).frame(width:32)}
        HStack {Text("Colour").font(.caption);Slider(value:$store.selected.puckColor,in:-100...100);Text("\(Int(store.selected.puckColor))").font(.caption.monospacedDigit()).frame(width:32)}
        Button("Reset tone & colour") {store.selected.puckTone=0;store.selected.puckColor=0}.font(.caption)
    }.padding(22).tint(FilmTheme.accent)}
}
struct RecipeEditor:View {
    @EnvironmentObject var store:FilmLabStore
    @Environment(\.dismiss) var dismiss
    @State private var recipe=Recipe(id:"draft",name:"My recipe")
    @State private var name="My recipe"
    let styles=[(1,"Standard"),(2,"Vivid"),(3,"Neutral"),(4,"Portrait"),(5,"Landscape"),(6,"Monochrome"),(7,"Clear"),(8,"Deep"),(9,"Light"),(10,"Sunset"),(11,"Night colour"),(12,"Autumn"),(14,"Sepia")]
    var body:some View {NavigationStack {Form {
        Section("Your recipe") {TextField("Name",text:$name);Picker("Creative style",selection:$recipe.style) {ForEach(styles,id:\.0) {s in Text(s.1).tag(s.0)}};Text("Conventional approximations, not proprietary film, OEM processing or camera modes. Night colour is a look, not Night capture.").font(.caption).foregroundStyle(.secondary)}
        Section("Colour & detail") {integerSlider("Saturation",value:$recipe.sat,range:-3...3);integerSlider("Contrast",value:$recipe.con,range:-3...3);integerSlider("Sharpening",value:$recipe.sharp,range:-3...3);integerSlider("Amber / blue",value:$recipe.ab,range:-7...7);integerSlider("Green / magenta",value:$recipe.gm,range:-7...7);integerSlider("Recipe EV (grade)",value:$recipe.ev,range:-15...15)}
        Section("Recipe white balance") {Picker("Mode",selection:$recipe.wbMode) {Text("Keep").tag(0);Text("Auto").tag(1);Text("Kelvin tint approximation").tag(14)};if recipe.wbMode==14 {integerSlider("Kelvin",value:$recipe.kelvin,range:2500...9900)};Text("This grades the JPEG. Use Pro WB to lock the actual camera white balance.").font(.caption).foregroundStyle(.secondary)}
        Section("Interchange parameters") {Picker("Effect metadata",selection:$recipe.pe) {ForEach(0..<14) {i in Text(["Off","Toy","Pop","Poster","Retro","High-key","Part colour","Rough mono","Soft focus","HDR art","Rich mono","Miniature","Illustration","Watercolour"][i]).tag(i)}};integerSlider("DRO metadata",value:$recipe.dro,range:0...6);integerSlider("Effect sub-parameter",value:$recipe.sub,range:0...20);Text("As on Android, many upstream effect / DRO flags are preserved for recipe compatibility, not a promise of that proprietary effect. Retro, high-key and monochrome affect the shared colour grade; detail below zero is preserved but not synthesised as blur.").font(.caption).foregroundStyle(.secondary)}
        Section("Notes") {TextEditor(text:$recipe.notes).frame(minHeight:90)}
    }.navigationTitle("Custom recipe").navigationBarTitleDisplayMode(.inline).toolbar {ToolbarItem(placement:.cancellationAction) {Button("Cancel") {dismiss()}};ToolbarItem(placement:.confirmationAction) {Button("Save copy") {recipe.name=name;do {try recipe.validate();store.selected=recipe;store.saveCustom(name:name);dismiss()} catch {store.error=error.localizedDescription}}}}
        .task {recipe=store.selected;name=recipe.displayName+" Custom";if recipe.kelvin==0 {recipe.kelvin=5600}}
    }.tint(FilmTheme.accent)}
    func integerSlider(_ title:String,value:Binding<Int>,range:ClosedRange<Int>)->some View {VStack(alignment:.leading) {HStack {Text(title);Spacer();Text("\(value.wrappedValue)").foregroundStyle(.secondary).monospacedDigit()};Slider(value:Binding(get:{Double(value.wrappedValue)},set:{value.wrappedValue=Int($0.rounded())}),in:Double(range.lowerBound)...Double(range.upperBound),step:1)}}
}
