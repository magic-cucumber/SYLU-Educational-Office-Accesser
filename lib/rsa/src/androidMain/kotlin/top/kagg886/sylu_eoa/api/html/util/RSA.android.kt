package top.kagg886.sylu_eoa.api.html.util

actual val RSA: RSAPlatform by lazy {
    System.loadLibrary("rsa")
    val internalRSA = NativeRSA()
    object : RSAPlatform {
        override fun encrypt(plaintext: String, exponent: String, moduls: String): String {
            return internalRSA.encrypt(plaintext, exponent, moduls)
        }
    }
}
