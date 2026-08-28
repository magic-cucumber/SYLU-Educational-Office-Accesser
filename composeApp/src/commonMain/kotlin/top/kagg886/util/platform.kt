package top.kagg886.util

import top.kagg886.eoa.config.BuildConfig

val Platform.Android.useDesugarApi: Boolean
    get() = BuildConfig.APP_DESUGAR_ENABLED
