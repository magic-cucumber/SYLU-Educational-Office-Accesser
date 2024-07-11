package com.kagg886.sylu_eoa.api.v2

import com.kagg886.sylu_eoa.api.v2.bean.ClassUnit
import com.kagg886.sylu_eoa.api.v2.bean.SubmitType
import com.kagg886.sylu_eoa.api.v2.network.InFileCookieSerializer
import com.kagg886.utils.LoggerReceiver
import com.kagg886.utils.createLogger
import com.kagg886.utils.registryLogReceiver
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.charset.StandardCharsets

private var log = createLogger("Test")

class SyluUserActionTest {

    @Test
    fun testUserProfile() = runBlocking {
        log.i(user.getUserProfile().toString())
    }

    @Test
    fun testQuestionsInfo() = runBlocking {
        val items = user.getAllUnRelatedItem()
        val eng = user.getRelatedQuestions(items[0])
        println(eng)
    }

    @Test
    fun testSubmitQuestion() = runBlocking {
        user.getAllUnRelatedItem().forEach {
            user.getRelatedQuestions(it).apply {
                for (i in this) {
                    //存在分数超出100分的情况,后端无校验
                    i.labelValue = "10"
                }

                user.submitRelatedQuestions(this,SubmitType.SAVE)
            }
        }
    }

    @Test
    fun testUnRelatedItem() = runBlocking {
        log.i(user.getAllUnRelatedItem().toString())
    }

    @Test
    fun testSchoolCalender() = runBlocking {
        log.i(user.getSchoolCalender().toString())
    }

    @Test
    fun testTermList() = runBlocking {
        val (list, default) = user.getAllAvailableTerms()
        list.forEach {
            log.i(it.toString())
        }
        log.i(default.toString())
    }

    @Test
    fun testExamResult() = runBlocking {
        val currentTerm = user.getAllAvailableTerms().default.asTerm()
        val list = user.getExamList()
        list.filter {
            it.year == currentTerm.xnm && it.semester == currentTerm.xqm
        }.forEach {
            log.i(it.toString())
        }
    }

    @Test
    fun getExamInfo() = runBlocking {
        val list = user.getExamList()[0]
        log.i(list.toString())
        log.i(user.getExamInfo(list).toString())
    }

    @Test
    fun getClassList() = runBlocking {
        user.getClassTable(user.getAllAvailableTerms().default).forEach {
            log.i(it.toString())
            log.i(Json.encodeToString(it))
            log.i(Json.decodeFromString<ClassUnit>(Json.encodeToString(it)).toString())
        }
    }

    @Test
    fun getGPAScoresTest() = runBlocking {
        user.getGPAScores().forEach { key ->
            log.i(key.toString())
        }
    }

    companion object {

        private var user: SyluUser = SyluUser("2203050528", InFileCookieSerializer(File("Cookie.json")))

        @JvmStatic
        @BeforeAll
        fun login() = runBlocking {
            registryLogReceiver(object : LoggerReceiver {
                override fun d(i: String) {}

                override fun i(i: String) {
                    println(i)
                }

                override fun w(i: String) {
                    println(i)
                }

                override fun e(i: String) {
                    System.err.println(i)
                }

            })


            log.i("currentDir:${File("").absolutePath}")
            if (!user.isLogin()) {
                user.login(File("password.txt").readText(StandardCharsets.UTF_8)) {
                    ""
                }
            }
        }
    }
}