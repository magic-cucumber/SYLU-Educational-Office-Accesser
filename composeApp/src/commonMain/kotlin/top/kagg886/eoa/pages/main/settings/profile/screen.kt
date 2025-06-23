package top.kagg886.eoa.pages.main.settings.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.serialization.Serializable
import org.orbitmvi.orbit.compose.collectAsState
import top.kagg886.eoa.LocalNavController
import top.kagg886.eoa.component.BackIconButton
import top.kagg886.eoa.pages.main.mainViewModel
import top.kagg886.eoa.pages.main.settings.list.SettingListRoute
import top.kagg886.eoa.pages.main.settings.list.SettingsModel
import top.kagg886.eoa.pages.main.settings.list.SettingsState

@Serializable
data object SettingsProfile

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsProfileScreen() {
    val nav = LocalNavController.current
    val mainRouteViewModel = mainViewModel()
    val mainState by mainRouteViewModel.collectAsState()

    val flow = nav.currentBackStackEntryFlow.collectAsState(initial = null)
    val owner = remember(flow) {
        nav.getBackStackEntry(SettingListRoute)
    }
    val model = viewModel(key = mainState.toString(), viewModelStoreOwner = owner) {
        SettingsModel(mainState, mainRouteViewModel.database)
    }

    val state by model.collectAsState()
    Surface(Modifier.fillMaxSize(0.8f)) {
        Column {
            TopAppBar(
                title = { Text(text = "个人信息") },
                navigationIcon = {
                    BackIconButton()
                }
            )
            Column(Modifier.weight(1f).padding(horizontal = 5.dp).verticalScroll(rememberScrollState())) {
                when (val s = state) {
                    is SettingsState.Success -> ProfileSuccess(s)
                    is SettingsState.Loading -> Text(text = "加载中")
                    is SettingsState.Failed -> Text(text = "加载失败")
                }
            }
        }
    }
}

@Composable
private fun ProfileSuccess(
    profile: SettingsState.Success,
) {
    val userProfile = profile.profile

    ProfileItem(
        label = "学号",
        value = profile.stuId
    )

    // 姓名
    ProfileItem(
        label = "姓名",
        value = userProfile.name
    )

    // 学院名称
    ProfileItem(
        label = "学院",
        value = userProfile.collegeName
    )

    // 专业名称
    ProfileItem(
        label = "专业",
        value = userProfile.studyName
    )

    // 邮箱
    ProfileItem(
        label = "邮箱",
        value = userProfile.email
    )

    // 电话
    ProfileItem(
        label = "电话",
        value = userProfile.phone
    )

    // 身份证
    ProfileItem(
        label = "身份证号",
        value = userProfile.id
    )

    // 政策
    ProfileItem(
        label = "政策",
        value = userProfile.policy
    )

    // 语言
    ProfileItem(
        label = "语言",
        value = userProfile.language
    )
}

@Composable
private fun ProfileItem(
    label: String,
    value: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
