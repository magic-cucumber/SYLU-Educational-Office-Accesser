package com.kagg886.sylu_eoa.api.seats.util

import com.kagg886.sylu_eoa.api.seats.SeatManager
import com.kagg886.sylu_eoa.api.seats.bean.Rooms
import com.kagg886.sylu_eoa.api.seats.bean.Seat
import com.kagg886.sylu_eoa.api.seats.bean.SeatQueryModel
import com.kagg886.sylu_eoa.api.seats.bean.SeatUsage
import com.kagg886.sylu_eoa.api.v2.SyluUser
import com.kagg886.utils.Logger
import com.kagg886.utils.LoggerReceiver
import com.kagg886.utils.createLogger
import com.kagg886.utils.registryLogReceiver
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

val config = SeatQueryModel(
    room = Rooms.L3_CORRIDOR,
    date = LocalDate.now(),
    startTime = LocalTime.of(12,0),
    endTime = LocalTime.of(21,0)
)
class SeatsUtilKtTest {

    @Test
    fun testFilterSeats():Unit = runBlocking {
        val conf = SeatUsage.build(LocalDateTime.now())
        val echo = manager.getSeatList(config).filterSeats(conf).let {
            assert(it.isNotEmpty())
            it[0]
        }
        logger.i("选择的座位：$echo")
        delay(3000)
        manager.reserve(echo,conf)
        logger.i("选座完毕")
    }

    @Test
    fun testGetSeatsManager():Unit = runBlocking {
        logger.i(manager.getSeatList(config).toString())
    }

    companion object {
        private lateinit var manager:SeatManager
        private lateinit var logger:Logger
        @JvmStatic
        @BeforeAll
        fun reg() {
            registryLogReceiver(object : LoggerReceiver {
                override fun d(msg: String) {
                    println(msg)
                }

                override fun i(msg: String) {
                    println(msg)
                }

                override fun w(msg: String) {
                    println(msg)
                }

                override fun e(msg: String) {
                    println(msg)
                }

            })
            logger = createLogger("SeatsTest")
            runBlocking {
                manager = SyluUser("2203050528").apply {
                    login(File("password.txt").readText())
                }.getSeatsManager()
            }
        }
    }
}