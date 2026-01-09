package top.kagg886.backend.config

import top.kagg886.mkmb.MMKV
import top.kagg886.mkmb.MMKVMode
import top.kagg886.mkmb.mmkvWithID
import top.kagg886.sylu_eoa.api.v2.EOAClient
import top.kagg886.sylu_eoa.api.v2.EOAClientProvider
import top.kagg886.sylu_eoa.api.v2.Storage
import top.kagg886.util.string

private val mmkv = MMKV.mmkvWithID("login-properties", mode = MMKVMode.MULTI_PROCESS)
object AppLoginPropertiesMMKV : MMKV by mmkv,
    AppLoginPropertiesMMKVType {
    override var username: String by string("username", "")
    override var password: String by string("password", "")
    override var token: String by string("session-key", "")

    private var _clientId by string(
        "client-id",
        EOAClientProvider.providers
            .apply { check(isNotEmpty()) { "we should register a EOA Client on sub modules!" } }[0]
            .id
    )

    override var clientId: String = _clientId
        set(value) {
            field = value
            _clientId = value
            client = EOAClientProvider.providers.first { it.id == clientId }.provide().apply {
                init(
                    object : Storage {
                        override fun get(): String = token
                        override fun set(value: String) {
                            token = value
                        }
                    }
                )
                username = this@AppLoginPropertiesMMKV.username
                password = this@AppLoginPropertiesMMKV.password
            }
        }

    override var client: EOAClient =
        EOAClientProvider.providers.first { it.id == clientId }.provide().apply {
            init(
                object : Storage {
                    override fun get(): String = token
                    override fun set(value: String) {
                        token = value
                    }
                }
            )
            username = this@AppLoginPropertiesMMKV.username
            password = this@AppLoginPropertiesMMKV.password
        }
        private set

    override fun clear() {
        mmkv.clear()
        client = EOAClientProvider.providers.first { it.id == clientId }.provide().apply {
            init(
                object : Storage {
                    override fun get(): String = token
                    override fun set(value: String) {
                        token = value
                    }
                }
            )
        }
    }
}

sealed interface AppLoginPropertiesMMKVType {
    var username: String
    var password: String
    var token: String
    var clientId: String

    val client: EOAClient
}
