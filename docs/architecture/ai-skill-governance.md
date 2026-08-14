---
title: AI Skill 与规则治理架构
type: architecture
status: active
last_verified: 2026-08-14
---

# AI Skill 与规则治理架构

## 背景与目的

本仓库的 AI 规则需要同时解决两个相反的问题：一方面，AI 必须知道当前项目独有的实现和高风险边界；另一方面，规则过多、重复或过于通用会消耗上下文，使真正重要的约束失去权重。

治理目标不是增加 Skill 数量，而是让 AI 用最少的读取成本命中足以正确完成任务的事实和决策。

## 本文说明什么

本文说明 `AGENTS.md`、`.agents` 规则和场景 Skill 如何分工，什么内容值得成为 Skill，以及团队怎样修改和验证路由。具体 Android 类、页面、接口和资源仍以当前代码和对应 Skill 为准；修改历史交给 Git，不在本文重复记录。

## 当前模型

规则按任务需要逐层读取：

```text
AGENTS.md 红线与任务档位
  → android-codegen-quick-rules.md 一级路由
    ├─ 局部任务 → 目标代码和必要资源
    ├─ 单一标准场景 → 命中的 SKILL.md → 必要时读取 references/
    └─ 组合或高风险场景 → android-codegen-scenarios.md → 命中的 SKILL.md

需要项目背景时另走文档路由：
docs/README.md → 首轮一份当前任务需要的文档 → 证据不足时再扩展
```

| 层级 | 负责 | 不负责 |
| --- | --- | --- |
| `AGENTS.md` | 常驻红线、执行档位、读取优先级和输出边界 | 展开场景实现细节 |
| `.agents/README.md` | 文件与 Skill 导航、维护原则 | 调度逻辑和重复规则正文 |
| quick rules | 标准任务的一级分流和局部快路径 | 完整场景说明 |
| scenarios | 多 Skill 组合和跨领域高风险补充 | 维护单个 Skill 的完整触发边界 |
| `SKILL.md` description | 单个 Skill 何时触发、何时不触发 | 展开实现步骤 |
| `SKILL.md` 正文 | 会改变项目实现选择的规则 | 重复触发说明和行业常识 |
| `references/` | 真实符号、能力边界和按需代码索引 | 默认加载的完整代码清单 |
| `docs/README.md` | 开发者导览、项目文档索引和 AI 读取入口 | 普通代码任务的默认上下文 |
| 文档读取规则 | 当前文档按任务选择，版本归档只在追溯历史时读取 | 替代任务路由或证明当前代码行为 |
| `evals/skill-routing-cases.json` | 保存路由回归样例 | 普通开发任务的必读规则 |
| `agents/openai.yaml` | Codex 展示信息和明确的 `$skill-name` 调用入口 | 通用规则正文 |

## Skill 准入标准

新建或保留 Skill，至少应满足以下一项：

- AI 在真实任务中容易重复犯错。
- 当前项目存在通用知识无法推出的实现约束。
- 可以为 AI 提供准确、稳定的代码搜索入口。
- 决策涉及生命周期、并发、状态一致性、协议兼容等高代价风险。

下列内容不应独立成为 Skill：

- 驼峰命名、Boolean 前缀、缩进、行长等通用语言规范。
- “遵循最佳实践”“代码要清晰”等没有判别力的描述。
- 当前任务不存在的其他端、其他项目或未来架构推测。
- 可以从目标文件和附近同类实现直接得出的普通写法。

准入判断的核心问题是：这条规则能否阻止一个具体且现实的错误。如果不能，优先不写。

## 如何判断是否加载 Skill

单个 Skill 的触发范围只写在其 description，同时写清应触发和不应触发的任务。quick rules 只保留档位、轻量路径和常用指针；scenarios 只说明多个 Skill 怎样组合，以及没有独立 Skill 的高风险场景。

