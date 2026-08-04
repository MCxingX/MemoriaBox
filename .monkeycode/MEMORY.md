# 用户指令记忆

本文件记录了用户的指令、偏好和教导，用于在未来的交互中提供参考。

## 格式

### 用户指令条目
用户指令条目应遵循以下格式：

[用户指令摘要]
- Date: [YYYY-MM-DD]
- Context: [提及的场景或时间]
- Instructions:
  - [用户教导或指示的内容，逐行描述]

### 项目知识条目
Agent 在任务执行过程中发现的条目应遵循以下格式：

[项目知识摘要]
- Date: [YYYY-MM-DD]
- Context: Agent 在执行 [具体任务描述] 时发现
- Category: [运维部署|构建方法|测试方法|排错调试|工作流协作|环境配置]
- Instructions:
  - [具体的知识点，逐行描述]

## 去重策略
- 添加新条目前，检查是否存在相似或相同的指令
- 若发现重复，跳过新条目或与已有条目合并
- 合并时，更新上下文或日期信息
- 这有助于避免冗余条目，保持记忆文件整洁

## 条目

MemoriaBox Android 构建环境
- Date: 2026-08-04
- Context: Agent 在构建 Release APK 验证备份格式改动时更新
- Category: 构建方法
- Instructions:
  - 构建 MemoriaBox APK 使用 OpenJDK 17、Android SDK `/opt/android-sdk` 和项目自带 `./gradlew`。
  - 构建命令：`export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 ANDROID_HOME=/opt/android-sdk ANDROID_SDK_ROOT=/opt/android-sdk && ./gradlew :app:assembleRelease --no-daemon`。
  - Debug 编译验证命令：`./gradlew :app:compileDebugKotlin --no-daemon`。
  - 单元测试命令：`./gradlew :app:testDebugUnitTest --no-daemon`。
  - Release APK 签名验证使用命令：`/opt/android-sdk/build-tools/34.0.0/apksigner verify --print-certs app/build/outputs/apk/release/app-release.apk`。
  - 签名配置位于 `keystore.properties`（已被 .gitignore 忽略），Release 构建必须依赖该文件存在。
  - GitHub Release 发布使用 `gh release create <tag> <apk> --title ... --notes ...`。

MemoriaBox 正式签名规则
- Date: 2026-08-04
- Context: 用户明确要求将所有 GitHub Release APK 改用正式签名
- Category: 工作流协作
- Instructions:
  - 上传 GitHub Release 的 APK 一律使用正式签名，禁止使用调试签名。
  - 正式签名 keystore：`app/membox-release.keystore`，别名 `membox`，有效期 25 年（10,950 天）。
  - 密码保存在本机 `keystore.properties` 与 `/tmp/membox-keystore-pw.txt`，两文件均不入库。
  - 密钥库与密码是唯一正式签名凭证，丢失后将无法更新已发布应用，务必提醒用户妥善备份。
  - 替换 Release 资产流程：`gh release delete-asset <tag> app-release.apk --yes`，然后 `gh release upload <tag> <new-apk>`。
