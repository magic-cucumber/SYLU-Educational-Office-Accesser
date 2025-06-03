package top.kagg886.sylu_eoa.api.html.util

internal interface RSAPlatform {
    fun encrypt(plaintext: String, exponent: String, modulus: String): String
}

internal expect val RSA: RSAPlatform
