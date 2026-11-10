package top.kagg886.backend.config

import top.kagg886.mkmb.MMKV
import top.kagg886.mkmb.MMKVMode
import top.kagg886.mkmb.mmkvWithID
import top.kagg886.util.string

object AppGymMMKV : MMKV by MMKV.mmkvWithID("gym", mode = MMKVMode.MULTI_PROCESS) {
    var password by string("password")
}
