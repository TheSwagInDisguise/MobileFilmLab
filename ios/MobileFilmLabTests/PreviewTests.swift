import XCTest
import CoreImage
import Metal
import Combine
import UIKit
import FilmLabCore
@testable import MobileFilmLab

final class PreviewTests: XCTestCase {
    private func pattern() -> CIImage {
        let rect = CGRect(x: 0, y: 0, width: 24, height: 32)
        var result = CIImage(color: .black).cropped(to: rect)
        for (area, color) in [
            (CGRect(x: 0, y: 16, width: 12, height: 16), CIColor(red: 1, green: 0, blue: 0)),
            (CGRect(x: 12, y: 16, width: 12, height: 16), CIColor(red: 0, green: 1, blue: 0)),
            (CGRect(x: 0, y: 0, width: 12, height: 16), CIColor(red: 0, green: 0, blue: 1)),
            (CGRect(x: 12, y: 0, width: 12, height: 16), CIColor(red: 1, green: 1, blue: 1))
        ] { result = CIImage(color: color).cropped(to: area).composited(over: result) }
        return result
    }
    func testCachedPreviewTransformMatchesAllEightyLookMatrices() throws {
        let context = PhotoProcessor().context
        let input = CIImage(color: CIColor(red: 0.31, green: 0.52, blue: 0.73, alpha: 1)).cropped(to: CGRect(x: 0, y: 0, width: 4, height: 4))
        var preferences = CapturePreferences(); preferences.red = 1.03; preferences.blue = 0.97
        for recipe in try Catalogue.load().recipes {
            let expected = PhotoProcessor.matrix(input, recipe: recipe, preferences: preferences)
            let actual = PreviewColourTransform(recipe: recipe, preferences: preferences).apply(input)
            var a = [UInt8](repeating: 0, count: 64), b = a
            context.render(expected, toBitmap: &a, rowBytes: 16, bounds: input.extent, format: .RGBA8, colorSpace: PhotoProcessor.colourSpace)
            context.render(actual, toBitmap: &b, rowBytes: 16, bounds: input.extent, format: .RGBA8, colorSpace: PhotoProcessor.colourSpace)
            for i in a.indices { XCTAssertLessThanOrEqual(abs(Int(a[i]) - Int(b[i])), 1, recipe.id) }
        }
    }
    func testPreviewLayoutNormalizesOriginAndLetterboxesWithoutStretching() throws {
        let input = CIImage(color: CIColor(red: 1, green: 0, blue: 0)).cropped(to: CGRect(x: 13, y: 29, width: 40, height: 20))
        let fitted = try XCTUnwrap(PreviewImageLayout.fitted(input, to: CGSize(width: 80, height: 80)))
        XCTAssertEqual(fitted.extent, CGRect(x: 0, y: 0, width: 80, height: 80))
        var pixel = [UInt8](repeating: 0, count: 4)
        let context = PhotoProcessor().context
        context.render(fitted, toBitmap: &pixel, rowBytes: 4, bounds: CGRect(x: 40, y: 40, width: 1, height: 1), format: .RGBA8, colorSpace: PhotoProcessor.colourSpace)
        XCTAssertGreaterThan(pixel[0], 250); XCTAssertLessThan(pixel[1], 5); XCTAssertEqual(pixel[3], 255)
        context.render(fitted, toBitmap: &pixel, rowBytes: 4, bounds: CGRect(x: 40, y: 5, width: 1, height: 1), format: .RGBA8, colorSpace: PhotoProcessor.colourSpace)
        XCTAssertLessThan(pixel[0], 20); XCTAssertLessThan(pixel[1], 20); XCTAssertEqual(pixel[3], 255)
    }
    func testAvailablePreviewBackendProducesUprightCornerColours() throws {
        let input = try XCTUnwrap(PreviewImageLayout.fitted(pattern(), to: CGSize(width: 24, height: 32)))
        if let device = MTLCreateSystemDefaultDevice() {
            let queue = try XCTUnwrap(device.makeCommandQueue())
            let context = CIContext(mtlCommandQueue: queue, options: [.workingColorSpace: NSNull(), .cacheIntermediates: false])
            let descriptor = MTLTextureDescriptor.texture2DDescriptor(pixelFormat: .bgra8Unorm, width: 24, height: 32, mipmapped: false)
            descriptor.storageMode = .shared; descriptor.usage = [.shaderRead, .shaderWrite, .renderTarget]
            let texture = try XCTUnwrap(device.makeTexture(descriptor: descriptor)), command = try XCTUnwrap(queue.makeCommandBuffer())
            try PreviewImageLayout.encode(input, context: context, texture: texture, commandBuffer: command)
            command.commit(); command.waitUntilCompleted() // Test-only readback, never used by the live renderer.
            XCTAssertEqual(command.status, .completed)
            var pixels = [UInt8](repeating: 0, count: 24 * 32 * 4)
            texture.getBytes(&pixels, bytesPerRow: 24 * 4, from: MTLRegionMake2D(0, 0, 24, 32), mipmapLevel: 0)
            for (x, y, rgb) in [(4, 4, [255, 0, 0]), (20, 4, [0, 255, 0]), (4, 28, [0, 0, 255]), (20, 28, [255, 255, 255])] {
                let offset = (y * 24 + x) * 4
                XCTAssertEqual(Array(pixels[offset..<(offset + 3)]), [UInt8(rgb[2]), UInt8(rgb[1]), UInt8(rgb[0])])
                XCTAssertEqual(pixels[offset + 3], 255)
            }
            print("MFL_PREVIEW_TEST_BACKEND=Metal")
        } else {
            let context = PhotoProcessor().context
            let image = try XCTUnwrap(context.createCGImage(input, from: input.extent))
            XCTAssertEqual(image.width, 24); XCTAssertEqual(image.height, 32)
            let top = try XCTUnwrap(image.cropping(to: CGRect(x: 4, y: 4, width: 1, height: 1)))
            var pixel = [UInt8](repeating: 0, count: 4)
            let bitmap = try XCTUnwrap(CGContext(data: &pixel, width: 1, height: 1, bitsPerComponent: 8, bytesPerRow: 4, space: PhotoProcessor.colourSpace, bitmapInfo: CGImageAlphaInfo.premultipliedLast.rawValue))
            bitmap.draw(top, in: CGRect(x: 0, y: 0, width: 1, height: 1))
            XCTAssertGreaterThan(pixel[0], 250); XCTAssertLessThan(pixel[1], 5); XCTAssertLessThan(pixel[2], 5)
            print("MFL_PREVIEW_TEST_BACKEND=CoreImageFallback; simulator has no Metal device")
        }
    }
    func testPreviewDiagnosticWindowsAreBoundedAndExpireWithoutInput() {
        let metrics = PreviewPerformance()
        for frame in 0..<30 { let time = 10 + Double(frame) / 30; metrics.input(width: 720, height: 960, at: time); metrics.presented(at: time) }
        XCTAssertEqual(metrics.snapshot(at: 11).inputFPS, 30)
        XCTAssertEqual(metrics.snapshot(at: 11).presentedFPS, 30)
        XCTAssertEqual(metrics.snapshot(at: 13).inputFPS, 0)
        for _ in 0..<1000 { metrics.input(width: 720, height: 960, at: 20) }
        XCTAssertLessThanOrEqual(metrics.snapshot(at: 20).inputFPS, 240)
        metrics.configure(renderer: "test only", size: CGSize(width: 1080, height: 1440))
        XCTAssertEqual(metrics.snapshot(at: 20).drawableSize, "1080 × 1440")
        // Synthetic timestamps test the counters; these are not camera FPS measurements.
    }
    @MainActor func testLookThumbnailPublishesOnMainOnlyWhileActive() async throws {
        let model = LookThumbnailModel(), recipe = try Catalogue.load().recipes[0]
        let cg = try XCTUnwrap(PhotoProcessor().context.createCGImage(pattern(), from: pattern().extent))
        let source = UIImage(cgImage: cg), done = expectation(description: "thumbnail delivered")
        var observation: AnyCancellable? = model.$image.dropFirst().sink { image in
            if image != nil { XCTAssertTrue(Thread.isMainThread); done.fulfill() }
        }
        model.activate(); model.request(source, recipe: recipe)
        await fulfillment(of: [done], timeout: 10)
        observation?.cancel(); observation = nil
        XCTAssertNotNil(model.image)
        let noUpdate = expectation(description: "inactive tile does not render"); noUpdate.isInverted = true
        observation = model.$image.dropFirst().sink { _ in noUpdate.fulfill() }
        model.deactivate(); model.request(source, recipe: recipe)
        await fulfillment(of: [noUpdate], timeout: 0.25)
        observation?.cancel()
    }
}
