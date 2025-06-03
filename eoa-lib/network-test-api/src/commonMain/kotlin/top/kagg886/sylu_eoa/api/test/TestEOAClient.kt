package top.kagg886.sylu_eoa.api.test

import top.kagg886.sylu_eoa.api.v2.EOAClient
import top.kagg886.sylu_eoa.api.v2.InvalidCredentialsException
import top.kagg886.sylu_eoa.api.v2.Storage
import top.kagg886.sylu_eoa.api.v2.bean.ClassUnit
import top.kagg886.sylu_eoa.api.v2.bean.ExamItem
import top.kagg886.sylu_eoa.api.v2.bean.GPAScore
import top.kagg886.sylu_eoa.api.v2.bean.GPAScoreSummary
import top.kagg886.sylu_eoa.api.v2.bean.SchoolCalender
import top.kagg886.sylu_eoa.api.v2.bean.TermPicker
import top.kagg886.sylu_eoa.api.v2.bean.TermResult
import top.kagg886.sylu_eoa.api.v2.bean.UserProfile

internal class TestEOAClient: EOAClient {
    override suspend fun getUserProfile(): UserProfile {
        TODO("Not yet implemented")
    }

    override suspend fun getSchoolCalender(): SchoolCalender {
        TODO("Not yet implemented")
    }

    override suspend fun getAllAvailableTerms(): TermResult {
        TODO("Not yet implemented")
    }

    override suspend fun getExamList(picker: TermPicker): List<ExamItem> {
        TODO("Not yet implemented")
    }

    override suspend fun getExamInfo(examItem: ExamItem): List<List<String>> {
        TODO("Not yet implemented")
    }

    override suspend fun getClassTable(picker: TermPicker): List<ClassUnit> {
        TODO("Not yet implemented")
    }

    override suspend fun getGPAScores(): List<GPAScoreSummary> {
        TODO("Not yet implemented")
    }

    override suspend fun getGPAScoreList(summary: GPAScoreSummary): List<GPAScore> {
        TODO("Not yet implemented")
    }

    override fun init(storage: Storage) {

    }

    override var username: String = ""
    override var password: String = ""

    override suspend fun login(captchaHandler: (suspend (a: ByteArray) -> String)?) {
        throw InvalidCredentialsException()
    }

    override suspend fun logout() {
    }

}