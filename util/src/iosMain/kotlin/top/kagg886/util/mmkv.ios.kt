package top.kagg886.util

import co.touchlab.kermit.Severity
import top.kagg886.mkmb.MMKV
import top.kagg886.mkmb.MMKVOptions
import top.kagg886.mkmb.initializeWithMultiProcess

actual fun initializeMMKV() =
    if (!MMKV.initialized) MMKV.initializeWithMultiProcess(
        dataPath.resolve("config").absolutePath().toString(),
        MMKVOptions().apply {
            logFunc = { level, tag, it ->
                logger.log(
                    severity = when (level) {
                        MMKVOptions.LogLevel.Debug -> Severity.Debug
                        MMKVOptions.LogLevel.Info -> Severity.Info
                        MMKVOptions.LogLevel.Warning -> Severity.Warn
                        MMKVOptions.LogLevel.Error -> Severity.Error
                        MMKVOptions.LogLevel.None -> Severity.Assert
                    },
                    tag = "MMKV $tag",
                    message = it,
                    throwable = null
                )
            }
        }
    ) else Unit
