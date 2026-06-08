# Calendar Theme UX Upgrade Requirements

## Introduction

本规格定义 MemoriaBox 日历、主题、排版和相关新增能力的产品升级范围。目标是在保留“温柔、极简、整洁”和随机颜文字等核心个性设计的前提下，提高日历的可读性、主题一致性、中文排版成熟度和备份导入信任感。

本规格与 `monthly-diary-calendar-summary` 已完成能力兼容，聚焦视觉系统、交互体验和新增视图形态，不重复实现月度日记总结、小蓝点播放和提醒逻辑。

## Requirements

### Requirement 1: 日历主题统一

**User Story:** 作为用户，我希望日历页面跟随当前主题显示，让页面看起来像同一个产品的一部分。

#### Acceptance Criteria

1. WHEN 用户切换主题 THEN 日历页背景、日期格、选中态、按钮和摘要卡片 SHALL 使用当前主题的语义色。
2. WHEN 当前主题为暗色模式 THEN 日历页 SHALL 保持文字、标记和选中态满足 WCAG AA 对比度。
3. WHEN 当前主题为护眼、暖色、奶油、薄荷或薰衣草 THEN 日历页 SHALL 使用对应柔和色调，避免固定红紫蓝渐变破坏一致性。
4. WHEN 日历存在节气、农历、纪念日、日记标记 THEN 标记颜色 SHALL 来源于统一语义 token。

### Requirement 2: 日期选中态清晰化

**User Story:** 作为用户，我希望清楚知道今天、选中日期和有内容日期，避免在密集日历中迷失。

#### Acceptance Criteria

1. WHEN 日期为今天 THEN 日期格 SHALL 显示稳定且克制的“今天”视觉提示。
2. WHEN 日期被选中 THEN 日期格 SHALL 显示高优先级选中态，并与“今天”状态可区分。
3. WHEN 日期同时为今天且被选中 THEN 日期格 SHALL 使用组合状态，明确表达两个状态。
4. WHEN 日期存在日记或纪念日 THEN 日期格 SHALL 显示内容标记，且不会遮挡日期数字和农历文字。
5. WHEN 系统字体放大 THEN 日期格 SHALL 保持文字可读，标记不溢出。

### Requirement 3: 当天摘要区

**User Story:** 作为用户，我希望点选日期后立刻看到当天最重要的信息，减少进入详情页的成本。

#### Acceptance Criteria

1. WHEN 用户选中日期 THEN 日历下方 SHALL 显示当天摘要区。
2. WHEN 当天有纪念日 THEN 摘要区 SHALL 展示纪念日名称、倒数或已过天数。
3. WHEN 当天有日记 THEN 摘要区 SHALL 展示日记数量和最近一条日记摘要。
4. WHEN 当天没有内容 THEN 摘要区 SHALL 展示温柔的空状态和创建入口。
5. WHEN 屏幕为 compact 尺寸 THEN 摘要区 SHALL 使用短文案和紧凑间距。

### Requirement 4: 日历标记系统统一

**User Story:** 作为用户，我希望不同类型内容的标记一致且容易辨认。

#### Acceptance Criteria

1. WHEN 日期存在纪念日 THEN 标记 SHALL 使用纪念日语义样式。
2. WHEN 日期存在日记 THEN 标记 SHALL 使用日记语义样式，并兼容已有小蓝点能力。
3. WHEN 日期存在农历节日或节气 THEN 标记 SHALL 使用节日语义样式。
4. WHEN 同一天存在多类内容 THEN 日期格 SHALL 使用最多 3 个标记或聚合标记，避免拥挤。
5. WHEN 用户查看图例 THEN 系统 SHALL 解释不同标记的含义。

### Requirement 5: 主题 token 系统

**User Story:** 作为维护者，我希望主题颜色有语义 token，减少页面硬编码颜色。

#### Acceptance Criteria

1. WHEN 页面需要日历、卡片、徽章、摘要、危险操作颜色 THEN SHALL 从语义 token 读取。
2. WHEN 新增主题 THEN 开发者 SHALL 能通过补齐 token 让核心页面自动适配。
3. WHEN token 缺失 THEN 系统 SHALL 有 Material colorScheme 的安全回退。
4. WHEN 扫描 UI 代码 THEN 日历升级相关组件 SHALL 避免新增硬编码主题色。

### Requirement 6: 中文排版层级补齐