修改任务档位、场景组合或 description 时，把正向、排除、组合和容易误判的任务写入 [`skill-routing-cases.json`](../../.agents/evals/skill-routing-cases.json)。普通开发任务不读取这些样例。多个 Skill 组合时，每个 Skill 只负责自己的状态所有权和风险边界，不因标题中出现相关词而连锁加载。

## AI 什么时候读取项目文档

Skill 负责“怎样做某类任务”，项目文档负责“当前项目已经确认了什么”。普通代码任务先看目标代码和命中的规则，不把 docs 全量装入上下文；只有任务需要项目背景、跨模块决定或稳定业务规则时，才从 [`docs/README.md`](../README.md) 选择首轮一份文档。

- 当前文档：由导览或当前任务选中后读取，不填写 `ai_read`。
- `history-only`：只用于版本比较、回归、迁移、回滚或追溯决定，不参与普通实现路由。

会议纪要、团队复盘、经验分享和纯协作说明不属于项目文档，不进入 `docs`。尚未确认但会影响实现的内容放在对应功能文档的待确认事项中，并保持 `draft` 或未完成状态。

链接只表示可以继续查找，不表示必须继续读取。版本归档只记录 Git 难以解释、又会影响后续维护的差异和原因；当前代码与 `active` 文档始终优先于归档。

## 关键决策

### 删除通用命名 Skill

原 `android-naming` 大部分内容属于 AI 已掌握的行业常识，独立加载会增加 token，却不能显著降低错误率。因此删除该 Skill，并将真正影响项目实现的规则归还所属领域：

- drawable 数值、透明度和圆角命名归 XML 资源规则。
- `ItemXxxVM` 归 DataBinding 列表 Skill。
- `XxxBody`、`XxxInfo`、`XxxData` 归 API Skill。
- scheme、host、事件值和历史拼写的兼容约束归导航 Skill。

这不是取消命名约束，而是让约束在实际使用它的任务中出现。

### 新增 Tab/ViewPager2 Skill

Tab + ViewPager2 是已确认的 AI 失败场景：AI 知道 Android 官方 API，但无法仅凭通用知识判断当前项目 Adapter 和快捷绑定的真实能力。

当前项目事实包括：

- `FragmentStateCacheAdapter.setFragments()` 只替换列表，不发出 Adapter 通知，并以 Fragment 实例 `hashCode()` 作为 itemId。
- Fragment 列表的安全默认是初始化时稳定设置一次，不能在 Tab 点击或数据刷新时重建。
- `NeedUpdateFragmentStateAdapter` 使用位置 itemId，只覆盖有限的同位置刷新和尾部增删，不是任意动态 Tab 的现成方案。
- `ViewPagerHelper` 和 `PillIndicatorView.attachToViewPager2()` 内部注册匿名 callback，没有对应的解绑入口。
- Tab 点击与页面切换回调若同时发起加载，容易产生重复请求和状态反复写入。

这些事实集中在 [`android-tab-viewpager`](../../.agents/skills/android-tab-viewpager/SKILL.md)，详细搜索入口放在其 [`project-index.md`](../../.agents/skills/android-tab-viewpager/references/project-index.md)。其他项目的业务页面、包名和业务词不进入 FastClaw Skill。

### 拆分 Tab 容器与 Paging

Tab 容器和分页子页具有不同的状态所有权：

- Tab Skill 负责容器、Fragment 稳定性、选中同步和 callback 生命周期。
- Paging Skill 负责每个子 Fragment 的 Pager、PagingSource、Adapter 和 LoadState。

因此普通 ViewPager2 不需要加载 Paging Skill；只修改分页子页也不需要重复加载容器规则。多 Tab 分页场景组合读取两者。

### 收紧 API Skill

API Skill 区分跨层接口接入和已有链路中的局部修改，准确触发条件只以其 description 为准。这个边界让新增接口保留完整约束，同时避免单字段、参数或调用点修改加载整份 API 手册。

## 不变量与失败行为

