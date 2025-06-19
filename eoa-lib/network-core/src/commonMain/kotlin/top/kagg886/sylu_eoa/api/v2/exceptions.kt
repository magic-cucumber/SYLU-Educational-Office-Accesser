package top.kagg886.sylu_eoa.api.v2

/**
 * 客户端异常
 * @author kagg886
 */
sealed class EOAClientException(message: String,cause: Throwable? = null): Exception(message,cause)

/**
 * 用户名和密码错误
 *
 * 当 用户名和密码 错误时, 会抛出此异常
 *
 * 此外，若用户在外部修改了密码，调用其他API，也会抛出此异常。
 * @author kagg886
 */
class InvalidCredentialsException(): EOAClientException("用户名和密码错误")

/**
 * 登录次数过多
 *
 * 当 重复次数过多时(一般为5次)。
 *
 * 此时会抛出这个错误。
 */
class RetryLimitException(): EOAClientException("登录次数过多，请稍后再试。")

/**
 * 需要验证码
 *
 * 当需要验证码时, 会抛出此异常
 *
 * 当且仅当 [EOAClient.login] 时, 会抛出此异常
 * @author kagg886
 */
class NeedCaptchaException(): EOAClientException("需要验证码")
/**
 * 未知错误
 *
 * 当出现未知错误时, 会抛出此异常
 *
 * @author kagg886
 */
class UnknownException(message: String,cause: Throwable?=null): EOAClientException(message = message,cause = cause)
