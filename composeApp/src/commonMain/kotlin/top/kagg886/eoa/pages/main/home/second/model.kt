package top.kagg886.eoa.pages.main.home.second

import androidx.lifecycle.ViewModel
import io.ktor.network.sockets.*
import kotlinx.coroutines.*
import org.orbitmvi.orbit.OrbitContainer
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.annotation.OrbitExperimental
import org.orbitmvi.orbit.syntax.Syntax
import org.orbitmvi.orbit.viewmodel.orbitContainer
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
import top.kagg886.util.race
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * ================================================
 * Author:     886kagg
 * Created on: 2025/9/20 15:10
 * ================================================
 */

class SecondClassModel(
    database: AppDatabase
) : ViewModel(), OrbitContainerHost<SecondClassState, SecondClassState, SecondClassSideEffect> {
    private val log = "SecondClassModel".asTaggedLogger
    private val secondClassDao = database.secondClassDao()

    override val container: OrbitContainer<SecondClassState, SecondClassState, SecondClassSideEffect> = orbitContainer(SecondClassState.Initial) {
        val cache = secondClassDao.all()

        if (cache.isNotEmpty()) {
            reduce { SecondClassState.Success<Nothing>(false, cache) }
        }

        //twPassword为blank时跳转到配置页面
        if (AppSecondClassMMKV.twPassword.isBlank()) {
            if (cache.isEmpty()) {
                reduce {
                    SecondClassState.RequireLogin<Nothing>(
                        AppSecondClassMMKV.vpnPassword,
                        AppSecondClassMMKV.twPassword
                    )
                }
            }
            return@orbitContainer
        }
        login().join()
    }

    fun exit() = intent {
        AppSecondClassMMKV.clear()
        secondClassDao.clear()
        postSideEffect(SecondClassSideEffect.Toast(level = SnackBarType.Warning, "退出成功"))
        reduce { SecondClassState.RequireLogin<Nothing>("", "") }
    }

    @OptIn(OrbitExperimental::class)
    fun login(
        vPassword: String = AppSecondClassMMKV.vpnPassword,
        tPassword: String = AppSecondClassMMKV.twPassword,
    ) = intent {
        if (tPassword.isBlank()) {
            postSideEffect(SecondClassSideEffect.Toast(level = SnackBarType.Warning, message = "团委网密码不能为空"))
            return@intent
        }

        runOn<SecondClassState.RequireLogin<*>> {
            reduce { state.copy(vpn = vPassword, tw = tPassword, progress = true) }
        }
        runOn<SecondClassState.Success<*>> {
            reduce { state.copy(loading = true) }
        }

        /**
         * 1. vPassword存在时，同时使用vpn，内网模式并行登录，取最快返回的结果(如果已连接内网，则一般内网比外网快)。
         * 2. vPassword不存在时，只使用内网模式登录。
         */
        val data = try {
            when {
                vPassword.isBlank() -> loginByInternal(tPassword, 20.seconds)
                else -> supervisorScope {
                    race(
                        async { loginByInternal(tPassword, 5.seconds) },
                        async { loginByVpn(vPassword, tPassword, 20.seconds) }
                    )
                }
            }
        } catch (e: Throwable) {
            if (e is CancellationException) throw e

            log.e("无法获取第二课堂数据", e)
            postSideEffect(
                SecondClassSideEffect.Toast(
                    SnackBarType.Error,
                    "无法获取第二课堂数据，原因：${e.message ?: "未知错误"} \n详情请参考日志。"
                )
            )
            runOn<SecondClassState.RequireLogin<*>> {
                reduce { state.copy(vpn = vPassword, tw = tPassword, progress = false, additional = null) }
            }
            runOn<SecondClassState.Success<*>> {
                reduce {
                    state.copy(loading = false, additional = null)
                }
            }
            return@intent
        }

        secondClassDao.replaceAll(data)
        reduce { SecondClassState.Success<Nothing>(false, data) }
        AppSecondClassMMKV.vpnPassword = vPassword
        AppSecondClassMMKV.twPassword = tPassword
    }

    private suspend fun loginByInternal(
        tPassword: String,
        timeout: Duration
    ): Map<SecondClassDataSummary, List<SecondClassData>> = withTimeout(timeout) {
        log.i("开始内网登录")
        val tw = TWUser(
            baseURL = "http://xg.sylu.edu.cn/SyluTW/Sys/",
            user = AppLoginPropertiesMMKV.username,
        ).apply { addCloseable(this) }

        tw.login(tPassword)

        log.i("内网登录完成，开始获取信息")
        val data = tw.getData()
        log.i("成功获取二课数据。来源：内网")

        data
    }

    @OptIn(OrbitExperimental::class)
    private suspend fun Syntax<SecondClassState, SecondClassSideEffect>.loginByVpn(
        vPassword: String,
        tPassword: String,
        timeout: Duration,
    ): Map<SecondClassDataSummary, List<SecondClassData>> = withTimeout(timeout) {
        log.i("开始登录VPN")
        val vpn = VPNClient(
            AppLoginPropertiesMMKV.username,
            vPassword
        ).apply { addCloseable(this) }

        vpn.login(
            totpHandler = {
                log.i("处理TOTP二次验证")
                val deferred = CompletableDeferred<Int?>()
                runOn<SecondClassState.RequireLogin<SecondClassState.TOTPAcceptable>> {
                    reduce {
                        state.copy(additional = SecondClassState.RequireLogin.TOTP(deferred))
                    }
                }
                runOn<SecondClassState.Success<SecondClassState.TOTPAcceptable>> {
                    reduce {
                        state.copy(additional = SecondClassState.Success.TOTP(deferred))
                    }
                }
                val code = deferred.await()
                log.i("TOTP二次验证处理完成")
                runOn<SecondClassState.RequireLogin<*>> {
                    reduce {
                        state.copy(additional = null)
                    }
                }
                runOn<SecondClassState.Success<*>> {
                    reduce {
                        state.copy(additional = null)
                    }
                }

                code
            },
            captchaHandler = { background, slider ->
                log.i("处理滑动验证码")
                val deferred = CompletableDeferred<CaptchaReturn?>()
                runOn<SecondClassState.RequireLogin<SecondClassState.CaptchaAcceptable>> {
                    reduce {
                        state.copy(additional = SecondClassState.RequireLogin.Captcha(deferred, slider, background))
                    }
                }
                runOn<SecondClassState.Success<SecondClassState.CaptchaAcceptable>> {
                    reduce {
                        state.copy(additional = SecondClassState.Success.Captcha(deferred, slider, background))
                    }
                }
                val result = deferred.await()
                log.i("滑动验证码处理完成: $this")
                runOn<SecondClassState.RequireLogin<*>> {
                    reduce {
                        state.copy(additional = null)
                    }
                }
                runOn<SecondClassState.Success<*>> {
                    reduce {
                        state.copy(additional = null)
                    }
                }

                result
            }
        )
        val portal = vpn.portal()
            .first { it.name == "团委第二课堂系统" }
            .redirect

        log.d("成功登录VPN")

        if (timeout == Duration.ZERO) {
            throw SocketTimeoutException("timeout.")
        }

        val tw = TWUser(
            baseURL = "https://webvpn.sylu.edu.cn${portal.substringBefore("UserLogin.aspx")}",
            user = AppLoginPropertiesMMKV.username,
            ticket = vpn.ticket()
        ).apply { addCloseable(this) }

        tw.login(tPassword)
        log.i("VPN登录完成，开始获取信息")
        val data = tw.getData()
        log.i("成功获取二课数据。来源：VPN")

        data
    }
}


