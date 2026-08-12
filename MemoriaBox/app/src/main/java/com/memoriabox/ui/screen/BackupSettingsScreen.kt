package com.memoriabox.ui.screen

import android.app.Application
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.*
import com.memoriabox.ui.navigation.Screen
import com.memoriabox.ui.screen.components.*
import com.memoriabox.data.model.*
import com.memoriabox.viewmodel.*
import kotlinx.coroutines.launch

@Composable
fun BackupSettingsScreen(
    application: Application,
    onNavigateBack: () -> Unit
) {
    val viewModel = remember { createBackupViewModel(application) }
    val operationState by viewModel.operationState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val snackbarScope = rememberCoroutineScope()
    var backupPassword by rememberSaveable { mutableStateOf("") }
    var pendingImportUri by remember { mutableStateOf<Uri?>(null) }
    var importSummary by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(operationState.message) {
        operationState.message?.let { message ->
            if (operationState.importSummary != null) importSummary = operationState.importSummary
            snackbarHostState.showSnackbar(
                message = message,
                duration = if (operationState.importRestored) SnackbarDuration.Long else SnackbarDuration.Short
            )
            viewModel.clearOperationMessage()
        }
    }

    val dirPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            viewModel.saveBackupDirUri(uri)
            snackbarScope.launch { snackbarHostState.showSnackbar("已选择备份目录") }
        } else {
            snackbarScope.launch { snackbarHostState.showSnackbar("未选择备份目录") }
        }
    }

    val exportPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            viewModel.triggerManualBackup(uri, backupPassword)
        } else {
            snackbarScope.launch { snackbarHostState.showSnackbar("未选择备份目录") }
        }
    }

    val importPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            pendingImportUri = uri
            snackbarScope.launch {
                val header = viewModel.inspectBackup(uri)
                if (header == null) {
                    importSummary = "无法识别的备份文件，导入已取消"
                    pendingImportUri = null
                } else if (header.encrypted) {
                    pendingImportUri = uri
                } else {
                    pendingImportUri = null
                    viewModel.importBackup(uri, "")
                }
            }
        } else {
            snackbarScope.launch { snackbarHostState.showSnackbar("未选择备份文件") }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("备份设置") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { paddingValues ->
        BackupSettingsContent(
            modifier = Modifier.padding(paddingValues),
            backupPassword = backupPassword,
            onBackupPasswordChange = { backupPassword = it },
            onSelectDir = { dirPicker.launch(null) },
            onManualBackup = { exportPicker.launch(null) },
            onImport = { importPicker.launch(arrayOf("application/octet-stream", "application/x-sqlite3", "application/vnd.sqlite3", "*/*")) },
            isBusy = operationState.inProgress
        )
    }

    pendingImportUri?.let { uri ->
        AlertDialog(
            onDismissRequest = { pendingImportUri = null },
            title = { Text("确认导入备份") },
            text = {
                Text("该备份已加密，请输入备份密码后导入。导入会合并到当前数据，现有日子、日记和素材会保留。")
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.importBackup(uri, backupPassword)
                    pendingImportUri = null
                }) { Text("合并导入") }
            },
            dismissButton = { TextButton(onClick = { pendingImportUri = null }) { Text("取消") } }
        )
    }

    importSummary?.let { summary ->
        AlertDialog(
            onDismissRequest = { importSummary = null },
            title = { Text("导入摘要") },
            text = { Text(summary) },
            confirmButton = { TextButton(onClick = { importSummary = null }) { Text("知道了") } }
        )
    }
}

@Composable
fun WebDavSettingsScreen(
    application: Application,
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("WebDAV 设置") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { paddingValues ->
        WebDavSettingsContent(
            modifier = Modifier.padding(paddingValues)
        )
    }
}

