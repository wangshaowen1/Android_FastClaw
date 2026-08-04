---
title: App 分层架构
type: architecture
status: active
last_verified: 2026-08-04
---

# App 分层架构

## 背景与目的

主应用按 `base`、`common`、`core`、`kit` 和 `feature` 等包组织。分层用于指导职责、依赖和新增代码归属，不以目录绝对纯净为目标。

## 范围与非目标

本文覆盖 `app/src/main/java/com/shiyuan/fastclaw/` 下共享代码和功能代码的归属，以及 `baselibrary` 与主应用的边界。

本文不要求搬迁或清空 `app/base`，不启动 Gradle 模块化，也不要求仅为符合架构图而批量重写 import。代码位置以是否更容易理解、复用、测试和维护为判断依据。

## 当前模型

- `app` 通过 `modules.gradle` 和 `app/build.gradle` 使用 Maven 制品 `com.kuaige:baselibrary`，当前没有启用本地 `:baselibrary` 模块。
- `base`、`common`、`core`、`kit` 和 `feature` 是 `app` 内的 Kotlin/Java 包，不是五个独立 Gradle 模块。
- `app/base` 是基于 `baselibrary` 的 App 级适配集合，包含应用 ViewModel、UI 基类及少量业务适配；它不是独立架构层。
- 当前存在 `core` 或 `kit` 直接引用具体 `feature` 的实现。是否处理取决于真实耦合和维护风险，但共享能力绑定具体页面属于优先收敛方向。

## 职责与边界

推荐分层链为 `baselibrary → common → core / kit → feature`。箭头表示基础能力向业务功能的供给方向；右侧通常使用左侧能力，但不要求每个依赖逐层中转，也不代表强制物理模块边界。

| 位置 | 负责 | 不负责 |
| --- | --- | --- |
| `com.kuaige:baselibrary` | Android/MVVM 基类、通用基础组件、集中封装的第三方能力 | FastClaw 业务模型、应用资源和具体页面 |
| `common/` | 与具体业务无关的应用内工具、扩展、UI 组件和常量 | 全局业务状态和完整功能流程 |
| `core/` | 全局数据源、网络、数据库、用户与配置状态、路由及共享业务管理 | 具体 `feature` 页面的交互编排 |
| `kit/` | 可复用的领域能力；与 `core` 的边界按职责和复用方式判断 | 绑定某个具体业务页面 |
| `feature/` | 面向用户的页面、交互和完整业务流程 | 向共享层泄漏页面实现 |
| `base/` | 基于 `baselibrary` 的既有 App 级基类和适配代码 | 作为新架构层持续扩张 |

新增代码优先按 `common`、`core`、`kit`、`feature` 的职责放置。旧代码只在真实收益足以覆盖迁移和回归成本时调整。

## 治理优先级

| 优先级 | 判断条件 | 处理原则 |
| --- | --- | --- |
| 必须处理 | 循环依赖；共享能力绑定具体 `feature` 页面；ViewModel 接受或持有 View/生命周期对象；耦合导致无法复用或测试 | 优先消除方向性和职责风险，不以搬包代替解耦 |
| 顺手处理 | 类名或包名明显误导，或导致新增代码难以判断归属 | 在相关业务变更中渐进改名或迁移 |
| 不必处理 | 与理想架构图不完全一致，但职责清楚、依赖稳定且没有维护成本 | 保持现状，避免低价值的 import 重写 |

第三方 SDK 隔离等项目红线统一以 `AGENTS.md` 为准，本文不重复维护。

## `app/base` 的渐进治理

- `AppRepositoryViewModel` 等职责清楚且依赖稳定的基类可留在原处，不为命名清晰度单独批量迁移。
- `ToolbarViewModel`、`AppItemViewModel` 等通用 UI 类可在相关业务修改时评估迁入 `common/ui`，当前不紧急。
- `RecyclerViewVM` 当前未发现外部静态引用；Blur Dialog 基类也只发现定义和基类间继承。它们是删除候选，但删除前仍需检查反射、构建变体和资源引用。
- `BaseShareViewModel` 当前只有一个外部使用文件，且 API 直接接受 View。触及该分享场景时，应优先迁入具体 `feature` 或拆分 View 职责，不为调整包名而单独迁移。

## 关键决策

| 决策 | 原因 | 未采用方案及原因 |
| --- | --- | --- |
| 最底层基础能力使用 `baselibrary` 命名 | 与实际依赖制品一致 | `BaseModule` 或 `app/base` 会混淆制品与应用适配代码 |
| 保留 `app/base` | 它是 App 级适配集合，已有代码职责总体可理解 | 整体迁入 `core` 或清空目录只提升形式一致性，迁移收益低 |
| 分层作为指导思想 | 优先约束真实依赖风险和维护成本 | 强制物理边界会制造低价值重构 |
| 旧代码渐进治理 | 可结合业务验证降低回归成本 | 独立批量整理 import 难以产生对应业务价值 |

## 实现地图与验证

| 关注点 | 稳定代码锚点 | 验证方式 |
| --- | --- | --- |
| 构建模块 | `settings.gradle` | 核对实际 include 的模块 |
| `baselibrary` 依赖 | `modules.gradle`、`app/build.gradle` | 核对依赖坐标和使用方式 |
| App 分层包 | `app/src/main/java/com/shiyuan/fastclaw/` | 检查跨包 import 和职责 |
| App 级适配 | `app/src/main/java/com/shiyuan/fastclaw/base/` | 修改前检查调用方、反射、资源和变体引用 |

仅有包名差异不构成架构问题。处理必须级问题或迁移既有代码时，再编译受影响变体并回归对应页面。
