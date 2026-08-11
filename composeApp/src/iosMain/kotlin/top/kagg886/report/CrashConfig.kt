package top.kagg886.report

import top.kagg886.mkmb.MMKV
import top.kagg886.mkmb.MMKVMode
import top.kagg886.mkmb.mmkvWithID
import top.kagg886.util.boolean
import top.kagg886.util.string

internal object CrashConfig : MMKV by MMKV.mmkvWithID("crash-info", mode = MMKVMode.MULTI_PROCESS) {
    var hasUnResolveCrash by boolean("hasUnResolveCrash", false)
    var crashText by string("crashText", "")
}
