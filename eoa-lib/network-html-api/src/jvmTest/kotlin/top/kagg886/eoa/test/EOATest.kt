package top.kagg886.eoa.test

import kotlinx.coroutines.runBlocking
import top.kagg886.sylu_eoa.api.html.EOAHTMLClient
import top.kagg886.sylu_eoa.api.v2.Storage
import kotlin.properties.Delegates
import kotlin.test.Test

class EOARelatedTest {
    companion object {
        internal var client by Delegates.notNull<EOAHTMLClient>()
            private set

        suspend fun beforeAll() = run {
            this@Companion.client = EOAHTMLClient()
            client.init(
                object : Storage {
                    private var string: String? = null
                    override fun get(): String? = string

                    override fun set(value: String) {
                        string = value
                    }
                }
            )
            client.login()
        }
    }

    @Test
    fun testEOAAllUnRelatedItem() = runBlocking {
        beforeAll()
        val calendar = client.getNotice(true)
        println(calendar)
    }
}