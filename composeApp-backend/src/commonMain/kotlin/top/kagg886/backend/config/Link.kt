package top.kagg886.backend.config

import kotlinx.serialization.Serializable

@Serializable
data class Link(
    val name: String,
    val url: String,
    val description: String,
)
