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
            reduce { SecondClassState.Success<Nothing>(false, cache) }
        }

        if (AppSecondClassMMKV.vpnPassword.isBlank() || AppSecondClassMMKV.twPassword.isBlank()) { //初始情况直接跳转到配置页面
            if (cache.isEmpty()) {
                reduce {
                    SecondClassState.RequireLogin<Nothing>(
                        AppSecondClassMMKV.vpnPassword,
                        AppSecondClassMMKV.twPassword
                    )
                }
            }
            return@container
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
        @OptIn(OrbitExperimental::class)
        suspend fun Syntax<SecondClassState, SecondClassSideEffect>.cleanLoading(
            vPassword: String,
            tPassword: String
        ) {
            runOn<SecondClassState.RequireLogin<*>> {
                reduce { state.copy(vpn = vPassword, tw = tPassword, progress = false) }
            }

            runOn<SecondClassState.Success<*>> {
                reduce {
                    state.copy(loading = false, additional = null)
                }
            }
        }


        runOn<SecondClassState.RequireLogin<*>> {
            reduce { state.copy(vpn = vPassword, tw = tPassword, progress = true) }
        }
        runOn<SecondClassState.Success<*>> {
            reduce { state.copy(loading = true) }
        }
        log.i("开始登录VPN")
        val vpn = VPNClient(
            AppLoginPropertiesMMKV.username,
            vPassword
        ).apply { addCloseable(this) }

        try {
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
                    log.i("TOTP二次验证处理完成")

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
        } catch (e: Throwable) {
            log.e("无法登录到 VPN", e)
            postSideEffect(
                SecondClassSideEffect.Toast(
                    SnackBarType.Error,
                    "无法登录到校园VPN，原因：${e.message ?: "未知错误"} \n详情请参考日志。"
                )
            )
            cleanLoading(vPassword, tPassword)
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
            cleanLoading(vPassword, tPassword)
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
            cleanLoading(vPassword, tPassword)
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
            cleanLoading(vPassword, tPassword)
            return@intent
        }

        secondClassDao.replaceAll(data)
        reduce { SecondClassState.Success<Nothing>(false, data) }
        AppSecondClassMMKV.vpnPassword = vPassword
        AppSecondClassMMKV.twPassword = tPassword
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
