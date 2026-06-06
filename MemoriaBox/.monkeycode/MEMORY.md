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

[Android UI 多分辨率自适应]
- Date: 2026-06-05
- Context: 用户反馈 vivo Y200i 日记蓝点显示问题，并强调项目存在多种 Android 机型和分辨率
- Instructions:
  - 涉及 Android UI 布局修复时，优先使用多分辨率、自适应和字体缩放兼容方案。
  - 避免以单一机型、单一分辨率或固定高度作为布局修复依据。
  - 日历、卡片、弹窗等密集布局需要兼容不同屏幕宽度、系统字体大小和显示大小。

[UI 调色排版 Skill 工作流]
- Date: 2026-06-06
- Context: 用户要求后续修改 UI、调色、排版问题时固定调用两个设计 skill
- Instructions:
  - 后续涉及 UI 修改、调色、视觉层级、布局排版、页面高级感或审美优化时，优先应用 `impeccable` 和 `design-taste-frontend` 两个 skill 的规则。
  - 若当前 opencode 会话尚未加载新安装的 skill，应直接读取 `/root/.config/opencode/skills/impeccable/SKILL.md` 和 `/root/.config/opencode/skills/design-taste-frontend/SKILL.md` 后执行。
  - UI 改造需要兼顾 `impeccable` 的产品级完成度、可访问性、对比度、响应式要求，以及 `design-taste-frontend` 的 anti-slop、设计读法和排版克制规则。
