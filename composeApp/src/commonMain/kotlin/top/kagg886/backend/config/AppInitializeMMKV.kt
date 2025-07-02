package top.kagg886.backend.config

import top.kagg886.mkmb.MMKV
import top.kagg886.mkmb.mmkvWithID
import top.kagg886.util.boolean
import top.kagg886.util.string

object AppInitializeMMKV : MMKV by MMKV.mmkvWithID("initialize-setting"), AppInitializeMMKVType {
    override var initialize: Boolean by boolean("initialize", false)
    override var announce: String by string("announce", "")
    override var link: String by string("link", "")
}

sealed interface AppInitializeMMKVType {
    var initialize: Boolean
    var announce: String
    var link: String
}
