@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package top.kagg886.eoa.pages.main.settings.feedback

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.automirrored.outlined.FormatListBulleted
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.FormatBold
import androidx.compose.material.icons.outlined.FormatItalic
import androidx.compose.material.icons.outlined.FormatListNumbered
import androidx.compose.material.icons.outlined.FormatStrikethrough
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.TextIncrease
import androidx.compose.material.icons.outlined.TextDecrease
import androidx.compose.material.icons.outlined.Title
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.TextUnit
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dokar.sonner.rememberToasterState
import com.mohamedrejeb.richeditor.model.HeadingStyle
import com.mohamedrejeb.richeditor.model.RichTextState
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import com.mohamedrejeb.richeditor.ui.material3.OutlinedRichTextEditor
import kotlinx.serialization.Serializable
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect
import top.kagg886.eoa.component.BackIconButton
import top.kagg886.eoa.component.bottomsheet.BottomSheetPageScaffold
import top.kagg886.eoa.component.bottomsheet.SheetPosition
import top.kagg886.eoa.util.createMenuButtonAnim
import top.kagg886.eoa.util.showSnackBar

@Serializable
data object FeedbackRoute

@Composable
fun FeedbackScreen() {
    val model = viewModel { FeedbackModel() }
    val state by model.collectAsState()
    val stack = rememberToasterState()
    val uri = LocalUriHandler.current

    BottomSheetPageScaffold(
        snack = stack,
        maxExpandedHeight = LocalWindowInfo.current.containerDpSize.height * 0.9f,
        initialPopupType = SheetPosition.Expanded,
        popupTypeChangeRequest = { it != SheetPosition.PartiallyExpanded }
    ) {
        model.collectSideEffect {
            when (it) {
                is FeedbackSideEffect.Toast -> stack.showSnackBar(it.type, it.message)
                is FeedbackSideEffect.OpenUrl -> uri.openUri(it.url)
                FeedbackSideEffect.Close -> close()
            }
        }

        FeedbackContent(
            state = state,
            onClose = { close() },
            onSubmit = { model.submit(it) }
        )
    }
}

@Composable
private fun FeedbackContent(
    state: FeedbackState,
    onSubmit: (String) -> Unit,
    onClose: () -> Unit,
) {
    AnimatedContent(
        targetState = state is FeedbackState.Loading,
        transitionSpec = { (fadeIn() togetherWith fadeOut()).using(SizeTransform(clip = false)) },
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxSize()
    ) { loading ->
        if (loading) FeedbackLoadingContent() else {
            FeedbackSuccessContent(
                isSubmitting = state is FeedbackState.Success && state.isSubmitting,
                onSubmit = onSubmit,
                onClose = onClose,
            )
        }
    }
}

@Composable
private fun FeedbackLoadingContent() =
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator()

            Spacer(Modifier.height(16.dp))

            Text(
                text = "正在准备你的反馈...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FeedbackSuccessContent(
    isSubmitting: Boolean,
    onSubmit: (String) -> Unit,
    onClose: () -> Unit
) {
    val editorState = rememberRichTextState()
    val canSubmit = editorState.annotatedString.text.isNotBlank() && !isSubmitting

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        TopAppBar(
            windowInsets = WindowInsets(),
            title = {
                Text("意见反馈")
            },
            navigationIcon = {
                BackIconButton(
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close"
                        )
                    },
                    enabled = !isSubmitting,
                    onBackPressed = { onClose() }
                )
            },
            actions = {
                IconButton(
                    onClick = { onSubmit(editorState.toMarkdown()) },
                    enabled = canSubmit
                ) {
                    AnimatedContent(
                        targetState = isSubmitting,
                        transitionSpec = createMenuButtonAnim {
                            isSubmitting
                        }
                    ) { submitting ->
                        if (submitting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Save,
                                contentDescription = "Save"
                            )
                        }
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            )
        )

        LaunchedEffect(editorState) {
            editorState.config.preserveStyleOnEmptyLine = true

            snapshotFlow {
                Triple(
                    editorState.annotatedString.text,
                    editorState.selection,
                    editorState.composition
                )
            }.collect {
                if (!editorState.selection.collapsed) {
                    return@collect
                }

                // IME composition 期间不要干涉样式。
                if (editorState.composition != null) {
                    return@collect
                }

                val currentParagraph = editorState
                    .getRichParagraphListByTextRange(editorState.selection)
                    .singleOrNull()
                    ?: return@collect

                if (
                    currentParagraph.headingStyle != HeadingStyle.Normal ||
                    !currentParagraph.isEmpty()
                ) {
                    return@collect
                }

                val paragraphIndex =
                    editorState.richParagraphList.indexOf(currentParagraph)

                if (paragraphIndex <= 0) {
                    return@collect
                }

                val previousHeading =
                    editorState.richParagraphList[paragraphIndex - 1].headingStyle

                if (previousHeading == HeadingStyle.Normal) {
                    return@collect
                }

                /*
                 * preserveStyleOnEmptyLine=true 会把：
                 *
                 * H1 defaultSpanStyle + 用户 SpanStyle
                 *
                 * 复制到新正文空行。
                 *
                 * 这里只移除 Heading 自己贡献的部分。
                 */
                editorState.removeSpanStyle(previousHeading.defaultSpanStyle)
            }
        }

        OutlinedRichTextEditor(
            state = editorState,
            enabled = !isSubmitting,
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                lineHeight = TextUnit.Unspecified,
            ),
            placeholder = {
                Text(
                    buildAnnotatedString {
                        append("反馈须知\n")

                        append("• 请仅提交")
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                            append("与本软件相关")
                        }
                        append("的建议、问题或意见。\n")

                        append("• 请确保反馈内容")
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                            append("遵守中国法律法规")
                        }
                        append("，请勿提交违法违规内容。\n")

                        append("• 如存在违反上述要求的情况，我们可能会")
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                            append("关闭该设备的反馈权限")
                        }
                        append("。")
                    },
                    style = MaterialTheme.typography.bodyLarge.copy(
                        lineHeight = TextUnit.Unspecified,
                        color = MaterialTheme.typography.bodyLarge.color.copy(alpha = 0.6f)
                    )
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                )
        )

        FeedbackEditorToolbar(state = editorState)
    }
}

