---
name: design-tokens
description: 颜色/主题 token 的两层结构与替换约定;命名清单与映射规则不在此重复
metadata:
  type: reference
---

FastClaw 颜色采用两层结构:`attrs.xml` 语义主题属性 + `colors.xml` 实际色值。

**新增或调整 UI 时:保留语义命名,按设计稿维护 `colors.xml` 实际色值。**

- 语义 attr 命名的**权威清单**以 `app/src/main/res/values/attrs.xml` 为准(品牌色 `colorBrand*`、文字色 `textColorPrimary/Title/Secondary/Hint/Disabled`、背景 `bgPrimary` 系、填充 `fillPrimary` 系、边框 `outline*`、功能色 `funcColor*`)。
- Figma→token 的**映射规则**见 `.agents/android-codegen-xml-resource-rules.md`《Resource Token》,本文件不重复。
- 新增 UI 一律引用 `?attr/<语义名>`,不直接引用 `@color/<具体名>`,不硬编码 hex;色值以设计稿为准,不臆测。
- 已废弃数字命名 `textColor1`、`themeColor1`、`bgColor3` 等不得再使用。

**为什么记在 memory**:这是跨会话需要反复确认的项目事实(两层结构 + 不可硬编码 + 不可臆测色值),便于 Codex/Claude 召回;具体命名和规则由规则层与 attrs.xml 承载,避免三处漂移。

## 2026-06-17 补充

- Figma/XML 还原时，`#F5000000`、`#E6000000` 这类 8 位 ARGB 颜色即使能正确表示透明黑，也不能直接写在布局 `android:textColor` 中；必须优先映射到已有 `@color` / `?attr` 资源。没有等价资源时，先新增清晰命名的颜色资源，再在 XML 中引用。
- Figma/XML 还原时不要使用 `@style/Text.Medium` 表达字重。中等字重 500 使用 `android:fontFamily="sans-serif-medium"`；加粗 600 及以上使用 `android:textStyle="bold"`。
- 从 Figma 下载图片资源时默认只下载 2x，不下载 3x 或原始 imageRef 尺寸；需要其他倍率时必须由用户明确指定。
- TextView 的宽高不要硬编码为固定 dp，默认使用 `wrap_content`；需要跟随父布局约束时使用 `0dp` + 约束，确有视觉下限/上限时使用 `minWidth` / `minHeight` / `maxWidth` 等约束属性。
