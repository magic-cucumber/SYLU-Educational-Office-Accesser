package top.kagg886.backend.config

import top.kagg886.mkmb.MMKV
import top.kagg886.mkmb.mmkvWithID
import top.kagg886.sylu_eoa.api.v2.EOAClient
import top.kagg886.sylu_eoa.api.v2.Storage
import top.kagg886.sylu_eoa.api.v3.EOAHTMLClient
import top.kagg886.util.string

object AppLoginPropertiesMMKV : MMKV by MMKV.mmkvWithID("login-properties"), AppLoginPropertiesMMKVType {
    override var username: String  by string("username", "")
    override var password: String  by string("password", "")
    override var token: String by string("session-key", "")

    override val client: EOAClient by lazy {
        val client = EOAHTMLClient()

        client.init(
            object : Storage {
                override fun get(): String? = token
                override fun set(value: String) {
                    token = value
                }
            }
        )

        client.username = username
        client.password = password

        client
    }
}

sealed interface AppLoginPropertiesMMKVType {
    var username: String
    var password: String
    var token: String
    val client: EOAClient
}
