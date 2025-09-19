package top.kagg886.eoa.second.bean

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * ================================================
 * Author:     886kagg
 * Created on: 2025/9/20 00:08
 * ================================================
 */

@Serializable
data class Group(
    @SerialName("group_name")
    val name: String,
)

@Serializable
data class Resource(
    val name: String,
    val redirect: String,
)

@Serializable
data class Portal(
    val group: Group,
    val resource: List<Resource>,
)

@Serializable
data class PortalReturn(
    val data: List<Portal>
)
