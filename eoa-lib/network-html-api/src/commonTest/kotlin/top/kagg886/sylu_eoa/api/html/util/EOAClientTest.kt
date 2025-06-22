package top.kagg886.sylu_eoa.api.html.util

import kotlinx.coroutines.runBlocking
import top.kagg886.sylu_eoa.api.v2.EOAClientException
import top.kagg886.sylu_eoa.api.v2.Storage
import top.kagg886.sylu_eoa.api.html.EOAHTMLClient
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
                user.login()
            }
            println(ex.message)
        }
    }

    @Test
    fun testEOAGPA() {
        val user = EOAHTMLClient()
        user.init(MemoryStorage())
        runBlocking {
            user.login()
            val picker = user.getGPAScores()
            for (term in picker) {
                val gpa = user.getGPAScoreList(term)
                logger.i("${term.name} GPA: $gpa")
            }
        }
    }

    @Test
    fun testEOAReLogin() {
        val user = EOAHTMLClient()
        user.init(MemoryStorage())
        runBlocking {
            user.login()
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
                    user.login {
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

    @Test
    fun testEOASystemNotice() {
        val user = EOAHTMLClient()
        user.init(MemoryStorage())
        runBlocking {
            user.login()
            val picker = user.getNotice()
            for (term in picker) {
                logger.i(term.toString())
            }

            val picker1 = user.getNotice(true)
            for (term in picker1) {
                logger.i(term.toString())
            }
        }
    }
}

class MemoryStorage : Storage {
    private var storage = ""
    override fun get(): String? = storage.apply {
        logger.d("Get Token: $this")
    }
    override fun set(value: String) {
        this.storage = value
        logger.d("Set Token: $value")
    }
}
