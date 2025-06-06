package top.kagg886.sylu_eoa.api.html

import dev.whyoleg.sweetspi.ServiceProvider
import top.kagg886.sylu_eoa.api.v2.EOAClient
import top.kagg886.sylu_eoa.api.v2.EOAClientProvider

@ServiceProvider
object EOAHTMLClientProvider : EOAClientProvider {
    override val id: String = "top.kagg886.sylu_eoa.api.html.EOAHTMLClientProvider"
    override val name: String = "公开API"
    override val description: String = "基于逆向教务网页端得来\n掉线机制不明，可能会重复登录。"
    override val version: String = "1.0"

    override fun provide(): EOAClient = EOAHTMLClient()
}
