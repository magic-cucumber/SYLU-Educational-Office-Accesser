package com.kagg886.sylu_eoa.screen.page

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.outlined.Done
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kagg886.sylu_eoa.screen.LocalNavController
import com.kagg886.sylu_eoa.screen.LocalTopBar
import com.kagg886.sylu_eoa.ui.model.impl.ClassTableViewModel
import com.kagg886.sylu_eoa.ui.model.impl.SchoolCalenderViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ClassAllPage() {
    val top = LocalTopBar.current
    val nav = LocalNavController.current
    LaunchedEffect(Unit) {
        top.value = {
            TopAppBar(title = {
                Text("本学期课程列表")
            }, navigationIcon = {
                IconButton(onClick = {
                    nav.popBackStack()
                }) {
                    Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, contentDescription = null)
                }
            })
        }
    }

    val tableModel: ClassTableViewModel = viewModel(viewModelStoreOwner = LocalContext.current as ViewModelStoreOwner)
    val state by tableModel.data.collectAsState()
    val calenderModel: SchoolCalenderViewModel =
        viewModel(viewModelStoreOwner = LocalContext.current as ViewModelStoreOwner)
    val state1 by calenderModel.data.collectAsState()

    val event = remember(state, state1) {
        state!!.flatMapToEvent(state1!!);
    }

    var filterSuccess by remember { mutableStateOf(false) }

    Column {
        FilterChip(
            onClick = { filterSuccess = !filterSuccess },
            label = {
                Text(if (filterSuccess) "未完成" else "全部")
            },
            selected = filterSuccess,
            leadingIcon = if (filterSuccess) {
                {
                    Icon(
                        imageVector = Icons.Outlined.Warning,
                        contentDescription = "Done icon",
                        modifier = Modifier.size(FilterChipDefaults.IconSize)
                    )
                }
            } else {
                null
            },
        )

        LazyColumn {
            items(state!!) { u ->
                val me = event.filter { it.title == u.name }
                val all = me.size
                val unknown = me.filter { it.endDate <= System.currentTimeMillis() }.size
                if (all == unknown && filterSuccess) {
                    return@items
                }
                ListItem(
                    headlineContent = {
                        Text(u.name)
                    },
                    supportingContent = {
                        Text(u.weekEachLesson)
                    },
                    trailingContent = {
                        Text("${String.format("%.2f",(unknown*100.0 / all))}% ($unknown / $all)")
                    },
                    modifier = Modifier.animateItemPlacement()
                )
            }
        }
    }

}