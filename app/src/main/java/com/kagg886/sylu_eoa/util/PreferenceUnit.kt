package com.kagg886.sylu_eoa.util

import androidx.datastore.preferences.core.*

open class PreferenceUnit<T>(
    val key: Preferences.Key<T>,
    val default: T
)


object Announcement : PreferenceUnit<String>(stringPreferencesKey("announcement"), "")
object NightMode : PreferenceUnit<Int>(intPreferencesKey("nightMode"), 0)

object ReadAboutOnFirst : PreferenceUnit<Boolean>(booleanPreferencesKey("readAboutOnFirst"), false)

object Account : PreferenceUnit<String>(stringPreferencesKey("account"), "")
object Password : PreferenceUnit<String>(stringPreferencesKey("password"), "")
object StorePassword : PreferenceUnit<Boolean>(booleanPreferencesKey("store-password"), false)


//过期时间
object DayExpired : PreferenceUnit<Int>(intPreferencesKey("day-expired"), 7)

//提前多少分钟提醒
object CalenderTipTime : PreferenceUnit<Int>(intPreferencesKey("time-tip"), 20)

//课表
object ClassList : PreferenceUnit<String>(stringPreferencesKey("class-list"), "")
object ClassListExpire : PreferenceUnit<Long>(longPreferencesKey("class-list-expire"), -1)

//校历
object SchoolCalenderBean : PreferenceUnit<String>(stringPreferencesKey("school-calender-bean"), "")
object SchoolCalenderBeanExpire : PreferenceUnit<Long>(longPreferencesKey("school-calender-bean-expire"), -1)

//考试条目
object ExamBean : PreferenceUnit<String>(stringPreferencesKey("exam-bean"), "")
object ExamBeanExpire : PreferenceUnit<Long>(longPreferencesKey("exam-bean-expire"), -1)

//学年-学期选择器
object PickerBean : PreferenceUnit<String>(stringPreferencesKey("picker"), "")
object PickerBeanExpire : PreferenceUnit<Long>(longPreferencesKey("picker-expire"), -1)

//个人信息
object ProfileBean : PreferenceUnit<String>(stringPreferencesKey("profile"), "")
object ProfileBeanExpire : PreferenceUnit<Long>(longPreferencesKey("profile-expire"), -1)

//GPA绩点
object GPABean : PreferenceUnit<String>(stringPreferencesKey("gpa"), "")
object GPABeanExpire : PreferenceUnit<Long>(longPreferencesKey("gpa-expire"), -1)

//二课分数
object SECClassBean: PreferenceUnit<String>(stringPreferencesKey("sec-class"),"")
object SECClassPass: PreferenceUnit<String>(stringPreferencesKey("sec-pass"),"")
object SECClassBeanExpire : PreferenceUnit<Long>(longPreferencesKey("sec-class-expire"), -1)