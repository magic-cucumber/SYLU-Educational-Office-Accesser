package top.kagg886.sylu_eoa.api.html.util

import dev.whyoleg.cryptography.BinarySize.Companion.bits
import dev.whyoleg.cryptography.CryptographyProvider
import dev.whyoleg.cryptography.CryptographyProviderApi
import dev.whyoleg.cryptography.DelicateCryptographyApi
import dev.whyoleg.cryptography.providers.base.materials.JsonWebKeys
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.test.Test
import dev.whyoleg.cryptography.algorithms.RSA as InternalRSA

@OptIn(
    CryptographyProviderApi::class,
    DelicateCryptographyApi::class,
    ExperimentalEncodingApi::class,
)
class RSAPublicKeyTest {
    @Test
    fun printsKeyMaterial() {
        val keyPair = CryptographyProvider.Default
            .get(InternalRSA.PKCS1)
            .keyPairGenerator(2048.bits)
            .generateKeyBlocking()

        val publicKey = JsonWebKeys.decodeRsaPublicKey(
            algorithmId = InternalRSA.PKCS1,
            digest = null,
            jwkKey = keyPair.publicKey.encodeToByteArrayBlocking(InternalRSA.PublicKey.Format.JWK),
        )

        println("exponent=${Base64.encode(publicKey.e)}")
        println("modulus=${Base64.encode(publicKey.n)}")
        println(
            keyPair.privateKey
                .encodeToByteArrayBlocking(InternalRSA.PrivateKey.Format.PEM.PKCS1)
                .decodeToString(),
        )
    }
}
