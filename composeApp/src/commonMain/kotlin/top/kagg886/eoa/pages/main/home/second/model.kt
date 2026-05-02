package top.kagg886.eoa.pages.main.home.second

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.CompletableDeferred
import org.orbitmvi.orbit.Container
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.annotation.OrbitExperimental
import org.orbitmvi.orbit.syntax.Syntax
import org.orbitmvi.orbit.viewmodel.container
import top.kagg886.backend.config.AppLoginPropertiesMMKV
import top.kagg886.backend.config.AppSecondClassMMKV
import top.kagg886.backend.database.AppDatabase
import top.kagg886.eoa.second.SecondClassData
import top.kagg886.eoa.second.SecondClassDataSummary
import top.kagg886.eoa.second.TWUser
import top.kagg886.eoa.util.SnackBarType
import top.kagg886.eoa.vpn.VPNClient
import top.kagg886.eoa.vpn.bean.CaptchaReturn
import top.kagg886.util.asTaggedLogger

/**
 * ================================================
 * Author:     886kagg
 * Created on: 2025/9/20 15:10
 * ================================================
 */

class SecondClassModel(
    database: AppDatabase
) : ViewModel(), ContainerHost<SecondClassState, SecondClassSideEffect> {
    private val log = "SecondClassModel".asTaggedLogger
    private val secondClassDao = database.secondClassDao()

    override val container: Container<SecondClassState, SecondClassSideEffect> = container(SecondClassState.Initial) {
        val cache = secondClassDao.all()

        if (cache.isNotEmpty()) {
            reduce { SecondClassState.Success(cache) }
        }

        if (AppSecondClassMMKV.vpnPassword.isBlank() || AppSecondClassMMKV.twPassword.isBlank()) { //初始情况直接跳转到配置页面
            if (cache.isEmpty()) {
                reduce { SecondClassState.RequireLogin("", "") }
            }
            return@container
        }

        if (cache.isEmpty()) {
            reduce {
                SecondClassState.RequireLogin(
                    AppSecondClassMMKV.vpnPassword,
                    AppSecondClassMMKV.twPassword
                )
            }
        }
        login().join()
    }

    fun exit() = intent {
        AppSecondClassMMKV.clear()
        secondClassDao.clear()
        postSideEffect(SecondClassSideEffect.Toast(level = SnackBarType.Warning, "退出成功"))
        reduce { SecondClassState.RequireLogin("", "") }
    }

    @OptIn(OrbitExperimental::class)
    fun login(
        vPassword: String = AppSecondClassMMKV.vpnPassword,
        tPassword: String = AppSecondClassMMKV.twPassword
    ) = intent {
        runOn<SecondClassState.RequireLogin> {
            reduce { state.copy(vpn = vPassword, tw = tPassword, progress = true) }
        }
        log.i("开始登录VPN")
        val vpn = VPNClient(
            AppLoginPropertiesMMKV.username,
            vPassword
        ).apply { addCloseable(this) }

        try {
            vpn.login { background, slider ->
                log.i("处理滑动验证码")
                CompletableDeferred<CaptchaReturn?>().apply {
                    postSideEffect(SecondClassSideEffect.RequireCaptcha(background, slider, this))
                }.await().apply { log.i("滑动验证码处理完成: $this") }
            }
        } catch (e: Throwable) {
            log.e("无法登录到 VPN", e)
            postSideEffect(
                SecondClassSideEffect.Toast(
                    SnackBarType.Error,
                    "无法登录到校园VPN，原因：${e.message ?: "未知错误"} \n详情请参考日志。"
                )
            )
            reduceLoginState(vPassword, tPassword)
            return@intent
        }

        val portal = try {
            vpn.portal()
                .first { it.name == "团委第二课堂系统" }
                .redirect
        } catch (e: Throwable) {
            log.e("无法获取团委网入口", e)
            postSideEffect(
                SecondClassSideEffect.Toast(
                    SnackBarType.Error,
                    "无法获取团委网入口，原因：${e.message ?: "未知错误"} \n详情请参考日志。"
                )
            )
            reduceLoginState(vPassword, tPassword)
            return@intent
        }

        val tw = TWUser(
            baseURL = "https://webvpn.sylu.edu.cn${portal.substringBefore("UserLogin.aspx")}",
            user = AppLoginPropertiesMMKV.username,
            ticket = vpn.ticket()
        ).apply { addCloseable(this) }


        try {
            tw.login(tPassword)
        } catch (e: Throwable) {
            log.e("无法登录到 团委网", e)
            postSideEffect(
                SecondClassSideEffect.Toast(
                    SnackBarType.Error,
                    "无法登录到团委网，原因：${e.message ?: "未知错误"} \n详情请参考日志。"
                )
            )
            reduceLoginState(vPassword, tPassword)
            return@intent
        }

        val data = try {
            tw.getData()
        } catch (e: Throwable) {
            log.e("无法获取第二课堂数据", e)
            postSideEffect(
                SecondClassSideEffect.Toast(
                    SnackBarType.Error,
                    "无法获取第二课堂数据，原因：${e.message ?: "未知错误"} \n详情请参考日志。"
                )
            )
            reduceLoginState(vPassword, tPassword)
            return@intent
        }

        secondClassDao.replaceAll(data)
        reduce { SecondClassState.Success(data) }
        AppSecondClassMMKV.vpnPassword = vPassword
        AppSecondClassMMKV.twPassword = tPassword
    }

    @OptIn(OrbitExperimental::class)
    private suspend fun Syntax<SecondClassState, SecondClassSideEffect>.reduceLoginState(
        vPassword: String,
        tPassword: String
    ) {
        runOn<SecondClassState.RequireLogin> {
            reduce { state.copy(vpn = vPassword, tw = tPassword, progress = false) }
        }
    }
}


sealed interface SecondClassState {
    data object Initial : SecondClassState

    data class RequireLogin(val vpn: String, val tw: String, val progress: Boolean = false) : SecondClassState

    data class Success(val value: Map<SecondClassDataSummary, List<SecondClassData>>) : SecondClassState
}


sealed interface SecondClassSideEffect {
    data class Toast(val level: SnackBarType = SnackBarType.Info, val message: String) : SecondClassSideEffect
    data class RequireCaptcha(
        val background: ByteArray,
        val slider: ByteArray,
        val callback: CompletableDeferred<CaptchaReturn?>
    ) : SecondClassSideEffect {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is RequireCaptcha) return false

            if (!background.contentEquals(other.background)) return false
            if (!slider.contentEquals(other.slider)) return false
            if (callback != other.callback) return false

            return true
        }

        override fun hashCode(): Int {
            var result = background.contentHashCode()
            result = 31 * result + slider.contentHashCode()
            result = 31 * result + callback.hashCode()
            return result
        }
    }
}