@Composable
private fun FeedbackEditorToolbar(
    state: RichTextState,
    modifier: Modifier = Modifier,
) {
    var showHeadingMenu by remember { mutableStateOf(false) }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .imePadding()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box {
            ToolbarIconButton(
                icon = Icons.Outlined.Title,
                contentDescription = "Heading",
                selected = state.currentHeadingStyle.level in 1..6,
                onClick = { showHeadingMenu = true },
            )

            DropdownMenu(
                expanded = showHeadingMenu,
                onDismissRequest = { showHeadingMenu = false },
            ) {
                listOf(
                    HeadingStyle.Normal,
                    HeadingStyle.H1,
                    HeadingStyle.H2,
                    HeadingStyle.H3,
                    HeadingStyle.H4,
                    HeadingStyle.H5,
                    HeadingStyle.H6,
                ).forEach { heading ->
                    DropdownMenuItem(
                        text = {
                            Text(if (heading == HeadingStyle.Normal) "正文" else "H${heading.level}")
                        },
                        trailingIcon = if (state.currentHeadingStyle == heading) {
                            {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        } else {
                            null
                        },
                        onClick = {
                            state.setHeadingStyle(heading)
                            showHeadingMenu = false
                        },
                    )
                }
            }
        }

        ToolbarDivider()

        ToolbarIconButton(
            icon = Icons.Outlined.FormatBold,
            contentDescription = "Bold",
            selected = state.currentSpanStyle.fontWeight == FontWeight.Bold,
            onClick = { state.toggleSpanStyle(SpanStyle(fontWeight = FontWeight.Bold)) },
        )
        ToolbarIconButton(
            icon = Icons.Outlined.FormatItalic,
            contentDescription = "Italic",
            selected = state.currentSpanStyle.fontStyle == FontStyle.Italic,
            onClick = { state.toggleSpanStyle(SpanStyle(fontStyle = FontStyle.Italic)) },
        )
        ToolbarIconButton(
            icon = Icons.Outlined.FormatStrikethrough,
            contentDescription = "Strikethrough",
            selected = state.currentSpanStyle.textDecoration
                ?.contains(TextDecoration.LineThrough) == true,
            onClick = { state.toggleSpanStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) },
        )

        ToolbarDivider()

        ToolbarIconButton(
            icon = Icons.AutoMirrored.Outlined.FormatListBulleted,
            contentDescription = "Bulleted list",
            selected = state.isUnorderedList,
            onClick = { state.toggleUnorderedList() },
        )
        ToolbarIconButton(
            icon = Icons.Outlined.FormatListNumbered,
            contentDescription = "Numbered list",
            selected = state.isOrderedList,
            onClick = { state.toggleOrderedList() },
        )
    }
}

@Composable
private fun ToolbarIconButton(
    icon: ImageVector,
    contentDescription: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                Color.Transparent
            },
            contentColor = if (selected) {
                MaterialTheme.colorScheme.onSecondaryContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        ),
        modifier = modifier
            // Workaround to prevent the rich editor from losing focus
            // when clicking on the button (happens only on Desktop)
            .focusProperties { canFocus = false },
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
        )
    }
}

@Composable
private fun ToolbarDivider() {
    VerticalDivider(
        modifier = Modifier
            .padding(horizontal = 4.dp)
            .height(24.dp),
    )
}
