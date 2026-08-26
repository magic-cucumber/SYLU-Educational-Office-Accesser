import kotlinx.coroutines.runBlocking
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.SYSTEM
import top.kagg886.util.sound.SoundResource
import kotlin.test.Test

class SoundTest {

    @Test
    fun loadsTestWav() = runBlocking {
        val wav = sequenceOf("test.wav".toPath()).firstOrNull(FileSystem.SYSTEM::exists)
            ?: error("test.wav not found in lib/sound or the sound module directory")

        val resource = SoundResource(FileSystem.SYSTEM.read(wav) { readByteArray() })
        try {
            println("play next")
            resource.play()
            println("play next")
            resource.play()
        } finally {
            resource.close()
        }
    }
}
