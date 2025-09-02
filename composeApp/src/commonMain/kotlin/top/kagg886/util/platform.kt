package top.kagg886.util

/**
 * ================================================
 * Author:     886kagg
 * Created on: 2025/9/3 13:23
 * ================================================
 */

sealed interface Platform {
    companion object;

    sealed interface Android : Platform {
        val version: Int
        data class Phone(override val version: Int):  Android
        data class Pad(override val version: Int):  Android
    }

    sealed interface Apple : Platform {

        data object IPhoneOS : Apple
        data object IPadOS : Apple
    }

    sealed interface Desktop: Platform {
        data object Windows : Desktop
        data object Linux : Desktop
        data object MacOS : Desktop
    }
}

expect val Platform.Companion.current: Platform
