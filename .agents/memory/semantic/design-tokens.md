---
name: design-tokens
description: 颜色/主题 token 的两层结构与替换约定;命名清单与映射规则不在此重复
metadata:
  type: reference
---

模板工程颜色采用两层结构:`attrs.xml` 语义主题属性 + `colors.xml` 实际色值。

**新建项目时:保留语义命名,按设计稿替换 `colors.xml` 实际色值,不沿用模板示例值。**

- 语义 attr 命名的**权威清单**以 `app/src/main/res/values/attrs.xml` 为准(品牌色 `colorBrand*`、文字色 `textColorPrimary/Title/Secondary/Hint/Disabled`、背景 `bgPrimary` 系、填充 `fillPrimary` 系、边框 `outline*`、功能色 `funcColor*`)。
- Figma→token 的**映射规则**见 `.agents/android-codegen-xml-resource-rules.md`《Resource Token》,本文件不重复。
- 新增 UI 一律引用 `?attr/<语义名>`,不直接引用 `@color/<具体名>`,不硬编码 hex;色值以设计稿为准,不臆测。
- 已废弃数字命名 `textColor1`、`themeColor1`、`bgColor3` 等不得再使用。

**为什么记在 memory**:这是跨会话需要反复确认的项目事实(两层结构 + 不可硬编码 + 不可臆测色值),便于 Codex/Claude 召回;具体命名和规则由规则层与 attrs.xml 承载,避免三处漂移。
