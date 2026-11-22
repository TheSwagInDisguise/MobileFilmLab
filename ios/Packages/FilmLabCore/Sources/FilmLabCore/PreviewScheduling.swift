import Foundation
import Dispatch

/// One pending immutable frame, never a FIFO of stale camera images.
/// The renderer may own a consumed frame while a producer replaces this single pending slot.
public final class LatestFrameMailbox<Value>: @unchecked Sendable {
    public struct Frame {
        public let value: Value
        public let sequence: UInt64
        public let generation: UInt64
    }
    public struct Statistics: Equatable, Sendable {
        public let offered: UInt64
        public let consumed: UInt64
        public let replaced: UInt64
        public let generation: UInt64
        public let pendingCount: Int
    }
    private let lock = NSLock()
    private var pending: Frame?
    private var offered: UInt64 = 0
    private var consumed: UInt64 = 0
    private var replaced: UInt64 = 0
    private var generation: UInt64 = 0
    public init() {}
    public func offer(_ value: Value) {
        lock.lock(); defer { lock.unlock() }
        offered &+= 1
        if pending != nil { replaced &+= 1 }
        pending = Frame(value: value, sequence: offered, generation: generation)
    }
    public func takeLatest() -> Frame? {
        lock.lock(); defer { lock.unlock() }
        guard let frame = pending else { return nil }
        pending = nil; consumed &+= 1
        return frame
    }
    public func clear() {
        lock.lock(); defer { lock.unlock() }
        pending = nil; generation &+= 1
    }
    public var statistics: Statistics {
        lock.lock(); defer { lock.unlock() }
        return Statistics(offered: offered, consumed: consumed, replaced: replaced, generation: generation, pendingCount: pending == nil ? 0 : 1)
    }
}

/// Background analysis/thumbnail work gets one active and at most one replacement request.
/// Submitting never waits for rendering; callbacks are deliberately not run under the lock.
public final class LatestOnlyWorker<Value>: @unchecked Sendable {
    private let lock = NSLock()
    private let queue: DispatchQueue
    private let process: (Value) -> Void
    private var pending: Value?
    private var running = false
    public init(queue: DispatchQueue, process: @escaping (Value) -> Void) {
        self.queue = queue; self.process = process
    }
    public func submit(_ value: Value) {
        lock.lock(); pending = value
        let start = !running; running = true
        lock.unlock()
        if start { queue.async { self.drain() } }
    }
    public func cancelPending() {
        lock.lock(); pending = nil; lock.unlock()
    }
    private func drain() {
        while true {
            lock.lock()
            guard let value = pending else { running = false; lock.unlock(); return }
            pending = nil; lock.unlock()
            process(value)
        }
    }
}

public enum PreviewSizing {
    public struct Dimensions: Equatable, Sendable { public let width: Int; public let height: Int }
    public struct Fit: Equatable, Sendable { public let scale: Double; public let x: Double; public let y: Double }
    /// Limits only the display drawable, never AVCapturePhotoOutput or saved-photo dimensions.
    public static func drawable(width: Double, height: Double, screenScale: Double, maximumEdge: Int = 1440) -> Dimensions? {
        guard width.isFinite, height.isFinite, screenScale.isFinite, width > 0, height > 0, screenScale > 0 else { return nil }
        let edge = Double(min(4096, max(64, maximumEdge)))
        let scale = min(min(8, screenScale), edge / max(width, height))
        return Dimensions(width: max(1, min(Int(edge), Int((width * scale + 0.000001).rounded(.down)))), height: max(1, min(Int(edge), Int((height * scale + 0.000001).rounded(.down)))))
    }
    public static func aspectFit(inputWidth: Double, inputHeight: Double, outputWidth: Double, outputHeight: Double) -> Fit? {
        let values = [inputWidth, inputHeight, outputWidth, outputHeight]
        guard values.allSatisfy({ $0.isFinite && $0 > 0 }) else { return nil }
        let scale = min(outputWidth / inputWidth, outputHeight / inputHeight)
        return Fit(scale: scale, x: (outputWidth - inputWidth * scale) / 2, y: (outputHeight - inputHeight * scale) / 2)
    }
}
