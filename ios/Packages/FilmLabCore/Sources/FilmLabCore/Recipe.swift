import Foundation

public struct Recipe: Codable, Identifiable, Equatable, Sendable {
    public var id: String; public var name: String; public var group: String
    public var style=1, sat=0, con=0, sharp=0, wbMode=1, kelvin=0, ab=0, gm=0, pe=0, ev=0, dro=6, sub=0
    public var puckTone=0.0, puckColor=0.0
    public var isFavourite=false, isCustom=false, version=1
    public var notes=""
    public init(id: String,name: String,group: String="CUSTOM") {self.id=id;self.name=name;self.group=group}
    public var isMonochrome: Bool {style==6 || pe==7 || pe==10}
    public var displayName: String {
        switch id {case "stock_oppo_natural":return "iPhone Natural (AVFoundation)";case "stock_oppo_vivid":return "Vivid Colour";case "stock_oppo_bw":return "Soft Monochrome";default:return name}
    }
    public var summary: String {"S \(sat) · C \(con) · Detail \(sharp) · \(wbMode==14 ? "\(kelvin)K" : "Auto WB") · EV \(String(format:"%+0.1f",Double(ev)/3))"}
    public func validate() throws {
        guard !id.trimmingCharacters(in:.whitespacesAndNewlines).isEmpty,id.utf16.count<=100,!name.trimmingCharacters(in:.whitespacesAndNewlines).isEmpty,name.utf16.count<=80,!name.utf16.contains(where: {$0<32}),Catalogue.groupIDs.contains(group),[1,2,3,4,5,6,7,8,9,10,11,12,14].contains(style),(-3...3).contains(sat),(-3...3).contains(con),(-3...3).contains(sharp),[0,1,14].contains(wbMode),wbMode != 14 || (2500...9900).contains(kelvin),(-7...7).contains(ab),(-7...7).contains(gm),(-15...15).contains(ev),(0...13).contains(pe),(0...6).contains(dro),(0...20).contains(sub),puckTone.isFinite,puckColor.isFinite,(-100...100).contains(puckTone),(-100...100).contains(puckColor),version==1,notes.utf16.count<=4000 else {throw FilmError.invalid("Invalid recipe fields or limits")}
    }
}
public enum FilmError: Error, LocalizedError {
    case invalid(String)
    public var errorDescription: String? {switch self {case .invalid(let message):return message}}
}
public struct RecipeGroup: Codable, Identifiable, Sendable {public var id: String;public var name: String;public var order: Int;public var displayName: String {id=="OPPO" ? "Natural baselines" : name}}
public struct Catalogue: Codable, Sendable {
    public var type: String;public var version: Int;public var upstreamCount: Int;public var baselineCount: Int;public var groups: [RecipeGroup];public var recipes: [Recipe]
    public static let groupIDs=Set(["OPPO","SONY","FUJI_SIM","FUJI_FILM","KODAK","CINE","RICOH_GR","LEICA","HASSELBLAD","CANON_NIKON","PANA_OLYMPUS","OTHER_STOCKS","ILFORD","CUSTOM"])
    public static func load() throws -> Catalogue {
        guard let url=Bundle.module.url(forResource:"catalogue",withExtension:"json") else {throw FilmError.invalid("Missing full recipe catalogue")}
        let value=try JSONDecoder().decode(Catalogue.self,from:Data(contentsOf:url))
        guard value.type=="mobilefilmlab.catalogue",value.recipes.count==80,Set(value.recipes.map(\.id)).count==80,value.upstreamCount==77 else {throw FilmError.invalid("Catalogue is incomplete")}
        try value.recipes.forEach {try $0.validate()};return value
    }
}
public enum CaptureFormat: String, Codable, CaseIterable, Identifiable, Sendable {
    case jpeg,raw,rawJPEG,proRAW,proRAWJPEG
    public var id: String {rawValue}
    public var label: String {switch self {case .jpeg:return "JPEG";case .raw:return "RAW (Bayer DNG)";case .rawJPEG:return "RAW + JPEG";case .proRAW:return "Apple ProRAW (DNG)";case .proRAWJPEG:return "Apple ProRAW + JPEG"}}
    public var writesRaw: Bool {self != .jpeg}
    public var writesJPEG: Bool {self == .jpeg || self == .rawJPEG || self == .proRAWJPEG}
    public var usesProRAW: Bool {self == .proRAW || self == .proRAWJPEG}
}
public enum CapturePipeline: String,Codable,CaseIterable,Identifiable,Sendable {
    case original,smartHDR,qualityHDR,legacyFusion
    public var id: String {rawValue}
    public var label: String {switch self {case .original:return "Apple quality · Original";case .smartHDR:return "Smart HDR · handheld";case .qualityHDR:return "Quality HDR · still scenes";case .legacyFusion:return "3-frame fusion · experimental"}}
}
public struct CapturePreferences: Codable, Equatable, Sendable {
    public var format: CaptureFormat = .jpeg
    public var pipeline: CapturePipeline = .original
    public var grain=0
    public var balanced=false,maximum=false,saveOriginal=false,responsive=false,compare=false,haptics=true,enlarge28=false,saveToPhotos=true
    public var grid="Thirds"
    public var level=true
    public var crop=1.0
    public var red=1.0,green=1.0,blue=1.0
    public init() {}
    public mutating func normalize() {
        grain=min(3,max(0,grain));crop=min(3,max(1,crop))
        red=min(1.2,max(0.8,red));green=min(1.2,max(0.8,green));blue=min(1.2,max(0.8,blue))
        if format.writesRaw {pipeline = .original;crop=1;responsive=false}
        if pipeline != .original {responsive=false}
        if !format.writesJPEG {compare=false}
    }
}
