package top.kagg886.util

actual val Platform.Companion.current: Platform by lazy {
    val s = System.getProperty("os.name").lowercase()
    when {
        s.contains("win") -> Platform.Desktop.Windows
        s.contains("linux") -> Platform.Desktop.Linux
        s.contains("mac") -> Platform.Desktop.MacOS
        else -> throw IllegalArgumentException("Unknown platform: $s")
    }
}
