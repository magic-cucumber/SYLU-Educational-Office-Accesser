package top.kagg886.report

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.orbitmvi.orbit.OrbitContainer
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.viewmodel.orbitContainer
import top.kagg886.backend.database.AppDatabase
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class AppModel(private val database: AppDatabase) : ViewModel(), OrbitContainerHost<AppModelState, AppModelState, Unit> {
    override val container: OrbitContainer<AppModelState, AppModelState, Unit> =
        orbitContainer(AppModelState.Initializing) {
            delay(3.seconds)
            val progress = MutableStateFlow<Float?>(null)
            val label = MutableStateFlow("正在查询解决方案...")
            val success = MutableStateFlow(false)
            reduce { AppModelState.CrashAutoUpload(progress,label,success) }
            delay(3.seconds)
            progress.emit(0.0f)
            label.emit("正在分析崩溃日志")

            coroutineScope {
                launch {
                    for (i in 1..100) {
                        progress.emit(i / 100f)
                        delay(50.milliseconds)
                    }
                }

                launch {
                    delay(1.seconds)
                    label.emit("正在打包崩溃日志...")

                    delay(1.seconds)
                    label.emit("正在上传崩溃日志...")
                }
            }

            success.emit(true)
            label.emit("上传成功，您可以安全重启了")
        }

}


sealed interface AppModelState {
    data object Initializing : AppModelState
    data object CrashManually : AppModelState
    data class CrashAutoUpload(val progress: MutableStateFlow<Float?>, val label: MutableStateFlow<String>,val success: MutableStateFlow<Boolean>): AppModelState
}