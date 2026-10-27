package com.mobilefilmlab.camera.camera

import java.util.concurrent.atomic.AtomicBoolean

/** A reader can outlive its closed camera session while a worker writes an acquired RAW image. */
internal class RetainedResource(private val closeResource: () -> Unit) {
    private var retired = false
    private var leases = 0
    private var closed = false

    @Synchronized fun <T : AutoCloseable> retain(value: T): ResourceLease<T> {
        check(!retired) { "Resource already retired" }
        leases++
        return ResourceLease(value) { release() }
    }
    @Synchronized fun retire() { retired = true; closeIfIdle() }
    @Synchronized private fun release() { check(leases > 0); leases--; closeIfIdle() }
    private fun closeIfIdle() {
        if (retired && leases == 0 && !closed) { closed = true; closeResource() }
    }
}

internal class ResourceLease<T : AutoCloseable>(val value: T, private val released: () -> Unit) : AutoCloseable {
    private val closed = AtomicBoolean(false)
    override fun close() {
        if (closed.compareAndSet(false, true)) {
            try { value.close() } finally { released() }
        }
    }
}
