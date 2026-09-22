package top.kagg886.eoa.pages.main.home.second

import top.kagg886.eoa.util.BaseViewModel
import io.ktor.network.sockets.*
import kotlinx.coroutines.*
import org.orbitmvi.orbit.syntax.Syntax
import org.orbitmvi.orbit.annotation.OrbitExperimental
import top.kagg886.backend.config.AppLoginPropertiesMMKV
import top.kagg886.backend.config.AppSecondClassMMKV
import top.kagg886.backend.database.AppDatabase
import top.kagg886.eoa.config.BuildConfig
import top.kagg886.eoa.second.SecondClassData
import top.kagg886.eoa.second.SecondClassDataSummary
import top.kagg886.eoa.second.TWUser
import top.kagg886.eoa.util.SnackBarType
import top.kagg886.eoa.vpn.VPNClient
import top.kagg886.eoa.vpn.bean.CaptchaReturn
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
) : BaseViewModel<SecondClassState, SecondClassSideEffect>(name = "SecondClassModel", initial = SecondClassState.Initial) {
    private val secondClassDao = database.secondClassDao()

    override suspend fun Syntax<SecondClassState, SecondClassSideEffect>.init() {
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
            return
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
         * 0. 测试返回测试数据即可。
         * 1. vPassword存在时，同时使用vpn，内网模式并行登录，取最快返回的结果(如果已连接内网，则一般内网比外网快)。
         * 2. vPassword不存在时，只使用内网模式登录。
         */
        val data = try {
            when {
                AppLoginPropertiesMMKV.clientId == "top.kagg886.sylu_eoa.api.test.EOATestClientProvider" -> loginDemo(tPassword,vPassword)
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

            logger.e("无法获取第二课堂数据", e)
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

    @OptIn(OrbitExperimental::class)
    private suspend fun Syntax<SecondClassState, SecondClassSideEffect>.loginDemo(
        tPassword: String,
        vPassword: String
    ): Map<SecondClassDataSummary, List<SecondClassData>> {
        if (vPassword == "captcha") {
            while (true) {
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
                    reduce { state.copy(additional = null) }
                }
                runOn<SecondClassState.Success<*>> {
                    reduce { state.copy(additional = null) }
                }

                if (code == 123456) break
            }
        } else if (vPassword != "test" && vPassword.isNotBlank()) {
            error("模拟统一认证密码错误")
        }

        if (tPassword != "test" && tPassword != "captcha") {
            error("模拟团委网密码错误")
        }

        val map = mapOf<SecondClassDataSummary, List<SecondClassData>>(
            SecondClassDataSummary("思想成长", 20.0) to listOf(
                SecondClassData("学习贯彻党的二十大精神专题讲座", "学生工作部", "2025-03-12", "学生", 180, 2.0),
                SecondClassData("大学生职业生涯规划讲座", "学生处", "2025-04-08", "学生", 120, 1.5),
                SecondClassData("心理健康教育主题活动", "大学生心理健康教育中心", "2025-05-16", "学生", 80, 3.0),
                SecondClassData("国家安全教育日知识竞赛", "保卫处", "2025-06-04", "学生", 260, 2.5)
            ),
            SecondClassDataSummary("实践实习", 20.0) to listOf(
                SecondClassData("暑期社会实践优秀成果分享会", "校团委", "2025-07-10", "学生", 95, 1.0),
                SecondClassData("企业参观与职业体验活动", "创新创业学院", "2025-09-20", "学生", 45, 2.0),
                SecondClassData("专业认知实习成果展示", "机械工程学院", "2025-10-18", "学生", 70, 1.5)
            ),
            SecondClassDataSummary("创新创业", 20.0) to listOf(
                SecondClassData("大学生创新创业训练计划宣讲", "创新创业学院", "2025-03-20", "学生", 150, 4.5),
                SecondClassData("校园创新创意大赛", "校团委", "2025-04-25", "学生", 60, 5.0),
                SecondClassData("互联网+大学生创新创业大赛培训", "创新创业学院", "2025-05-09", "学生", 110, 4.0),
                SecondClassData("科技作品交流展示活动", "教务处", "2025-06-13", "学生", 75, 4.0),
                SecondClassData("创业项目路演训练营", "创新创业学院", "2025-11-07", "学生", 35, 4.5)
            ),
            SecondClassDataSummary("志愿公益", 20.0) to listOf(
                SecondClassData("校园迎新志愿服务", "校团委", "2025-09-01", "志愿者", 200, 4.0),
                SecondClassData("社区敬老志愿服务", "青年志愿者协会", "2025-09-27", "志愿者", 30, 5.0),
                SecondClassData("无偿献血宣传志愿活动", "校医院", "2025-10-24", "志愿者", 50, 6.0),
                SecondClassData("校园环保公益行动", "后勤管理处", "2025-11-15", "志愿者", 90, 5.0)
            ),
            SecondClassDataSummary("文体活动", 20.0) to listOf(
                SecondClassData("校园春季运动会", "体育部", "2025-04-18", "运动员", 800, 5.0),
                SecondClassData("校园十佳歌手大赛", "校团委", "2025-05-23", "参赛者", 120, 6.0),
                SecondClassData("篮球联赛", "体育部", "2025-10-10", "运动员", 160, 4.0),
                SecondClassData("校园文化艺术节", "校团委", "2025-11-21", "参演者", 300, 5.0)
            )
        )

        return mapOf(
            *map.entries.map { it.key to it.value }.toTypedArray(),
            SecondClassDataSummary("All", map.keys.maxOf { it.max }) to map.values.flatten()
        )
    }

    private suspend fun loginByInternal(
        tPassword: String,
        timeout: Duration
    ): Map<SecondClassDataSummary, List<SecondClassData>> = withTimeout(timeout) {
        logger.i("开始内网登录")
        val tw = TWUser(
            baseURL = "http://xg.${BuildConfig.MESSAGE_API_ENDPOINT}/SyluTW/Sys/",
            user = AppLoginPropertiesMMKV.username,
        ).apply { addCloseable(this) }

        tw.login(tPassword)

        logger.i("内网登录完成，开始获取信息")
        val data = tw.getData()
        logger.i("成功获取二课数据。来源：内网")

        data
    }

    @OptIn(OrbitExperimental::class)
    private suspend fun Syntax<SecondClassState, SecondClassSideEffect>.loginByVpn(
        vPassword: String,
        tPassword: String,
        timeout: Duration,
    ): Map<SecondClassDataSummary, List<SecondClassData>> = withTimeout(timeout) {
        logger.i("开始登录VPN")
        val vpn = VPNClient(
            AppLoginPropertiesMMKV.username,
            vPassword
        ).apply { addCloseable(this) }

        vpn.login(
            totpHandler = {
                logger.i("处理TOTP二次验证")
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
                logger.i("TOTP二次验证处理完成")
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
                logger.i("处理滑动验证码")
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
                logger.i("滑动验证码处理完成: $this")
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

        logger.d("成功登录VPN")

        if (timeout == Duration.ZERO) {
            throw SocketTimeoutException("timeout.")
        }

        val tw = TWUser(
            baseURL = "https://webvpn.${BuildConfig.MESSAGE_API_ENDPOINT}${portal.substringBefore("UserLogin.aspx")}",
            user = AppLoginPropertiesMMKV.username,
            ticket = vpn.ticket()
        ).apply { addCloseable(this) }

        tw.login(tPassword)
        logger.i("VPN登录完成，开始获取信息")
        val data = tw.getData()
        logger.i("成功获取二课数据。来源：VPN")

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
