import Foundation

public enum CameraFacing: String, Sendable { case back, front, other }
public enum CameraLensKind: String, Sendable { case ultraWide, wide, telephoto, trueDepth, other }

/// Presentation-only snapshot. No camera identifiers or ratios are invented by the UI.
public struct CameraLensMetadata: Sendable {
    public var id: String
    public var facing: CameraFacing
    public var kind: CameraLensKind
    public var virtual: Bool
    public var constituents: [String]
    public var switchFactors: [Double]
    public init(id: String, facing: CameraFacing, kind: CameraLensKind, virtual: Bool = false, constituents: [String] = [], switchFactors: [Double] = []) {
        self.id = id; self.facing = facing; self.kind = kind; self.virtual = virtual
        self.constituents = constituents; self.switchFactors = switchFactors
    }
}
public struct PresentedCameraLens: Identifiable, Equatable, Sendable {
    public var id: String
    public var name: String
    public var shortName: String
    public var facing: CameraFacing
    public var kind: CameraLensKind
    public var virtual: Bool
    public var relativeZoom: Double?
}

public enum LensPresentation {
    /// Apple documents [1] + crossover factors in constituentDevices order.
    /// Normalize to the actual Wide constituent, not a hard-coded 0.5x or telephoto value.
    /// Invalid/incomplete metadata deliberately falls back to plain lens names.
    public static func lenses(_ metadata: [CameraLensMetadata]) -> [PresentedCameraLens] {
        var seen = Set<String>()
        let devices = metadata.filter { !$0.id.isEmpty && seen.insert($0.id).inserted }
        let byID = Dictionary(uniqueKeysWithValues: devices.map { ($0.id, $0) })
        var ratios: [String: Double] = [:]
        let groups = devices.filter { $0.virtual && $0.facing == .back }.sorted {
            if $0.constituents.count != $1.constituents.count { return $0.constituents.count > $1.constituents.count }
            return $0.id < $1.id
        }
        for group in groups {
            let factors = [1.0] + group.switchFactors
            guard group.constituents.count > 1, factors.count == group.constituents.count,
                  Set(group.constituents).count == group.constituents.count,
                  factors.allSatisfy({ $0.isFinite && $0 > 0 }),
                  zip(factors, factors.dropFirst()).allSatisfy({ $0.0 < $0.1 }),
                  let wide = group.constituents.firstIndex(where: {
                      byID[$0]?.kind == .wide && byID[$0]?.virtual == false && byID[$0]?.facing == .back
                  }) else { continue }
            let baseline = factors[wide]
            for (id, factor) in zip(group.constituents, factors) {
                guard let device = byID[id], !device.virtual, device.facing == .back else { continue }
                let ratio = factor / baseline
                if ratio.isFinite, (0.05...40).contains(ratio), ratios[id] == nil { ratios[id] = ratio }
            }
        }
        var result = devices.map { device -> PresentedCameraLens in
            let name: String
            let fallback: String
            if device.facing == .front { name = device.kind == .trueDepth ? "Front depth camera" : "Front camera"; fallback = "Front" }
            else if device.virtual {
                name = device.constituents.count > 1 ? "Automatic · \(device.constituents.count) lenses" : "Automatic camera"
                fallback = "Auto"
            }
            else {
                switch device.kind {
                case .ultraWide: name = "Ultra Wide"; fallback = "Ultra"
                case .wide: name = "Main"; fallback = "Main"
                case .telephoto: name = "Telephoto"; fallback = "Tele"
                case .trueDepth: name = "Front camera"; fallback = "Front"
                case .other: name = "Camera"; fallback = "Lens"
                }
            }
            let ratio = !device.virtual && device.facing == .back ? (ratios[device.id] ?? (device.kind == .wide ? 1.0 : nil)) : nil
            return PresentedCameraLens(id: device.id, name: name, shortName: ratio.map(zoomLabel) ?? fallback,
                facing: device.facing, kind: device.kind, virtual: device.virtual, relativeZoom: ratio)
        }
        // Never silently collapse different physical devices just because they share a type or ratio.
        let collisions = Dictionary(grouping: result.indices.filter { !result[$0].virtual }, by: {
            result[$0].facing.rawValue + ":" + result[$0].shortName
        })
        for indices in collisions.values where indices.count > 1 {
            let ordered = indices.sorted { result[$0].id < result[$1].id }
            for (offset, index) in ordered.enumerated() {
                let number = offset + 1
                result[index].name += " \(number)"
                let base: String
                switch result[index].kind { case .ultraWide: base = "Ultra"; case .wide: base = "Main"; case .telephoto: base = "Tele"; case .trueDepth: base = "Front"; case .other: base = "Lens" }
                result[index].shortName = "\(base) \(number)"
            }
        }
        return result.sorted {
            let a = facingOrder($0.facing), b = facingOrder($1.facing)
            if a != b { return a < b }
            if $0.virtual != $1.virtual { return !$0.virtual }
            let ak = kindOrder($0.kind), bk = kindOrder($1.kind)
            if ak != bk { return ak < bk }
            if let az = $0.relativeZoom, let bz = $1.relativeZoom, az != bz { return az < bz }
            return $0.id < $1.id
        }
    }
    public static func primaryRear(_ lenses: [PresentedCameraLens]) -> [PresentedCameraLens] {
        let physical = lenses.filter { $0.facing == .back && !$0.virtual }
        return physical.isEmpty ? Array(lenses.filter { $0.facing == .back }.prefix(1)) : physical
    }
    public static func preferredRearID(_ lenses: [PresentedCameraLens]) -> String? {
        lenses.first { $0.facing == .back && !$0.virtual && $0.kind == .wide }?.id ?? primaryRear(lenses).first?.id
    }
    public static func preferredFrontID(_ lenses: [PresentedCameraLens]) -> String? {
        lenses.first { $0.facing == .front && $0.kind == .trueDepth }?.id ?? lenses.first { $0.facing == .front }?.id
    }
    public static func zoomLabel(_ ratio: Double) -> String {
        guard ratio.isFinite, ratio > 0 else { return "Lens" }
        var text = String(format: "%.1f", locale: Locale(identifier: "en_US_POSIX"), ratio)
        if text.hasSuffix(".0") { text.removeLast(2) }
        return text + "×"
    }
    private static func facingOrder(_ facing: CameraFacing) -> Int { switch facing { case .back: return 0; case .front: return 1; case .other: return 2 } }
    private static func kindOrder(_ kind: CameraLensKind) -> Int { switch kind { case .ultraWide: return 0; case .wide: return 1; case .telephoto: return 2; case .trueDepth: return 3; case .other: return 4 } }
}
