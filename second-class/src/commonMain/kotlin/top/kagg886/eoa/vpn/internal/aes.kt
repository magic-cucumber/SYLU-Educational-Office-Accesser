package top.kagg886.eoa.vpn.internal

import dev.whyoleg.cryptography.CryptographyProvider
import dev.whyoleg.cryptography.DelicateCryptographyApi
import dev.whyoleg.cryptography.algorithms.AES
import io.ktor.util.encodeBase64
import io.ktor.utils.io.core.toByteArray
import top.kagg886.eoa.util.internal.random

/**
 * ================================================
 * Author:     886kagg
 * Created on: 2025/9/19 23:57
 * ================================================
 */
@OptIn(DelicateCryptographyApi::class)
internal fun aes(n: String, f: String): String {
    // 随机64字符 + 明文”用 AES-CBC(Pkcs7) 加密（密钥为去空格后的 f 的 UTF-8 字节，IV 为随机16字符的 UTF-8 字节）
    val n = random(64) + n
    val cipher = CryptographyProvider.Default.get(AES.CBC)
    val secretKey = cipher.keyDecoder()
        .decodeFromByteArrayBlocking(AES.Key.Format.RAW, f.trim().toByteArray())

    return secretKey.cipher(padding = true)
        .encryptWithIvBlocking(
            iv = random(16).toByteArray(),
            plaintext = n.toByteArray()
        )
        .encodeBase64()
}
