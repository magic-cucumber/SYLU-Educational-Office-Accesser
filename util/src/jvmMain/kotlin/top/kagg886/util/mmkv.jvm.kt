package top.kagg886.util

import co.touchlab.kermit.Severity
import top.kagg886.mkmb.MMKV
import top.kagg886.mkmb.MMKVOptions
import top.kagg886.mkmb.initialize

actual fun initializeMMKV() =
    if (!MMKV.initialized) MMKV.initialize(dataPath.resolve("config").absolutePath().toString()) {
        logFunc = { level, tag, it ->
            logger.log(
                severity = when (level) {
                    MMKVOptions.LogLevel.Debug -> Severity.Verbose
                    MMKVOptions.LogLevel.Info -> Severity.Debug
                    MMKVOptions.LogLevel.Warning -> Severity.Info
                    MMKVOptions.LogLevel.Error -> Severity.Warn
                    MMKVOptions.LogLevel.None -> Severity.Error
                },
                tag = "MMKV $tag",
                message = it,
                throwable = null
            )
        }
    } else Unit
