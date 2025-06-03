package top.kagg886.sylu_eoa.api.html.util
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.cstr
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.toKString
@OptIn(ExperimentalForeignApi::class)
internal actual val RSA: RSAPlatform by lazy {
    object : RSAPlatform {
        override fun encrypt(plaintext: String, exponent: String, modulus: String): String = memScoped {
            val pointer = eoa.rsa_encrypt_unsafe(
                plaintext.cstr.ptr,
                exponent.cstr.ptr,
                modulus.cstr.ptr
            )!!
            val str = memScoped {
                pointer.toKString()
            }
            eoa.free_string(pointer)
            return str
        }
    }
}
