# 念记 - 纪念日管理系统

集纪念日管理、倒数日提醒、个性化卡片展示于一体的情感记录工具。

## 功能特性

- 盒子（分组）管理：创建、编辑、删除、归档盒子
- 日子（事件）管理：倒数日、纪念日、正计时，支持农历选择
- 个性化排版：1x1、2x4、3x3等多种卡片布局模式
- 自定义背景：色盘选择、图片上传、模糊滤镜
- 本地自动备份：防抖策略，保留5份备份
- 手动导出/导入：加密备份文件
- WebDAV备份：支持云端存储
- 操作日志：完整的审计日志功能

## 环境要求

- Android Studio Hedgehog 2023.1.1 或更高版本
- JDK 17
- Android SDK API 34
- Android SDK Build-Tools 34.0.0

## 构建步骤

### 1. 打开项目

```bash
cd NianJi
```

### 2. 使用Android Studio打开项目

1. 启动 Android Studio
2. 选择 **Open** → 选择此项目文件夹
3. 等待 Gradle 同步完成

### 3. 构建APK

```bash
./gradlew assembleDebug
```

### 4. 安装到设备

```bash
./gradlew installDebug
```

## 技术栈

| 组件 | 技术 |
|------|------|
| UI | Jetpack Compose (Material 3) |
| 架构 | MVVM + Repository |
| 数据库 | Room + SQLCipher (加密) |
| 异步处理 | Kotlin Coroutines + Flow |
| 导航 | Navigation Compose |
| 文件授权 | Storage Access Framework (SAF) |
| 加密 | AES-256-GCM |
| WebDAV | OkHttp |
| 图片加载 | Coil |

## 项目结构

```
com.memoriabox/
├── data/
│   ├── model/       # 数据模型
│   ├── dao/         # 数据访问对象
│   ├── repository/  # 数据仓库
│   └── backup/      # 备份逻辑
├── ui/
│   ├── theme/       # 主题配置
│   ├── screen/      # 界面组件
│   ├── navigation/  # 导航配置
│   └── components/  # 通用组件
├── viewmodel/       # ViewModel
└── utils/           # 工具类
```

## 备份策略

- **自动备份**：每次数据变更后延迟20秒（可配置10/20/30秒），保留5份
- **手动备份**：用户点击导出按钮
- **定时备份**（可选）：按用户配置的时间间隔

## 安全特性

- 数据库加密：SQLCipher
- 备份文件加密：AES-256-GCM + PBKDF2
- WebDAV密码存储：EncryptedSharedPreferences
- 操作日志不包含敏感信息

## 许可证

本项目仅供学习和参考使用
