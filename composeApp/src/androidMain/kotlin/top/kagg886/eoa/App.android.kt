package top.kagg886.eoa

import top.kagg886.eoa.config.BuildConfig
import top.kagg886.eoa.pages.update.detail.UpdateInfo

actual fun downloadResourceUrl(info: UpdateInfo): String =
    info.assets.first { it.name == if (BuildConfig.APP_DESUGAR_ENABLED) "app-release-6.apk" else "app-release.apk" }.browser_download_url
