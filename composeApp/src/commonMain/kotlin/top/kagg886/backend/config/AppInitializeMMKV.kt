package top.kagg886.backend.config

import top.kagg886.mkmb.MMKV
import top.kagg886.mkmb.mmkvWithID
import top.kagg886.util.boolean

object AppInitializeMMKV: MMKV by MMKV.mmkvWithID("initialize-setting"), AppInitializeMMKVType {
    override var initialize: Boolean by boolean("initialize", false)
}

sealed interface AppInitializeMMKVType {
    var initialize: Boolean
}
