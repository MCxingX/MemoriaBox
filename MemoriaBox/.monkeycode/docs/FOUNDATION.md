# MemoriaBox 地基说明

本文件用于后续 AI 或开发者接手时快速确认功能边界、核心数据流和发布前检查清单。内容基于当前代码仓库实际实现。

## 不可遗漏的核心功能

- 盒子与日子：用户可以创建盒子，管理倒数日、纪念日、已过日、生日和待办。
- 自定义卡片：事件卡支持封面、海报、玻璃、分栏、光轨、徽章模板，支持头像/背景图、渐变色、文字颜色和展示字段。
- 卡片模板切换：首页盒子详情中的事件卡可通过左右拖动切换模板，并保存到 `Event.cardTemplate`。
- 生日逻辑：`EventType.BIRTHDAY` 使用下一次生日计算；当天生日显示生日快乐和祝福词；普通倒数日不显示生日祝福。
- 底部导航：中间项是随机颜文字核心交互，不能替换成标准添加按钮。
- 日历日记：日历支持按日期查看/新增/编辑/删除日记，并支持日记媒体和背景媒体。
- 月总结：月总结从日记和 `diary_media` 构建，图片直接显示，视频可按顺序自动播放。
- 好友管理：好友按生日距离排序，一月内生日靠前，超过一月继续保留，未设置生日排最后。
- 备份导入：导入采用合并追加策略，不能清空用户旧数据；盒子、标签、好友、好友关系、事件、事件标签、日记、日记媒体和日志都参与导入。
- 自动备份：盒子、事件、日记、日记媒体、好友等用户数据变更后应触发 `BackupManager.onDataChanged()`。

## 关键数据流

- `MemoriaApp` 创建主数据库和 `BackupManager`。
- `AppDatabase` 当前版本为 7，实体包括 `Box`、`Event`、`Friend`、`Label`、`FriendRelation`、`EventLabel`、`LogEntry`、`DiaryEntry`、`DiaryMedia`。
- `CalendarViewModel.allDiaries` 订阅全部日记。
- `CalendarViewModel.allDiaryMedia` 基于全部日记 ID 订阅全部当前日记媒体，日历页应使用它构建 `diaryMediaMap`。
- `CalendarViewModel.saveDiaryWithMedia()` 保存日记和媒体后触发自动备份。
- `MonthlySummaryHelper.buildSummary()` 基于某月日记和媒体生成月总结 slide。
- `BackupManager.mergeDatabase()` 使用事务合并导入数据，当前策略是主键相同替换，主键不同保留。

## 发布前检查清单

- 事件卡仍显示自定义模板，不退化为统一普通倒计时卡。
- 生日当天显示生日祝福，倒数日当天保持普通倒数日表达。
- 生日过后显示下一年生日倒计时，不显示负数。
- 日历只保留月历主流程，不恢复故事、周视图、日程视图切换入口。
- 新增带图片或视频的日记后，日历详情和月总结都有可见内容。
- 好友列表不筛掉任何好友，未设置生日的好友仍显示。
- 导入备份不清空现有数据。
- 底部导航中间随机颜文字仍存在。

## 推荐验证命令

```bash
# Kotlin 编译验证
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 ANDROID_HOME=/usr/lib/android-sdk
/tmp/gradle-8.7/bin/gradle :app:compileDebugKotlin --no-daemon

# Debug 包构建验证
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 ANDROID_HOME=/usr/lib/android-sdk
/tmp/gradle-8.7/bin/gradle :app:assembleDebug --no-daemon

# Release 包构建验证
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 ANDROID_HOME=/usr/lib/android-sdk
/tmp/gradle-8.7/bin/gradle :app:assembleRelease --no-daemon
```

## 给后续 AI 的接手建议

- 优先读 `PRODUCT.md`、`.monkeycode/MEMORY.md` 和本文件，再改代码。
- 优先小步修复，避免重写长文件。
- 涉及 UI 时保留 Material 3 产品感、动态字体兼容和多屏幕自适应。
- 涉及数据时先确认备份、导入、自动备份和 Room migration 是否同步。
- 发布新版本时递增 `versionCode` 和 `versionName`，创建新 tag，不覆盖旧 tag。
