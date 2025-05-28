package top.kagg886.sylu_eoa.api.v3.util

import kotlinx.coroutines.runBlocking
import top.kagg886.sylu_eoa.api.v2.EOAClientException
import top.kagg886.sylu_eoa.api.v2.Storage
import top.kagg886.sylu_eoa.api.v3.EOAHTMLClient
import top.kagg886.util.logger
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class EOAClientTest {
    @Test
    fun testEOALoginFailed() {
        val user = EOAHTMLClient()
        user.init(MemoryStorage())
        runBlocking {
            val ex = assertFailsWith(EOAClientException::class) {
                user.login("2201010101","123456")
            }
            println(ex.message)
        }
    }

    @Test
    fun testEOAReLogin() {
        val user = EOAHTMLClient()
        user.init(MemoryStorage())
        runBlocking {
            user.login("2203050528","123456")
            val picker = user.getAllAvailableTerms().default
            user.logout()
            println(user.getClassTable(picker).toString())
        }
    }

    @Test
    fun testEOALoginVerifyCode() {
        val user = EOAHTMLClient()
        user.init(MemoryStorage())
        runBlocking {
            var hasVerifyCode = false
            repeat(6) {
                assertFailsWith(EOAClientException::class) {
                    user.login("2201010101","123456") {
                        logger.i("发现验证码")
                        hasVerifyCode = true
                        return@login "123456"
                    }
                }
            }
            if (!hasVerifyCode) {
                logger.w("没有发现验证码")
            }
            assertTrue(hasVerifyCode)
        }
    }
}

class MemoryStorage : Storage {
    private var storage = ""
    override fun get(): String? = storage.apply {
        logger.i("Get Token: $this")
    }
    override fun set(value: String) {
        this.storage = value
        logger.i("Set Token: $value")
    }
}
