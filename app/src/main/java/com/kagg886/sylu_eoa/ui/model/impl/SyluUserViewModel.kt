package com.kagg886.sylu_eoa.ui.model.impl

import android.content.Context
import android.net.ConnectivityManager
import androidx.lifecycle.viewModelScope
import com.kagg886.sylu_eoa.api.v2.LoginFailedException
import com.kagg886.sylu_eoa.api.v2.SyluUser
import com.kagg886.sylu_eoa.api.v2.bean.TermResult
import com.kagg886.sylu_eoa.api.v2.network.CookieSerializer
import com.kagg886.sylu_eoa.getApp
import com.kagg886.sylu_eoa.ui.model.BaseViewModel
import com.kagg886.sylu_eoa.ui.model.LoadingState
import com.kagg886.sylu_eoa.util.*
import com.kagg886.utils.createLogger
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.nio.charset.StandardCharsets

private val log = createLogger("NetworkProtect")

class SyluUserViewModel : BaseViewModel<SyluUser>() {
    private val context by lazy {
        getApp()
    }

    private var _storePass = MutableStateFlow(false)
    val storePass = _storePass.asStateFlow()


//    private var _skipCheckLogin = MutableStateFlow(false)
//    val skipCheckLogin = _skipCheckLogin.asStateFlow()

    private val _syncStatus = MutableStateFlow<LoadingState>(LoadingState.LOADING)
    val syncStatus = _syncStatus.asStateFlow()

    val manager by lazy {
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    }

    fun clearLogin() {
        context.updateConfig(Account)
        context.updateConfig(Password)
        setStorePassword(false)
        viewModelScope.launch {
            delay(1000)
            clearLoading()
        }
    }

    fun login(
        user0: String,
        pass: String,
        captchaHandler: (suspend (a: ByteArray) -> String),
        continueHandler: (i: Throwable?) -> Unit,
    ) {
        viewModelScope.launch {
            kotlin.runCatching {
                if (user0.isEmpty()) {
                    throw IllegalArgumentException("请输入账号！")
                }
                if (pass.isEmpty()) {

                    throw IllegalArgumentException("请输入密码！")
                }
                val user = newSyluUser(user0)
                user.login(pass) {
                    captchaHandler(it)
                }

                //若登录不一样直接销毁数据
                val oldLogin = context.getConfig(Account).first()
                if (oldLogin != user0) {
                    context.updateConfig(ClassListExpire, -1)
                    context.updateConfig(SchoolCalenderBeanExpire, -1)
                    context.updateConfig(ExamBeanExpire, -1)
                    context.updateConfig(PickerBeanExpire, -1)
                }

                context.updateConfig(Password, if (_storePass.value) user.getPassword()!! else "")
                setDataLoadSuccess(user)
                loadAllData(user)
                continueHandler(null)
            }.onFailure {
                continueHandler(it)
            }
        }
    }

    fun setStorePassword(new: Boolean) {
        _storePass.value = new
        context.updateConfig(StorePassword, new)
    }

    override fun setDataLoadSuccess(new: SyluUser?) {
        super.setDataLoadSuccess(new)
        context.updateConfig(Account, new!!.user)
    }

    override suspend fun onDataFetch(): SyluUser {
        val account0 = context.getConfig(Account).first()
        val password0 = context.getConfig(Password).first()
        _storePass.value = context.getConfig(StorePassword).first()

        //未填写账户，返回null
        if (account0.isEmpty()) {
            throw LoginFailedException("未登录")
        }

        val user = newSyluUser(account0)

//        //跳过登录检查
//        if (skipCheckLogin.first()) {
//            return user
//        }
        //尝试检查在线，不在线登录
        viewModelScope.launch {
            if (!manager.isNetWorkConnected) {
                log.i("network unavailable, stop check login and use offline data")
                _syncStatus.value = LoadingState.FAILED
                return@launch
            }
            val isLogin = kotlin.runCatching {
                user.isLogin()
            }.getOrElse {
                _syncStatus.value = LoadingState.FAILED
                true
            }

            if (!isLogin) {
                if (password0.isEmpty()) {
                    setDataLoadError(LoginFailedException("登录状态过期，且未记住密码"))
                    return@launch
                }
                user.login(password0)
            }
            loadAllData(user)
        }

//        manager.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
//            override fun onAvailable(network: Network) {
//                viewModelScope.launch {
//                    log.i("network detected,now data fetch")
//                    kotlin.runCatching {
//                        if (manager.isNetWorkConnected) {
//                            loadAllData(user)
//                        }
//                    }
//                }
//            }
//        })
        return user
    }

