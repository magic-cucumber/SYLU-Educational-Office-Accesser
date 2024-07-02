package com.kagg886.sylu_eoa.util

import android.net.ConnectivityManager
import android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET
import android.os.Build

val ConnectivityManager.isNetWorkConnected
    get() = run {
        val networkCapabilities = getNetworkCapabilities(activeNetwork)
        networkCapabilities?.hasCapability(NET_CAPABILITY_INTERNET) ?: false
    }
