package com.kagg886.sylu_eoa.ui.model.impl

import androidx.lifecycle.viewModelScope
import com.kagg886.sylu_eoa.api.v2.SyluUser
import com.kagg886.sylu_eoa.api.v2.bean.ClassUnit
import com.kagg886.sylu_eoa.api.v2.bean.ExamItem
import com.kagg886.sylu_eoa.getApp
import com.kagg886.sylu_eoa.ui.model.BaseViewModel
import com.kagg886.sylu_eoa.util.ClassList
import com.kagg886.sylu_eoa.util.ClassListExpire
import com.kagg886.sylu_eoa.util.DayExpired
import com.kagg886.sylu_eoa.util.Password
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val json = Json {
    ignoreUnknownKeys = true
}

class ExamDetailsViewModel : BaseViewModel<List<List<String>>>() {
    private val context by lazy {
        getApp()
    }


    fun loadDataByUser(user: SyluUser, exam: ExamItem) {
        setDataLoading()
        viewModelScope.launch {
            var success = false
            do {
                try {
                    setDataLoadSuccess(user.getExamInfo(exam))
                    success = true
                } catch (e:Throwable) {
                    user.login(
                        context.getConfig(Password).first()
                    )
                    continue
                }
            } while (!success)
        }
    }

    override suspend fun onDataFetch(): List<List<String>>? {
        return null
    }
}