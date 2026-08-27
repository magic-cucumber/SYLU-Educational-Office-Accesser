package top.kagg886.eoa.pages.main.home.link.tips

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kotlinx.serialization.Serializable
import top.kagg886.eoa.LocalNavController
import top.kagg886.eoa.component.Markdown
import top.kagg886.eoa.component.dialog.DialogPageScaffold
import top.kagg886.eoa.config.BuildConfig

/**
 * ================================================
 * Author:     886kagg
 * Created on: 2025/7/2 11:03
 * ================================================
 */

@Serializable
data object LinkTipsRoute

@Composable
fun LinkTipsScreen() {
    val nav = LocalNavController.current
    DialogPageScaffold(
        confirmButton = {
            TextButton(
                onClick = { nav.popBackStack() },
            ) {
                Text("确定")
            }
        }
    ) {
        Markdown(
            content = """
                友情链接页面长期接受投稿。
                
                #### 投稿须知
                1. 只收录与学校本身有关的网站，其网站管理者必须有**校级组织或指导教师。**
                2. 不接受任何第三方网站，不接受盈利性质的网站。
                3. 需要内网登录的网站必须标注。
                
                #### 投稿方式
                1. 前往[EOA官方群](${BuildConfig.MESSAGE_QQ_GROUP_URL})私聊管理员
                2. 如果您在本机配置了邮箱客户端，点击[此链接](mailto:${BuildConfig.MESSAGE_MAIL}?subject=EOA%E5%8F%8B%E9%93%BE%E6%96%B0%E5%A2%9E%E7%94%B3%E8%AF%B7&body=1.%20%E5%8F%8B%E9%93%BE%E5%90%8D%E7%A7%B0%EF%BC%9A%0A2.%20%E5%8F%8B%E9%93%BE%E5%9C%B0%E5%9D%80%EF%BC%9A%0A3.%20%E5%8F%8B%E9%93%BE%E7%AE%80%E4%BB%8B%EF%BC%9A)后，填写内容并发送。
                3. 前往[EOA的Github仓库](https://github.com/kagg886/SYLU-Educational-Office-Accesser/)，为其新增一个**Issue**或**PullRequest**。
            """.trimIndent(),
            modifier = Modifier.verticalScroll(rememberScrollState())
        )
    }
}
