package top.kagg886.eoa

import top.kagg886.eoa.config.BuildConfig
import top.kagg886.eoa.pages.update.detail.UpdateInfo

actual fun downloadResourceUrl(info: UpdateInfo): String =
    "https://${BuildConfig.MESSAGE_GITEE_HOST}/kagg886/sylu-educational-office-accesser/releases/latest"
