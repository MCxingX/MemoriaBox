package com.memoriabox.utils

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 更新提示的弹出决策测试。
 * 覆盖三种状态：正常提示、跳过单个版本、永久屏蔽，以及手动检查的优先级。
 */
class UpdatePromptPolicyTest {

    private fun shouldPrompt(
        target: String,
        skipped: String? = null,
        never: Boolean = false,
        manual: Boolean = false
    ) = AppSettings.shouldPromptUpdate(target, skipped, never, manual)

    @Test
    fun `默认情况下有新版本就提示`() {
        assertTrue(shouldPrompt("4.0.6.263070100"))
    }

    @Test
    fun `跳过的版本不再提示`() {
        assertFalse(shouldPrompt("4.0.6.263070100", skipped = "4.0.6.263070100"))
    }

    @Test
    fun `出现更高版本时即使跳过过旧版本仍会提示`() {
        assertFalse(shouldPrompt("4.0.6.263070100", skipped = "4.0.6.263070100"))
        assertTrue(shouldPrompt("4.0.7.263070200", skipped = "4.0.6.263070100"))
    }

    @Test
    fun `跳过记录为空时正常提示`() {
        assertTrue(shouldPrompt("4.0.6.263070100", skipped = null))
        assertTrue(shouldPrompt("4.0.6.263070100", skipped = ""))
    }

    @Test
    fun `永久屏蔽后不再自动提示`() {
        assertFalse(shouldPrompt("4.0.6.263070100", never = true))
        assertFalse(shouldPrompt("4.0.9.263070300", never = true))
    }

    @Test
    fun `手动检查更新可以突破跳过与永久屏蔽`() {
        assertTrue(shouldPrompt("4.0.6.263070100", skipped = "4.0.6.263070100", manual = true))
        assertTrue(shouldPrompt("4.0.6.263070100", never = true, manual = true))
        assertTrue(shouldPrompt("4.0.6.263070100", skipped = "4.0.6.263070100", never = true, manual = true))
    }
}
