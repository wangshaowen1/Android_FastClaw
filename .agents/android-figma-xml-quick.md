# Android Figma XML 快速规则

本文件用于 Figma/截图到 Android XML 的首轮实现或检查。只有目标节点信息不足、整页重写、复杂资源导出或用户要求完整精确还原时，才读取 `.agents/skills/android-figma-xml-ui/SKILL.md`。

## 首轮动作

Figma 节点数据和资源统一通过 Figma API 读取；不走 MCP。

1. 从 Figma URL 解析 `fileKey` 和 `node-id`。
2. 有 `node-id` 时只读取目标节点；没有 `node-id` 时先定位目标节点，不直接分析整个文件。
3. 用 `rg -n` 定位目标 XML、Fragment/Activity/ViewModel 和同类 XML。
4. 按目标节点关键值精确查资源：文本色语义、间距、圆角、目标 drawable 名、颜色值、描边值。
5. 先完成目标区域，再做相关项自检：XML 解析、直接资源存在、BindingAdapter 存在、触达行 token/字体/drawable 命名。

## 首轮不要做

- 不读整个 Figma 文件。
- 不递归扫描整个 `res`。
- 不读取所有 style/theme/attrs/colors/dimens，只查本次值相关项。
- 不清理目标文件以外的历史 XML 问题。
- 不跑 Gradle，除非用户要求或当前任务必须验证构建。

## Android 映射

- 页面 XML 默认 `<layout>`，ViewModel 变量名 `layout`，item 变量名 `item`。
- 点击优先用项目已有 click binding 或 `setOnSingleClickListener`。
- Figma 数值优先映射项目资源:`style/theme/attr/color/dimen/drawable/selector/BindingAdapter`,不裸写值。

> 完整 token 映射(spacing/radius、`Text1`–`Text5` 文本色、主题/背景/填充语义色、字体兜底、letterSpacing/lineHeight)和 drawable 命名规则,**以 `android-codegen-xml-resource-rules.md`《Resource Token》《命名与资源》为准,本文件不重复**。常用速记:`4/8/12/16/24dp`→`spacing_xs/s/m/l/xl`;`4/8/12/16dp` 圆角→`radius_s/m/l/xl`;`Text1`–`Text5`→`?attr/textColorPrimary/textColorTitle/textColorSecondary/textColorHint/textColorDisabled`。

## 输出

- 说明处理的目标页面/组件/区域。
- 说明已做的静态检查。
- 只列本次无法映射或需要人工确认的点。
