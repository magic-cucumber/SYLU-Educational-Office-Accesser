package top.kagg886.sylu_eoa.api.graduate

import dev.whyoleg.sweetspi.ServiceProvider
import top.kagg886.sylu_eoa.api.v2.EOAClient
import top.kagg886.sylu_eoa.api.v2.EOAClientProvider

@ServiceProvider(services = [])
object EOAGraduateClientProvider : EOAClientProvider {
    override val id: String = "top.kagg886.sylu_eoa.api.graduate.EOAHTMLClientProvider"
    override val name: String = "研究生专用"
    override val description: String = "基于逆向教务网页端得来\n测试版本，不保证可用性"
    override val version: String = "1.0"

    override fun provide(): EOAClient = EOAGraduateClient()
}
