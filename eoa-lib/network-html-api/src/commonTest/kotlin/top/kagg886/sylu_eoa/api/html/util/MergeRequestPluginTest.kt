package top.kagg886.sylu_eoa.api.html.util

import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity
import io.ktor.client.*
import io.ktor.client.request.*
import kotlinx.coroutines.*
import kotlin.test.BeforeTest
import kotlin.test.Test

/**
 * ================================================
 * Author:     886kagg
 * Created on: 2025/7/7 23:17
 * ================================================
 */

class MergeRequestPluginTest {
    @BeforeTest
    fun set() {
        Logger.setMinSeverity(severity = Severity.Verbose)
    }


    @Test
    fun testMergeRequest() {
        val client = HttpClient {
            install(RequestMergePlugin)
        }

        runBlocking(Dispatchers.IO) {
            (1..1000).map {
                async {
                    client.get("https://www.baidu.com")
                }
            }.awaitAll()
        }
    }

    @Test
    fun testCancelSomeRequestButOtherCanProcess() {
        val client = HttpClient {
            install(RequestMergePlugin)
        }

        runBlocking(Dispatchers.IO) {
            val req1 = async {
                client.get("https://www.google.com")
            }

            val req2 = async {
                client.get("https://www.google.com")
            }

            req1.cancel("故意的", IllegalStateException())

            req2.await() //should be success and throw timeout
        }


    }
}
