package top.kagg886.sylu_eoa.api.v3.util
import eoa.rsa_encrypt_unsafe
import eoa.free_string
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.cstr
import kotlinx.cinterop.memScoped

@OptIn(ExperimentalForeignApi::class)
actual val RSA: RSAPlatform by lazy {
    object : RSAPlatform {
        override fun encrypt(plaintext: String, exponent: String, modulus: String): String = memScoped {
            val pointer = rsa_encrypt_unsafe(
                plaintext.cstr.ptr,
                exponent.cstr.ptr,
                modulus.cstr.ptr
            )
            val str = pointer.toKString()
            free_string(pointer)
            return str
        }
    }
}
