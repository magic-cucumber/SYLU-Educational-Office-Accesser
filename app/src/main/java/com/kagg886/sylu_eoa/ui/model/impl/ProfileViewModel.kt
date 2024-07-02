package com.kagg886.sylu_eoa.ui.model.impl

import androidx.lifecycle.viewModelScope
import com.kagg886.sylu_eoa.api.v2.SyluUser
import com.kagg886.sylu_eoa.api.v2.bean.UserProfile
import com.kagg886.sylu_eoa.getApp
import com.kagg886.sylu_eoa.ui.model.BaseViewModel
import com.kagg886.sylu_eoa.util.DayExpired
import com.kagg886.sylu_eoa.util.ProfileBean
import com.kagg886.sylu_eoa.util.ProfileBeanExpire
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.flow.lastOrNull
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val json = Json {
    ignoreUnknownKeys = true
}

class ProfileViewModel : BaseViewModel<UserProfile>() {
    private val context by lazy {
        getApp()
    }

    init {
        viewModelScope.launch {
            context.getConfig(ProfileBean).collect {
                if (it.isEmpty()) {
                    setDataLoading()
                    return@collect
                }
                setDataLoadSuccess(Json.decodeFromString(it))
            }
        }
    }
    override suspend fun onDataFetch(): UserProfile {
        val list = context.getConfig(ProfileBean).first()
        val cd = json.decodeFromString<UserProfile>(list)
        return cd
    }

    fun loadDataByUser(user: SyluUser) {
        setDataLoading()
    }
}