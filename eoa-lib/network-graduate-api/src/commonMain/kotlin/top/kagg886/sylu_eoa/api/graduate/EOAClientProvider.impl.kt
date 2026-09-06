package top.kagg886.sylu_eoa.api.graduate

import dev.whyoleg.sweetspi.ServiceProvider
import top.kagg886.sylu_eoa.api.v2.EOAClient
import top.kagg886.sylu_eoa.api.v2.EOAClientProvider

@ServiceProvider(services = [])
object EOAGraduateClientProvider : EOAClientProvider {
    override val id: String = "top.kagg886.sylu_eoa.api.graduate.EOAHTMLClientProvider"
    override val name: String = "研究教务系统"
    override val description: String = "API来自https://yjsgl.sylu.edu.cn/\n目前为实验性，不保证日常可用。"
    override val version: String = "0.0.1"

    override fun provide(): EOAClient = EOAGraduateClient()
}
