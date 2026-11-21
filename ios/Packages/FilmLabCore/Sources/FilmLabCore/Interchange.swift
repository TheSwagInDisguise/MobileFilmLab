import Foundation
import CZlib

public enum Interchange {
    public static let limit=262144
    public static let prefix="FILMRECIPE:1:"
    private struct Envelope: Codable {var type="mobilefilmlab.recipe";var version=1;var recipes:[Recipe]}
    public static func encode(_ recipes:[Recipe]) throws -> Data {
        guard (1...100).contains(recipes.count) else {throw FilmError.invalid("Export 1–100 recipes")}
        try recipes.forEach {try $0.validate()}
        let encoder=JSONEncoder();encoder.outputFormatting=[.prettyPrinted,.sortedKeys,.withoutEscapingSlashes]
        let data=try encoder.encode(Envelope(recipes:recipes));guard data.count<=limit else {throw FilmError.invalid("Recipe document exceeds 256KB")};return data
    }
    public static func decode(_ data:Data) throws -> [Recipe] {
        guard !data.isEmpty,data.count<=limit else {throw FilmError.invalid("Recipe document exceeds 256KB or is empty")}
        let decoder=JSONDecoder();let recipes:[Recipe]
        if let array=try? decoder.decode([Recipe].self,from:data) {recipes=array}
        else {
            let envelope=try decoder.decode(Envelope.self,from:data)
            guard envelope.type=="mobilefilmlab.recipe",envelope.version==1 else {throw FilmError.invalid("Unsupported recipe format/version")};recipes=envelope.recipes
        }
        guard (1...100).contains(recipes.count) else {throw FilmError.invalid("Import 1–100 recipes")}
        try recipes.forEach {try $0.validate()};return recipes
    }
    public static func qrPayload(_ recipe:Recipe) throws -> String {
        let input=try encode([recipe]);var size=compressBound(uLong(input.count));var bytes=[UInt8](repeating:0,count:Int(size))
        let status=input.withUnsafeBytes {src in bytes.withUnsafeMutableBufferPointer {dst in compress2(dst.baseAddress,&size,src.bindMemory(to:Bytef.self).baseAddress,uLong(input.count),Z_DEFAULT_COMPRESSION)}}
        guard status==Z_OK else {throw FilmError.invalid("QR compression failed")}
        let result=prefix+Data(bytes.prefix(Int(size))).base64EncodedString().replacingOccurrences(of:"+",with:"-").replacingOccurrences(of:"/",with:"_").replacingOccurrences(of:"=",with:"")
        guard result.count<=2200 else {throw FilmError.invalid("Recipe is too large for QR; export a recipe file")};return result
    }
    public static func decodeQR(_ text:String) throws -> [Recipe] {
        guard text.hasPrefix(prefix),text.count<=2200 else {throw FilmError.invalid("Not a supported recipe QR")}
        var base=String(text.dropFirst(prefix.count)).replacingOccurrences(of:"-",with:"+").replacingOccurrences(of:"_",with:"/")
        base+=String(repeating:"=",count:(4-base.count%4)%4)
        guard let input=Data(base64Encoded:base) else {throw FilmError.invalid("Invalid QR encoding")}
        var size=uLongf(limit);var output=[UInt8](repeating:0,count:limit)
        let status=input.withUnsafeBytes {src in output.withUnsafeMutableBufferPointer {dst in uncompress(dst.baseAddress,&size,src.bindMemory(to:Bytef.self).baseAddress,uLong(input.count))}}
        guard status==Z_OK else {throw FilmError.invalid("QR decompression failed or exceeds 256KB")}
        return try decode(Data(output.prefix(Int(size))))
    }
}
