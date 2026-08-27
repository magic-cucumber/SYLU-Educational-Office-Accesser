package top.kagg886.sylu_eoa.api.html.util

import dev.whyoleg.cryptography.BinarySize.Companion.bits
import dev.whyoleg.cryptography.CryptographyProviderApi
import dev.whyoleg.cryptography.DelicateCryptographyApi
import dev.whyoleg.cryptography.CryptographyProvider
import dev.whyoleg.cryptography.providers.base.materials.JsonWebKeys
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import okio.Buffer
import okio.buffer
import okio.use
import dev.whyoleg.cryptography.algorithms.RSA as InternalRSA

@OptIn(CryptographyProviderApi::class, DelicateCryptographyApi::class)
private val keyPair = CryptographyProvider.Default
    .get(InternalRSA.PKCS1)
    .keyPairGenerator(2048.bits)
    .generateKeyBlocking()

@OptIn(CryptographyProviderApi::class, DelicateCryptographyApi::class)
class RsaSinkTest {
    @Test
    fun encryptsRandomDataWithoutBlockSplitting() {
        val plaintext = Random.nextBytes(ByteArray(64))
        val ciphertext = encrypt(keyPair.publicKey, plaintext)

        assertEquals(rsaBlockSize(keyPair.publicKey), ciphertext.size)
        assertDecryptedEquals(plaintext, decrypt(keyPair.privateKey, ciphertext))
    }

    @Test
    fun encryptsRandomDataWithBlockSplitting() {
        val plaintext = Random.nextBytes(ByteArray(512))
        val ciphertext = encrypt(keyPair.publicKey, plaintext)

        val maxPlaintextSize = rsaBlockSize(keyPair.publicKey) - 11
        val expectedBlockCount = (plaintext.size + maxPlaintextSize - 1) / maxPlaintextSize
        assertEquals(rsaBlockSize(keyPair.publicKey) * expectedBlockCount, ciphertext.size)
        assertDecryptedEquals(plaintext, decrypt(keyPair.privateKey, ciphertext))
    }

    private fun encrypt(publicKey: InternalRSA.PKCS1.PublicKey, plaintext: ByteArray): ByteArray {
        val ciphertext = Buffer()
        ciphertext.encrypt(publicKey).buffer().use { it.write(plaintext) }
        return ciphertext.readByteArray()
    }

    private fun decrypt(privateKey: InternalRSA.PKCS1.PrivateKey, ciphertext: ByteArray): ByteArray {
        val publicKey = privateKey.getPublicKeyBlocking()
        val blockSize = rsaBlockSize(publicKey)
        require(ciphertext.size % blockSize == 0)

        val decryptor = privateKey.decryptor()
        val plaintext = Buffer()
        ciphertext.asList().chunked(blockSize).forEach { block ->
            plaintext.write(decryptor.decryptBlocking(block.toByteArray()))
        }
        return plaintext.readByteArray()
    }

    private fun assertDecryptedEquals(expected: ByteArray, actual: ByteArray) {
        assertEquals(expected.size, actual.size)
        expected.indices.forEach { index ->
            assertEquals(expected[index], actual[index], "Byte mismatch at index $index")
        }
    }

    private fun rsaBlockSize(publicKey: InternalRSA.PKCS1.PublicKey): Int {
        val jwk = publicKey.encodeToByteArrayBlocking(InternalRSA.PublicKey.Format.JWK)
        return JsonWebKeys.decodeRsaPublicKey(
            algorithmId = InternalRSA.PKCS1,
            digest = null,
            jwkKey = jwk,
        ).n.size
    }
}