**User Story:** 作为用户，我希望标题、正文、标签和辅助信息层级清楚，长中文也舒适可读。

#### Acceptance Criteria

1. WHEN 页面显示大标题、区块标题、正文、辅助文字、标签 THEN SHALL 使用明确的 Typography 层级。
2. WHEN 文案较长 THEN 文本 SHALL 设置合理行高、截断或换行策略。
3. WHEN 系统字体放大 THEN 关键操作按钮和日期文本 SHALL 保持可用。
4. WHEN 中英文数字混排 THEN 倒数天数、日期、标题 SHALL 保持对齐和可读。

### Requirement 7: 主题推荐与分层

**User Story:** 作为用户，我希望主题选择有清晰分组，能快速找到适合自己的风格。

#### Acceptance Criteria

1. WHEN 用户打开主题设置 THEN 主题 SHALL 按推荐、护眼、个性、暗色等分组展示。
2. WHEN 当前主题已选中 THEN 主题项 SHALL 显示明确选中态。
3. WHEN 主题适合夜间或护眼 THEN 主题项 SHALL 有简短说明。
4. WHEN 用户首次进入主题设置 THEN 默认推荐 SHALL 与“温柔、极简、整洁”的产品方向一致。

### Requirement 8: 主题预览墙

**User Story:** 作为用户，我希望切换主题前看到它在日历和卡片上的效果。

#### Acceptance Criteria

1. WHEN 用户查看主题列表 THEN 每个主题 SHALL 展示迷你预览卡。
2. WHEN 预览卡渲染 THEN SHALL 包含背景、卡片、日期选中态和标记色。
3. WHEN 用户点击预览卡 THEN 系统 SHALL 应用对应主题或进入确认动作。
4. WHEN 屏幕较窄 THEN 预览墙 SHALL 自适应为单列或横向滚动布局。

### Requirement 9: 周视图

**User Story:** 作为用户，我希望在关注最近几天时使用更轻量的周视图。

#### Acceptance Criteria

1. WHEN 用户切换到周视图 THEN 日历 SHALL 显示当前周 7 天。
2. WHEN 用户左右滑动或点击切换 THEN 周视图 SHALL 切换上一周或下一周。
3. WHEN 周视图存在内容标记 THEN SHALL 沿用统一标记系统。
4. WHEN 用户选择某一天 THEN 下方摘要区 SHALL 同步更新。

### Requirement 10: 议程列表视图

**User Story:** 作为用户，我希望按时间线查看近期纪念日和日记，不只依赖月历格子。

#### Acceptance Criteria

1. WHEN 用户切换到议程视图 THEN 系统 SHALL 按日期展示未来和最近内容。
2. WHEN 某日包含多个事项 THEN 列表项 SHALL 聚合显示并保留进入详情入口。
3. WHEN 没有近期内容 THEN 系统 SHALL 展示空状态和创建入口。
4. WHEN 内容很多 THEN 列表 SHALL 支持流畅滚动和合理分页或懒加载。

### Requirement 11: 日期跳转

**User Story:** 作为用户，我希望快速跳到某年某月或回到今天。

#### Acceptance Criteria

1. WHEN 用户点击月份标题或跳转入口 THEN 系统 SHALL 提供年月选择能力。
2. WHEN 用户确认年月 THEN 日历 SHALL 跳转到目标月份。
3. WHEN 用户点击“回到今天” THEN 日历 SHALL 回到今天所在日期并选中今天。
4. WHEN 目标年月无内容 THEN 日历 SHALL 正常显示空月份状态。

### Requirement 12: 月度记忆热力图

**User Story:** 作为用户，我希望看到本月记录频率和生活密度。

#### Acceptance Criteria

1. WHEN 月份存在日记或纪念日数据 THEN 系统 SHALL 可展示月度记忆热力图。
2. WHEN 某天内容较多 THEN 热力强度 SHALL 更明显。
3. WHEN 用户切换主题 THEN 热力图 SHALL 使用当前主题适配色。
4. WHEN 数据为空 THEN 热力图 SHALL 显示鼓励式空状态。

### Requirement 13: 纪念日故事卡片

**User Story:** 作为用户，我希望重要纪念日像故事一样被展示，而非只是一行日期。

#### Acceptance Criteria

