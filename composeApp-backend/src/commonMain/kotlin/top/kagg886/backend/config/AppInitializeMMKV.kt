package top.kagg886.backend.config

import top.kagg886.mkmb.MMKV
import top.kagg886.mkmb.MMKVMode
import top.kagg886.mkmb.mmkvWithID
import top.kagg886.util.boolean
import top.kagg886.util.json
import top.kagg886.util.jsonOrNull
import top.kagg886.util.string

object AppInitializeMMKV : MMKV by MMKV.mmkvWithID("initialize-setting", mode = MMKVMode.MULTI_PROCESS),
    AppInitializeMMKVType {
    override var initialize: Boolean by boolean("initialize", false)
    override var calendarId: String by string("calendarId", "")
    override var size: Pair<Int, Int> by json("size", 800 to 600)
    override var offset: Pair<Int, Int>? by jsonOrNull("offset")
    override var announce: String by string("announce", "")
    override var link: List<Link> by json("link", listOf())

    override var tutorialSummary: Boolean by boolean("tutorial-summary", true)
    override var tutorialCourseList: Boolean by boolean("tutorial-course-list", true)
    override var tutorialCourseManage: Boolean by boolean("tutorial-course-manage", true)
    override var tutorialExamList: Boolean by boolean("tutorial-exam-list", true)
    override var tutorialSecondClassLogin: Boolean by boolean("tutorial-second-class-login", true)
}

sealed interface AppInitializeMMKVType {
    var initialize: Boolean
    var calendarId: String
    var size: Pair<Int, Int>
    var offset: Pair<Int, Int>?

    var announce: String
    var link: List<Link>

    var tutorialSummary: Boolean
    var tutorialCourseList: Boolean
    var tutorialCourseManage: Boolean
    var tutorialExamList: Boolean
    var tutorialSecondClassLogin: Boolean


}
