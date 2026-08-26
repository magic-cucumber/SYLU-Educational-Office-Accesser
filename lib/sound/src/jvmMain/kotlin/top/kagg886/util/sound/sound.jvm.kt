package top.kagg886.util.sound

import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.ByteArrayInputStream
import java.util.concurrent.atomic.AtomicBoolean
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Clip
import javax.sound.sampled.LineEvent
import javax.sound.sampled.LineListener
import kotlin.coroutines.resume

private class JvmSoundResource(bytes: ByteArray) : SoundResource {
    private val clip: Clip = AudioSystem.getClip().also { clip ->
        try {
            AudioSystem.getAudioInputStream(ByteArrayInputStream(bytes)).use { input ->
                clip.open(input)
            }
        } catch (cause: Throwable) {
            clip.close()
            throw cause
        }
    }

    private val playMutex = Mutex()
    private val clipLock = Any()

    @Volatile
    private var closed = false

    override suspend fun play() = playMutex.withLock {
        suspendCancellableCoroutine { continuation ->
            val completed = AtomicBoolean(false)
            val listener = object : LineListener {
                override fun update(event: LineEvent) {
                    if (event.type != LineEvent.Type.STOP || !completed.compareAndSet(false, true)) {
                        return
                    }

                    synchronized(clipLock) {
                        clip.removeLineListener(this)
                    }
                    continuation.resume(Unit)
                }
            }

            continuation.invokeOnCancellation {
                if (!completed.compareAndSet(false, true)) return@invokeOnCancellation

                synchronized(clipLock) {
                    clip.removeLineListener(listener)
                    if (!closed) clip.stop()
                }
            }

            synchronized(clipLock) {
                check(!closed) { "Sound resource is closed" }
                if (!continuation.isActive) return@suspendCancellableCoroutine

                clip.stop()
                clip.framePosition = 0
                clip.addLineListener(listener)

                try {
                    clip.start()
                } catch (cause: Throwable) {
                    clip.removeLineListener(listener)
                    if (completed.compareAndSet(false, true)) {
                        continuation.resumeWith(Result.failure(cause))
                    }
                }
            }
        }
    }

    override fun close() {
        synchronized(clipLock) {
            if (closed) return

            closed = true
            clip.stop()
            clip.close()
        }
    }
}

actual fun SoundResource(res: ByteArray): SoundResource = JvmSoundResource(res)