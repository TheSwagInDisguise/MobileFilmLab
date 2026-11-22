import Foundation
import Combine
import UIKit
import CoreImage
import FilmLabCore

/// Slow thumbnail updates do not invalidate the camera controller or the viewfinder.
final class LiveLookSource: ObservableObject {
    @Published private(set) var image: UIImage?
    func update(_ image: UIImage) { self.image = image }
}

/// Visible tiles render off-main, with one pending replacement rather than a queue of old looks.
final class LookThumbnailModel: ObservableObject {
    @Published private(set) var image: UIImage?
    private struct Request { let source: CGImage; let recipe: Recipe; let generation: UInt64 }
    private static let queue = DispatchQueue(label: "com.mobilefilmlab.preview.look-tiles", qos: .utility)
    private static let context = CIContext(options: [.workingColorSpace: NSNull(), .cacheIntermediates: false, .priorityRequestLow: true])
    private let lock = NSLock()
    private var active = false
    private var generation: UInt64 = 0
    private lazy var worker = LatestOnlyWorker<Request>(queue: Self.queue) { [weak self] request in
        guard let self, self.isCurrent(request.generation) else { return }
        dispatchPrecondition(condition: .notOnQueue(.main))
        let cg: CGImage? = autoreleasepool {
            let input = CIImage(cgImage: request.source)
            let output = PhotoProcessor.matrix(input, recipe: request.recipe, preferences: CapturePreferences())
            return Self.context.createCGImage(output, from: output.extent, format: .RGBA8, colorSpace: PhotoProcessor.colourSpace)
        }
        guard let cg else { return }
        DispatchQueue.main.async { [weak self] in
            guard let self, self.isCurrent(request.generation) else { return }
            self.image = UIImage(cgImage: cg)
        }
    }
    func activate() { lock.lock(); active = true; lock.unlock() }
    func deactivate() {
        lock.lock(); active = false; generation &+= 1; lock.unlock()
        worker.cancelPending()
    }
    func request(_ source: UIImage?, recipe: Recipe) {
        guard let cg = source?.cgImage else { return }
        lock.lock()
        guard active else { lock.unlock(); return }
        generation &+= 1; let version = generation; lock.unlock()
        worker.submit(Request(source: cg, recipe: recipe, generation: version))
    }
    private func isCurrent(_ version: UInt64) -> Bool {
        lock.lock(); defer { lock.unlock() }; return active && generation == version
    }
}
