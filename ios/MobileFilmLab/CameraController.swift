import AVFoundation
import Combine
import CoreImage
import CoreMotion
import UIKit
import ImageIO
import FilmLabCore

/// Capture state is owned by `queue`. UI state is published on main; no camera work blocks main.
final class CameraController:NSObject,ObservableObject,AVCaptureVideoDataOutputSampleBufferDelegate {
    @Published var routes:[CameraRoute]=[]
    @Published var selectedID=""
    @Published var capabilities=CameraCapabilities()
    @Published var preview:UIImage?
    @Published var lookSource:UIImage?
    @Published var histogram:[Double]=Array(repeating:0,count:32)
    @Published var status="Camera permission required"
    @Published var ready=false
    @Published var busy=false
    @Published var liveISO:Float=0
    @Published var liveTime:Double=0
    @Published var roll:Double=0
    @Published var motionAvailable=false
    @Published var focusPoint:CGPoint?
    var previewObserver:((CIImage)->Void)?
    private let session=AVCaptureSession(),output=AVCapturePhotoOutput(),video=AVCaptureVideoDataOutput()
    private let queue=DispatchQueue(label:"com.mobilefilmlab.camera.session",qos:.userInitiated)
    private let previewQueue=DispatchQueue(label:"com.mobilefilmlab.camera.preview",qos:.userInitiated)
    private let previewContext=CIContext(options:[.workingColorSpace:NSNull(), .cacheIntermediates:false])
    private let motion=CMMotionManager(),motionQueue=OperationQueue()
    private let lock=NSLock()
    private var previewDevice:AVCaptureDevice?
    private var previewRecipe=Recipe(id:"stock_oppo_natural",name:"Natural",group:"OPPO"),previewPrefs=CapturePreferences()
    private var showPreviewOriginal=false
    private var scene:SceneStats?,lastLuma:[UInt8]?,rotation:Double?
    private var devices:[String:AVCaptureDevice]=[:],input:AVCaptureDeviceInput?
    private var namedRoutes: [String: CameraRoute] = [:]
    private var defaultRearID: String?
    private var defaultFrontID: String?
    private var rememberedRearID: String?
    private var rememberedFrontID: String?
    var selectedLens: CameraRoute? { routes.first { $0.id == selectedID } }
    var canFlipCamera: Bool { routes.contains { $0.facing == .back } && routes.contains { $0.facing == .front } }
    var visibleLenses: [CameraRoute] {
        if let selectedLens, selectedLens.facing == .front { return [selectedLens] }
        var visible = routes.filter { $0.facing == .back && !$0.virtual }
        if let selectedLens, selectedLens.facing == .back && selectedLens.virtual { visible.append(selectedLens) }
        if visible.isEmpty, let selectedLens { return [selectedLens] }
        return visible
    }
    var automaticRearRoute: CameraRoute? {
        routes.filter { $0.facing == .back && $0.virtual }.sorted {
            if $0.constituentCount != $1.constituentCount { return $0.constituentCount > $1.constituentCount }
            return $0.id < $1.id
        }.first
    }
    private var controls=ManualControls(),flash:FlashChoice = .off
    private var transaction:BurstTransaction?
    private var pending:[Int64:PhotoReceiver]=[:]
    private var lastPreview=0.0,frames=0
    private var tokens:[NSObjectProtocol]=[]
    private var configured=false,foreground=true
    override init() {
        super.init()
        tokens.append(NotificationCenter.default.addObserver(forName:AVCaptureSession.runtimeErrorNotification,object:session,queue:nil) {[weak self] n in
            let error=n.userInfo?[AVCaptureSessionErrorKey] as? AVError
            self?.queue.async {guard let self else {return};self.finish(error:FilmError.invalid(error?.localizedDescription ?? "Camera interrupted"));self.publish {self.ready=false;self.preview=nil;self.status="Camera error. Reopen the camera to reconnect."}}
        })
        tokens.append(NotificationCenter.default.addObserver(forName:AVCaptureSession.wasInterruptedNotification,object:session,queue:nil) {[weak self] _ in self?.queue.async {guard let self else {return};self.finish(error:FilmError.invalid("Capture interrupted; no incomplete image was saved"));self.publish {self.ready=false;self.status="Camera interrupted by iOS"}}})
        tokens.append(NotificationCenter.default.addObserver(forName:AVCaptureSession.interruptionEndedNotification,object:session,queue:nil) {[weak self] _ in self?.start()})
    }
    deinit {tokens.forEach {NotificationCenter.default.removeObserver($0)};motion.stopDeviceMotionUpdates()}
    private func publish(_ change:@escaping ()->Void) {DispatchQueue.main.async(execute:change)}
    func start() {
        #if DEBUG && targetEnvironment(simulator)
        if isShowingLensFixture { installLensFixture(); return }
        #endif
        switch AVCaptureDevice.authorizationStatus(for:.video) {
        case .authorized:queue.async {[weak self] in self?.startAuthorized()}
        case .notDetermined:AVCaptureDevice.requestAccess(for:.video) {[weak self] allowed in if allowed {self?.start()} else {self?.publish {self?.status="Allow Camera access in iOS Settings"}}}
        default:publish {self.ready=false;self.status="Camera access is denied. Open iOS Settings to allow it."}
        }
    }
    func setForeground(_ active:Bool) {
        #if DEBUG && targetEnvironment(simulator)
        if isShowingLensFixture { return }
        #endif
        queue.async {self.foreground=active
            if active {self.startAuthorized()} else {self.finish(error:FilmError.invalid("Capture cancelled when app left the foreground"));if self.session.isRunning {self.session.stopRunning()};self.motion.stopDeviceMotionUpdates();self.publish {self.ready=false}}
        }
    }
    private func startAuthorized() {
        guard foreground,AVCaptureDevice.authorizationStatus(for:.video) == .authorized else {return}
        do {
            if !configured {
                let discovery=AVCaptureDevice.DiscoverySession(deviceTypes:[.builtInWideAngleCamera,.builtInUltraWideCamera,.builtInTelephotoCamera,.builtInDualCamera,.builtInDualWideCamera,.builtInTripleCamera,.builtInTrueDepthCamera],mediaType:.video,position:.unspecified)
                devices=Dictionary(uniqueKeysWithValues:discovery.devices.map {($0.uniqueID,$0)})
                let metadata = discovery.devices.map { device -> CameraLensMetadata in
                    let facing: CameraFacing = device.position == .back ? .back : device.position == .front ? .front : .other
                    let kind: CameraLensKind
                    switch device.deviceType {
                    case .builtInUltraWideCamera: kind = .ultraWide
                    case .builtInWideAngleCamera: kind = .wide
                    case .builtInTelephotoCamera: kind = .telephoto
                    case .builtInTrueDepthCamera: kind = .trueDepth
                    default: kind = .other
                    }
                    return CameraLensMetadata(id: device.uniqueID, facing: facing, kind: kind, virtual: device.isVirtualDevice,
                        constituents: device.constituentDevices.map(\.uniqueID), switchFactors: device.virtualDeviceSwitchOverVideoZoomFactors.map(\.doubleValue))
                }
                let labels = LensPresentation.lenses(metadata)
                let routes = labels.compactMap { label -> CameraRoute? in
                    guard let device = devices[label.id] else { return nil }
                    return CameraRoute(lens: label, detail: device.localizedName, constituentCount: device.constituentDevices.count)
                }
                namedRoutes = Dictionary(uniqueKeysWithValues: routes.map { ($0.id, $0) })
                defaultRearID = LensPresentation.preferredRearID(labels)
                defaultFrontID = LensPresentation.preferredFrontID(labels)
                publish { self.routes = routes }
                guard let id = defaultRearID ?? labels.first?.id, let initial = devices[id] else { throw FilmError.invalid("No camera is available") }
                session.beginConfiguration();session.sessionPreset = .photo
                session.automaticallyConfiguresCaptureDeviceForWideColor=false
                guard session.canAddOutput(output),session.canAddOutput(video) else {session.commitConfiguration();throw FilmError.invalid("Photo output unavailable")}
                session.addOutput(output);session.addOutput(video)
                video.videoSettings=[kCVPixelBufferPixelFormatTypeKey as String:kCVPixelFormatType_32BGRA]
                video.alwaysDiscardsLateVideoFrames=true;video.setSampleBufferDelegate(self,queue:previewQueue)
                session.commitConfiguration();configured=true
                try selectDevice(initial)
            }
            if !session.isRunning {session.startRunning()}
            startMotion();publish {self.ready=true;self.status="Ready · genuine camera feed"}
        } catch {publish {self.ready=false;self.status=error.localizedDescription}}
    }
    private func name(_ d:AVCaptureDevice)->String { namedRoutes[d.uniqueID]?.name ?? "Camera" }
    func select(_ id:String) {
        #if DEBUG && targetEnvironment(simulator)
        if isShowingLensFixture {
            publish {
                guard let route = self.routes.first(where: { $0.id == id }) else { return }
                self.selectedID = id; self.capabilities = self.fixtureCapabilities(route)
                if route.facing == .front { self.rememberedFrontID = id } else { self.rememberedRearID = id }
            }
            return
        }
        #endif
        queue.async {guard self.transaction==nil,self.pending.isEmpty,let d=self.devices[id] else {return};do {try self.selectDevice(d)} catch {self.publish {self.status=error.localizedDescription}}}
    }
    func flipCamera() {
        #if DEBUG && targetEnvironment(simulator)
        if isShowingLensFixture {
            let target = selectedLens?.facing == .front ? (rememberedRearID ?? defaultRearID) : (rememberedFrontID ?? defaultFrontID)
            if let target { select(target) }; return
        }
        #endif
        queue.async {
            guard self.transaction == nil, self.pending.isEmpty else { return }
            let next = self.input?.device.position == .front
                ? (self.rememberedRearID ?? self.defaultRearID) : (self.rememberedFrontID ?? self.defaultFrontID)
            guard let next, let device = self.devices[next] else { return }
            do { try self.selectDevice(device) } catch { self.publish { self.status = error.localizedDescription } }
        }
    }
    private func selectDevice(_ device:AVCaptureDevice) throws {
        let newInput=try AVCaptureDeviceInput(device:device)
        session.beginConfiguration()
        let old=input;if let old {session.removeInput(old)}
        guard session.canAddInput(newInput) else {if let old,session.canAddInput(old) {session.addInput(old)};session.commitConfiguration();throw FilmError.invalid("This camera route is unavailable")}
        session.addInput(newInput);input=newInput
        output.maxPhotoQualityPrioritization = .quality
        if output.isAppleProRAWSupported {output.isAppleProRAWEnabled=true}
        if output.isZeroShutterLagSupported {output.isZeroShutterLagEnabled=false}
        if output.isResponsiveCaptureSupported {output.isResponsiveCaptureEnabled=false}
        if output.isFastCapturePrioritizationSupported {output.isFastCapturePrioritizationEnabled=false}
        if let largest=device.activeFormat.supportedMaxPhotoDimensions.max(by:{Int64($0.width)*Int64($0.height)<Int64($1.width)*Int64($1.height)}) {output.maxPhotoDimensions=largest}
        if let c=video.connection(with:.video),c.isVideoRotationAngleSupported(90) {c.videoRotationAngle=90;if c.isVideoMirroringSupported {c.automaticallyAdjustsVideoMirroring=false;c.isVideoMirrored=device.position == .front}}
        session.commitConfiguration()
        try device.lockForConfiguration();device.videoZoomFactor=1
        // The shared grade is defined in SDR sRGB, not an unlabelled Display P3 interpretation.
        if device.activeFormat.supportedColorSpaces.contains(.sRGB) {device.activeColorSpace = .sRGB}
        if device.isFocusModeSupported(.continuousAutoFocus) {device.focusMode = .continuousAutoFocus}
        if device.isWhiteBalanceModeSupported(.continuousAutoWhiteBalance) {device.whiteBalanceMode = .continuousAutoWhiteBalance}
        if device.isExposureModeSupported(.continuousAutoExposure) {device.exposureMode = .continuousAutoExposure}
        device.unlockForConfiguration();controls=ManualControls();flash = .off
        let raw=output.availableRawPhotoPixelFormatTypes
        var c=CameraCapabilities();c.cameraID=device.uniqueID;c.name=name(device)
        c.bayer=raw.first(where:{AVCapturePhotoOutput.isBayerRAWPixelFormat($0)})
        c.proRAW=raw.first(where:{AVCapturePhotoOutput.isAppleProRAWPixelFormat($0)})
        c.minISO=device.activeFormat.minISO;c.maxISO=device.activeFormat.maxISO
        c.minTime=CMTimeGetSeconds(device.activeFormat.minExposureDuration);c.maxTime=CMTimeGetSeconds(device.activeFormat.maxExposureDuration)
        c.minEV=device.minExposureTargetBias;c.maxEV=device.maxExposureTargetBias;c.maxWB=device.maxWhiteBalanceGain
        c.customExposure=device.isExposureModeSupported(.custom);c.manualWB=device.isWhiteBalanceModeSupported(.locked)
        c.focus=device.isFocusPointOfInterestSupported || device.isExposurePointOfInterestSupported;c.manualFocus=device.isLockingFocusWithCustomLensPositionSupported;c.flash=device.hasFlash;c.torch=device.hasTorch
        if device.isFocusModeSupported(.continuousAutoFocus) {c.focusModes.append("AF-C")}
        if device.isFocusModeSupported(.autoFocus) {c.focusModes.append("AF-S")}
        if c.manualFocus {c.focusModes.append("MF")}
        c.flashChoices=[.off]
        if output.supportedFlashModes.contains(.auto) {c.flashChoices.append(.auto)}
        if output.supportedFlashModes.contains(.on) {c.flashChoices.append(.on)}
        if device.hasTorch {c.flashChoices.append(.torch)}
        c.responsive=output.isZeroShutterLagSupported || output.isResponsiveCaptureSupported
        c.dimensions=device.activeFormat.supportedMaxPhotoDimensions
        lock.lock();previewDevice=device;scene=nil;lastLuma=nil;lock.unlock()
        if device.position == .front { rememberedFrontID = device.uniqueID }
        else if device.position == .back { rememberedRearID = device.uniqueID }
        publish {self.selectedID=device.uniqueID;self.capabilities=c;self.status="Ready · \(c.name)"}
    }
    func updateLook(_ r:Recipe,_ p:CapturePreferences) {lock.lock();previewRecipe=r;previewPrefs=p;lock.unlock()}
    func compareOriginal(_ show:Bool) {lock.lock();showPreviewOriginal=show;lock.unlock()}
    func setControls(_ m:ManualControls) {queue.async {guard self.transaction==nil,let d=self.input?.device else {return};self.controls=m;do {try self.apply(m,to:d)} catch {self.publish {self.status=error.localizedDescription}}}}
    private func apply(_ m:ManualControls,to d:AVCaptureDevice) throws {
        try d.lockForConfiguration();defer {d.unlockForConfiguration()}
        if m.isManual,d.isExposureModeSupported(.custom) {
            let seconds=clamp(m.seconds ?? CMTimeGetSeconds(d.exposureDuration),CMTimeGetSeconds(d.activeFormat.minExposureDuration),CMTimeGetSeconds(d.activeFormat.maxExposureDuration))
            let iso=clamp(m.iso ?? d.iso,d.activeFormat.minISO,d.activeFormat.maxISO)
            d.setExposureModeCustom(duration:CMTime(seconds:seconds,preferredTimescale:1000000000),iso:iso,completionHandler:nil)
        } else if d.isExposureModeSupported(.continuousAutoExposure) {d.exposureMode = .continuousAutoExposure;d.setExposureTargetBias(clamp(m.ev,d.minExposureTargetBias,d.maxExposureTargetBias),completionHandler:nil)}
        if let k=m.kelvin,d.isWhiteBalanceModeSupported(.locked) {
            var gains=d.deviceWhiteBalanceGains(for:AVCaptureDevice.WhiteBalanceTemperatureAndTintValues(temperature:k,tint:0))
            gains.redGain=clamp(gains.redGain,1,d.maxWhiteBalanceGain);gains.greenGain=clamp(gains.greenGain,1,d.maxWhiteBalanceGain);gains.blueGain=clamp(gains.blueGain,1,d.maxWhiteBalanceGain)
            d.setWhiteBalanceModeLocked(with:gains,completionHandler:nil)
        } else if d.isWhiteBalanceModeSupported(.continuousAutoWhiteBalance) {d.whiteBalanceMode = .continuousAutoWhiteBalance}
        if m.focus=="AF-S",d.isFocusModeSupported(.autoFocus) {d.focusMode = .autoFocus}
        else if m.focus=="MF",d.isLockingFocusWithCustomLensPositionSupported {d.setFocusModeLocked(lensPosition:m.focusPosition,completionHandler:nil)}
        else if d.isFocusModeSupported(.continuousAutoFocus) {d.focusMode = .continuousAutoFocus}
    }
    func manualFocus(_ value:Float) {queue.async {guard self.transaction==nil,let d=self.input?.device,d.isLockingFocusWithCustomLensPositionSupported else {return};do {try d.lockForConfiguration();d.setFocusModeLocked(lensPosition:clamp(value,0,1),completionHandler:nil);d.unlockForConfiguration()} catch {self.publish {self.status=error.localizedDescription}}}}
    func focus(at point:CGPoint) {queue.async {guard self.transaction==nil,let d=self.input?.device,d.isFocusPointOfInterestSupported || d.isExposurePointOfInterestSupported else {return};do {try d.lockForConfiguration();defer {d.unlockForConfiguration()}
        // Preview is portrait, aspect-fit; mirrored front preview needs a mirrored POI.
        self.lock.lock();let crop=self.previewPrefs.crop;self.lock.unlock()
        let px=(point.x-0.5)/crop+0.5,py=(point.y-0.5)/crop+0.5
        let p=CGPoint(x:py,y:d.position == .front ? px : 1-px)
        if d.isFocusPointOfInterestSupported {d.focusPointOfInterest=p;if d.isFocusModeSupported(.autoFocus) {d.focusMode = .autoFocus}}
        if d.isExposurePointOfInterestSupported,!self.controls.isManual {d.exposurePointOfInterest=p}
        self.publish {self.focusPoint=point}
    } catch {self.publish {self.status=error.localizedDescription}}}}
    func setFlash(_ choice:FlashChoice) {queue.async {guard self.transaction==nil,let d=self.input?.device else {return};self.flash=choice;do {try d.lockForConfiguration();defer {d.unlockForConfiguration()};if d.hasTorch {d.torchMode=choice == .torch && d.isTorchAvailable ? .on : .off}} catch {self.publish {self.status=error.localizedDescription}}}}
    private func startMotion() {
        guard motion.isDeviceMotionAvailable,!motion.isDeviceMotionActive else {return}
        motion.deviceMotionUpdateInterval=0.1
        motion.startDeviceMotionUpdates(to:motionQueue) {[weak self] data,_ in guard let self,let data else {return};let r=data.rotationRate
            self.lock.lock();self.rotation=sqrt(r.x*r.x+r.y*r.y+r.z*r.z);self.lock.unlock()
            self.publish {self.motionAvailable=true;self.roll=atan2(data.gravity.x,-data.gravity.y)*180/Double.pi}
        }
    }
    func capture(_ preferences:CapturePreferences,angle:CGFloat) async throws -> PhotoBurst {
        try await withCheckedThrowingContinuation {continuation in queue.async {
            guard self.transaction==nil,self.pending.isEmpty,self.session.isRunning,self.foreground,let device=self.input?.device else {continuation.resume(throwing:FilmError.invalid("Camera not ready"));return}
            var p=preferences;p.normalize()
            let types=self.output.availableRawPhotoPixelFormatTypes
            let raw=p.format.writesRaw ? types.first(where:{p.format.usesProRAW ? AVCapturePhotoOutput.isAppleProRAWPixelFormat($0) : AVCapturePhotoOutput.isBayerRAWPixelFormat($0)}) : nil
            guard !p.format.writesRaw || raw != nil else {continuation.resume(throwing:FilmError.invalid("Selected RAW type is not exposed by this camera. Choose a physical rear lens or JPEG."));return}
            let t=BurstTransaction(preferences:p,rawType:raw,controls:self.controls,flash:self.flash,angle:angle,continuation:continuation)
            self.transaction=t;self.publish {self.busy=true;self.status="Capturing safety original…"}
            // Disable buffered capture for RAW/bracket transactions. These flags are not proprietary Night mode.
            let responsive=p.responsive && !p.format.writesRaw && p.pipeline == .original && !self.controls.isManual
            self.session.beginConfiguration()
            if !responsive {
                if self.output.isFastCapturePrioritizationEnabled {self.output.isFastCapturePrioritizationEnabled=false}
                if self.output.isResponsiveCaptureEnabled {self.output.isResponsiveCaptureEnabled=false}
                if self.output.isZeroShutterLagEnabled {self.output.isZeroShutterLagEnabled=false}
            } else {
                if self.output.isZeroShutterLagSupported,!self.output.isZeroShutterLagEnabled {self.output.isZeroShutterLagEnabled=true}
                if self.output.isResponsiveCaptureSupported,!self.output.isResponsiveCaptureEnabled {self.output.isResponsiveCaptureEnabled=true}
                if self.output.isFastCapturePrioritizationSupported,!self.output.isFastCapturePrioritizationEnabled {self.output.isFastCapturePrioritizationEnabled=true}
            }
            self.session.commitConfiguration()
            if let c=self.output.connection(with:.video),c.isVideoRotationAngleSupported(angle) {c.videoRotationAngle=angle}
            do {try device.lockForConfiguration();device.videoZoomFactor=1;device.unlockForConfiguration()} catch {self.finish(error:error);return}
            self.queue.asyncAfter(deadline:.now()+35) {[weak self,weak t] in guard let self,let t,self.transaction === t else {return};self.finish(error:FilmError.invalid("Camera timed out; try Original or a different lens"))}
            self.takePhoto(t,helper:nil)
        }}
    }
    private func takePhoto(_ t:BurstTransaction,helper:Exposure?) {
        guard transaction === t else {return}
        guard foreground else {finish(error:FilmError.invalid("Camera no longer in foreground"));return}
        if helper != nil,ProcessInfo.processInfo.thermalState == .serious || ProcessInfo.processInfo.thermalState == .critical {t.fallback="HDR stopped for device temperature";finish();return}
        if helper != nil,Date().timeIntervalSince(t.started)>20 {t.fallback="HDR time budget reached";finish();return}
        let settings:AVCapturePhotoSettings
        if let raw=t.rawType {settings=AVCapturePhotoSettings(rawPixelFormatType:raw,processedFormat:t.preferences.format.writesJPEG ? [AVVideoCodecKey:AVVideoCodecType.jpeg] : nil)}
        else {settings=AVCapturePhotoSettings(format:[AVVideoCodecKey:AVVideoCodecType.jpeg])}
        let bayer=t.rawType.map {AVCapturePhotoOutput.isBayerRAWPixelFormat($0)} ?? false
        settings.photoQualityPrioritization=bayer || helper != nil ? .speed : .quality
        if let d=input?.device {
            let supported=d.activeFormat.supportedMaxPhotoDimensions
            let largest=supported.max(by:{Int64($0.width)*Int64($0.height)<Int64($1.width)*Int64($1.height)})
            let small=supported.filter {Int64($0.width)*Int64($0.height)<=13000000}.max(by:{Int64($0.width)*Int64($0.height)<Int64($1.width)*Int64($1.height)}) ?? supported.first
            if let dimensions=(t.preferences.maximum && t.preferences.pipeline == .original) ? largest : small {settings.maxPhotoDimensions=dimensions}
        }
        let flashMode:AVCaptureDevice.FlashMode=t.flash == .on ? .on : t.flash == .auto ? .auto : .off
        if helper==nil,output.supportedFlashModes.contains(flashMode) {settings.flashMode=flashMode}
        let id=settings.uniqueID
        let receiver=PhotoReceiver(id:id) {[weak self,weak t] result in self?.queue.async {
            guard let self else {return};self.pending[id]=nil
            guard let t,self.transaction === t else {return}
            switch result {
            case .failure(let error):if t.frames.isEmpty {self.finish(error:error)} else {t.fallback="HDR helper failed: \(error.localizedDescription)";self.finish()}
            case .success(let frame):self.received(frame,t,planned:helper)
            }
        }}
        pending[id]=receiver;output.capturePhoto(with:settings,delegate:receiver)
    }
    private func received(_ frame:PhotoFrame,_ t:BurstTransaction,planned:Exposure?) {
        if t.frames.isEmpty {
            if frame.jpeg==nil && frame.raw==nil {finish(error:FilmError.invalid("Camera returned no image resources"));return}
            if let warning=frame.warning {t.frames.append(frame);t.fallback=warning;finish();return}
            if (t.preferences.format.writesRaw && frame.raw==nil) || (t.preferences.format.writesJPEG && frame.jpeg==nil) {
                t.frames.append(frame);t.fallback="Requested format was incomplete; available originals retained";finish();return
            }
        }
        if let planned,let base=t.frames.first {
            guard let e=frame.exposure,abs(e/planned.product-1)<0.4,frame.requestID != base.requestID,frame.jpeg != base.jpeg,frame.focal==nil || base.focal==nil || abs(frame.focal!-base.focal!)<0.2 else {t.fallback="HDR request/metadata guard rejected a helper";finish();return}
            let limit=96*1024*1024
            if t.compressed+(frame.jpeg?.count ?? 0)>limit {t.fallback="HDR compressed-memory budget reached";finish();return}
            if planned.long {t.longs.append(t.frames.count)} else {t.shorts.append(t.frames.count)}
        }
        t.frames.append(frame);t.compressed+=(frame.jpeg?.count ?? 0)+(frame.raw?.count ?? 0)
        guard t.preferences.pipeline != .original,!t.preferences.format.writesRaw else {finish();return}
        guard let d=input?.device,d.isExposureModeSupported(.custom),!t.controls.isManual,t.flash == .off else {t.fallback="HDR requires automatic exposure and flash/torch off";finish();return}
        if t.frames.count==1 {
            guard let iso=frame.iso,let seconds=frame.seconds else {t.fallback="Exposure metadata unavailable";finish();return}
            lock.lock();let s=scene;lock.unlock()
            let fresh=s.flatMap {ProcessInfo.processInfo.systemUptime-$0.uptime<1.5 ? $0 : nil}
            guard let plan=HDRPolicy.plan(base:Exposure(iso:iso,seconds:seconds),isoRange:Double(d.activeFormat.minISO)...Double(d.activeFormat.maxISO),timeRange:CMTimeGetSeconds(d.activeFormat.minExposureDuration)...CMTimeGetSeconds(d.activeFormat.maxExposureDuration),quality:t.preferences.pipeline == .qualityHDR,legacy:t.preferences.pipeline == .legacyFusion,scene:fresh) else {t.fallback="Cannot meet safe HDR exposure plan";finish();return}
            t.plan=plan
            do {try d.lockForConfiguration();if d.isFocusModeSupported(.locked) {d.focusMode = .locked};if d.isWhiteBalanceModeSupported(.locked) {d.whiteBalanceMode = .locked};d.unlockForConfiguration()} catch {t.fallback="Could not lock camera for HDR";finish();return}
        }
        let index=t.frames.count-1
        guard index<t.plan.count else {finish();return}
        let next=t.plan[index]
        do {
            try d.lockForConfiguration()
            d.setExposureModeCustom(duration:CMTime(seconds:next.seconds,preferredTimescale:1000000000),iso:Float(next.iso)) {[weak self,weak t] _ in self?.queue.async {guard let self,let t,self.transaction === t else {return};self.publish {self.status="HDR frame \(t.frames.count)/\(t.plan.count+1)…"};self.takePhoto(t,helper:next)}}
            d.unlockForConfiguration()
        } catch {t.fallback="HDR exposure could not be set";finish()}
    }
    private func finish(error:Error?=nil) {
        guard let t=transaction else {return};transaction=nil
        if let d=input?.device {try? apply(t.controls,to:d)}
        publish {self.busy=false;self.status=error?.localizedDescription ?? "Processing photograph…"}
        if let error,t.frames.isEmpty {t.continuation.resume(throwing:error);return}
        guard !t.frames.isEmpty else {t.continuation.resume(throwing:error ?? FilmError.invalid("No image delivered"));return}
        let fallback=error?.localizedDescription ?? t.fallback
        var note=t.preferences.format.writesRaw ? (t.preferences.format.usesProRAW ? "Computational Apple ProRAW; unmodified DNG" : "Single-exposure Bayer RAW; unmodified DNG") : "AVFoundation quality photo"
        if let fallback,t.preferences.pipeline == .original {note+="; WARNING: \(fallback)"}
        t.continuation.resume(returning:PhotoBurst(frames:t.frames,shorts:t.shorts,longs:t.longs,pipeline:t.preferences.pipeline,note:note,fallback:fallback))
    }
    func captureOutput(_ output:AVCaptureOutput,didOutput sampleBuffer:CMSampleBuffer,from connection:AVCaptureConnection) {
        let now=ProcessInfo.processInfo.systemUptime;guard now-lastPreview>1/12 else {return};lastPreview=now
        guard let buffer=CMSampleBufferGetImageBuffer(sampleBuffer) else {return}
        autoreleasepool {
            let image=CIImage(cvPixelBuffer:buffer);let scale=min(1,960/image.extent.height);let small=image.transformed(by:CGAffineTransform(scaleX:scale,y:scale))
            lock.lock();let recipe=previewRecipe,prefs=previewPrefs,movement=rotation,before=showPreviewOriginal,device=previewDevice;lock.unlock()
            let framed=PhotoProcessor.crop(small,factor:prefs.crop)
            let c=before ? framed : PhotoProcessor.matrix(framed,recipe:recipe,preferences:prefs)
            guard let cg=previewContext.createCGImage(c,from:c.extent) else {return}
            let result=UIImage(cgImage:cg)
            frames+=1
            if frames%4==0 {sampleScene(small,motion:movement,now:now)}
            if frames%8==0 {let thumb=small.transformed(by:CGAffineTransform(scaleX:0.20,y:0.20));if let cg=previewContext.createCGImage(thumb,from:thumb.extent) {let source=UIImage(cgImage:cg);publish {self.lookSource=source}}}
            let iso=device?.iso ?? 0,time=device.map {CMTimeGetSeconds($0.exposureDuration)} ?? 0
            publish {self.preview=result;self.liveISO=iso;self.liveTime=time}
        }
    }
    #if DEBUG && targetEnvironment(simulator)
    var isShowingLensFixture: Bool { ProcessInfo.processInfo.arguments.contains("--ui-lens-fixture") }
    private func installLensFixture() {
        let metadata: [CameraLensMetadata] = [
            CameraLensMetadata(id: "fixture-ultra", facing: .back, kind: .ultraWide),
            CameraLensMetadata(id: "fixture-main", facing: .back, kind: .wide),
            CameraLensMetadata(id: "fixture-tele", facing: .back, kind: .telephoto),
            CameraLensMetadata(id: "fixture-auto", facing: .back, kind: .other, virtual: true, constituents: ["fixture-ultra", "fixture-main", "fixture-tele"], switchFactors: [2, 10]),
            CameraLensMetadata(id: "fixture-dual", facing: .back, kind: .other, virtual: true, constituents: ["fixture-main", "fixture-tele"], switchFactors: [5]),
            CameraLensMetadata(id: "fixture-front-wide", facing: .front, kind: .wide),
            CameraLensMetadata(id: "fixture-front", facing: .front, kind: .trueDepth, virtual: true)
        ]
        let labels = LensPresentation.lenses(metadata)
        publish {
            guard self.routes.isEmpty else { return }
            self.routes = labels.map { CameraRoute(lens: $0, detail: "SIMULATOR SAMPLE METADATA", constituentCount: $0.id == "fixture-auto" ? 3 : 2) }
            self.defaultRearID = LensPresentation.preferredRearID(labels); self.defaultFrontID = LensPresentation.preferredFrontID(labels)
            self.selectedID = "fixture-main"; self.rememberedRearID = "fixture-main"
            if let route = self.routes.first(where: { $0.id == self.selectedID }) { self.capabilities = self.fixtureCapabilities(route) }
            self.liveISO = 100; self.liveTime = 0.01
            self.status = "Sample lens metadata · no camera feed"; self.ready = false
        }
    }
    private func fixtureCapabilities(_ route: CameraRoute) -> CameraCapabilities {
        var result = CameraCapabilities(); result.cameraID = route.id; result.name = route.name
        result.customExposure = true; result.manualWB = true; result.manualFocus = true
        result.focusModes = ["AF-C", "AF-S", "MF"]
        // Sentinel UI metadata only, never submitted to AVCapturePhotoOutput. Capture remains disabled.
        if route.facing == .back && !route.virtual { result.bayer = 1; result.proRAW = 2 }
        return result
    }
    #endif
    private func sampleScene(_ image:CIImage,motion:Double?,now:Double) {
        let w=96,h=128;let i=image.transformed(by:CGAffineTransform(scaleX:CGFloat(w)/image.extent.width,y:CGFloat(h)/image.extent.height))
        var rgba=[UInt8](repeating:0,count:w*h*4)
        previewContext.render(i,toBitmap:&rgba,rowBytes:w*4,bounds:CGRect(x:0,y:0,width:w,height:h),format:.RGBA8,colorSpace:CGColorSpace(name:CGColorSpace.sRGB))
        var luma=[UInt8](repeating:0,count:w*h),bins=[Int](repeating:0,count:256);var clipped=0,shadow=0
        for p in luma.indices {
            let red=Int(rgba[p*4]),green=Int(rgba[p*4+1]),blue=Int(rgba[p*4+2])
            let v=(red*54+green*183+blue*19+128)>>8
            luma[p]=UInt8(v);bins[v]+=1
            if max(red,max(green,blue))>=250 {clipped+=1}
            if v<50 {shadow+=1}
        }
        var cumulative=0,p98=255;for x in 0..<256 {cumulative+=bins[x];if Double(cumulative)>Double(w*h)*0.98 {p98=x;break}}
        lock.lock();scene=SceneStats(clipped:Double(clipped)/Double(w*h),percentile:p98,shadows:Double(shadow)/Double(w*h),motion:motion,uptime:now);lastLuma=luma;lock.unlock()
        let peak=max(1,(0..<32).map {bins[($0*8)..<($0*8+8)].reduce(0,+)}.max() ?? 1)
        let graph=(0..<32).map {Double(bins[($0*8)..<($0*8+8)].reduce(0,+))/Double(peak)}
        publish {self.histogram=graph}
    }
}
private final class BurstTransaction {
    let preferences:CapturePreferences,rawType:UInt32?,controls:ManualControls,flash:FlashChoice,angle:CGFloat
    let continuation:CheckedContinuation<PhotoBurst,Error>
    let started=Date()
    var frames:[PhotoFrame]=[],shorts:[Int]=[],longs:[Int]=[],plan:[Exposure]=[]
    var compressed=0,fallback:String?
    init(preferences:CapturePreferences,rawType:UInt32?,controls:ManualControls,flash:FlashChoice,angle:CGFloat,continuation:CheckedContinuation<PhotoBurst,Error>) {self.preferences=preferences;self.rawType=rawType;self.controls=controls;self.flash=flash;self.angle=angle;self.continuation=continuation}
}
private final class PhotoReceiver:NSObject,AVCapturePhotoCaptureDelegate {
    let id:Int64,completion:(Result<PhotoFrame,Error>)->Void
    var jpeg:Data?,raw:Data?,metadata:[String:Any]=[:],error:Error?
    let date=Date()
    init(id:Int64,completion:@escaping (Result<PhotoFrame,Error>)->Void) {self.id=id;self.completion=completion}
    func photoOutput(_ output:AVCapturePhotoOutput,didFinishProcessingPhoto photo:AVCapturePhoto,error:Error?) {
        if let error {self.error=error;return}
        guard let data=photo.fileDataRepresentation() else {self.error=FilmError.invalid("Camera returned no photo bytes");return}
        if photo.isRawPhoto {raw=data;if metadata.isEmpty {metadata=photo.metadata}} else {jpeg=data;metadata=photo.metadata}
    }
    func photoOutput(_ output:AVCapturePhotoOutput,didFinishCaptureFor resolvedSettings:AVCaptureResolvedPhotoSettings,error:Error?) {
        let failure=error ?? self.error
        if let failure,jpeg==nil && raw==nil {completion(.failure(failure));return}
        let exif=metadata[kCGImagePropertyExifDictionary as String] as? [String:Any] ?? [:]
        let iso=(exif[kCGImagePropertyExifISOSpeedRatings as String] as? [NSNumber])?.first?.doubleValue
        let time=(exif[kCGImagePropertyExifExposureTime as String] as? NSNumber)?.doubleValue
        let focal=(exif[kCGImagePropertyExifFocalLength as String] as? NSNumber)?.doubleValue
        completion(.success(PhotoFrame(jpeg:jpeg,raw:raw,metadata:metadata,iso:iso,seconds:time,focal:focal,timestamp:date,requestID:id,warning:failure?.localizedDescription)))
    }
}
