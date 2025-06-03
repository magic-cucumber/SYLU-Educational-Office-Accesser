package top.kagg886.backend.config

import top.kagg886.mkmb.MMKV
import top.kagg886.mkmb.mmkvWithID
import top.kagg886.sylu_eoa.api.v2.bean.SchoolCalender
import top.kagg886.sylu_eoa.api.v2.bean.TermResult
import top.kagg886.sylu_eoa.api.v2.bean.UserProfile
import top.kagg886.util.jsonOrNull

object AppSyncMMKV : MMKV by MMKV.mmkvWithID("app-sync-config"), AppSyncMMKVType {
    override var profile: UserProfile? by jsonOrNull("profile")
    override var picker: TermResult? by jsonOrNull("picker")
    override var calender: SchoolCalender? by jsonOrNull("school-calender")
}

interface AppSyncMMKVType {
    var profile: UserProfile?
    var picker: TermResult?
    var calender: SchoolCalender?

}
