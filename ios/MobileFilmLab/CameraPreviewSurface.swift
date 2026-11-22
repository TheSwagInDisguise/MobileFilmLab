import SwiftUI
import MetalKit
import CoreImage
import QuartzCore
import FilmLabCore

struct CameraPreviewFrame {
    let image: CIImage
    let receivedAt: TimeInterval
}

/// Immutable, cached coefficients. The saved-photo processor is not changed by this preview path.
struct PreviewColourTransform {
    private let identity: Bool
    private let parameters: [String: Any]
    init(matrix: ColourMatrix = ColourMatrix()) {
        identity = matrix.isIdentity
        let m = matrix.values
        parameters = [
            "inputRVector": CIVector(x: m[0], y: m[1], z: m[2], w: m[3]),
            "inputGVector": CIVector(x: m[5], y: m[6], z: m[7], w: m[8]),
            "inputBVector": CIVector(x: m[10], y: m[11], z: m[12], w: m[13]),
            "inputAVector": CIVector(x: m[15], y: m[16], z: m[17], w: m[18]),
            "inputBiasVector": CIVector(x: m[4] / 255, y: m[9] / 255, z: m[14] / 255, w: m[19] / 255)
        ]
    }
    init(recipe: Recipe, preferences: CapturePreferences) {
        self.init(matrix: ColourMatrix.recipe(recipe, red: preferences.red, green: preferences.green, blue: preferences.blue))
    }
    func apply(_ image: CIImage) -> CIImage { identity ? image : image.applyingFilter("CIColorMatrix", parameters: parameters) }
}

enum PreviewImageLayout {
    static func fitted(_ image: CIImage, to size: CGSize) -> CIImage? {
        let extent = image.extent
        guard let fit = PreviewSizing.aspectFit(inputWidth: extent.width, inputHeight: extent.height, outputWidth: size.width, outputHeight: size.height),
              extent.minX.isFinite, extent.minY.isFinite else { return nil }
        let bounds = CGRect(origin: .zero, size: size)
        let normalized = image.transformed(by: CGAffineTransform(translationX: -extent.minX, y: -extent.minY))
        let placed = normalized.transformed(by: CGAffineTransform(scaleX: fit.scale, y: fit.scale))
            .transformed(by: CGAffineTransform(translationX: fit.x, y: fit.y))
        let background = CIImage(color: CIColor(red: 0.025, green: 0.035, blue: 0.035, alpha: 1)).cropped(to: bounds)
        return placed.composited(over: background).cropped(to: bounds)
    }
    /// A Metal texture's top memory row must contain the logical top of the camera image.
    static func encode(_ image: CIImage, context: CIContext, texture: MTLTexture, commandBuffer: MTLCommandBuffer) throws {
        let destination = CIRenderDestination(mtlTexture: texture, commandBuffer: commandBuffer)
        destination.isFlipped = true
        destination.colorSpace = PhotoProcessor.colourSpace
        destination.alphaMode = .none
        _ = try context.startTask(toRender: image, from: image.extent, to: destination, at: .zero)
    }
}

final class PreviewPerformance: @unchecked Sendable {
    struct Snapshot {
        var renderer = "Waiting for preview surface"
        var inputFPS = 0
        var presentedFPS = 0
        var inputSize = "No frames"
        var drawableSize = "Not allocated"
        var renderErrors = 0
    }
    private let lock = NSLock()
    private var value = Snapshot()
    private var inputs: [TimeInterval] = [], presentations: [TimeInterval] = []
    private var firstRender: (() -> Void)?
    private var hasRendered = false
    func input(width: Int, height: Int, at now: TimeInterval) {
        lock.lock(); defer { lock.unlock() }
        inputs.append(now); inputs.removeAll { now - $0 > 1 || $0 > now }
        if inputs.count > 240 { inputs.removeFirst(inputs.count - 240) }
        value.inputSize = "\(width) × \(height)"
    }
    func presented(at now: TimeInterval) {
        lock.lock()
        presentations.append(now); presentations.removeAll { now - $0 > 1 || $0 > now }
        if presentations.count > 240 { presentations.removeFirst(presentations.count - 240) }
        let notify = hasRendered ? nil : firstRender
        hasRendered = true; firstRender = nil; lock.unlock()
        if let notify { DispatchQueue.main.async(execute: notify) }
    }
    func onFirstRender(_ callback: @escaping () -> Void) {
        lock.lock(); let ready = hasRendered
        if !ready { firstRender = callback }; lock.unlock()
        if ready { DispatchQueue.main.async(execute: callback) }
    }
    func configure(renderer: String, size: CGSize? = nil) {
        lock.lock(); defer { lock.unlock() }
        value.renderer = renderer
        if let size { value.drawableSize = "\(Int(size.width)) × \(Int(size.height))" }
    }
    func failed() { lock.lock(); value.renderErrors += 1; lock.unlock() }
    func snapshot(at now: TimeInterval = ProcessInfo.processInfo.systemUptime) -> Snapshot {
        lock.lock(); defer { lock.unlock() }
        var result = value
        result.inputFPS = inputs.filter { now - $0 <= 1 && now >= $0 }.count
        result.presentedFPS = presentations.filter { now - $0 <= 1 && now >= $0 }.count
        return result
    }
}

