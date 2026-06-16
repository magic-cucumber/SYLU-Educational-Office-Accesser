package top.kagg886.eoa.pages.main.settings.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.serialization.Serializable
import org.orbitmvi.orbit.compose.collectAsState
import top.kagg886.eoa.LocalNavController
import top.kagg886.eoa.component.adaptive.NavigationSuiteType
import top.kagg886.eoa.component.dialog.DialogPageScaffold
import top.kagg886.eoa.pages.main.MainScreen
import top.kagg886.eoa.pages.main.mainViewModelOrNull
import top.kagg886.eoa.pages.main.settings.list.SettingListRoute
import top.kagg886.eoa.pages.main.settings.list.SettingsModel
import top.kagg886.eoa.pages.main.settings.list.SettingsState
import top.kagg886.eoa.util.currentLayoutType

@Serializable
data object SettingsProfile

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsProfileScreen() = MainScreen {
    val nav = LocalNavController.current
    val mainRouteViewModel = mainViewModelOrNull() ?: return@MainScreen
    val mainState by mainRouteViewModel.collectAsState()

    val flow = nav.currentBackStackEntryFlow.collectAsState(initial = null)
    val owner = remember(flow) {
        nav.getBackStackEntry(SettingListRoute)
    }
    val model = viewModel(key = mainState.toString(), viewModelStoreOwner = owner) {
        SettingsModel(mainState, mainRouteViewModel.database)
    }

    val state by model.collectAsState()

    DialogPageScaffold(
        title = { Text(text = "个人信息") },
        icon = { Icon(Icons.Default.Person, "") },
        confirmButton = {},
        dismissButton = {
            TextButton(
                onClick = {
                    nav.popBackStack()
                }
            ) {
                Text(text = "返回")
            }
        }
    ) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(0.8f).verticalScroll(rememberScrollState())) {
            when (val s = state) {
                is SettingsState.Success -> ProfileSuccess(s)
                is SettingsState.Loading -> Text(text = "加载中")
                is SettingsState.Failed -> Text(text = "加载失败")
            }
        }
    }
}

private val items = listOf<@Composable (SettingsState.Success, Modifier) -> Unit>(
    @Composable { profile, modifier ->
        ProfileItem(
            label = "学号",
            value = profile.stuId,
            modifier = modifier
        )
    },
    @Composable { profile, modifier ->
        ProfileItem(
            label = "姓名",
            value = profile.profile.name,
            modifier = modifier
        )
    },
    @Composable { profile, modifier ->
        ProfileItem(
            label = "学院",
            value = profile.profile.collegeName,
            modifier = modifier
        )
    },
    @Composable { profile, modifier ->
        ProfileItem(
            label = "专业",
            value = profile.profile.studyName,
            modifier = modifier
        )
    },
    @Composable { profile, modifier ->
        ProfileItem(
            label = "邮箱",
            value = profile.profile.email,
            modifier = modifier
        )
    },
    @Composable { profile, modifier ->
        ProfileItem(
            label = "电话",
            value = profile.profile.phone,
            modifier = modifier
        )
    },
    @Composable { profile, modifier ->
        ProfileItem(
            label = "身份证号",
            value = profile.profile.id,
            modifier = modifier
        )
    },
    @Composable { profile, modifier ->
        ProfileItem(
            label = "政策",
            value = profile.profile.policy,
            modifier = modifier
        )
    },
    @Composable { profile, modifier ->
        ProfileItem(
            label = "语言",
            value = profile.profile.language,
            modifier = modifier
        )
    }
)

@Composable
private fun ProfileSuccess(
    profile: SettingsState.Success,
) {
    val scope = currentLayoutType()

    if (scope == NavigationSuiteType.NavigationBar) {
        items.forEach { item ->
            item(profile, Modifier.fillMaxWidth())
        }
        return
    }

    items.chunked(2).forEach { rowItems ->
        Row(Modifier.fillMaxWidth()) {
            rowItems.first()(profile, Modifier.weight(1f))
            if (rowItems.size == 2) {
                rowItems.last()(profile, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ProfileItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    ListItem(
        modifier = modifier,
        headlineContent = { Text(text = label) },
        supportingContent = { Text(text = value) },
        colors = ListItemDefaults.colors(
            containerColor = AlertDialogDefaults.containerColor,
        )
    )
}
