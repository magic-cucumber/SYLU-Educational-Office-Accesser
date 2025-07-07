package top.kagg886.sylu_eoa.api.html.util

import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import top.kagg886.sylu_eoa.api.html.EOAHTMLClient
import kotlin.test.BeforeTest
import kotlin.test.Test

/**
 * ================================================
 * Author:     886kagg
 * Created on: 2025/7/7 23:43
 * ================================================
 */
class EOAMergeTest {
    @BeforeTest
    fun setVerbose() = Logger.setMinSeverity(Severity.Verbose)

    @Test
    fun testEOAMergeLogin() {
        val data = EOAHTMLClient()
        data.init(MemoryStorage())
        data.username = "2203050528"
        data.password = "123456"


        runBlocking(Dispatchers.IO) {
            val jobA = launch {
                data.login()
            }

            val jobB = launch {
                data.login()
            }
        }
    }
}
