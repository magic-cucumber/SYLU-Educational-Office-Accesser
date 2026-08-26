package top.kagg886.util.sound

import android.media.MediaDataSource
import android.media.MediaPlayer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

private class AndroidSoundResource(bytes: ByteArray) : SoundResource {
    private val player = MediaPlayer().also { player ->
        try {
            player.setDataSource(ByteArrayMediaDataSource(bytes))
            player.prepare()
        } catch (cause: Throwable) {
            player.release()
            throw cause
        }
    }

    private val playMutex = Mutex()
    private val playerLock = Any()
    private var closed = false
    private var activeContinuation: CancellableContinuation<Unit>? = null

    override suspend fun play() = playMutex.withLock {
        suspendCancellableCoroutine { continuation ->
            continuation.invokeOnCancellation {
                val shouldReset = synchronized(playerLock) {
                    if (activeContinuation !== continuation) {
                        false
                    } else {
                        activeContinuation = null
                        clearListeners()
                        !closed
                    }
                }

                if (shouldReset) {
                    runCatching {
                        player.pause()
                        player.seekTo(0)
                    }
                }
            }

            synchronized(playerLock) {
                check(!closed) { "Sound resource is closed" }
                if (!continuation.isActive) return@suspendCancellableCoroutine

                activeContinuation = continuation
                player.setOnCompletionListener { finish(continuation) }
                player.setOnErrorListener { _, what, extra ->
                    finish(
                        continuation,
                        IllegalStateException("MediaPlayer error: what=$what, extra=$extra"),
                    )
                    true
                }

                try {
                    player.seekTo(0)
                    player.start()
                } catch (cause: Throwable) {
                    finish(continuation, cause)
                }
            }
        }
    }

    private fun finish(
        continuation: CancellableContinuation<Unit>,
        cause: Throwable? = null,
    ) {
        synchronized(playerLock) {
            if (activeContinuation !== continuation) return

            activeContinuation = null
            clearListeners()
        }

        if (cause == null) {
            continuation.resume(Unit)
        } else {
            continuation.resumeWith(Result.failure(cause))
        }
    }

    private fun clearListeners() {
        player.setOnCompletionListener(null)
        player.setOnErrorListener(null)
    }

    override fun close() {
        val continuation = synchronized(playerLock) {
            if (closed) return

            closed = true
            val current = activeContinuation
            activeContinuation = null
            clearListeners()
            current
        }

        runCatching { player.stop() }
        player.release()
        continuation?.resumeWith(Result.failure(IllegalStateException("Sound resource is closed")))
    }
}

private class ByteArrayMediaDataSource(
    private val bytes: ByteArray,
) : MediaDataSource() {
    override fun getSize(): Long = bytes.size.toLong()

    override fun readAt(
        position: Long,
        buffer: ByteArray,
        offsetInBytes: Int,
        sizeInBytes: Int,
    ): Int {
        if (position < 0 || position >= bytes.size) return -1

        val start = position.toInt()
        val count = minOf(sizeInBytes, bytes.size - start)
        bytes.copyInto(
            destination = buffer,
            destinationOffset = offsetInBytes,
            startIndex = start,
            endIndex = start + count,
        )
        return count
    }

    override fun close() = Unit
}

actual fun SoundResource(res: ByteArray): SoundResource = AndroidSoundResource(res)