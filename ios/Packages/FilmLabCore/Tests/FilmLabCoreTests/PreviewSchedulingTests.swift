import XCTest
import Foundation
import Dispatch
import FilmLabCore

final class PreviewSchedulingTests: XCTestCase {
    func testNewestFrameReplacesStaleWorkRatherThanBuildingLatency() {
        let mailbox = LatestFrameMailbox<Int>()
        for frame in 0..<600 { mailbox.offer(frame) }
        XCTAssertEqual(mailbox.statistics.pendingCount, 1)
        XCTAssertEqual(mailbox.statistics.offered, 600)
        XCTAssertEqual(mailbox.statistics.replaced, 599)
        XCTAssertEqual(mailbox.takeLatest()?.value, 599)
        XCTAssertNil(mailbox.takeLatest())
        XCTAssertEqual(mailbox.statistics.pendingCount, 0)
        XCTAssertEqual(mailbox.statistics.consumed, 1)
    }
    func testClearInvalidatesOldGenerationAndRetainsNoPendingFrame() {
        let mailbox = LatestFrameMailbox<String>()
        mailbox.offer("old lens"); let old = mailbox.takeLatest()
        mailbox.offer("stale"); mailbox.clear()
        XCTAssertNil(mailbox.takeLatest())
        mailbox.offer("new lens"); let new = mailbox.takeLatest()
        XCTAssertEqual(new?.value, "new lens")
        XCTAssertNotEqual(old?.generation, new?.generation)
        XCTAssertEqual(mailbox.statistics.pendingCount, 0)
    }
    func testConcurrentProducersKeepOnePendingSlot() {
        let mailbox = LatestFrameMailbox<Int>()
        DispatchQueue.concurrentPerform(iterations: 1000) { mailbox.offer($0) }
        XCTAssertEqual(mailbox.statistics.offered, 1000)
        XCTAssertEqual(mailbox.statistics.replaced, 999)
        XCTAssertEqual(mailbox.statistics.pendingCount, 1)
        XCTAssertTrue((0..<1000).contains(mailbox.takeLatest()!.value))
    }
    func testConsumedFramesAreNotArtificiallyLimitedToTwelvePerSecond() {
        let mailbox = LatestFrameMailbox<Int>()
        for frame in 0..<60 { mailbox.offer(frame); XCTAssertEqual(mailbox.takeLatest()?.value, frame) }
        XCTAssertEqual(mailbox.statistics.consumed, 60)
        XCTAssertEqual(mailbox.statistics.replaced, 0)
        // Scheduling/ownership test only, not a measured camera or display frame rate.
    }
    func testBackgroundWorkerCoalescesWhileBusy() {
        let entered = DispatchSemaphore(value: 0), release = DispatchSemaphore(value: 0)
        let done = expectation(description: "latest request processed")
        let lock = NSLock(); var processed: [Int] = []
        let worker = LatestOnlyWorker<Int>(queue: DispatchQueue(label: "test.preview.worker")) { value in
            if value == 0 { entered.signal(); _ = release.wait(timeout: .now() + 5) }
            lock.lock(); processed.append(value); lock.unlock()
            if value == 100 { done.fulfill() }
        }
        worker.submit(0)
        XCTAssertEqual(entered.wait(timeout: .now() + 5), .success)
        for value in 1...100 { worker.submit(value) }
        release.signal(); wait(for: [done], timeout: 5)
        lock.lock(); let result = processed; lock.unlock()
        XCTAssertEqual(result, [0, 100])
    }
    func testWorkerCancellationDiscardsPendingNotActiveRequest() {
        let entered = DispatchSemaphore(value: 0), release = DispatchSemaphore(value: 0)
        let queue = DispatchQueue(label: "test.preview.cancel")
        let worker = LatestOnlyWorker<Int>(queue: queue) { value in
            XCTAssertEqual(value, 1)
            entered.signal(); _ = release.wait(timeout: .now() + 5)
        }
        worker.submit(1); XCTAssertEqual(entered.wait(timeout: .now() + 5), .success)
        worker.submit(2); worker.cancelPending(); release.signal()
        queue.sync {} // Wait for the active drain to observe the empty pending slot.
    }
    func testDrawableIsBoundedWithoutUpsizingOrChangingAspect() {
        let phone = PreviewSizing.drawable(width: 400, height: 533.333333, screenScale: 3)!
        XCTAssertEqual(phone.height, 1440)
        XCTAssertEqual(phone.width, 1080)
        let small = PreviewSizing.drawable(width: 100, height: 200, screenScale: 2)!
        XCTAssertEqual(small.width, 200); XCTAssertEqual(small.height, 400)
        XCTAssertNil(PreviewSizing.drawable(width: .nan, height: 100, screenScale: 3))
        XCTAssertNil(PreviewSizing.drawable(width: 100, height: 0, screenScale: 3))
    }
    func testAspectFitPreservesLandscapeAndPortraitWithoutStretching() {
        let landscape = PreviewSizing.aspectFit(inputWidth: 160, inputHeight: 90, outputWidth: 300, outputHeight: 400)!
        XCTAssertEqual(landscape.scale, 1.875, accuracy: 0.000001)
        XCTAssertEqual(landscape.x, 0, accuracy: 0.000001)
        XCTAssertEqual(landscape.y, 115.625, accuracy: 0.000001)
        let portrait = PreviewSizing.aspectFit(inputWidth: 300, inputHeight: 400, outputWidth: 300, outputHeight: 400)!
        XCTAssertEqual(portrait.scale, 1); XCTAssertEqual(portrait.x, 0); XCTAssertEqual(portrait.y, 0)
        XCTAssertNil(PreviewSizing.aspectFit(inputWidth: 0, inputHeight: 400, outputWidth: 300, outputHeight: 400))
    }
}
