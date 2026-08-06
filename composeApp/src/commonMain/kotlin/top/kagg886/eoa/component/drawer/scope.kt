package top.kagg886.eoa.component.drawer

/**
 * ================================================
 * Author:     iveou
 * Created on: 2026/8/6 15:14
 * ================================================
 */

interface DrawerSheetPageScaffoldScope {
    fun close()
}

internal class DrawerSheetPageScaffoldScopeImpl(
    private val onClose: () -> Unit
) : DrawerSheetPageScaffoldScope {
    override fun close() = onClose()
}
