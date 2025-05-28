package top.kagg886.sylu_eoa.api.v3.util

interface RSAPlatform {
    fun encrypt(plaintext: String, exponent: String, modulus: String): String
}

expect val RSA: RSAPlatform
