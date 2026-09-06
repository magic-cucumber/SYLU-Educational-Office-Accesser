package top.kagg886.sylu_eoa.api.html

import dev.whyoleg.sweetspi.ServiceProvider
import top.kagg886.sylu_eoa.api.v2.EOAClient
import top.kagg886.sylu_eoa.api.v2.EOAClientProvider

@ServiceProvider(services = [])
object EOAHTMLClientProvider : EOAClientProvider {
    override val id: String = "top.kagg886.sylu_eoa.api.html.EOAHTMLClientProvider"
    override val name: String = "本科生教务系统"
    override val description: String = "API来自jxw.sylu.edu.cn。\n掉线机制不明，可能会重复登录。"
    override val version: String = "1.8.0"

    override fun provide(): EOAClient = EOAHTMLClient()
}
