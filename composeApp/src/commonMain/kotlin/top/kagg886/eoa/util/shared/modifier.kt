package top.kagg886.eoa.util.shared

import androidx.compose.ui.Modifier
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@OptIn(ExperimentalContracts::class)
inline fun Modifier.applyIf(condition: Boolean, modifier: Modifier.() -> Modifier): Modifier {
    contract {
        callsInPlace(modifier, InvocationKind.AT_MOST_ONCE)
    }
    return if (condition) modifier() else this
}

@OptIn(ExperimentalContracts::class)
inline fun <T : Any> Modifier.applyIf(value: T?, modifier: Modifier.(T) -> Modifier): Modifier {
    contract {
        callsInPlace(modifier, InvocationKind.AT_MOST_ONCE)
    }
    return if (value != null) modifier(value) else this
}
