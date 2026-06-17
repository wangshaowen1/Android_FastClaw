# Agent 工作流架构说明

> 给**开发者 / 维护者**:解释这套 AI 代码生成工作流怎么组织、相比历史改了什么、为什么这么改。
> 日常使用看根目录 `AGENTS.md` 和本目录 `README.md` 即可,本文是设计背景。

## 一句话

一套**跨工具(Codex + Claude Code + Android Studio Gemini)、轻量、渐进披露**的 Android AI 代码生成工作流:薄入口 `AGENTS.md` 只承载红线 + 档位 + 路由,规则细则 / skill / 记忆按需加载,目录与格式对齐各家官方推荐。

核心理念:**我们要 AI 帮写代码,不是搭流程仪式。** 规则越多、越冗余、越冲突,只会让 AI 更慢、更易幻觉、更费 token、注意力更稀释。

## 当前架构

```
根目录
├── AGENTS.md      薄调度入口(~67 行):Gradle 默认 · 红线禁令(唯一来源) · 执行档位 · 路由清单 · 输出
│                  ← Codex / Cursor / Copilot / Gemini CLI 等原生读取
├── CLAUDE.md      Claude Code 入口:@import AGENTS.md(官方模式)+ 记忆分层说明
│                  ← Claude Code 不原生读 AGENTS.md,经此复用同一份规则
└── .agents/       ← 对齐 Android Studio Quail 1+ 官方 skills 路径
    ├── README.md                 目录导航(只回答"哪个文件管什么")
    ├── ARCHITECTURE.md           本文
    ├── android-codegen-*.md      规则细则(单一职责,按场景/档位按需读)
    │     quick-rules / check-quick / figma-xml-quick / project-rules /
    │     architecture-rules / xml-resource-rules / scenarios / checklist
    ├── skills/<name>/            场景 skill(命中场景才加载 = 渐进披露)
    │     ├── SKILL.md            主体(name+description 决定何时加载;所有工具读)
    │     ├── references/         项目示例(命中后按需读)
    │     └── agents/openai.yaml  仅 Codex 用;Claude / Android Studio 跳过不读
    └── memory/                   CoALA 记忆分层(团队 + Codex + Claude 共享)
          ├── semantic/           稳定约定:workflow-principles、design-tokens
          ├── episodic/           决策日志:decisions.md(按需手动追加)
          └── user-preferences.md 协作偏好
```

| 层 | 职责 | 加载时机 |
| --- | --- | --- |
| `AGENTS.md` / `CLAUDE.md` | 红线、档位、路由、行为准则 | **每次会话常驻** |
| `.agents/android-codegen-*.md` | 规则细则,每个文件单一职责 | 命中档位 / 场景才读 |
| `.agents/skills/*/SKILL.md` | 单一场景的完整操作手册 | 命中该场景才读 |
| `.agents/memory/` | 跨会话的稳定约定与历史决策 | 召回时读 |

执行分**档位**(progressive disclosure):快速局部修改 → 标准代码生成 → Figma/UI → 代码审查 → 文档维护。能用轻量路径完成就不升级,小改不读完整规范、不读 skill。

## 设计原则

- **单一来源**:每条规则只有一个家;红线禁令只在 `AGENTS.md`《关键禁令》,其它文档只引用不重复。
- **渐进披露**:薄入口 + 细则外链;按需加载,不把全部规则灌进上下文。
- **结构由真实失败倒逼**:不为"完整性 / 对称性"预先堆砌规则或抽象层。
- **注意力是预算**:加任何规则 / 框架前,先问"它减少的真实错误是否大于消耗的注意力";指不出它防住哪个**已发生**的失败,就不加。
- **跨工具单文件源**:规则只写一份(`AGENTS.md`),各工具按各自机制复用,杜绝多份漂移。

## 历史对比与改进

这套工作流是从来源模板(FastFilm / AIMarry)复制演化而来,本轮做了系统性重构:

