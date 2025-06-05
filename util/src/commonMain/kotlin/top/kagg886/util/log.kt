package top.kagg886.util

import co.touchlab.kermit.Logger

val Any.logger: Logger
    get() = Logger.withTag(this::class.qualifiedName ?: this::class.simpleName ?: "Unknown")

val String.asTaggedLogger: Logger
    get() = Logger.withTag(this)