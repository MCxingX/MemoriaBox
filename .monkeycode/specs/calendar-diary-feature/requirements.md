# Requirements Document

## Introduction

为 MemoriaBox 应用的日历视图添加日记功能，允许用户在日历日期下方编写日记、添加图片，并在点击日期时以滚动文字动画展示当天日记内容，背景可显示照片或视频。

## Glossary

- **日历日记**: 绑定到特定日期的日记条目，包含文字、图片和视频
- **滚动文字动画**: 文字逐字显示的动画效果，类似语音输入的实时转写展示
- **媒体背景**: 日记内容区域背后的照片或短视频背景

## Requirements

### Requirement 1: 日历日期下方写日记

**User Story:** AS 用户，I want 在日历的每个日期下方直接写日记，so that 我可以快速记录当天发生的事情而不需要跳转到其他页面

#### Acceptance Criteria

1. WHEN 用户查看日历视图时，THE 系统 SHALL 在每个日期格子下方显示日记输入区域的入口
2. WHEN 用户点击日期格子的日记入口时，THE 系统 SHALL 弹出日记编辑界面
3. WHILE 用户处于日记编辑界面时，THE 系统 SHALL 提供文字输入框供用户输入日记内容
4. WHEN 用户完成日记编辑并保存时，THE 系统 SHALL 将日记内容与对应日期绑定存储

### Requirement 2: 日记添加图片功能

**User Story:** AS 用户，I want 在日记中添加图片，so that 我可以记录当天的视觉记忆

#### Acceptance Criteria

1. WHEN 用户在编辑日记时，THE 系统 SHALL 提供添加图片的按钮
2. WHEN 用户点击添加图片按钮时，THE 系统 SHALL 允许用户从相册选择图片或拍照
3. WHILE 日记包含图片时，THE 系统 SHALL 在日历视图中该日期下方显示图片缩略图标记
4. WHEN 用户查看日记详情时，THE 系统 SHALL 展示完整尺寸的图片

### Requirement 3: 点击日期展示日记内容

**User Story:** AS 用户，I want 点击日历日期时看到当天发生了什么，so that 我可以快速回顾当天的记录

#### Acceptance Criteria

1. WHEN 用户点击有日记的日期时，THE 系统 SHALL 展示该日期的日记内容
2. WHILE 日记内容展示时，THE 系统 SHALL 使用滚动文字动画效果逐字显示日记文字
3. WHEN 日记包含图片时，THE 系统 SHALL 在文字下方展示图片
4. WHEN 日记包含视频时，THE 系统 SHALL 在文字下方提供视频播放控件

### Requirement 4: 滚动文字动画效果

**User Story:** AS 用户，I want 日记文字以类似语音输入的滚动动画出现，so that 我有沉浸式的回顾体验

#### Acceptance Criteria

1. WHEN 日记内容开始展示时，THE 系统 SHALL 逐字显示文字，每个字间隔 50-100 毫秒
2. WHILE 文字动画播放中，THE 系统 SHALL 提供跳过动画直接显示全文的选项
3. WHEN 文字动画播放完成时，THE 系统 SHALL 显示完整日记内容
4. WHILE 用户滚动查看日记时，THE 系统 SHALL 保持文字流畅滚动

### Requirement 5: 日记背景图功能

**User Story:** AS 用户，I want 为日记设置照片或短视频作为背景，so that 日记展示更加生动直观

#### Acceptance Criteria

1. WHEN 用户编辑日记时，THE 系统 SHALL 允许用户选择照片或短视频作为背景
2. WHILE 日记展示时，THE 系统 SHALL 在文字后方显示选择的背景媒体
3. WHEN 背景为视频时，THE 系统 SHALL 自动播放短视频循环（静音）
4. WHILE 背景媒体展示时，THE 系统 SHALL 在媒体上方添加半透明遮罩确保文字可读性
5. WHEN 背景媒体加载失败时，THE 系统 SHALL 回退到默认纯色背景

### Requirement 6: 日历视图日记标记

**User Story:** AS 用户，I want 在日历上快速识别哪些日期有日记，so that 我可以高效浏览和查找记录

#### Acceptance Criteria

1. WHILE 日历视图展示时，THE 系统 SHALL 在有日记的日期下方显示小圆点或图标标记
2. WHEN 日期包含图片日记时，THE 系统 SHALL 显示图片图标标记
3. WHEN 日期包含视频日记时，THE 系统 SHALL 显示视频图标标记
4. WHEN 日期只有文字日记时，THE 系统 SHALL 显示文字图标标记
