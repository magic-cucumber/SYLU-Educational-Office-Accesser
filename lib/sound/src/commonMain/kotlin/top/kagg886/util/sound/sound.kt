package top.kagg886.util.sound

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext

interface SoundResource : AutoCloseable {
    suspend fun play()
}

@Composable
fun rememberSoundResource(res: ByteArray): State<SoundResource?> {
    val state = remember(res) { mutableStateOf<SoundResource?>(null) }

    LaunchedEffect(res) {
        val resource = withContext(Dispatchers.IO + NonCancellable) {
            SoundResource(res.copyOf())
        }

        if (isActive) {
            state.value = resource
        } else {
            resource.close()
        }
    }

    DisposableEffect(state) {
        onDispose {
            state.value?.close()
            state.value = null
        }
    }

    return state
}


expect fun SoundResource(res: ByteArray): SoundResource
