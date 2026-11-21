import SwiftUI
import AVFoundation
import FilmLabCore

struct CameraView:View {
    @EnvironmentObject var store:FilmLabStore
    @EnvironmentObject var camera:CameraController
    @State private var recipes=false
    @State private var settings=false
    @State private var library=false
    @State private var styles=false
    @State private var pro=false
    @State private var controls=ManualControls()
    @State private var flash:FlashChoice = .off
    var body:some View {
        GeometryReader {geometry in
            VStack(spacing:12) {
                header
                HStack {
                    Button {pro.toggle();if !pro {controls=ManualControls();camera.setControls(controls)}} label:{Text(pro ? "PRO" : "AUTO").font(.caption.bold()).padding(.horizontal,13).padding(.vertical,8).background(pro ? FilmTheme.accent : Color.white.opacity(0.1),in:Capsule()).foregroundStyle(pro ? .black : .white)}.accessibilityLabel("Toggle Auto and Pro controls")
                    Spacer()
                    Menu {
                        ForEach(CaptureFormat.allCases.filter {camera.capabilities.supports($0)}) {f in Button(f.label) {store.preferences.format=f;store.normalize()}}
                    } label:{Text(store.preferences.format.label).font(.caption.bold());Image(systemName:"chevron.down").font(.caption2)}.accessibilityIdentifier("captureFormat")
                    Spacer()
                    Button {store.timer=store.timer==0 ? 3 : store.timer==3 ? 10 : 0} label:{Image(systemName:store.timer==0 ? "timer" : "\(store.timer).circle").frame(width:32,height:32)}.accessibilityLabel("Timer \(store.timer) seconds")
                    if camera.capabilities.flash || camera.capabilities.torch {Button {flash=flash.next;camera.setFlash(flash)} label:{Image(systemName:flash == .off ? "bolt.slash" : flash == .torch ? "flashlight.on.fill" : "bolt.fill").foregroundStyle(flash == .off ? .white : FilmTheme.accent).frame(width:32,height:32)}.accessibilityLabel("Flash \(flash.rawValue)")}
                }.foregroundStyle(.white)
                preview.frame(maxHeight:.infinity)
                if pro {manualPanel.disabled(!camera.ready)}
                Button {recipes=true} label:{
                    HStack(spacing:12) {RoundedRectangle(cornerRadius:4).fill(FilmTheme.accent).frame(width:3,height:30);VStack(alignment:.leading,spacing:4) {Text(store.selected.displayName).font(.subheadline.weight(.semibold));Text(store.selected.summary).font(.caption2).foregroundStyle(FilmTheme.muted).lineLimit(1)};Spacer();Image(systemName:"square.stack.3d.up").font(.title3)}.padding(12).background(FilmTheme.panel,in:RoundedRectangle(cornerRadius:15))
                }.foregroundStyle(.white).accessibilityIdentifier("openRecipes")
                HStack {
                    Button {library=true} label:{ZStack {RoundedRectangle(cornerRadius:13).fill(Color.white.opacity(0.12));if let latest=store.captures.first,let image=store.image(latest) {Image(uiImage:image).resizable().scaledToFill().frame(width:52,height:52).clipped().clipShape(RoundedRectangle(cornerRadius:13))} else {Image(systemName:"photo.on.rectangle").font(.title2)}}.frame(width:52,height:52)}.accessibilityLabel("Photo library")
                    Spacer()
                    Button {Task {await store.shoot(camera)}} label:{ZStack {Circle().stroke(Color.white.opacity(0.7),lineWidth:2).frame(width:80,height:80);Circle().fill(store.processing ? Color.white.opacity(0.25) : .white).frame(width:66,height:66);if store.processing {ProgressView().tint(.black)}}}.disabled(!camera.ready || store.processing || camera.busy || store.catalogue==nil).accessibilityLabel("Take photo").accessibilityIdentifier("shutter")
                    Spacer()
                    Button {styles=true} label:{Image(systemName:"slider.horizontal.3").font(.title2).frame(width:52,height:52).background(Color.white.opacity(0.1),in:RoundedRectangle(cornerRadius:13))}.accessibilityLabel("Tone and colour pad")
                }.foregroundStyle(.white)
                Text(store.processing ? (camera.busy ? camera.status : "Processing and saving…") : "UNPUBLISHED iOS TEST · Photos only · No app-added AI").font(.system(size:9,weight:.medium,design:.monospaced)).foregroundStyle(FilmTheme.muted).lineLimit(1).minimumScaleFactor(0.7)
            }.padding(.horizontal,18).padding(.top,8).padding(.bottom,8).frame(width:geometry.size.width,height:geometry.size.height)
        }.background(Color(red:0.025,green:0.035,blue:0.035).ignoresSafeArea())
            .background(HardwareCaptureKeys(enabled:camera.ready && !store.processing && !recipes && !settings && !library && !styles) {Task {await store.shoot(camera)}})
            .sheet(isPresented:$recipes) {RecipeDrawer().presentationDetents([.medium,.large]).presentationDragIndicator(.visible)}
            .sheet(isPresented:$styles) {StylePad().presentationDetents([.height(440)]).presentationDragIndicator(.visible)}
            .sheet(isPresented:$settings) {SettingsView()}
            .sheet(isPresented:$library) {LibraryView()}
            .onChange(of:controls) {_,m in camera.setControls(m)}
            .onChange(of:camera.selectedID) {_,_ in controls=ManualControls();flash = .off}
    }
    private var header:some View {HStack {HStack(spacing:9) {Image(systemName:"camera.aperture").font(.title3).foregroundStyle(FilmTheme.accent);Text("MobileFilmLab").font(.system(size:18,weight:.semibold,design:.rounded))};Spacer();Button {settings=true} label:{Image(systemName:"gearshape").font(.title3).frame(width:42,height:42)}.accessibilityLabel("Settings").accessibilityIdentifier("openSettings")}.foregroundStyle(.white)}
    private var preview:some View {
        GeometryReader {geo in
            ZStack {
                RoundedRectangle(cornerRadius:22).fill(FilmTheme.panel)
                if let image=camera.preview {
                    Image(uiImage:image).resizable().scaledToFit()
                        .overlay {if store.preferences.grid != "Off" {GridOverlay(mode:store.preferences.grid).stroke(.white.opacity(0.35),lineWidth:0.6)}}
                        .overlay {if store.preferences.level && camera.motionAvailable {Rectangle().fill(abs(camera.roll)<1.5 ? FilmTheme.accent : .white.opacity(0.65)).frame(width:72,height:1.5).rotationEffect(.degrees(-camera.roll))}}
                        .overlay {if let p=camera.focusPoint {Rectangle().stroke(FilmTheme.accent,lineWidth:1.5).frame(width:54,height:54).position(x:geo.size.width*p.x,y:geo.size.height*p.y)}}
                        .contentShape(Rectangle()).onTapGesture {point in camera.focus(at:CGPoint(x:clamp(point.x/geo.size.width,0,1),y:clamp(point.y/geo.size.height,0,1)))}
                } else {
                    VStack(spacing:16) {Image(systemName:"camera").font(.system(size:36,weight:.ultraLight));Text(camera.status).font(.subheadline).multilineTextAlignment(.center);Button("Enable / reconnect camera") {camera.start()};Button("Open iOS Settings") {if let url=URL(string:UIApplication.openSettingsURLString) {UIApplication.shared.open(url)}}}.padding(26).foregroundStyle(FilmTheme.muted)
                }
                VStack(spacing:10) {
                    HStack(alignment:.top) {
                        VStack(alignment:.leading,spacing:4) {Text(store.preferences.format.writesRaw ? (store.preferences.format.usesProRAW ? "ProRAW · computational DNG" : "Bayer RAW · single exposure") : store.preferences.pipeline.label).font(.system(size:10,weight:.medium));if store.preferences.format.writesRaw {Text("Look applies to JPEG / preview only").font(.system(size:9))}}
                        Spacer();HistogramView(bins:camera.histogram).frame(width:70,height:28)
                    }.padding(12).background(LinearGradient(colors:[.black.opacity(0.6),.clear],startPoint:.top,endPoint:.bottom))
                    HStack {Spacer();Text("Hold Original").font(.system(size:10,weight:.semibold)).padding(9).background(.black.opacity(0.45),in:Capsule()).onLongPressGesture(minimumDuration:0.01,pressing:{camera.compareOriginal($0)},perform:{}).accessibilityLabel("Hold to compare ungraded camera preview")}.padding(.trailing,12)
                    Spacer()
                    if store.countdown>0 {Text("\(store.countdown)").font(.system(size:64,weight:.light)).shadow(radius:10);Spacer()}
                    if store.preferences.crop>1 {Text("2× JPEG CENTRE CROP · NOT A LENS").font(.system(size:9,weight:.semibold,design:.monospaced)).padding(7).background(.black.opacity(0.5),in:Capsule())}
                    lensControls
                    HStack {Text(camera.liveISO>0 ? "ISO \(Int(camera.liveISO))" : "ISO AUTO");Spacer();Text(camera.liveTime>0 ? exposureLabel(camera.liveTime) : "AE");Spacer();Text(camera.capabilities.name)}.font(.system(size:9,design:.monospaced)).padding(.horizontal,15).padding(.bottom,12)
                }.foregroundStyle(.white)
            }.clipShape(RoundedRectangle(cornerRadius:22))
        }.aspectRatio(3/4,contentMode:.fit)
    }
    private var lensControls:some View {
        ScrollView(.horizontal,showsIndicators:false) {
            HStack(spacing:8) {
                ForEach(camera.routes) {r in
                    Button {camera.select(r.id)} label:{
                        Text(r.name).font(.caption2.weight(.semibold)).padding(.horizontal,12).padding(.vertical,10)
                            .background(camera.selectedID==r.id ? FilmTheme.accent : Color.black.opacity(0.5),in:Capsule())
                            .foregroundStyle(camera.selectedID==r.id ? Color.black : Color.white)
                    }.disabled(store.processing || camera.busy)
                }
                if !store.preferences.format.writesRaw {
                    Button {store.preferences.crop=store.preferences.crop==1 ? 2 : 1} label:{
                        Text("2× crop").font(.caption2.bold()).padding(.horizontal,12).padding(.vertical,10)
                            .background(store.preferences.crop>1 ? FilmTheme.accent : Color.black.opacity(0.5),in:Capsule())
                            .foregroundStyle(store.preferences.crop>1 ? Color.black : Color.white)
                    }
                }
            }.padding(.horizontal,12)
        }
    }
    private var manualPanel:some View {
        VStack(spacing:7) {
            if controls.isManual {Text("Manual exposure locks ISO + shutter; MTR uses the last metered value.").font(.system(size:9)).foregroundStyle(.secondary)}
            if camera.capabilities.customExposure {
                ProSlider(label:"ISO",value:Binding(get:{Double(controls.iso ?? camera.liveISO)},set:{controls.iso=Float($0)}),range:Double(camera.capabilities.minISO)...Double(max(camera.capabilities.minISO+1,camera.capabilities.maxISO)),auto:!controls.isManual,display:controls.iso==nil && controls.isManual ? "MTR \(Int(camera.liveISO))" : nil,reset:{controls.iso=nil})
                ProSlider(label:"Shutter",value:Binding(get:{log2(max(camera.capabilities.minTime,controls.seconds ?? camera.liveTime))},set:{controls.seconds=pow(2,$0)}),range:log2(max(0.000001,camera.capabilities.minTime))...log2(max(0.000002,camera.capabilities.maxTime)),auto:!controls.isManual,display:(controls.seconds==nil && controls.isManual ? "MTR " : "")+exposureLabel(controls.seconds ?? camera.liveTime),reset:{controls.seconds=nil})
            }
            ProSlider(label:"EV",value:Binding(get:{Double(controls.ev)},set:{controls.ev=Float($0)}),range:Double(camera.capabilities.minEV)...Double(camera.capabilities.maxEV),auto:false,display:String(format:"%+0.1f",controls.ev),reset:{controls.ev=0}).disabled(controls.isManual)
            if camera.capabilities.manualWB {ProSlider(label:"WB",value:Binding(get:{Double(controls.kelvin ?? 5600)},set:{controls.kelvin=Float($0)}),range:2500...9900,auto:controls.kelvin==nil,display:"\(Int(controls.kelvin ?? 5600))K",reset:{controls.kelvin=nil})}
            HStack {Picker("Focus",selection:$controls.focus) {Text("AF-C").tag("AF-C");Text("AF-S").tag("AF-S");if camera.capabilities.manualFocus {Text("MF").tag("MF")}}.pickerStyle(.segmented)
                if controls.focus=="MF" {Slider(value:$controls.focusPosition,in:0...1)}
            }
        }.tint(FilmTheme.accent).padding(10).background(FilmTheme.panel,in:RoundedRectangle(cornerRadius:12))
    }
}
struct ProSlider:View {
    var label:String;@Binding var value:Double;var range:ClosedRange<Double>;var auto:Bool;var display:String?=nil;var reset:()->Void
    var body:some View {HStack(spacing:8) {Text(label).frame(width:43,alignment:.leading);Slider(value:$value,in:range);Text(auto ? "AUTO" : (display ?? String(Int(value)))).monospacedDigit().frame(width:67,alignment:.trailing);Button(action:reset) {Image(systemName:"arrow.uturn.backward")}.accessibilityLabel("Reset \(label) to auto")}.font(.system(size:10)).foregroundStyle(.white)}
}
func exposureLabel(_ t:Double)->String {guard t>0 else {return "AUTO"};return t<0.5 ? "1/\(Int((1/t).rounded()))s" : String(format:"%0.2fs",t)}
struct GridOverlay:Shape {
    var mode:String
    func path(in r:CGRect)->Path {var p=Path();let values:[CGFloat]=mode=="Golden" ? [0.382,0.618] : mode=="Centre" ? [0.5] : [1/3,2/3];for v in values {p.move(to:CGPoint(x:r.width*v,y:0));p.addLine(to:CGPoint(x:r.width*v,y:r.height));p.move(to:CGPoint(x:0,y:r.height*v));p.addLine(to:CGPoint(x:r.width,y:r.height*v))};return p}
}
struct HistogramView:View {var bins:[Double];var body:some View {GeometryReader {g in HStack(alignment:.bottom,spacing:1) {ForEach(bins.indices,id:\.self) {i in Rectangle().fill(.white.opacity(0.65)).frame(width:max(1,(g.size.width-31)/32),height:max(1,g.size.height*bins[i]))}}}.accessibilityLabel("Live ungraded luminance histogram")}}
