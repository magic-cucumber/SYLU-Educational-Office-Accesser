package top.kagg886.eoa.pages.login

import top.kagg886.eoa.util.BaseViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import org.orbitmvi.orbit.syntax.Syntax
import org.orbitmvi.orbit.annotation.OrbitExperimental
import top.kagg886.backend.config.AppLoginPropertiesMMKV
import top.kagg886.eoa.util.SnackBarType
import top.kagg886.sylu_eoa.api.v2.EOAClientProvider

class LoginViewModel : BaseViewModel<LoginViewModelState, LoginSideEffect>(name = "LoginViewModel", initial = LoginViewModelState.Empty) {

    override suspend fun Syntax<LoginViewModelState, LoginSideEffect>.init() {
            if (AppLoginPropertiesMMKV.username.isNotEmpty() && AppLoginPropertiesMMKV.password.isNotEmpty()) {
                postSideEffect(LoginSideEffect.NavigateToMain)
                return
            }
            reduce {
                LoginViewModelState.WaitLogin.Waiting(
                    provider = EOAClientProvider.providers,
                    selected = EOAClientProvider.providers.first { it.id == AppLoginPropertiesMMKV.clientId }
                )
            }
    }

    fun startLogin(username: String, password: String) = intent {
        if (state is LoginViewModelState.WaitLogin.Waiting) {
            val oldState = state as LoginViewModelState.WaitLogin.Waiting
            reduce {
                LoginViewModelState.WaitLogin.Processing("登录中...",  oldState.provider, oldState.selected)
            }
            logger.i("开始登录")
            try {
                AppLoginPropertiesMMKV.client.username = username
                AppLoginPropertiesMMKV.client.password = password
                AppLoginPropertiesMMKV.client.login {
                    logger.w("发现验证码")
                    val defer = CompletableDeferred<String>(viewModelScope.coroutineContext[Job])
                    reduce {
                        LoginViewModelState.WaitLogin.VerifyCode(it, defer,  oldState.provider, oldState.selected)
                    }
                    defer.await()
                }
                AppLoginPropertiesMMKV.username = username
                AppLoginPropertiesMMKV.password = password
                logger.i("登录成功")
                postSideEffect(LoginSideEffect.NavigateToMain)
            } catch (e: Exception) {
                logger.e("登录失败", e)
                postSideEffect(LoginSideEffect.Toast(type = SnackBarType.Error, e.message!!))
                reduce { oldState }
            }
        }
    }

    @OptIn(OrbitExperimental::class)
    fun setLoginClient(provider: EOAClientProvider) = intent {
        runOn<LoginViewModelState.WaitLogin.Waiting> {
            AppLoginPropertiesMMKV.clientId = provider.id
            reduce {
                state.copy(
                    selected = provider,
                )
            }
        }
    }

    @OptIn(OrbitExperimental::class)
    fun processVerifyCode(code: String?) = intent {
        runOn<LoginViewModelState.WaitLogin.VerifyCode> {
            if (code == null) {
                postSideEffect(LoginSideEffect.Toast(SnackBarType.Error, "验证码为空,取消登录"))
                reduce {
                    LoginViewModelState.WaitLogin.Waiting(
                        state.provider,
                        state.selected
                    )
                }
                return@runOn
            }
            state.defer.complete(code)
            reduce {
                LoginViewModelState.WaitLogin.Processing("发送验证码...",state.provider,  state.selected)
            }
        }
    }
}

sealed interface LoginViewModelState {
    data object Empty : LoginViewModelState

    sealed interface WaitLogin : LoginViewModelState {
        val provider: List<EOAClientProvider>
        val selected: EOAClientProvider
        data class Waiting(override val provider: List<EOAClientProvider>,override val selected: EOAClientProvider) :
            WaitLogin

        data class Processing(val toast: String,override val provider: List<EOAClientProvider>,override val selected: EOAClientProvider) : WaitLogin

        data class VerifyCode(
            val data: ByteArray,
            val defer: CompletableDeferred<String>,
            override val provider: List<EOAClientProvider>,override val selected: EOAClientProvider
        ) : WaitLogin {
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (other !is VerifyCode) return false

                if (!data.contentEquals(other.data)) return false

                return true
            }

            override fun hashCode(): Int {
                return data.contentHashCode()
            }
        }
    }
}

sealed interface LoginSideEffect {
    data object NavigateToMain : LoginSideEffect
    data class Toast(val type: SnackBarType, val message: String) : LoginSideEffect
}
