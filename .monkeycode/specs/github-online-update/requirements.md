# 在线更新需求文档

## 简介

在线更新功能通过 MemoriaBox 官方 GitHub Release 检测新版本，在用户确认后下载 APK、验证安装包，并在用户再次确认后交由 Android 系统安装器覆盖安装。

## 术语

- **官方 Release**：`MCxingX/MemoriaBox` 仓库中已发布且未标记为草稿或预发布的 GitHub Release。
- **官方校验文件**：官方 Release 中与 APK 同名并追加 `.sha256` 的 SHA-256 文本资产。
- **镜像候选**：仅负责传输官方 APK URL 内容的 HTTPS 下载加速地址。
- **更新包**：官方 Release 中名称以 `.apk` 结尾的 Android 安装包。

## 需求

### 需求 1：版本检测

**用户故事：** 作为用户，我希望应用检测官方 GitHub Release，以便及时获得新版本。

#### 验收标准

1. WHEN 应用进入主界面，更新系统 SHALL 自动请求官方 Release 元数据。
2. WHEN 用户点击“版本检测”，更新系统 SHALL 立即请求官方 Release 元数据并展示检测结果。
3. WHEN 官方版本高于当前版本，更新系统 SHALL 展示版本号、更新说明和更新操作。
4. WHEN 当前版本为最新版本，更新系统 SHALL 向手动检测用户展示“已是最新版本”。

### 需求 2：用户选择与传输回退

**用户故事：** 作为用户，我希望自主选择是否下载新版本，以便控制流量和更新时间。

#### 验收标准

1. WHEN 检测到新版本，更新系统 SHALL 展示“稍后”和“下载更新”操作并等待用户选择。
2. WHEN 用户选择“下载更新”，更新系统 SHALL 开始下载更新包。
3. WHEN GitHub 直连下载成功，更新系统 SHALL 使用直连下载结果。
4. IF GitHub 直连下载失败，更新系统 SHALL 并行测试 HTTPS 镜像候选并选择有效候选中延迟最低的地址。
5. WHILE 更新包正在下载，更新系统 SHALL 展示下载进度。
6. IF 下载失败，更新系统 SHALL 保留当前应用并展示可重试的错误信息。

### 需求 3：安装包校验

**用户故事：** 作为用户，我希望应用在安装前验证更新包，以便确认安装包来自预期版本且内容完整。

#### 验收标准

1. WHEN 读取官方 Release，更新系统 SHALL 从官方 GitHub 地址获取官方校验文件。
2. WHEN 更新包下载完成，更新系统 SHALL 比对更新包 SHA-256 与官方校验值。
3. WHEN SHA-256 匹配，更新系统 SHALL 校验包名等于 `com.memoriabox`。
4. WHEN 包名匹配，更新系统 SHALL 校验 APK 版本名称等于 Release 标签版本且版本码高于当前版本码。
5. WHEN 版本校验通过，更新系统 SHALL 校验 APK 签名证书与当前已安装应用签名证书一致。
6. IF 任一校验失败，更新系统 SHALL 删除不可用结果并阻止安装。

### 需求 4：用户确认与覆盖安装

**用户故事：** 作为用户，我希望决定安装时机，以便控制应用更新。

#### 验收标准

1. WHEN 新版本可用，更新系统 SHALL 允许用户选择“稍后”或“下载更新”。
2. WHEN 更新包完成校验，更新系统 SHALL 允许用户选择“稍后”或“安装更新”。
3. WHEN 用户选择“安装更新”，更新系统 SHALL 在进入系统安装器前再次请求用户确认。
4. IF Android 未授权当前应用安装未知来源应用，更新系统 SHALL 打开当前应用的安装来源授权页面。
5. WHEN 安装来源授权有效，更新系统 SHALL 使用 FileProvider URI 打开 Android 系统安装器。
6. WHEN Android 完成覆盖安装，应用 SHALL 尝试重新打开主界面并提供可点击的启动通知作为回退入口。

### 需求 5：发布资产

**用户故事：** 作为发布者，我希望每个 GitHub Release 都包含可验证资产，以便客户端可靠更新。

#### 验收标准

1. WHEN 发布新版本，发布流程 SHALL 上传 `app-release.apk`。
2. WHEN 发布新版本，发布流程 SHALL 上传 `app-release.apk.sha256`。
3. WHEN 发布新版本，GitHub Release 标签 SHALL 与 APK `versionName` 一致并使用 `v` 前缀。
4. WHEN 发布新版本，APK SHALL 使用与已发布版本一致的签名证书。