1. WHEN 纪念日临近 THEN 系统 SHALL 可展示故事卡片。
2. WHEN 纪念日有关联日记或图片 THEN 卡片 SHALL 展示摘要或封面。
3. WHEN 用户点击故事卡片 THEN 系统 SHALL 进入对应纪念日或日记详情。
4. WHEN 内容缺少素材 THEN 卡片 SHALL 使用主题化占位视觉。

### Requirement 14: 生日关系提醒

**User Story:** 作为用户，我希望生日提醒能体现对象关系和提醒优先级。

#### Acceptance Criteria

1. WHEN 纪念日类型为生日 THEN 系统 SHALL 支持展示关系标签。
2. WHEN 生日临近 THEN 摘要或议程视图 SHALL 优先显示生日事项。
3. WHEN 用户查看生日项 THEN 系统 SHALL 展示年龄或倒数信息。
4. WHEN 未设置关系 THEN 系统 SHALL 使用通用生日展示。

### Requirement 15: 待办轻量看板

**User Story:** 作为用户，我希望围绕纪念日记录轻量待办，比如准备礼物或写卡片。

#### Acceptance Criteria

1. WHEN 用户为日期或纪念日添加待办 THEN 系统 SHALL 保存待办标题和完成状态。
2. WHEN 日期有待办 THEN 日历摘要和议程列表 SHALL 显示待办提示。
3. WHEN 用户完成待办 THEN UI SHALL 即时更新完成状态。
4. WHEN 待办为空 THEN 看板 SHALL 显示轻量创建入口。

### Requirement 16: 小组件主题同步

**User Story:** 作为用户，我希望桌面小组件和应用内主题保持一致。

#### Acceptance Criteria

1. WHEN 用户切换主题 THEN 小组件 SHALL 使用对应主题色或最接近的安全配色。
2. WHEN 小组件运行在暗色系统环境 THEN SHALL 保持文字可读。
3. WHEN 小组件无法完整支持复杂主题 THEN SHALL 使用简化 token。
4. WHEN 主题更新后 THEN 小组件 SHALL 触发刷新。

### Requirement 17: 备份导入确认与导入摘要

**User Story:** 作为用户，我希望导入备份前明确知道现有数据会被保护，导入后知道发生了什么。

#### Acceptance Criteria

1. WHEN 用户选择导入备份 THEN 系统 SHALL 展示确认说明，明确合并导入和保护当前数据。
2. WHEN 备份需要密码 THEN 系统 SHALL 提供密码输入并保护输入内容。
3. WHEN 导入完成 THEN 系统 SHALL 展示导入摘要，包括新增、更新、跳过或失败数量。
4. WHEN 导入失败 THEN 系统 SHALL 展示可理解错误，不泄露敏感信息。
5. WHEN 当前已有数据 THEN 导入 SHALL 保留旧数据。

### Requirement 18: 核心个性设计保护

**User Story:** 作为产品维护者，我希望优化时保留用户确认的核心个性设计。

#### Acceptance Criteria

1. WHEN 修改底部导航中间项 THEN 开发者 SHALL 保留随机颜文字逻辑。
2. WHEN 新手引导仍在进行 THEN 系统 MAY 在中间颜文字附近提示它可以用于新增。
3. WHEN 新手引导结束 THEN 中间项 SHALL 只显示随机颜文字，不显示新增提示、角标、气泡或任何提醒。
4. WHEN 发现看似不常规但可能是产品个性的交互 THEN 开发者 SHALL 先确认再改动。
5. WHEN 进行视觉标准化 THEN 系统 SHALL 保留 MemoriaBox 的温柔手账气质。
6. WHEN 需求和实现冲突 THEN SHALL 优先保护已确认的核心交互。

### Requirement 19: 好友管理生日排序保留

**User Story:** 作为用户，我希望好友生日按近期优先展示，同时所有好友都被保留在列表中。

#### Acceptance Criteria

1. WHEN 好友生日距离今天在一个月以内 THEN 好友 SHALL 按距离今天越近越靠前排序。
2. WHEN 好友生日超过一个月 THEN 好友 SHALL 继续保留在列表中，并排在一个月内生日好友之后。
3. WHEN 好友生日为空 THEN 好友 SHALL 保留在列表中，并排在有生日信息好友之后。
4. WHEN 好友生日跨年临近 THEN 排序 SHALL 使用下一次生日日期计算距离。
5. WHEN 用户删除好友后 THEN 列表 SHALL 正常刷新并保持上述排序规则。
