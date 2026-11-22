import Foundation
import AVFoundation
import CoreImage
import FilmLabCore

struct CameraRoute: Identifiable, Equatable {
    var lens: PresentedCameraLens
    var detail: String
    var constituentCount: Int = 0
    var id: String { lens.id }
    var name: String { lens.name }
    var shortName: String { lens.shortName }
    var virtual: Bool { lens.virtual }
    var facing: CameraFacing { lens.facing }
    var kind: CameraLensKind { lens.kind }
}
struct CameraCapabilities {
    var cameraID="",name="No camera",bayer:UInt32?,proRAW:UInt32?
    var minISO:Float=25,maxISO:Float=3200,minTime:Double=0.0001,maxTime:Double=1
    var minEV:Float = -3,maxEV:Float=3,maxWB:Float=4
    var customExposure=false,manualWB=false,focus=false,manualFocus=false,flash=false,torch=false,responsive=false
    var flashChoices:[FlashChoice]=[.off]
    var focusModes:[String]=[]
    var dimensions:[CMVideoDimensions]=[]
    func supports(_ f:CaptureFormat)->Bool {f == .jpeg || (f.usesProRAW ? proRAW != nil : bayer != nil)}
}
struct ManualControls:Equatable {
    var iso:Float?,seconds:Double?,kelvin:Float?
    var ev:Float=0
    var focusPosition:Float=0.5
    var focus="AF-C"
    var isManual:Bool {iso != nil || seconds != nil}
}
enum FlashChoice:String,CaseIterable,Identifiable {case off,auto,on,torch;var id:String {rawValue};var next:FlashChoice {Self.allCases[(Self.allCases.firstIndex(of:self)!+1)%4]}}
struct PhotoFrame {
    var jpeg:Data?
    var raw:Data?
    var metadata:[String:Any]
    var iso:Double?
    var seconds:Double?
    var focal:Double?
    var timestamp:Date
    var requestID:Int64
    var warning:String?
    init(jpeg:Data?,raw:Data?,metadata:[String:Any],iso:Double?,seconds:Double?,focal:Double?,timestamp:Date,requestID:Int64,warning:String?=nil) {
        self.jpeg=jpeg;self.raw=raw;self.metadata=metadata;self.iso=iso;self.seconds=seconds;self.focal=focal;self.timestamp=timestamp;self.requestID=requestID;self.warning=warning
    }
    var exposure:Double? {guard let i=iso,let t=seconds,i>0,t>0 else {return nil};return i*t}
}
struct PhotoBurst {
    var frames:[PhotoFrame]
    var shorts:[Int]=[],longs:[Int]=[]
    var pipeline:CapturePipeline
    var note:String="",fallback:String?
}
struct SavedCapture:Codable,Identifiable {
    var id:String;var date:Date;var title:String;var jpeg:String?;var raw:String?;var note:String;var width:Int;var height:Int
}
