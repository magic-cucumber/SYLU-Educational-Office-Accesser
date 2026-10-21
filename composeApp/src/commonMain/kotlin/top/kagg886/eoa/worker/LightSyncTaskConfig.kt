package top.kagg886.eoa.worker

import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
data class LightSyncTaskConfig(
    val appSyncSuccessTime: Instant = Instant.DISTANT_PAST,
    val bgSyncSuccessTime: Instant = Instant.DISTANT_PAST
)