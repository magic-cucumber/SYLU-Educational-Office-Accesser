package top.kagg886.eoa.component.adaptive

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuite
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldLayout
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import top.kagg886.eoa.util.currentLayoutType
import top.kagg886.eoa.util.isNavigationBar

/** App-specific title, menu and FAB slots around the library's adaptive navigation layout. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavigationSuiteScaffold(
    modifier: Modifier = Modifier,
    layoutType: NavigationSuiteType = currentLayoutType(),
    suite: (NavigationSuiteScope.() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val slots by rememberNavigationSuite(suite)
    val title = slots.title
    val back = slots.back
    val menu = slots.menu
    val navigationSuiteModifier = slots.navigationModifier ?: Modifier
    // Keep the original Rail/Drawer presentation while the library chooses the layout type.
    val navigationType = when (layoutType) {
        NavigationSuiteType.WideNavigationRailCollapsed -> NavigationSuiteType.NavigationRail
        NavigationSuiteType.WideNavigationRailExpanded -> NavigationSuiteType.NavigationDrawer
        else -> layoutType
    }
    val horizontal = navigationType.isNavigationBar
    val expanded = navigationType == NavigationSuiteType.NavigationDrawer
    val showNavigation = navigationType != NavigationSuiteType.None
    val navigationHeader: @Composable () -> Unit = {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (expanded) {
                Box(Modifier.heightIn(min = 56.dp).fillMaxWidth()) {
                    Row(
                        modifier = Modifier.align(Alignment.CenterStart),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Spacer(Modifier.width(8.dp))
                        Box(Modifier.sizeIn(48.dp), contentAlignment = Alignment.Center) {
                            back?.invoke()
                        }
                        Spacer(Modifier.width(8.dp))
                        ProvideTextStyle(MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)) {
                            title?.invoke()
                        }
                    }
                    Box(Modifier.align(Alignment.CenterEnd)) {
                        menu?.invoke()
                    }
                }
                Box(
                    Modifier.heightIn(min = 56.dp).fillMaxWidth().padding(horizontal = 16.dp)
                ) {
                    NavigationFab(
                        expanded = true,
                        fab = slots.fab,
                        modifier = (slots.fab?.modifier ?: Modifier).fillMaxWidth().sizeIn(minWidth = 80.dp),
                    )
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
    ) {
        NavigationSuiteScaffoldLayout(
            navigationSuiteType = navigationType,
            navigationSuite = {
                if (navigationType == NavigationSuiteType.NavigationRail) {
                    NavigationRail(modifier = navigationSuiteModifier) {
                        Spacer(Modifier.height(16.dp))
                        Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                            menu?.invoke()
                        }
                        Spacer(Modifier.height(24.dp))
                        Box(
                            Modifier.defaultMinSize(minWidth = 56.dp, minHeight = 56.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            NavigationFab(
                                expanded = false,
                                fab = slots.fab,
                                modifier = slots.fab?.modifier ?: Modifier
                            )
                        }
                        Spacer(Modifier.height(16.dp))
                        slots.navigationItems?.invoke(navigationType)
                    }
                } else {
                    NavigationSuite(
                        navigationSuiteType = navigationType,
                        modifier = navigationSuiteModifier,
                        primaryActionContent = navigationHeader,
                        content = { slots.navigationItems?.invoke(navigationType) },
                    )
                }
            },
            primaryActionContent = {
                if (horizontal) {
                    NavigationFab(
                        expanded = false,
                        fab = slots.fab,
                        modifier = slots.fab?.modifier ?: Modifier
                    )
                }
            },
        ) {
            // The navigation component already handles these edges. Leave the top inset to Scaffold.
            val consumedInsets = when (navigationType) {
                NavigationSuiteType.ShortNavigationBarCompact,
                NavigationSuiteType.ShortNavigationBarMedium ->
                    ShortNavigationBarDefaults.windowInsets.only(WindowInsetsSides.Bottom)
                NavigationSuiteType.WideNavigationRailCollapsed,
                NavigationSuiteType.WideNavigationRailExpanded ->
                    WideNavigationRailDefaults.windowInsets.only(WindowInsetsSides.Start)
                NavigationSuiteType.NavigationBar ->
                    NavigationBarDefaults.windowInsets.only(WindowInsetsSides.Bottom)
                NavigationSuiteType.NavigationRail ->
                    NavigationRailDefaults.windowInsets.only(WindowInsetsSides.Start)
                NavigationSuiteType.NavigationDrawer ->
                    DrawerDefaults.windowInsets.only(WindowInsetsSides.Start)
                else -> WindowInsets(0, 0, 0, 0)
            }
            Scaffold(
                modifier = Modifier.consumeWindowInsets(consumedInsets),
                topBar = {
                    if (!expanded || !showNavigation) {
                        if (title != null || back != null || (menu != null && (horizontal || !showNavigation))) {
                            TopAppBar(
                                title = {
                                    ProvideTextStyle(MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)) {
                                        title?.invoke()
                                    }
                                },
                                navigationIcon = { back?.invoke() },
                                actions = { if (horizontal || !showNavigation) menu?.invoke() },
                            )
                        }
                    }
                },
            ) { padding ->
                Box(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding)) {
                    content()
                }
            }
        }
    }
}

@Composable
private fun NavigationFab(
    modifier: Modifier = Modifier,
    expanded: Boolean,
    fab: NavigationSuiteFab?,
) {
    if (fab == null) return
    val icon = fab.icon
    val text = fab.text
    val badge = fab.badge
    val onClick = fab.onClick ?: {}
    val button: @Composable () -> Unit = {
        if (expanded) {
            ExtendedFloatingActionButton(
                onClick = onClick,
                modifier = modifier,
                icon = { icon?.invoke() },
                text = { text?.invoke() },
            )
        } else {
            FloatingActionButton(onClick = onClick, modifier = modifier) {
                (icon ?: text)?.invoke()
            }
        }
    }
    if (badge == null) button() else BadgedBox(badge = { badge() }) { button() }
}

@DslMarker
internal annotation class NavigationSuiteDsl

@NavigationSuiteDsl
class NavigationSuiteScope internal constructor() {
    private var title: (@Composable () -> Unit)? = null
    private var back: (@Composable () -> Unit)? = null
    private var menu: (@Composable () -> Unit)? = null
    private var navigationItems: (@Composable (NavigationSuiteType) -> Unit)? = null
    private var navigationModifier: Modifier? = null
    private var fab: NavigationSuiteFab? = null

    fun title(content: (@Composable () -> Unit)? = null) {
        title = content
    }

    fun back(content: (@Composable () -> Unit)? = null) {
        back = content
    }

    fun menu(content: (@Composable () -> Unit)? = null) {
        menu = content
    }

    fun navigationItems(
        modifier: Modifier? = null,
        content: (@Composable (NavigationSuiteType) -> Unit)? = null,
    ) {
        navigationModifier = modifier
        navigationItems = content
    }

    fun navigationModifier(modifier: Modifier? = null) {
        navigationModifier = modifier
    }

    fun fab(
        badge: (@Composable () -> Unit)? = null,
        modifier: Modifier? = null,
        content: NavigationSuiteFabScope.() -> Unit = {},
    ) {
        fab = NavigationSuiteFabScope().apply(content).toFab(badge, modifier)
    }

    internal fun toState() = NavigationSuiteSlots(
        title = title,
        back = back,
        menu = menu,
        navigationItems = navigationItems,
        navigationModifier = navigationModifier,
        fab = fab,
    )
}

@NavigationSuiteDsl
class NavigationSuiteFabScope internal constructor() {
    private var text: (@Composable () -> Unit)? = null
    private var icon: (@Composable () -> Unit)? = null
    private var onClick: (() -> Unit)? = null

    fun text(content: (@Composable () -> Unit)? = null) {
        text = content
    }

    fun icon(content: (@Composable () -> Unit)? = null) {
        icon = content
    }

    fun onClick(block: (() -> Unit)? = null) {
        onClick = block
    }

    internal fun toFab(badge: (@Composable () -> Unit)?, modifier: Modifier?) =
        NavigationSuiteFab(text, icon, onClick, badge, modifier)
}

internal data class NavigationSuiteSlots(
    val title: (@Composable () -> Unit)?,
    val back: (@Composable () -> Unit)?,
    val menu: (@Composable () -> Unit)?,
    val navigationItems: (@Composable (NavigationSuiteType) -> Unit)?,
    val navigationModifier: Modifier?,
    val fab: NavigationSuiteFab?,
)

internal data class NavigationSuiteFab(
    val text: (@Composable () -> Unit)?,
    val icon: (@Composable () -> Unit)?,
    val onClick: (() -> Unit)?,
    val badge: (@Composable () -> Unit)?,
    val modifier: Modifier?,
)

@Composable
private fun rememberNavigationSuite(
    content: (NavigationSuiteScope.() -> Unit)?,
): State<NavigationSuiteSlots> {
    val latestContent = rememberUpdatedState(content)
    return remember {
        derivedStateOf {
            NavigationSuiteScope().apply { latestContent.value?.invoke(this) }.toState()
        }
    }
}
