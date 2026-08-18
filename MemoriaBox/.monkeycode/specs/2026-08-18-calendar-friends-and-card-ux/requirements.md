# Requirements Document

## Introduction

本需求调整卡片背景裁剪、工具入口、好友生日、日历月份切换和分类创建流程，保持现有数据与其他功能区域可用。

## Glossary

- **卡片模板**：事件卡当前使用的视觉模板及其展示比例。
- **年度生日**：按月份和日期每年重复的生日记录。
- **指定生日**：包含年份、月份和日期的生日记录。

## Requirements

### Requirement 1: 卡片背景裁剪

**User Story:** 作为用户，我希望在选择卡片背景时看到真实卡片比例，以便准确决定图片显示区域。

#### Acceptance Criteria

1. WHEN 用户选择图片背景，系统 SHALL 显示与当前卡片模板一致的裁剪比例。
2. WHILE 用户调整图片，系统 SHALL 显示最终卡片裁剪区域和缩放位置。
3. WHEN 用户保存裁剪，系统 SHALL 按预览区域保存图片并应用到卡片。

### Requirement 2: 工具入口整理

**User Story:** 作为用户，我希望设置页面保持聚焦，以便快速找到核心功能。

#### Acceptance Criteria

1. WHEN 用户打开设置页面，系统 SHALL 隐藏日子工具箱和更多工具入口。
2. WHEN 系统构建导航，系统 SHALL 移除工具箱专属页面及其路由引用。
3. WHILE 其他页面使用标签、统计、照片和导出功能，系统 SHALL 保持这些功能可用。

### Requirement 3: 好友生日录入

**User Story:** 作为用户，我希望用月份和日期快速记录好友生日。

#### Acceptance Criteria

1. WHEN 用户新增或编辑好友，系统 SHALL 要求好友名称、月份和日期。
2. WHEN 用户未填写年份，系统 SHALL 将生日按年度生日处理。
3. WHEN 用户填写年份，系统 SHALL 保留指定生日的年份信息。

### Requirement 4: 下一生日周期

**User Story:** 作为用户，我希望生日经过当天后立即进入下一年度周期。

#### Acceptance Criteria

1. WHEN 当前日期超过生日日期，系统 SHALL 计算下一年度生日的剩余天数。
2. WHEN 下一年度包含闰年，系统 SHALL 按实际日历天数计算剩余天数。
3. WHEN 当前日期为生日当天，系统 SHALL 显示当天生日状态。

### Requirement 5: 日历月份滑动

**User Story:** 作为用户，我希望通过左右滑动切换月份。

#### Acceptance Criteria

1. WHEN 用户在日历卡片上向左滑动，系统 SHALL 切换到下一个月份。
2. WHEN 用户在日历卡片上向右滑动，系统 SHALL 切换到上一个月份。
3. WHILE 用户进行短距离拖动，系统 SHALL 使用与日子卡片相近的手势灵敏度完成切换。

### Requirement 6: 分类创建

**User Story:** 作为用户，我希望创建分类时只输入名称。

#### Acceptance Criteria

1. WHEN 用户新增分类，系统 SHALL 显示分类名称输入框和保存操作。
2. WHEN 用户保存分类，系统 SHALL 要求分类名称非空。
3. WHEN 用户编辑已有分类，系统 SHALL 保留已有图标、图片和颜色数据。
