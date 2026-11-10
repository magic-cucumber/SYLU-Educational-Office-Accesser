package top.kagg886.eoa.gym.internal

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class GymContext(
    val token: String,
    @SerialName("std_number")
    val studentNumber: String,
    @SerialName("user_id")
    val userId: String,
)