package top.kagg886.eoa.worker

internal actual suspend  fun LightSyncTask.register() = Unit
internal actual suspend  fun LightSyncTask.unregister() = Unit
internal actual suspend fun LightSyncTask.isRegister(): Boolean = false
internal actual suspend fun LightSyncTask.canRegister(): Boolean = false
