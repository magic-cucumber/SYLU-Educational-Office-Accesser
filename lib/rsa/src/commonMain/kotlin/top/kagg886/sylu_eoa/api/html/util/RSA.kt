package top.kagg886.sylu_eoa.api.html.util

import dev.whyoleg.cryptography.CryptographyProvider
import dev.whyoleg.cryptography.CryptographyProviderApi
import dev.whyoleg.cryptography.DelicateCryptographyApi
import dev.whyoleg.cryptography.algorithms.SHA512
import dev.whyoleg.cryptography.providers.base.materials.JsonWebKeys
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import dev.whyoleg.cryptography.algorithms.RSA as InternalRSA

object RSA {
    @OptIn(CryptographyProviderApi::class, DelicateCryptographyApi::class, ExperimentalEncodingApi::class)
    fun encrypt(plaintext: String, exponent: String, modulus: String): String {
        val cipher = CryptographyProvider.Default.get(InternalRSA.PKCS1)
        val jwk = JsonWebKeys.encodeRsaPublicKey(
            algorithmId = InternalRSA.PKCS1,
            digest = null,
            n = Base64.decode(modulus),
            e = Base64.decode(exponent),
        )

        val pubKey = cipher.publicKeyDecoder(SHA512).decodeFromByteArrayBlocking(
            format = InternalRSA.PublicKey.Format.JWK,
            bytes = jwk,
        )

        return pubKey.encrypt(plaintext)
    }

    @OptIn(DelicateCryptographyApi::class, ExperimentalEncodingApi::class)
    fun encrypt(plaintext: String, x509: String): String {
        val cipher = CryptographyProvider.Default.get(InternalRSA.PKCS1)
        val pubKey = cipher.publicKeyDecoder(SHA512).decodeFromByteArrayBlocking(
            format = InternalRSA.PublicKey.Format.DER.Generic,
            bytes = Base64.decode(x509),
        )

        return pubKey.encrypt(plaintext)
    }
}

@OptIn(DelicateCryptographyApi::class)
private fun InternalRSA.PKCS1.PublicKey.encrypt(plaintext: String): String {
    return Base64.encode(encryptor().encryptBlocking(plaintext.encodeToByteArray()))
}
