package com.kagg886.sylu_eoa.ui.model.impl

import androidx.lifecycle.viewModelScope
import com.kagg886.sylu_eoa.api.v2.SyluUser
import com.kagg886.sylu_eoa.api.v2.bean.ClassUnit
import com.kagg886.sylu_eoa.getApp
import com.kagg886.sylu_eoa.ui.model.BaseViewModel
import com.kagg886.sylu_eoa.util.ClassList
import com.kagg886.sylu_eoa.util.ClassListExpire
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

private val json = Json {
    ignoreUnknownKeys = true
}

class ClassTableViewModel : BaseViewModel<List<ClassUnit>>() {
    private val context by lazy {
        getApp()
    }

    init {
        viewModelScope.launch {
            context.getConfig(ClassList).collect {
                if (it.isEmpty()) {
                    setDataLoading()
                    return@collect
                }
                setDataLoadSuccess(Json.decodeFromString(it))
            }
        }
    }

    override suspend fun onDataFetch(): List<ClassUnit> {
        val list = context.getConfig(ClassList).first()
        return json.decodeFromString<List<ClassUnit>>(list)
    }

    fun loadDataByUser(user: SyluUser) {
        setDataLoading()
    }
}