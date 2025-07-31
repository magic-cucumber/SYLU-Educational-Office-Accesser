package top.kagg886.backend.config

import top.kagg886.mkmb.MMKV
import top.kagg886.mkmb.mmkvWithID
import top.kagg886.util.string

/**
 * ================================================
 * Author:     886kagg
 * Created on: 2025/7/26 23:11
 * ================================================
 */

object AppAiMMKV : MMKV by MMKV.mmkvWithID("ai-setting"), AppAiMMKVType {
    override var endpoint: String by string("endpoint", "")
    override var apiKey: String by string("api-key", "")
    override var model: String by string("model", "")
}

sealed interface AppAiMMKVType {
    var endpoint: String
    var apiKey: String
    var model: String
}