/// No ObservableObject and no UIImage per camera frame. SwiftUI only owns the native surface.
struct CameraPreviewSurface: UIViewRepresentable {
    let frames: LatestFrameMailbox<CameraPreviewFrame>
    let performance: PreviewPerformance
    func makeUIView(context: Context) -> CameraPreviewHost { CameraPreviewHost(frames: frames, performance: performance) }
    func updateUIView(_ view: CameraPreviewHost, context: Context) {}
    static func dismantleUIView(_ view: CameraPreviewHost, coordinator: ()) { view.stop() }
}

@MainActor private final class PreviewDisplayLinkTarget: NSObject {
    weak var host: CameraPreviewHost?
    @objc func tick() { host?.drawFallback() }
}

final class CameraPreviewHost: UIView {
    private let frames: LatestFrameMailbox<CameraPreviewFrame>
    private let performance: PreviewPerformance
    private var metalView: MTKView?
    private var renderer: MetalCameraPreviewRenderer?
    private var fallbackView: UIImageView?
    private var fallbackLink: CADisplayLink?
    private let linkTarget = PreviewDisplayLinkTarget()
    private let fallbackQueue = DispatchQueue(label: "com.mobilefilmlab.preview.fallback", qos: .userInitiated)
    private var fallbackContext: CIContext?
    private var fallbackBusy = false
    private var tokens: [NSObjectProtocol] = []
    private var active = true
    init(frames: LatestFrameMailbox<CameraPreviewFrame>, performance: PreviewPerformance) {
        self.frames = frames; self.performance = performance
        super.init(frame: .zero)
        backgroundColor = UIColor(red: 0.025, green: 0.035, blue: 0.035, alpha: 1)
        clipsToBounds = true; isAccessibilityElement = true
        accessibilityIdentifier = "cameraPreviewSurface"; accessibilityLabel = "Live camera viewfinder"
        if let device = MTLCreateSystemDefaultDevice(), let renderer = MetalCameraPreviewRenderer(device: device, frames: frames, performance: performance) {
            let view = MTKView(frame: .zero, device: device)
            view.framebufferOnly = false; view.colorPixelFormat = .bgra8Unorm
            view.colorspace = PhotoProcessor.colourSpace
            view.preferredFramesPerSecond = 60 // Display cadence only; sensor exposure and frame rate are not forced.
            view.autoResizeDrawable = false; view.enableSetNeedsDisplay = false; view.isPaused = true
            view.isAccessibilityElement = false; view.delegate = renderer
            addSubview(view); metalView = view; self.renderer = renderer
            performance.configure(renderer: "Metal GPU"); accessibilityValue = "Metal GPU"
        } else {
            // Non-Metal environments retain live looks without publishing images through SwiftUI.
            let view = UIImageView(); view.contentMode = .scaleAspectFit; addSubview(view); fallbackView = view
            fallbackContext = CIContext(options: [.workingColorSpace: NSNull(), .cacheIntermediates: false])
            linkTarget.host = self
            let link = CADisplayLink(target: linkTarget, selector: #selector(PreviewDisplayLinkTarget.tick))
            link.preferredFramesPerSecond = 30; link.isPaused = true; link.add(to: .main, forMode: .common); fallbackLink = link
            performance.configure(renderer: "Core Image native fallback"); accessibilityValue = "Core Image native fallback"
        }
        performance.onFirstRender { [weak self] in
            DispatchQueue.main.async {
                guard let self else { return }
                self.accessibilityValue = (self.renderer == nil ? "Core Image native fallback" : "Metal GPU") + " · frame rendered"
            }
        }
        tokens.append(NotificationCenter.default.addObserver(forName: UIApplication.willResignActiveNotification, object: nil, queue: .main) { [weak self] _ in self?.active = false; self?.updateActivity() })
        tokens.append(NotificationCenter.default.addObserver(forName: UIApplication.didBecomeActiveNotification, object: nil, queue: .main) { [weak self] _ in self?.active = true; self?.updateActivity() })
    }
    required init?(coder: NSCoder) { fatalError("init(coder:) is not supported") }
    override func didMoveToWindow() { super.didMoveToWindow(); active = UIApplication.shared.applicationState == .active; updateActivity() }
    override func layoutSubviews() {
        super.layoutSubviews(); metalView?.frame = bounds; fallbackView?.frame = bounds
        guard let pixels = PreviewSizing.drawable(width: bounds.width, height: bounds.height, screenScale: window?.screen.scale ?? UIScreen.main.scale) else { return }
        let size = CGSize(width: pixels.width, height: pixels.height)
        if let metalView, metalView.drawableSize != size { metalView.drawableSize = size }
        performance.configure(renderer: renderer == nil ? "Core Image native fallback" : "Metal GPU", size: size)
    }
    private func updateActivity() {
        let paused = !active || window == nil
        metalView?.isPaused = paused; fallbackLink?.isPaused = paused
    }
    func stop() { metalView?.isPaused = true; fallbackLink?.invalidate(); fallbackLink = nil }
    deinit { tokens.forEach { NotificationCenter.default.removeObserver($0) }; fallbackLink?.invalidate() }
    fileprivate func drawFallback() {
        guard !fallbackBusy, active, window != nil, let context = fallbackContext,
              let pixels = PreviewSizing.drawable(width: bounds.width, height: bounds.height, screenScale: window?.screen.scale ?? 2),
              let frame = frames.takeLatest() else { return }
        fallbackBusy = true
        let size = CGSize(width: pixels.width, height: pixels.height)
        fallbackQueue.async { [weak self] in
            let rendered: CGImage? = autoreleasepool {
                guard let image = PreviewImageLayout.fitted(frame.value.image, to: size) else { return nil }
                return context.createCGImage(image, from: image.extent, format: .RGBA8, colorSpace: PhotoProcessor.colourSpace)
            }
            DispatchQueue.main.async { [weak self] in
                guard let self else { return }; self.fallbackBusy = false
                guard self.frames.statistics.generation == frame.generation else { return }
                if let rendered { self.fallbackView?.image = UIImage(cgImage: rendered); self.performance.presented(at: ProcessInfo.processInfo.systemUptime) }
                else { self.performance.failed() }
            }
        }
    }
}

@MainActor private final class MetalCameraPreviewRenderer: NSObject, @preconcurrency MTKViewDelegate {
    private let frames: LatestFrameMailbox<CameraPreviewFrame>
    private let performance: PreviewPerformance
    private let queue: MTLCommandQueue
    private let context: CIContext
    private let inFlight = DispatchSemaphore(value: 2)
    private var lastFrame: LatestFrameMailbox<CameraPreviewFrame>.Frame?
    private var needsRedraw = false
    init?(device: MTLDevice, frames: LatestFrameMailbox<CameraPreviewFrame>, performance: PreviewPerformance) {
        guard let queue = device.makeCommandQueue() else { return nil }
        self.queue = queue; self.frames = frames; self.performance = performance
        context = CIContext(mtlCommandQueue: queue, options: [.workingColorSpace: NSNull(), .cacheIntermediates: false])
        super.init()
    }
    func mtkView(_ view: MTKView, drawableSizeWillChange size: CGSize) { needsRedraw = true }
    func draw(in view: MTKView) {
        let statistics = frames.statistics
        if lastFrame?.generation != statistics.generation { lastFrame = nil }
        guard statistics.pendingCount > 0 || (needsRedraw && lastFrame != nil),
              inFlight.wait(timeout: .now()) == .success else { return }
        guard let drawable = view.currentDrawable, let command = queue.makeCommandBuffer() else { inFlight.signal(); return }
        if let newest = frames.takeLatest() { lastFrame = newest }
        guard let frame = lastFrame,
              let image = PreviewImageLayout.fitted(frame.value.image, to: CGSize(width: drawable.texture.width, height: drawable.texture.height)) else { inFlight.signal(); return }
        do {
            try PreviewImageLayout.encode(image, context: context, texture: drawable.texture, commandBuffer: command)
            let semaphore = inFlight, performance = performance
            command.addCompletedHandler { [frame] buffer in
                withExtendedLifetime(frame) {
                    if buffer.status == .completed { performance.presented(at: ProcessInfo.processInfo.systemUptime) }
                    else { performance.failed() }
                    semaphore.signal()
                }
            }
            command.present(drawable); command.commit(); needsRedraw = false
        } catch { performance.failed(); inFlight.signal() }
    }
}
