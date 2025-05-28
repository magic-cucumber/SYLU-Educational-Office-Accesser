package top.kagg886.eoa.pages.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.annotation.OrbitExperimental
import org.orbitmvi.orbit.viewmodel.container
import top.kagg886.backend.config.AppLoginPropertiesMMKV

class LoginViewModel : ViewModel(), ContainerHost<LoginViewModelState, LoginSideEffect> {

    override val container = container<LoginViewModelState, LoginSideEffect>(LoginViewModelState.Empty) {
        if (AppLoginPropertiesMMKV.username.isNotEmpty() && AppLoginPropertiesMMKV.password.isNotEmpty()) {
            postSideEffect(LoginSideEffect.NavigateToMain)
            return@container
        }
        reduce {
            LoginViewModelState.WaitLogin.Waiting
        }
    }

    fun startLogin(username: String, password: String) = intent {
        reduce {
            LoginViewModelState.WaitLogin.Processing("登录中...")
        }

        try {
            AppLoginPropertiesMMKV.client.username = username
            AppLoginPropertiesMMKV.client.password = password
            AppLoginPropertiesMMKV.client.login {
                val defer = CompletableDeferred<String>(viewModelScope.coroutineContext[Job])
                reduce {
                    LoginViewModelState.WaitLogin.VerifyCode(it,defer)
                }
                defer.await()
            }
            AppLoginPropertiesMMKV.username = username
            AppLoginPropertiesMMKV.password = password

            //数据同步等

            postSideEffect(LoginSideEffect.NavigateToMain)
        } catch (e: Exception) {
            // 处理异常
            reduce {
                LoginViewModelState.WaitLogin.Failed(e)
            }
        }
    }

    @OptIn(OrbitExperimental::class)
    fun processVerifyCode(code:String?) = intent {
        runOn<LoginViewModelState.WaitLogin.VerifyCode> {
            if (code == null) {
                reduce {
                    LoginViewModelState.WaitLogin.Failed("拒绝输入验证码")
                }
                return@runOn
            }
            state.defer.complete(code)
            reduce {
                LoginViewModelState.WaitLogin.Processing("发送验证码...")
            }
        }
    }
}

sealed interface LoginViewModelState {
    data object Empty: LoginViewModelState

    sealed interface WaitLogin : LoginViewModelState {
        data object Waiting : WaitLogin
        data class Processing(val toast: String): WaitLogin

        data class VerifyCode(
            val data: ByteArray,
            val defer: CompletableDeferred<String>
        ): WaitLogin {
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

        data class Failed(val msg: String): WaitLogin {
            constructor(e: Throwable): this(e.message ?: "未知错误")
        }
    }
}

sealed interface LoginSideEffect {
    data object NavigateToMain: LoginSideEffect
}
