package com.memoriabox.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*

sealed class Screen(val route: String) {
    object Boxes : Screen("boxes")
    object Calendar : Screen("calendar")
    object Logs : Screen("logs")
    object Settings : Screen("settings")
    object BoxDetail : Screen("box_detail/{boxId}") {
        fun createRoute(boxId: String) = "box_detail/$boxId"
    }
    object BackupSettings : Screen("backup_settings")
    object WebDavSettings : Screen("webdav_settings")
    object Todo : Screen("todo")
    object Statistics : Screen("statistics")
    object Friends : Screen("friends")
    object PhotoWall : Screen("photo_wall")
    object Export : Screen("export")
    object Birthday : Screen("birthday")
    object Timeline : Screen("timeline")
    object AiSuggestions : Screen("ai_suggestions")
    object Achievements : Screen("achievements")
    object SyncStatus : Screen("sync_status")
    object DayTools : Screen("day_tools")
    object CustomizationSettings : Screen("customization_settings")
}

sealed class BottomNavigationItem(
    val route: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    object Boxes : BottomNavigationItem(
        Screen.Boxes.route,
        "日子",
        androidx.compose.material.icons.Icons.Filled.Event
    )
    object Calendar : BottomNavigationItem(
        Screen.Calendar.route,
        "日历",
        androidx.compose.material.icons.Icons.Filled.CalendarToday
    )
    object Todo : BottomNavigationItem(
        Screen.Todo.route,
        "待办",
        androidx.compose.material.icons.Icons.Filled.CheckCircle
    )
    object Settings : BottomNavigationItem(
        Screen.Settings.route,
        "我的",
        androidx.compose.material.icons.Icons.Filled.Person
    )
}

val bottomNavItems = listOf(
    BottomNavigationItem.Boxes,
    BottomNavigationItem.Calendar,
    BottomNavigationItem.Todo,
    BottomNavigationItem.Settings
)