| 维度 | 历史状态 | 现在 | 为什么改 |
| --- | --- | --- | --- |
| 颜色 attr | 数字命名 `themeColor1`、`textColor1-5`、`bgColor1-6`、`fillColor1-4` | 语义命名 `colorBrand`、`textColorPrimary`、`bgSurface*`、`fillPrimary`… | 数字名不可维护、不利换肤;语义名对齐 Material 3 词汇,AI 不易幻觉 |
| 字号 style | 数字命名 `Text16/18/20/28…`(8 个) | 语义 `Text.Title/Subtitle/Body/Caption…` | 同上;符合 Android 官方点记法约定,字号字重不变(纯重命名) |
| 目录名 | `design/`(暗示设计稿,实则装规则/skill/记忆,误导) | `.agents/` | 对齐 Android Studio 官方 skills 路径(可被自动发现),点前缀符合工具配置惯例,名实相符 |
| 红线禁令 | 在 6-7 个文件里**重复**;且与 attrs 有**冲突**(`textColorBody` 不存在)、硬编码旧包名 `com.zhixin.dating` | 收敛到 `AGENTS.md` **唯一来源**;冲突与硬编码清零 | 重复 = 改一处漏多处会漂移;冲突 = AI 生成编译不过或用回废弃名 |
| 入口文档 | 有冗余的 `guidelines.md`(第二份索引);`AGENTS.md` 臃肿(~95 行) | 删 `guidelines.md`;`AGENTS.md` 瘦身为薄入口(~67 行) | 官方建议 AGENTS.md 越短越好;常驻文档越短,AI 解析越快、注意力越集中 |
| 规则文件职责 | 内容交叉重复 | 每个文件单一职责,互相引用不重复 | 让 AI(和人)一眼知道去哪查,减少冲突 |
| 记忆 | 混入 AIMarry 残留;无分层 | CoALA 分层(semantic / episodic);清除残留 | 学界分类框架（CoALA）；操作纪律明确：semantic 可编辑去重，episodic 只追加不改写 |
| 记忆写入 | 搬来的 PreCompact 自动 hook(`extract_memory.py`) | **移除 hook,改纯手动** | 本环境(Trae)无 `claude` CLI 跑不通;作为模板会给下游带来"谁有 CLI 谁被动触发花 token"的成本;属非核心 |
| 语言 | 中英混排 | 统一中文 | 团队一致,降低理解成本 |
| 跨工具 | 只有 `AGENTS.md`,Claude Code 读不到 | `CLAUDE.md` 经 `@import AGENTS.md` 复用 | Claude Code 不原生读 AGENTS.md(官方确认),import 是官方推荐的零分叉做法 |

## 官方 / 行业对标

| 组件 | 对标标准 | 状态 |
| --- | --- | --- |
| `AGENTS.md` | Linux Foundation 跨工具开放标准(Codex/Cursor/Copilot/Gemini/Aider 等) | ✅ |
| `CLAUDE.md @import` | Anthropic 官方跨工具复用模式 | ✅ |
| `.agents/skills/` | Android Studio Quail 1+ 官方 skills 路径 | ✅ 精确命中 |
| `SKILL.md`(name+description+渐进披露) | Agent Skills 开放标准 / Anthropic Skills | ✅ |
| `references/` `agents/` 子目录 | Skills 标准可选资源目录(`agents/openai.yaml` 为 Codex 附加) | ✅ |
| 记忆 semantic / episodic 分层 | CoALA 记忆分类（学界分类框架，非工业界标准） | ✅ |
| 颜色 token 语义命名 | Material 3 surface/outline 词汇(其余为合理自定义) | ✅ 部分对齐 |

## 给开发者的日常用法

1. 打开 Claude Code / Codex,直接说任务——`AGENTS.md` 已自动加载红线 + 档位 + 路由。
2. 小改直接做;中等任务它会自己按场景读对应 skill;Figma 还原才进 Figma 流程。
3. 想沉淀经验,会话结尾说一句"本轮有没有需要更新的 memory?",由 AI 判断是否写入 `.agents/memory/`(仓库已记录的不重复存,只存前瞻性判断)。
4. 维护规则时守住:**红线只进 `AGENTS.md`;每个文件单一职责;能不加就不加。**
