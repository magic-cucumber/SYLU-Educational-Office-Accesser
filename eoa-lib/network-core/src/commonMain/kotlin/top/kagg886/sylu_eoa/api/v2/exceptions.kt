package top.kagg886.sylu_eoa.api.v2

/**
 * 客户端异常
 * @author kagg886
 */
sealed class EOAClientException(message: String,cause: Throwable? = null): Exception(message,cause)

/**
 * 无法自动登录的异常
 *
 * 该类异常表示在EOA无法通过自动登录的方式登录，则会抛出。
 *
 * @author kagg886
 */
sealed class InvalidCredentialsException(override val message: String): EOAClientException(message = message)

/**
 * 用户名或密码错误
 *
 * 当用户名或密码错误时, 会抛出此异常
 *
 * 当且仅当 [EOAClient.login] 时, 会抛出此异常
 * @author kagg886
 */
class BadCredentialsException(): InvalidCredentialsException("用户名或密码错误")

/**
 * 需要验证码
 *
 * 当需要验证码时, 会抛出此异常
 *
 * 当且仅当 [EOAClient.login] 时, 会抛出此异常
 * @author kagg886
 */
class NeedCaptchaException(): InvalidCredentialsException("需要验证码")

/**
 * 登录次数过多
 *
 * 当 重复次数过多时(一般为5次)。
 *
 * 此时会抛出这个错误。
 */
class RetryLimitException(override val cause: Throwable?) :
    EOAClientException("登录次数过多，请稍后再试。${cause?.message}", cause = cause)

/**
 * 未知错误
 *
 * 当出现未知错误时, 会抛出此异常
 *
 * @author kagg886
 */
class UnknownException(message: String,cause: Throwable?=null): EOAClientException(message = message,cause = cause)
