package top.kagg886.eoa.worker

import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.okio.OkioSerializer
import androidx.datastore.core.okio.OkioStorage
import androidx.room3.withWriteTransaction
import co.touchlab.kermit.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.atTime
import kotlinx.datetime.plus
import kotlinx.serialization.json.Json
import okio.BufferedSink
import okio.BufferedSource
import okio.FileSystem
import okio.SYSTEM
import top.kagg886.backend.config.AppLoginPropertiesInternalMMKV
import top.kagg886.backend.config.AppLoginPropertiesMMKV
import top.kagg886.backend.config.AppLoginPropertiesMMKV.clientId
import top.kagg886.backend.config.AppLoginPropertiesMMKV.token
import top.kagg886.backend.config.AppSyncMMKV
import top.kagg886.backend.database.AppDatabase
import top.kagg886.backend.database.dao.CourseEntity
import top.kagg886.backend.database.dao.CourseExtendEntity
import top.kagg886.backend.database.dao.CourseRecordEntity
import top.kagg886.sylu_eoa.api.v2.EOAClientProvider
import top.kagg886.sylu_eoa.api.v2.NeedCaptchaException
import top.kagg886.sylu_eoa.api.v2.Storage
import top.kagg886.util.dataPath
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours

object LightSyncTask : AutoCloseable {
    private val logger = Logger.withTag("LightSyncTask")
    private val scope = CoroutineScope(Dispatchers.IO)
    val store = DataStoreFactory.create(
        storage = OkioStorage(
            fileSystem = FileSystem.SYSTEM,
            serializer = object : OkioSerializer<LightSyncTaskConfig> {
                override val defaultValue: LightSyncTaskConfig = LightSyncTaskConfig()
                private val json = Json {
                    ignoreUnknownKeys = true
                    encodeDefaults = true
                }

                override suspend fun readFrom(source: BufferedSource): LightSyncTaskConfig {
                    logger.d("read worker config from store")
                    return json.decodeFromString(source.readUtf8())
                }

                override suspend fun writeTo(t: LightSyncTaskConfig, sink: BufferedSink) {
                    logger.d("write worker config from store")
                    sink.writeUtf8(json.encodeToString(t)).flush()
                }

            },
            producePath = { dataPath.resolve("worker.json") },
        ),
        scope = scope,
    )

    suspend fun sync(database: AppDatabase) {
        val light = store.data.first()
        if (Clock.System.now() - light.appSyncSuccessTime < 4.hours) { //如果前台写库时间<4h则跳过课程同步
            logger.d("sync: last sync less than 4 hours, return.")
            return
        }
        if (AppLoginPropertiesInternalMMKV.username.isEmpty() || AppLoginPropertiesInternalMMKV.password.isEmpty()) {
            logger.d("sync: not login, return.")
            return
        }
        val client = EOAClientProvider.providers.first { it.id == clientId }.provide().apply {
            logger.d("current provider id: $clientId")
            init(
                object : Storage {
                    override fun get(): String = token
                    override fun set(value: String) {
                        token = value
                    }
                }
            )
            username = AppLoginPropertiesMMKV.username
            password = AppLoginPropertiesMMKV.password
        }

        client.captchaHandler = {
            logger.w("sync: we need a captcha, but we are in an -background task, skip.")
            throw NeedCaptchaException()
        }

        val currentTerm = AppSyncMMKV.picker?.default ?: run {
            logger.w("sync: login success but not sync success, skip.")
            return
        }
        val currentCalendar = AppSyncMMKV.calender ?: run {
            logger.w("sync: login success but not sync success, skip.")
            return
        }

        val table = try {
            client.getClassTable(currentTerm, firstDay = currentCalendar.start)
        } catch (t: Throwable) {
            if (t is CancellationException) throw t
            logger.e("sync: failed", t)
            return
        }

        //MainRouteViewModel 的 逻辑。
        val courseDao = database.courseDao()
        val recordDao = database.courseRecordDao()
        val extendDao = database.courseExtendDao()

        database.withWriteTransaction {
            courseDao.clear(
                currentCalendar.start.atTime(0, 0),
                currentCalendar.end.plus(1, DateTimeUnit.DAY).atTime(0, 0)
            )
            extendDao.clearAll()
            extendDao.insertAll(table.extend.flatMap { item ->
                item.ranges.map { week ->
                    CourseExtendEntity(
                        name = item.name,
                        teacherName = item.teacher,
                        weekNumber = week,
                        yearCode = currentTerm.asTerm().xnm,
                        semesterCode = currentTerm.asTerm().xqm
                    )
                }
            })
            for (records in table.tables.groupBy { it.id }.values) {
                val item = records.first()
                val courseId = courseDao.insert(
                    CourseEntity(
                        name = item.name,
                        teacherName = item.teacher,
                        classroomName = item.room,
                        credits = item.score.toFloat(),
                        isDegreeRequired = item.isDegreeProgram,
                        isExaminable = item.classType == "考试"
                    )
                )
                recordDao.insertAll(records.map {
                    CourseRecordEntity(
                        courseId = courseId,
                        startTime = it.startTime,
                        endTime = it.endTime
                    )
                })
            }
        }
        store.updateData { it.copy(bgSyncSuccessTime = Clock.System.now()) }
        logger.i("sync: success, tables:${table.tables.size}, extends:${table.extend.size}")
    }

    //理论上不应该调用。 WorkManager和 BG TaskScheduler 启动的进程和App进程一致。
    override fun close() = scope.cancel()
}

internal expect val LightSyncTask.isRegister: Boolean
internal expect fun LightSyncTask.register()
internal expect fun LightSyncTask.unregister()