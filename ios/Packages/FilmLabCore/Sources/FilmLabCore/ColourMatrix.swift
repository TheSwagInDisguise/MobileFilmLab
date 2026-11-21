import Foundation

/// Android-compatible affine sRGB matrix. Offsets use 0...255 units until the Core Image boundary.
public struct ColourMatrix: Equatable, Sendable {
    public var values:[Double]
    public init(_ values:[Double]=[1,0,0,0,0,0,1,0,0,0,0,0,1,0,0,0,0,0,1,0]) {precondition(values.count==20);self.values=values}
    public mutating func then(_ b:[Double]) {
        let a=values
        for y in 0..<4 {for x in 0..<5 {values[y*5+x]=(0..<4).reduce(0.0) {$0+b[y*5+$1]*a[$1*5+x]}+(x==4 ? b[y*5+4] : 0)}}
    }
    public var isIdentity: Bool {zip(values,ColourMatrix().values).allSatisfy {pair in abs(pair.0-pair.1)<0.000001}}
    public static func recipe(_ r:Recipe,red:Double=1,green:Double=1,blue:Double=1) -> ColourMatrix {
        var m=ColourMatrix()
        if r.isMonochrome {
            let w:[Double]
            switch r.name {case "Acros +R (red filter)","GR Hi-Contrast B&W":w=[0.70,0.20,0.10];case "Acros +Ye (yellow filter)":w=[0.50,0.40,0.10];case "Acros +G (green filter)":w=[0.25,0.65,0.10];default:w=[0.213,0.715,0.072]}
            m.then(w+[0,0]+w+[0,0]+w+[0,0]+[0,0,0,1,0])
        } else {
            let styleValues:[Int:Double]=[2:1.16,3:0.94,4:0.96,5:1.1,8:1.08]
            let style:Double=styleValues[r.style] ?? 1.0
            let base:Double=style+Double(r.sat)*0.12
            let adjustment:Double=1.0+r.puckColor*0.003
            let s:Double=clamp(base*adjustment,0.1,2.0)
            let R=0.213*(1-s),G=0.715*(1-s),B=0.072*(1-s)
            m.then([R+s,G,B,0,0,R,G+s,B,0,0,R,G,B+s,0,0,0,0,0,1,0])
        }
        let contrastValues:[Int:Double]=[3:0.96,9:0.92,8:1.1]
        let conStyle:Double=contrastValues[r.style] ?? 1.0
        let baseContrast:Double=conStyle+Double(r.con)*0.10
        let puckContrast:Double=1.0+r.puckTone*0.003
        let c:Double=clamp(baseContrast*puckContrast,0.5,1.8)
        let o:Double=(1.0-c)*128.0
        m.then([c,0,0,0,o,0,c,0,0,o,0,0,c,0,o,0,0,0,1,0])
        var R=1.0,G=1.0,B=1.0
        if r.wbMode==14,r.kelvin>0 {let d=Double(r.kelvin-5600)/1000;if d<0 {R += -d*0.08;B -= -d*0.08} else {B += d*0.07;R -= d*0.07}}
        let ab=Double(r.ab)+r.puckColor*0.05;R+=ab*0.025;B-=ab*0.025
        G+=Double(r.gm)*0.025;R-=Double(r.gm)*0.012;B-=Double(r.gm)*0.012
        m.then([clamp(R,0.4,1.8),0,0,0,0,0,clamp(G,0.4,1.8),0,0,0,0,0,clamp(B,0.4,1.8),0,0,0,0,0,1,0])
        if r.ev != 0 {let e=Double(r.ev)/3*16;m.then([1,0,0,0,e,0,1,0,0,e,0,0,1,0,e,0,0,0,1,0])}
        if r.style==14 {m.then([0.393,0.769,0.189,0,0,0.349,0.686,0.168,0,0,0.272,0.534,0.131,0,0,0,0,0,1,0])}
        if r.pe==4 {m.then([0.95,0,0,0,12,0,0.91,0,0,10,0,0,0.82,0,8,0,0,0,1,0])}
        else if r.pe==5 {m.then([0.82,0,0,0,38,0,0.84,0,0,38,0,0,0.88,0,40,0,0,0,1,0])}
        m.then([red,0,0,0,0,0,green,0,0,0,0,0,blue,0,0,0,0,0,1,0]);return m
    }
}
public func clamp<T:Comparable>(_ value:T,_ minimum:T,_ maximum:T)->T {min(maximum,max(minimum,value))}
