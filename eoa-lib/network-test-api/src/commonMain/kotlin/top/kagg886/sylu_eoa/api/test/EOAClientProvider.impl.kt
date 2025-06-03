package top.kagg886.sylu_eoa.api.test

import dev.whyoleg.sweetspi.ServiceProvider
import top.kagg886.sylu_eoa.api.v2.EOAClient
import top.kagg886.sylu_eoa.api.v2.EOAClientProvider

@ServiceProvider
internal object EOATestClientProvider : EOAClientProvider {
    override val id: String = this::class.qualifiedName!!
    override val name: String = "测试API"
    override val description: String = "全部为示例数据，非开发者请勿使用。"
    override val version: String = "1.0"

    override fun provide(): EOAClient = TestEOAClient()
}
