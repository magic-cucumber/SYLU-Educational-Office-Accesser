package top.kagg886.backend.config

import top.kagg886.eoa.util.Storage
import top.kagg886.mkmb.MMKV
import top.kagg886.mkmb.MMKVMode
import top.kagg886.mkmb.mmkvWithID
import top.kagg886.util.string

/**
 * ================================================
 * Author:     886kagg
 * Created on: 2025/9/20 15:17
 * ================================================
 */

object AppSecondClassMMKV : MMKV by MMKV.mmkvWithID("second-class", mode = MMKVMode.MULTI_PROCESS) {
    var vpnPassword by string("vpn-password")
    var twPassword by string("tw-password")
}
