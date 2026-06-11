# MemoriaBox

MemoriaBox 是一个本地优先的 Android 回忆与纪念日工具，用来管理盒子、倒数日、纪念日、生日、待办、好友、日记、照片、视频和月度总结。

当前最新版本：`v3.2.16`

下载地址：https://github.com/MCxingX/MemoriaBox/releases/tag/v3.2.16

APK：https://github.com/MCxingX/MemoriaBox/releases/download/v3.2.16/app-release.apk

SHA256：`328e5fefa86f17383ad8a692e17552e96ac21745d62e3dbc92d95321fa21e75b`

## v3.2.16 更新

- 恢复“我的卡片”自定义事件卡模板，保留背景图、头像、渐变、文字颜色和展示字段配置。
- 修复生日倒计时逻辑，生日过后自动计算下一年，当天生日显示专属祝福。
- 精简日历体验，保留月历、日记和事件标记主流程。
- 修复日记图片和视频在日历详情、月总结中的显示问题。
- 支持日记视频播放。
- 月总结支持图片和视频按顺序融合播放。
- 月总结改为手机沉浸式全屏展示，平板和大屏使用近全屏面板。
- 补齐日记、媒体和好友变更后的自动备份触发。
- 新增 `.monkeycode/docs/FOUNDATION.md`，作为后续维护和发布检查的地基说明。

## 核心功能

- 盒子管理：创建、编辑、归档和整理不同主题的纪念盒子。
- 事件管理：支持倒数日、纪念日、已过日、生日和待办。
- 自定义卡片：支持 Hero、Poster、Glass、Split、Neon、Badge 等模板，支持背景图、头像、渐变色、文字色和展示字段。
- 生日提醒：生日当天显示祝福，生日已过后自动进入下一年倒计时。
- 日历日记：在月历中查看事件和日记标记，按日期新增、编辑和查看日记。
- 媒体记录：日记支持图片、视频和背景媒体。
- 月总结：按月聚合日记文字、图片和视频，支持沉浸式播放。
- 好友管理：记录好友头像、生日和关系标签，按生日临近程度排序。
- 备份恢复：支持本地自动备份、手动导出、合并导入和 WebDAV 配置。
- 桌面小组件：提供盒子、倒数日和日历类小组件。

## 数据安全

- 应用采用本地优先的数据模型，核心数据存储在设备本地 Room 数据库中。
- 备份导入采用合并策略，保留已有数据，避免导入时清空旧数据。
- 用户数据变更后会触发自动备份，包括盒子、事件、日记、日记媒体和好友。
- 备份文件和 WebDAV 配置由应用内备份模块统一处理。

## 技术栈

| 模块 | 技术 |
| --- | --- |
| 语言 | Kotlin 2.0.0 |
| UI | Jetpack Compose + Material 3 |
| 架构 | MVVM + Repository + Flow |
| 数据库 | Room 2.6.1 |
| 数据库加密支持 | SQLCipher 4.5.4 |
| 异步 | Kotlin Coroutines |
| 导航 | Navigation Compose |
| 图片加载 | Coil |
| 网络 | OkHttp |
| 设置存储 | DataStore Preferences |
| 文件访问 | Storage Access Framework / DocumentFile |

## 环境要求

- JDK 17
- Android Gradle Plugin 8.6.1
- Android SDK 35
- Gradle 8.7 或兼容版本

## 构建

```bash
# 设置 Android 构建环境
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 ANDROID_HOME=/usr/lib/android-sdk

# Kotlin 编译验证
/tmp/gradle-8.7/bin/gradle :app:compileDebugKotlin --no-daemon

# Debug 包构建
/tmp/gradle-8.7/bin/gradle :app:assembleDebug --no-daemon

# Release 包构建
/tmp/gradle-8.7/bin/gradle :app:assembleRelease --no-daemon
```

Release APK 输出路径：`app/build/outputs/apk/release/app-release.apk`

## 项目结构

```text
app/src/main/java/com/memoriabox/
├── data/
│   ├── dao/             # Room DAO
│   ├── model/           # 数据实体和枚举
│   └── repository/      # Repository 层
├── database/            # Room 数据库、迁移和修复逻辑
├── receiver/            # 提醒广播接收器
├── ui/
│   ├── screen/          # 主要页面
│   ├── screen/components/ # 页面组件、事件卡、日记、月总结
│   ├── theme/           # Material 3 主题和设计 token
│   └── utils/           # UI 自适应工具
├── utils/               # 备份、导入、图片/视频、通知、月总结工具
├── viewmodel/           # ViewModel
└── widget/              # Android 小组件
```

## 维护文档

- `.monkeycode/docs/FOUNDATION.md`：核心功能边界、数据流和发布前检查清单。
- `.monkeycode/specs/calendar-theme-ux-upgrade/`：日历、主题、好友管理和 UI 升级规格。
- `.monkeycode/specs/monthly-diary-calendar-summary/`：日记、日历和月总结功能规格。
- `PRODUCT.md`：产品定位、用户画像、视觉原则和可访问性要求。

## 发布检查

- 确认 `versionCode` 和 `versionName` 已递增。
- 确认事件卡仍保留自定义模板和左右切换能力。
- 确认生日当天、生日已过、普通倒数日文案正确。
- 确认日历详情能显示日记图片和视频。
- 确认月总结能按顺序播放图片和视频。
- 确认备份导入使用合并策略，保留旧数据。
- 确认底部导航中间项仍为随机颜文字。
- 构建 Release APK 并记录 SHA256。
- 创建新的 Git tag 和 GitHub Release。

## 最新 Release

- `v3.2.16`：https://github.com/MCxingX/MemoriaBox/releases/tag/v3.2.16
- Commit：`92c9b1f fix: restore custom cards and diary media playback`
- APK：`app-release.apk`
- 大小：`15,846,845 bytes`
- SHA256：`328e5fefa86f17383ad8a692e17552e96ac21745d62e3dbc92d95321fa21e75b`

## 许可证

本项目仅供学习和参考使用。
