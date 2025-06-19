package top.kagg886.sylu_eoa.api.test

import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import top.kagg886.sylu_eoa.api.v2.EOAClient
import top.kagg886.sylu_eoa.api.v2.InvalidCredentialsException
import top.kagg886.sylu_eoa.api.v2.Storage
import top.kagg886.sylu_eoa.api.v2.bean.ClassUnit
import top.kagg886.sylu_eoa.api.v2.bean.ExamItem
import top.kagg886.sylu_eoa.api.v2.bean.GPAScore
import top.kagg886.sylu_eoa.api.v2.bean.GPAScoreSummary
import top.kagg886.sylu_eoa.api.v2.bean.SchoolCalender
import top.kagg886.sylu_eoa.api.v2.bean.SystemNotice
import top.kagg886.sylu_eoa.api.v2.bean.Term
import top.kagg886.sylu_eoa.api.v2.bean.TermPicker
import top.kagg886.sylu_eoa.api.v2.bean.TermResult
import top.kagg886.sylu_eoa.api.v2.bean.UserProfile
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

internal class TestEOAClient : EOAClient {
    @OptIn(ExperimentalEncodingApi::class)
    override suspend fun getUserProfile(): UserProfile {
        val avatar = Base64.decode(
            source = "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"
        )
        return UserProfile(
            name = "千早爱音",
            collegeName = "羽秋高级中学",
            studyName = "高一",
            avatar = avatar,
            email = "anon@tokyo.com",
            phone = "1145141919810",
            id = "1",
            policy = "群众",
            language = "日语"
        )
    }

    override suspend fun getSchoolCalender(): SchoolCalender {
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        val todayWeekMonday = today.minus(today.dayOfWeek.ordinal, DateTimeUnit.DAY)
        return SchoolCalender(
            start = todayWeekMonday.minus(7, DateTimeUnit.WEEK),
            end = todayWeekMonday.plus(7, DateTimeUnit.WEEK),
        )
    }

    override suspend fun getAllAvailableTerms(): TermResult {
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date.year
        val pickers = (today downTo (today - 20) step 2).map {
            TermPicker(
                yearName = "$it-${it + 1}" to "$it",
                yearCode = "1" to "1"
            )
        }
        return TermResult(
            list = pickers,
            default = pickers[0]
        )
    }

    override suspend fun getExamList(picker: TermPicker): List<ExamItem> {
        return listOf()
    }

    override suspend fun getExamInfo(examItem: ExamItem): List<List<String>> {
        return listOf()
    }

    override suspend fun getClassTable(picker: TermPicker): List<ClassUnit> {
        return listOf()
    }

    override suspend fun getGPAScores(): List<GPAScoreSummary> {
        return listOf()
    }

    override suspend fun getGPAScoreList(summary: GPAScoreSummary): List<GPAScore> {
        return listOf()
    }

    override suspend fun getNotice(hasRead: Boolean): List<SystemNotice> {
        return if (hasRead) {
            listOf(
                SystemNotice(
                    createTime = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()),
                    title = "羽丘第一学期测评公告",
                    content = "无",
                    id = "1",
                ),
                SystemNotice(
                    createTime = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()),
                    title = "关于MyGO!!!乐团的演唱延时通知",
                    content = "无",
                    id = "4",
                )
            )
        } else {
            listOf(
                SystemNotice(
                    createTime = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()),
                    title = "羽丘第二学期测评公告",
                    content = "无",
                    id = "2",
                ),
                SystemNotice(
                    createTime = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()),
                    title = "羽丘第三学期测评公告",
                    content = "无",
                    id = "3",
                )
            )
        }
    }

    override fun init(storage: Storage) {

    }

    override var username: String = ""
    override var password: String = ""

    override suspend fun login(captchaHandler: (suspend (a: ByteArray) -> String)?) {
        if (username != "test" || password != "test") {
            throw InvalidCredentialsException()
        }
    }

    override suspend fun logout() {
    }

    override suspend fun markNoticeReadable(noticeId: String): Boolean = true

}