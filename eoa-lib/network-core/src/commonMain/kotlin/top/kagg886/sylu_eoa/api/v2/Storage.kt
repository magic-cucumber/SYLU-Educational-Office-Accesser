package top.kagg886.sylu_eoa.api.v2

interface Storage {
    fun get(): String?
    fun set(value: String)
}
