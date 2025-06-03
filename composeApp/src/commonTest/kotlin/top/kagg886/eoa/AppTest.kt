package top.kagg886.eoa

import top.kagg886.backend.config.AppLoginPropertiesMMKV
import top.kagg886.backend.config.AppLoginPropertiesMMKVType
import top.kagg886.sylu_eoa.api.v2.EOAClientProvider
import top.kagg886.util.initializeMMKV
import kotlin.test.Test
import kotlin.test.assertEquals

class AppTest {
    @Test
    fun testAppProperties() {
        initializeMMKV()
        assertEquals(2, EOAClientProvider.providers.size)
        val mmkv: AppLoginPropertiesMMKVType = AppLoginPropertiesMMKV

        val oldProvider = mmkv.provider
        val oldClient = mmkv.client
        val oldId = mmkv.clientId

        println("oldId: $oldId")
        println("oldProvider: $oldProvider")
        println("oldClient: $oldClient")

        mmkv.clientId =
            EOAClientProvider.providers.first { it.id.contains("EOATestClientProvider") }.id

        println("newId: ${mmkv.clientId}")
        println("newProvider: ${mmkv.provider}")
        println("newClient: ${mmkv.client}")
    }
}