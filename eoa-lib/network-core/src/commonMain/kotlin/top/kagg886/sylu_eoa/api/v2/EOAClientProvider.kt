package top.kagg886.sylu_eoa.api.v2

import dev.whyoleg.sweetspi.Service
import dev.whyoleg.sweetspi.ServiceLoader

@Service
interface EOAClientProvider {
    val id: String
    val name: String
    val description: String
    val version: String

    fun provide(): EOAClient

    companion object {
        val providers by lazy {
            ServiceLoader.load(EOAClientProvider::class)
        }
    }
}