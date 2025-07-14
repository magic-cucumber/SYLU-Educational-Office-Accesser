package top.kagg886.eoa.pages.update.download

import androidx.lifecycle.ViewModel
import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.utils.io.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.io.readByteArray
import okio.Path
import okio.buffer
import org.orbitmvi.orbit.Container
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.viewmodel.container
import top.kagg886.eoa.util.SnackBarType
import top.kagg886.util.*

/**
 * ================================================
 * Author:     886kagg
 * Created on: 2025/7/14 14:13
 * ================================================
 */
class DownloadModel(private val url: String) : ViewModel(), ContainerHost<DownloadState, DownloadSideEffect> {
    override val container: Container<DownloadState, DownloadSideEffect> = container(DownloadState.Fetching) {
        startDownload().join()
    }

    private val client = HttpClient {
        install(ContentNegotiation) {
            json()
        }
    }

    override fun onCleared() = client.close()

    private fun startDownload() = intent {
        reduce { DownloadState.Fetching }
        val path = withContext(Dispatchers.IO) {
            client.prepareGet(url).execute { response ->
                if (!response.status.isSuccess()) {
                    return@execute null
                }
                val length = response.contentLength()
                val input = response.bodyAsChannel().counted()

                val path = cachePath.resolve("latest.apk").apply {
                    if (exists()) {
                        delete()
                    }
                    createNewFile()
                }

                path.sink().buffer().use { out ->
                    while (!input.isClosedForRead) {
                        val packet = input.readRemaining(DEFAULT_BUFFER_SIZE.toLong())
                        out.write(packet.readByteArray())
                        length?.let {
                            reduce { DownloadState.Progressing(input.totalBytesRead / it.toFloat()) }
                        }
                    }
                    out.flush()
                }
                path
            }
        }

        if (path == null) {
            postSideEffect(
                DownloadSideEffect.NavigateBack(
                    "下载失败。请检查日志",
                    SnackBarType.Error
                )
            )
            return@intent
        }

        reduce {
            DownloadState.Success(
                path = path
            )
        }
    }
}


sealed interface DownloadState {
    data object Fetching : DownloadState
    data class Progressing(val progress: Float) : DownloadState
    data class Success(val path: Path) : DownloadState
}

sealed interface DownloadSideEffect {
    data class NavigateBack(val msg: String, val type: SnackBarType = SnackBarType.Info) :
        DownloadSideEffect
}
