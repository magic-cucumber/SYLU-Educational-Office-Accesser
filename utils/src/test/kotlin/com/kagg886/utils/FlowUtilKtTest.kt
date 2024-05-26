package com.kagg886.utils

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

class FlowUtilKtTest {

    @Test
    fun throttleLatest():Unit = runBlocking {
        flow {
            for (i in 1..100) {
                emit(i)
                delay(100)
            }
        }.throttleLatest(1000).collect {
            println(it)
        }
    }
}