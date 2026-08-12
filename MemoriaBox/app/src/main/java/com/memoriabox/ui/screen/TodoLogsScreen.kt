package com.memoriabox.ui.screen

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import androidx.navigation.compose.*
import com.memoriabox.ui.navigation.Screen
import com.memoriabox.ui.screen.components.*
import com.memoriabox.data.model.*
import com.memoriabox.utils.AppSettings
import com.memoriabox.viewmodel.*
import java.util.Calendar
import java.util.Date

@Composable
fun ScreenBgWrapper(context: android.content.Context, page: String, content: @Composable () -> Unit) {
    val settingsVersion = AppSettings.settingsVersion
    val bgUri = remember(settingsVersion, page) {
        when (page) {
            "CALENDAR" -> AppSettings.getCalendarBgUri(context)
            "TODO" -> AppSettings.getTodoBgUri(context)
            "SETTINGS" -> AppSettings.getSettingsBgUri(context)
            "LOGS" -> AppSettings.getSettingsBgUri(context)
            else -> null
        }
    }
    Box(modifier = Modifier.fillMaxSize()) {
        if (!bgUri.isNullOrBlank()) {
            coil.compose.AsyncImage(model = bgUri, contentDescription = null, contentScale = androidx.compose.ui.layout.ContentScale.Crop, modifier = Modifier.matchParentSize())
            Box(modifier = Modifier.matchParentSize().background(Color.Black.copy(alpha = 0.32f)))
        } else {
            Box(
                modifier = Modifier.matchParentSize().background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f),
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.24f)
                        )
                    )
                )
            )
        }
        content()
    }
}

@Composable
fun TodoScreen(application: Application) {
    val todoVM = remember { createTodoViewModel(application) }
    val todoEvents by todoVM.todoEvents.collectAsState(initial = emptyList())
    val subtaskMap by todoVM.subtaskMap.collectAsState(initial = emptyMap())
    LaunchedEffect(todoEvents) {
        todoVM.loadSubtasks(todoEvents)
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("待办事项") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { paddingValues ->
        TodoListView(
            events = todoEvents,
            subtaskMap = subtaskMap,
            onToggleStatus = { todoVM.toggleTodoStatus(it) },
            onUpdatePriority = { event, priority -> todoVM.updatePriority(event, priority) },
            onAddSubtask = { todoId, title -> todoVM.addSubtask(todoId, title) },
            onToggleSubtask = { todoVM.toggleSubtask(it) },
            onDeleteSubtask = { todoVM.deleteSubtask(it) },
            isOverdue = { todoVM.isOverdue(it) },
            modifier = Modifier.padding(paddingValues)
        )
    }
}

@Composable
fun LogsScreen(application: Application) {
    val viewModel = remember { createLogViewModel(application) }
    val logs by viewModel.logs.collectAsState(initial = emptyList())
    var filter by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        LogFilterBar(
            onFilterChange = {
                filter = it
                viewModel.setFilter(it)
            },
            onDateRangeChange = { }
        )
        LogsList(logs = logs)
    }
}

