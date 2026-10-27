package com.mobilefilmlab.camera.camera

import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread

/** Real lease/retirement logic, with no claim of exercising a physical ImageReader. */
class RetainedResourceTest {
    private class Image : AutoCloseable { val closes=AtomicInteger();override fun close() { closes.incrementAndGet() } }
    @Test fun retiredReaderWaitsUntilAllOwnedImagesFinish() {
        var readerCloses=0;val owner=RetainedResource { readerCloses++ };val a=Image();val b=Image()
        val x=owner.retain(a);val y=owner.retain(b);owner.retire();assertEquals(0,readerCloses)
        x.close();assertEquals(0,readerCloses);y.close();assertEquals(1,readerCloses)
    }
    @Test fun closingAnImageKeepsTheActiveReaderAvailable() {
        var closes=0;val owner=RetainedResource { closes++ };owner.retain(Image()).close();assertEquals(0,closes)
        val next=owner.retain(Image());next.close();owner.retire();assertEquals(1,closes)
    }
    @Test fun duplicateCloseDoesNotDoubleReleaseNativeObjects() {
        var closes=0;val owner=RetainedResource { closes++ };val image=Image();val lease=owner.retain(image)
        owner.retire();lease.close();lease.close();owner.retire();assertEquals(1,image.closes.get());assertEquals(1,closes)
    }
    @Test fun retiredOwnerRejectsNewImages() {
        val owner=RetainedResource {};owner.retire()
        try { owner.retain(Image());fail("A retired reader must not acquire a new lease") } catch(_: IllegalStateException) {}
    }
    @Test fun concurrentSessionCloseAndWriterCompletionCloseExactlyOnce() {
        val closes=AtomicInteger();val owner=RetainedResource { closes.incrementAndGet() };val image=Image();val lease=owner.retain(image);val start=CountDownLatch(1)
        val a=thread { start.await();repeat(30) { owner.retire() } };val b=thread { start.await();repeat(30) { lease.close() } }
        start.countDown();a.join();b.join();assertEquals(1,closes.get());assertEquals(1,image.closes.get())
    }
}