- 红线禁令只在 `AGENTS.md` 定义；发现副本时应删除副本并保留引用。
- Skill 与当前代码不一致时，以当前分支同类实现为事实，并更新 Skill 或明确差异。
- 复制到新项目后必须重新确认 namespace、包名、业务域、scheme 和真实依赖。
- references 中没有完整业务实现时，应说明能力边界，不用来源项目路径填补空白。
- 动态 Tab、长任务、数据库迁移、登录和支付等无法从当前事实确认时，必须暴露待确认点，不假设现有封装已支持。
- 一条规则同时出现在多个文件时，应确定唯一归属，其他位置只保留路由链接。

## 实现地图

| 关注点 | 事实或规则锚点 | 验证方式 |
| --- | --- | --- |
| 常驻红线与档位 | [`AGENTS.md`](../../AGENTS.md) | 检查其他文档没有复制红线正文 |
| 一级路由与接口快路径 | [`.agents/android-codegen-quick-rules.md`](../../.agents/android-codegen-quick-rules.md) | 用局部任务和标准任务分别检查命中集合 |
| 组合场景 | [`.agents/android-codegen-scenarios.md`](../../.agents/android-codegen-scenarios.md) | 检查组合顺序和 Skill 职责没有重叠 |
| 路由回归样例 | [`.agents/evals/skill-routing-cases.json`](../../.agents/evals/skill-routing-cases.json) | 用无历史上下文的新会话核对档位、应加载和不应加载的 Skill |
| Skill 清单与维护原则 | [`.agents/README.md`](../../.agents/README.md) | 检查新增 Skill 是否通过准入标准 |
| Tab/ViewPager2 | [`.agents/skills/android-tab-viewpager/`](../../.agents/skills/android-tab-viewpager/) | 对照 Adapter、指示器和 callback 的当前实现 |
| Paging 边界 | [`.agents/skills/android-paging-state-ui/`](../../.agents/skills/android-paging-state-ui/) | 确认容器不持有子页分页状态 |
| API 边界 | [`.agents/skills/android-api-request/`](../../.agents/skills/android-api-request/) | 对照 quick path 与跨层触发条件 |
| 结构校验 | [`.agents/scripts/validate_agent_docs.py`](../../.agents/scripts/validate_agent_docs.py) | 修改规则或 Skill 后用 Python 3.9+ 运行，无错误再提交 |

## 维护与验证

维护 Skill 时至少检查：

- `description` 同时包含正向触发和明确排除项。
- 正文没有复制 `description` 的触发清单。
- `agents/openai.yaml` 的 `default_prompt` 明确引用 `$skill-name`。
- references 中的符号在当前分支真实存在，且没有机器绝对路径或来源项目业务路径。
- quick rules 只保留轻量路径和常用指针，scenarios 不再维护单个 Skill 的完整触发边界。
- 路由样例覆盖每个 Skill 的应触发和不应触发任务，并至少包含组合与模糊任务。
- 用 Python 3.9+ 运行 `.agents/scripts/validate_agent_docs.py`，确认：
  - `SKILL.md` 从首字节 `---` 开始，不含会导致发现失败的 UTF-8 BOM。
  - frontmatter 的 name/description、目录名和 `agents/openai.yaml` 一致，`default_prompt` 显式引用 `$skill-name`。
  - 每个 reference 由同目录 `SKILL.md` 直接链接，Skill 注册表与目录一致。
  - 本地 Markdown 链接有效，没有机器绝对路径。
  - 路由样例格式有效，引用的 Skill 存在，每个 Skill 同时有正向和排除覆盖。

校验脚本只验证确定性结构和样例覆盖，不判断 AI 是否真的路由正确。修改任务档位、场景组合或 description 后，还要用无历史上下文的新会话逐项或抽样执行路由样例；发现误判时先补回归样例，再调整规则。

新增或保留规则仍需回答“能否阻止已知错误或提供当前项目无法从通用知识推出的索引”。

Gradle 不用于验证文档结构；涉及真实代码行为时，只在用户明确要求后运行对应构建或测试。
