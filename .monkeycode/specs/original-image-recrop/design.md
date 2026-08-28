# 原图重复裁剪设计

Feature Name: original-image-recrop
Updated: 2026-08-27

## 描述

通过应用私有存储保留原图，并在编辑状态中记录原图 URI 与裁剪参数。展示 URI 继续作为现有业务数据的值，避免修改 Room 实体结构；编辑元数据使用独立持久化存储按展示 URI 索引。

## 架构

```mermaid
flowchart TD
    A[图片选择器] --> B[复制原图]
    B --> C[裁剪编辑器]
    C --> D[从原图生成展示图]
    D --> E[保存展示 URI]
    D --> F[保存原图 URI和编辑参数]
    E --> G[再次编辑]
    F --> G
    G --> C
```

## 组件与接口

- `ImageImportUtils`：复制原图、从原图生成展示图、读取和保存编辑元数据。
- `EventImageCropDialog`：接收初始原图 URI、缩放值和偏移值，并返回更新后的编辑参数。
- `CustomizationSettingsScreen`：处理页面背景和底部图标四个入口。
- `BoxDialog`：处理分类图标入口。
- `EventDialog`：处理事件卡片背景入口。

## 数据模型

编辑元数据包含：

- `sourceUri`：应用私有存储中的原图 URI。
- `cropAspectRatio`：生成展示图时使用的裁剪比例。
- `scale`：缩放比例。
- `offsetX`、`offsetY`：归一化裁剪偏移。

元数据以展示 URI 的稳定键保存。旧展示图没有元数据时，`sourceUri` 回退为展示 URI，参数回退为默认值。

## 正确性约束

1. 每次保存展示图都从 `sourceUri` 解码原图。
2. 展示图更新后，元数据索引同步指向新的展示 URI。
3. 旧图片数据缺少元数据时仍可展示和再次保存。
4. 四类入口使用各自的裁剪比例。

## 错误处理

- 原图复制失败时回退到选择器 URI，并沿用现有展示逻辑。
- 原图解码或裁剪失败时回退到原图副本。
- 元数据损坏时忽略元数据并以展示图作为编辑源。

## 测试策略

- 验证原图复制、展示图生成和元数据读写。
- 验证重复保存始终从原图生成结果。
- 验证缺少元数据的旧图片可以继续编辑。
- 使用 `compileReleaseKotlin` 和 Release APK 构建验证集成代码。
