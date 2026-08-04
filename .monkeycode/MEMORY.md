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

MemoriaBox 签名策略
- Date: 2026-08-04
- Context: 用户决定维持使用调试密钥（曾尝试正式密钥，最终切回）
- Category: 工作流协作
- Instructions:
  - GitHub Release APK 使用调试密钥签名：`app/membox-debug.keystore`，别名 `membox`，store/key 密码均为 `android`。
  - 正式密钥 `app/membox-release.keystore` 不再使用；若再次需要其密码可查看 `/tmp/membox-keystore-pw.txt`（若环境保留）。
  - 调试密钥丢失后同样无法用新密钥覆盖升级（Android 按签名识别应用），任何密钥都有此限制。
  - 替换 Release 资产流程：`gh release delete-asset <tag> app-release.apk --yes`，然后 `gh release upload <tag> <new-apk>`。
  - 构建验证命令：`/opt/android-sdk/build-tools/34.0.0/apksigner verify --print-certs app/build/outputs/apk/release/app-release.apk`，预期 SHA-256 指纹 `b8b434d797525f47f036e4c58386d02acb7a59524acd98bc9a257b56f4216e37`。