sealed interface SecondClassState {

    sealed interface AdditionalVerify<T> {
        val deferred: CompletableDeferred<T>
    }

    interface TOTPAcceptable : AdditionalVerify<Int?>
    interface CaptchaAcceptable : AdditionalVerify<CaptchaReturn?> {
        val fronted: ByteArray
        val background: ByteArray
    }

    data object Initial : SecondClassState

    data class RequireLogin<T : AdditionalVerify<*>>(
        val vpn: String,
        val tw: String,
        val progress: Boolean = false,
        val additional: T? = null
    ) : SecondClassState {
        data class Captcha(
            override val deferred: CompletableDeferred<CaptchaReturn?>,
            override val fronted: ByteArray,
            override val background: ByteArray
        ) : CaptchaAcceptable {
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (other !is Captcha) return false

                if (deferred != other.deferred) return false
                if (!fronted.contentEquals(other.fronted)) return false
                if (!background.contentEquals(other.background)) return false

                return true
            }

            override fun hashCode(): Int {
                var result = deferred.hashCode()
                result = 31 * result + fronted.contentHashCode()
                result = 31 * result + background.contentHashCode()
                return result
            }
        }

        data class TOTP(override val deferred: CompletableDeferred<Int?>) : TOTPAcceptable
    }

    data class Success<T : AdditionalVerify<*>>(
        val loading: Boolean = false,
        val value: Map<SecondClassDataSummary, List<SecondClassData>>,
        val additional: T? = null
    ) : SecondClassState {
        data class Captcha(
            override val deferred: CompletableDeferred<CaptchaReturn?>,
            override val fronted: ByteArray,
            override val background: ByteArray
        ) : CaptchaAcceptable {
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (other !is Captcha) return false

                if (deferred != other.deferred) return false
                if (!fronted.contentEquals(other.fronted)) return false
                if (!background.contentEquals(other.background)) return false

                return true
            }

            override fun hashCode(): Int {
                var result = deferred.hashCode()
                result = 31 * result + fronted.contentHashCode()
                result = 31 * result + background.contentHashCode()
                return result
            }
        }

        data class TOTP(override val deferred: CompletableDeferred<Int?>) : TOTPAcceptable
    }
}


sealed interface SecondClassSideEffect {
    data class Toast(val level: SnackBarType = SnackBarType.Info, val message: String) : SecondClassSideEffect
}
