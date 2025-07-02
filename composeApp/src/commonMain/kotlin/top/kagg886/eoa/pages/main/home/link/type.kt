package top.kagg886.eoa.pages.main.home.link

import kotlinx.serialization.Serializable

/**
 * ================================================
 * Author:     886kagg
 * Created on: 2025/7/2 10:48
 * ================================================
 */

@Serializable
data class Link(
    val name: String,
    val url: String,
    val description: String,
)