    suspend fun loadAllData(user: SyluUser, force: Boolean = false) {
        if (!manager.isNetWorkConnected) {
            log.i("network unavailable, stop check login and use offline data")
            return
        }
        _syncStatus.value = LoadingState.LOADING
        val failHandler = suspend {
            if (!user.isLogin()) {
                if (!force) {
                    throw IllegalArgumentException("")
                }
                user.login(context.getConfig(Password).first())
            }
        }
        kotlin.runCatching {
            withContext(Dispatchers.IO) {
                val currentTimeStamp = System.currentTimeMillis()
                val day = context.getConfig(DayExpired).first()

                //拉取学年学期选择器
                val job4 = launchUntilSuccess(failHandler) {
                    val expire = context.getConfig(PickerBeanExpire)
                    if (expire.first() > currentTimeStamp && !force) {
                        return@launchUntilSuccess
                    }
                    val job = launchUntilSuccess(failHandler) {
                        val term = user.getAllAvailableTerms()
                        context.updateConfigSuspend(PickerBean, Json.encodeToString(term))
                        context.updateConfigSuspend(PickerBeanExpire, System.currentTimeMillis() + day * 864_000_00)
                    }
                    if (expire.first() == -1L || force) {
                        job.join()
                    }
                }

                //拉取课程表
                val job1 = launchUntilSuccess(failHandler) {
                    job4.join()
                    val expire = context.getConfig(ClassListExpire)
                    if (expire.first() > currentTimeStamp && !force) {
                        return@launchUntilSuccess
                    }
                    val job = launchUntilSuccess(failHandler) {
                        job4.join()
                        val term = Json.decodeFromString<TermResult>(context.getConfig(PickerBean).first())
                        val list = user.getClassTable(term.default)
                        context.updateConfigSuspend(ClassList, Json.encodeToString(list))
                        context.updateConfigSuspend(ClassListExpire, System.currentTimeMillis() + day * 864_000_00)
                    }
                    if (expire.first() == -1L || force) {
                        job.join()
                    }
                }

                //拉取校历
                val job2 = launchUntilSuccess(failHandler) {
                    job1.join()
                    val expire = context.getConfig(SchoolCalenderBeanExpire)
                    if (expire.first() > currentTimeStamp && !force) {
                        return@launchUntilSuccess
                    }
                    val job = launchUntilSuccess(failHandler) {
                        val list = user.getSchoolCalender()
                        context.updateConfigSuspend(SchoolCalenderBean, Json.encodeToString(list))
                        context.updateConfigSuspend(
                            SchoolCalenderBeanExpire,
                            System.currentTimeMillis() + day * 864_000_00
                        )
                    }

                    if (expire.first() == -1L || force) {
                        job.join()
                    }
                }

                //拉取考试条目
                val job3 = launchUntilSuccess(failHandler) {
                    job2.join()
                    val expire = context.getConfig(ExamBeanExpire)
                    if (expire.first() > currentTimeStamp && !force) {
                        return@launchUntilSuccess
                    }
                    val job = launchUntilSuccess(failHandler) {
                        val list = user.getExamList()
                        context.updateConfigSuspend(ExamBean, Json.encodeToString(list))
                        context.updateConfigSuspend(ExamBeanExpire, System.currentTimeMillis() + day * 864_000_00)
                    }
                    if (expire.first() == -1L || force) {
                        job.join()
                    }
                }


                //拉取个人信息
                val job5 = launchUntilSuccess(failHandler) {
                    job3.join()
                    val expire = context.getConfig(ProfileBeanExpire)
                    if (expire.first() > currentTimeStamp && !force) {
                        return@launchUntilSuccess
                    }
                    val job = launchUntilSuccess(failHandler) {
                        val list = user.getUserProfile()
                        context.updateConfigSuspend(ProfileBean, Json.encodeToString(list))
                        context.updateConfigSuspend(ProfileBeanExpire, System.currentTimeMillis() + day * 864_000_00)
                    }
                    if (expire.first() == -1L || force) {
                        job.join()
                    }
                }

                //拉取gpa绩点
                val job6 = launchUntilSuccess(failHandler) {
                    job5.join()
                    val expire = context.getConfig(GPABeanExpire)
                    if (expire.first() > currentTimeStamp && !force) {
                        return@launchUntilSuccess
                    }
                    val job = launchUntilSuccess(failHandler) {
                        val list = user.getGPAScores()
                        context.updateConfigSuspend(GPABean, Json.encodeToString(list))
                        context.updateConfigSuspend(GPABeanExpire, System.currentTimeMillis() + day * 864_000_00)
                    }
                    if (expire.first() == -1L || force) {
                        job.join()
                    }
                }
                job6.join()

            }
        }.onFailure {
            log.e("fetching data failed", it)
            _syncStatus.value = LoadingState.FAILED
            return
        }
        _syncStatus.value = LoadingState.SUCCESS
    }
}

fun newSyluUser(name: String): SyluUser {
    return SyluUser(name, EncryptedInFileCookieSerializer(File(getApp().filesDir, "config.json")))
}

class EncryptedInFileCookieSerializer(private val filePath: File) : CookieSerializer {

    private val list = mutableMapOf<String, MutableMap<String, String>>()
    private val serializer = MapSerializer(String.serializer(), MapSerializer(String.serializer(), String.serializer()))

    private val des = DESCrypt(getDeviceId())

    init {
        //初始化
        kotlin.runCatching {
            val json = des.decrypt(filePath.readText(StandardCharsets.UTF_8))
            Json.decodeFromString(serializer, json).forEach {
                list[it.key] = it.value.toMutableMap()
            }
        }
    }


    override fun save(host: String, param: MutableMap<String, String>) {
        //保存
        list[host] = param
        filePath.writeText(des.encrypt(Json.encodeToString(serializer, list)))
    }

    override fun load(host: String): MutableMap<String, String> {
        if (list[host] == null) {
            list[host] = mutableMapOf()
        }
        return list[host]!!
    }

    override fun clear() {
        list.clear()
        filePath.delete()
    }
}

private fun CoroutineScope.launchUntilSuccess(failed: suspend () -> Unit = {}, block: suspend () -> Unit): Job =
    launch {
        var t: Throwable? = null

        do {
            kotlin.runCatching {
                block()
            }.onFailure {
                t = it
                failed()
            }.onSuccess {
                t = null
            }
        } while (t != null)
    }