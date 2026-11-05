package com.memoriabox.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 备份提醒的判定测试。
 * 重点是：刚备份过不提醒、超期提醒、从未备份必须提醒、提醒有节流。
 */
class BackupReminderTest {

    private val day = 24L * 60 * 60 * 1000

    @Test
    fun `从未备份时视为过期`() {
        val overdue = AppSettings.backupOverdueDays(lastBackupAt = 0L, now = 1_700_000_000_000L)
        assertEquals(AppSettings.BACKUP_REMIND_DAYS, overdue)
    }

    @Test
    fun `刚备份过不提醒`() {
        val now = 1_700_000_000_000L
        assertNull(AppSettings.backupOverdueDays(lastBackupAt = now, now = now))
        assertNull(AppSettings.backupOverdueDays(lastBackupAt = now - 5 * day, now = now))
    }

    @Test
    fun `刚好满阈值即视为过期`() {
        val now = 1_700_000_000_000L
        val exactly = now - AppSettings.BACKUP_REMIND_DAYS * day
        assertEquals(
            AppSettings.BACKUP_REMIND_DAYS,
            AppSettings.backupOverdueDays(lastBackupAt = exactly, now = now)
        )
    }

    @Test
    fun `超过阈值返回过期天数`() {
        val now = 1_700_000_000_000L
        val overdue = AppSettings.backupOverdueDays(lastBackupAt = now - 45 * day, now = now)
        assertEquals(45, overdue)
    }

    @Test
    fun `数据未过期时不展示提醒`() {
        val now = 1_700_000_000_000L
        assertFalse(
            AppSettings.shouldShowBackupNotice(
                lastBackupAt = now - 3 * day,
                lastNoticeAt = 0L,
                now = now
            )
        )
    }

    @Test
    fun `首次过期时展示提醒`() {
        val now = 1_700_000_000_000L
        assertTrue(
            AppSettings.shouldShowBackupNotice(
                lastBackupAt = now - 40 * day,
                lastNoticeAt = 0L,
                now = now
            )
        )
    }

    @Test
    fun `提醒在间隔内会被节流`() {
        val now = 1_700_000_000_000L
        assertFalse(
            AppSettings.shouldShowBackupNotice(
                lastBackupAt = now - 40 * day,
                lastNoticeAt = now - 2 * day,
                now = now
            )
        )
    }

    @Test
    fun `超过节流间隔后可以再次提醒`() {
        val now = 1_700_000_000_000L
        assertTrue(
            AppSettings.shouldShowBackupNotice(
                lastBackupAt = now - 40 * day,
                lastNoticeAt = now - 8 * day,
                now = now
            )
        )
    }

    @Test
    fun `备份完成后提醒立即消失`() {
        val now = 1_700_000_000_000L
        assertTrue(
            AppSettings.shouldShowBackupNotice(lastBackupAt = now - 40 * day, lastNoticeAt = 0L, now = now)
        )
        // 刚完成备份
        assertFalse(
            AppSettings.shouldShowBackupNotice(lastBackupAt = now, lastNoticeAt = 0L, now = now)
        )
    }
}
