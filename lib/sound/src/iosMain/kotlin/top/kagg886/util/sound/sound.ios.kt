package top.kagg886.util.sound

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import platform.AVFAudio.AVAudioPlayer
import platform.Foundation.NSData
import platform.Foundation.dataWithBytes

@OptIn(ExperimentalForeignApi::class)
private class IosSoundResource(bytes: ByteArray) : SoundResource {
    private val player = AVAudioPlayer(data = bytes.toNSData(), error = null)
        ?: error("Unable to create AVAudioPlayer")

    private val playMutex = Mutex()
    private var closed = false

    init {
        check(player.prepareToPlay()) { "Unable to prepare AVAudioPlayer" }
    }

    override suspend fun play() = playMutex.withLock {
        check(!closed) { "Sound resource is closed" }

        player.stop()
        player.currentTime = 0.0
        check(player.play()) { "Unable to start AVAudioPlayer" }

        try {
            while (player.playing) {
                delay(10)
            }
        } finally {
            if (!currentCoroutineContext().isActive) {
                player.stop()
            }
        }
    }

    override fun close() {
        if (closed) return

        closed = true
        player.stop()
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun ByteArray.toNSData(): NSData {
    if (isEmpty()) return NSData.dataWithBytes(bytes = null, length = 0uL)

    return usePinned { pinned ->
        NSData.dataWithBytes(
            bytes = pinned.addressOf(0),
            length = size.toULong(),
        )
    }
}

actual fun SoundResource(res: ByteArray): SoundResource = IosSoundResource(res)