# MemoriaBox 构建指南

本文件供 AI Agent 自动化构建使用。

## 构建环境要求

| 组件 | 版本 | 说明 |
|------|------|------|
| JDK | 17 | OpenJDK 17.x |
| Android SDK | 35 | compileSdk = 35 |
| Gradle | 8.7 | 项目不使用 gradlew，需系统安装 |
| Android Build Tools | 35.0.0 | 自动下载 |
| Android Platform Tools | latest | 自动下载 |

## 环境安装（Debian/Ubuntu）

```bash
# 安装 JDK 17
apt-get update && apt-get install -y openjdk-17-jdk

# 安装 Android SDK
apt-get install -y android-sdk

# 安装 Gradle 8.7（如果系统没有）
# 方式一：手动安装
wget https://services.gradle.org/distributions/gradle-8.7-bin.zip -O /tmp/gradle-8.7.zip
unzip /tmp/gradle-8.7.zip -d /tmp/
export PATH="/tmp/gradle-8.7/bin:$PATH"

# 方式二：使用 sdkmanager 安装构建工具
sdkmanager "platforms;android-35" "build-tools;35.0.0"
```

## 环境变量

每次构建前必须设置：

```bash
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
export ANDROID_HOME=/usr/lib/android-sdk
export PATH="/tmp/gradle-8.7/bin:$PATH"
```

如果 Gradle 安装在其他位置，修改 `PATH` 指向正确的 `gradle` 二进制文件。

## 构建命令

### Kotlin 编译验证（最快，约 30 秒）

```bash
/tmp/gradle-8.7/bin/gradle :app:compileDebugKotlin --no-daemon
```

### Debug 包（约 1 分钟）

```bash
/tmp/gradle-8.7/bin/gradle :app:assembleDebug --no-daemon
```

### Release 包（约 3-5 分钟，含 R8 混淆）

```bash
/tmp/gradle-8.7/bin/gradle :app:assembleRelease --no-daemon
```

## 输出路径

| 类型 | 路径 |
|------|------|
| Debug APK | `app/build/outputs/apk/debug/app-debug.apk` |
| Release APK | `app/build/outputs/apk/release/app-release.apk` |
| Release Mapping | `app/build/outputs/mapping/release/mapping.txt` |

## Release 签名配置

签名信息在 `keystore.properties`：

```
storeFile=app/memoriabox-release.jks
storePassword=***
keyAlias=memoriabox
keyPassword=***
```

`build.gradle.kts` 会自动读取 `keystore.properties` 配置签名：

```kotlin
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        load(keystorePropertiesFile.inputStream())
    }
}

signingConfigs {
    create("release") {
        storeFile = rootProject.file(keystoreProperties["storeFile"] as String)
        storePassword = keystoreProperties["storePassword"] as String
        keyAlias = keystoreProperties["keyAlias"] as String
        keyPassword = keystoreProperties["keyPassword"] as String
    }
}
```

## 版本管理

版本号在 `app/build.gradle.kts` 中：

```kotlin
defaultConfig {
    versionCode = 32      // 整数，每次发布递增
    versionName = "3.2.16" // 语义化版本号
}
```

发布新版本时：
1. 递增 `versionCode`
2. 更新 `versionName`
3. 构建 Release APK
4. 计算 SHA256：`sha256sum app/build/outputs/apk/release/app-release.apk`
5. 创建 Git tag：`git tag v3.2.16`
6. 创建 GitHub Release

## GitHub Release 创建

```bash
gh release create v3.2.16 \
  "app/build/outputs/apk/release/app-release.apk" \
  --title "MemoriaBox v3.2.16" \
  --notes "Release notes here"
```

## 常见问题

### Release 构建超时

R8 混淆在首次构建时较慢，设置超时为 300 秒（5 分钟）：

```bash
timeout 300 /tmp/gradle-8.7/bin/gradle :app:assembleRelease --no-daemon
```

### ANDROID_HOME 未设置

如果 `ANDROID_HOME` 为空，Gradle 会报错找不到 SDK。确保设置：

```bash
export ANDROID_HOME=/usr/lib/android-sdk
```

### keystore 文件不存在

如果 `app/memoriabox-release.jks` 不存在，Release 构建会失败。
Debug 构建不受影响。

### Gradle 版本不匹配

项目要求 Gradle 8.7。不要使用项目自带的 `./gradlew`（可能版本不一致），
使用系统安装的 `/tmp/gradle-8.7/bin/gradle`。

## 项目结构速查

```
MemoriaBox/
├── app/
│   ├── build.gradle.kts          # 应用构建配置
│   ├── memoriabox-release.jks     # Release 签名密钥
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/memoriabox/   # Kotlin 源码
│       └── res/                   # 资源文件
├── build.gradle.kts               # 根项目构建配置
├── settings.gradle.kts            # 项目设置
├── gradle/libs.versions.toml      # 依赖版本管理
├── keystore.properties            # 签名配置（不要提交到公开仓库）
├── AGENTS.md                      # 本文件
├── README.md                      # 项目说明
├── PRODUCT.md                     # 产品设计上下文
└── .monkeycode/                   # AI 维护文档
    ├── docs/FOUNDATION.md         # 地基说明
    ├── MEMORY.md                  # 用户指令记忆
    └── specs/                     # 功能规格文档
```

## CI/CD 集成参考

```yaml
# GitHub Actions 示例
name: Build Release
on:
  push:
    tags: ['v*']
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
      - name: Install Gradle
        run: |
          wget https://services.gradle.org/distributions/gradle-8.7-bin.zip
          unzip gradle-8.7-bin.zip
          echo "$PWD/gradle-8.7/bin" >> $GITHUB_PATH
      - name: Build Release
        run: |
          export ANDROID_HOME=$HOME/android-sdk
          mkdir -p $ANDROID_HOME
          gradle :app:assembleRelease --no-daemon
        env:
          JAVA_HOME: /usr/lib/jvm/java-17-openjdk-amd64
      - name: Create Release
        uses: softprops/action-gh-release@v2
        with:
          files: app/build/outputs/apk/release/app-release.apk
```
