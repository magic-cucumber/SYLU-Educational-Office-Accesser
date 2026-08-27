package top.kagg886.sylu_eoa.api.html.util

import dev.whyoleg.cryptography.CryptographyProviderApi
import dev.whyoleg.cryptography.DelicateCryptographyApi
import dev.whyoleg.cryptography.providers.base.materials.JsonWebKeys
import okio.Buffer
import okio.Sink
import okio.Timeout
import dev.whyoleg.cryptography.algorithms.RSA as InternalRSA

@OptIn(CryptographyProviderApi::class, DelicateCryptographyApi::class)
fun Sink.encrypt(pubKey: InternalRSA.PKCS1.PublicKey): Sink {
    val delegate = this
    val encryptor = pubKey.encryptor()

    val rsaBlockSize = JsonWebKeys.decodeRsaPublicKey(
        algorithmId = InternalRSA.PKCS1,
        digest = null,
        jwkKey = pubKey.encodeToByteArrayBlocking(
            InternalRSA.PublicKey.Format.JWK
        ),
    ).n.size

    val maxPlaintextSize = rsaBlockSize - 11
    val pending = Buffer()

    return object : Sink {
        private var closed = false

        override fun write(source: Buffer, byteCount: Long) {
            check(!closed) { "Sink is closed" }
            require(byteCount >= 0) { "byteCount < 0: $byteCount" }
            require(byteCount <= source.size) {
                "byteCount=$byteCount > source.size=${source.size}"
            }

            pending.write(source, byteCount)

            while (pending.size >= maxPlaintextSize) {
                encryptBlock(maxPlaintextSize.toLong())
            }
        }

        private fun encryptBlock(byteCount: Long) {
            val plaintext = pending.readByteArray(byteCount)
            val ciphertext = encryptor.encryptBlocking(plaintext)

            delegate.write(
                Buffer().write(ciphertext),
                ciphertext.size.toLong(),
            )
        }

        override fun flush() {
            check(!closed) { "Sink is closed" }
            delegate.flush()
        }

        override fun timeout(): Timeout {
            return delegate.timeout()
        }

        override fun close() {
            if (closed) return
            closed = true

            var failure: Throwable? = null

            try {
                if (pending.size > 0) {
                    encryptBlock(pending.size)
                }
                delegate.flush()
            } catch (cause: Throwable) {
                failure = cause
            }

            try {
                delegate.close()
            } catch (cause: Throwable) {
                if (failure == null) {
                    failure = cause
                } else {
                    failure.addSuppressed(cause)
                }
            }

            failure?.let { throw it }
        }
    }
}