package top.kagg886.eoa.vpn.internal

import com.niyaj.aes.ModeOfOperationCBC
import com.niyaj.aes.pkcs7Pad
import io.ktor.util.encodeBase64
import top.kagg886.eoa.util.internal.random

/**
 * ================================================
 * Author:     886kagg
 * Created on: 2025/9/19 23:57
 * ================================================
 */
internal fun aes(n: String, f: String): String {
    val n = random(64) + n
    val c = random(16)

    //随机64字符 + 明文”用 AES-CBC(Pkcs7) 加密（密钥为去空格后的 f 的 UTF-8 字节，IV 为随机16字符的 UTF-8 字节）


    // Encrypt
    return ModeOfOperationCBC(f.encodeToByteArray(), c.encodeToByteArray()).encrypt(pkcs7Pad(n.encodeToByteArray(), 16))
        .encodeBase64()
}
