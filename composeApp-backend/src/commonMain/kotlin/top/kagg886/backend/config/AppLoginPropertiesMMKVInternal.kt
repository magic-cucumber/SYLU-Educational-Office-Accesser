package top.kagg886.backend.config

import co.touchlab.kermit.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import top.kagg886.mkmb.MMKV
import top.kagg886.mkmb.MMKVMode
import top.kagg886.mkmb.mmkvWithID
import top.kagg886.sylu_eoa.api.v2.EOAClient
import top.kagg886.sylu_eoa.api.v2.EOAClientProvider
import top.kagg886.sylu_eoa.api.v2.Storage
import top.kagg886.util.string

private val mmkv = MMKV.mmkvWithID("login-properties", mode = MMKVMode.MULTI_PROCESS)

//不能在 扩展中 使用 AppLoginPropertiesMMKV 访问数据，因为此MMKV会根据SPI配置启用client
object AppLoginPropertiesInternalMMKV : MMKV by mmkv, AppLoginPropertiesInternalMMKVType {
    override var username: String by string("username", "")
}

sealed interface AppLoginPropertiesInternalMMKVType {
    var username: String
}
