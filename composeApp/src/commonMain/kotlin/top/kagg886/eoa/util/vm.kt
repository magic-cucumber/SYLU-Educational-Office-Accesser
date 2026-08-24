package top.kagg886.eoa.util

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.orbitmvi.orbit.OrbitContainer
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.annotation.OrbitInternal
import org.orbitmvi.orbit.syntax.Syntax
import org.orbitmvi.orbit.viewmodel.orbitContainer
import kotlin.uuid.Uuid


abstract class BaseViewModel<State : Any, Effect : Any>(
    val name: String = "random-${Uuid.random()}",
    initial: State
) :
    ViewModel(), OrbitContainerHost<State, State, Effect> {

    val logger = Logger.withTag(name)

    abstract suspend fun Syntax<State, Effect>.init()

    @OptIn(OrbitInternal::class)
    override val container: OrbitContainer<State, State, Effect> = orbitContainer(
        initialState = initial,
        buildSettings = {
            exceptionHandler = CoroutineExceptionHandler { context, throwable ->
                logger.e("unhandled exception caught in ${context}.", throwable)
                throw throwable
            }
        }
    ) {
        logger.d("prepare for init viewmodel")
        init()
        logger.d("viewmodel init success. state: $state")

        viewModelScope.launch {
            this@orbitContainer.containerContext.stateFlow.collect {
                logger.d("state changed. state: $it")
            }
        }
    }

    override fun intent(
        registerIdling: Boolean,
        transformer: suspend Syntax<State, Effect>.() -> Unit
    ): Job {
        logger.d("prepare for dispatch new intent. registerIdling: $registerIdling")
        val job = super.intent(registerIdling, transformer)
        logger.d("dispatch intent success, result: $job")

        return job
    }

    override suspend fun subIntent(transformer: suspend Syntax<State, Effect>.() -> Unit) {
        logger.d("prepare for dispatch new subintent.")
        super.subIntent(transformer)
        logger.d("dispatch subintent success")
    }

    override fun onCleared() {
        super.onCleared()
        logger.d("viewmodel has been cleared")
    }

    override fun addCloseable(closeable: AutoCloseable) {
        super.addCloseable(closeable)
        logger.d("register close-handler: $closeable")
    }
}
