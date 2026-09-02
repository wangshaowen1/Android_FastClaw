---
title: Feature 包结构与边界
type: architecture
status: active
last_verified: 2026-09-02
---

# Feature 包结构与边界

## 要解决的问题

`feature/` 用于按用户能力定位页面、状态与流程。本文规定顶层业务包、子功能包和共享能力的划分粒度，避免再次按 `adapter`、`dialog`、`viewModel`、`bean` 等技术角色拆目录，也避免把源码包误认为已经具备独立 Gradle 模块的依赖边界。

## 范围与结论

本文覆盖 `app/src/main/java/com/shiyuan/fastclaw/feature/`。当前工程仍是单 `app` 模块，`feature/*` 是源码导航边界，不提供模块级可见性或依赖约束。

本轮静态审计结论：当前业务粒度适合继续使用，并与 Android 官方“按屏幕或紧密相关流程划分 feature、追求高内聚低耦合、共享能力下沉 core/common”的原则一致。它不与 Now in Android 的目录逐字一致，也不应被宣称为可直接拆分的 feature 模块。

两处非业务能力已经移出 `feature/`：

- 通用图片预览归入 `core/ui/imagepreview/`，供业务页面与公共控件复用。
- 厂商推送 Activity 归入 `platform/push/`，作为第三方 SDK 到应用路由的入口适配，不再伪装成用户功能。

## 与官方原则的对应关系

| 官方原则 | 当前落法 | 结论 |
| --- | --- | --- |
| Feature 通常对应一个屏幕或一组紧密相关的屏幕 | 顶层包使用 `resume`、`report`、`order`、`message` 等用户能力名 | 对齐 |
| 高内聚、低耦合 | 页面、ViewModel、列表项和弹窗留在所属业务或子流程中 | 结构对齐；跨 feature 直接引用仍需渐进收敛 |
| App 层负责根导航与组装 | `main` 组合主 Tab，属于当前单模块下的应用壳例外 | 职责明确，但不是独立业务模块候选 |
| 多处复用的实现进入 core/common | 图片预览已进入 `core/ui`，厂商入口进入 `platform` | 对齐 |
| 粒度需权衡维护成本 | 小业务包不因文件少而强行合并；只在形成稳定子流程时增加子包 | 对源码包合适；不等同于建议创建同等数量的 Gradle 模块 |

官方明确说明不存在适用于所有项目的唯一模块化策略。官方关于“过细会增加配置和样板成本”的警告主要针对 Gradle 模块；本项目当前只是包级组织，因此判断重点是业务可发现性、职责和耦合，而不是文件数量。

## 当前 Feature 地图

| 顶层包 | 业务边界 | 稳定子包或说明 |
| --- | --- | --- |
| `clawagent/` | 小虾智能体会话、动作编排和对话卡片 | `message/` 保存智能体消息模型、适配与渲染 |
| `home/` | 既有首页内容与视频流 | `data/` 保存该功能专用数据访问 |
| `hr/` | HR 入口页 | 文件少但具有独立用户入口，不并回 `mine` |
| `interview/` | 模拟面试房间与交互 | 页面、消息、弹窗和列表渲染保持同包 |
| `invite/` | 邀请流程 | 独立用户流程 |
| `login/` | 登录及手机号相关流程 | `data/`、`flash/`、`phone/`、`phonebinding/`、`unified/` |
| `main/` | 主界面、主 Tab 和应用前台壳 | App 组装职责，不作为业务模块边界参考 |
| `message/` | 消息列表与招聘沟通 | `chat/`、`data/` |
| `mine/` | “我的”聚合页与页面状态 | 子业务已拆出，保留两文件是预期结果 |
| `opportunity/` | 机会、职位与宝典入口 | 同一业务族页面保持同包 |
| `order/` | 订单与订单记录 | 独立交易流程 |
| `payment/` | 支付方式与支付状态 | 多业务可进入，但仍是完整用户流程 |
| `report/` | 报告首页、生成与历史 | `data/`、`generation/`、`history/`、`interview/` |
| `resume/` | 在线简历、附件简历和理解度 | `experience/`、`preference/`、`profile/`、`upload/` |
| `settings/` | 设置、账号与备案信息 | 页面和对应状态保持同包 |
| `shrimp/` | 虾分余额、明细与充值 | 独立资产流程 |
| `splash/` | 启动与隐私同意流程 | 全局用户流程 |
| `upgrade/` | 版本升级交互 | 全局用户流程；检测能力仍由共享管理器提供 |
| `web/` | 带业务桥接的 Web 容器 | `data/`；当前不是纯通用浏览器，不能直接下沉 core |

## 划分规则

### 顶层业务包

满足以下任一主要条件时，可以建立 `feature/<business>/`：

- 对应明确的用户目的、导航目的地或一组连续业务流程。
- 有稳定的业务术语和状态所有者，后续修改可由同一业务上下文理解。
- 即使只有 Activity/Fragment 与 ViewModel 两个文件，也能独立回答“用户在完成什么”。

不要仅按文件数量合并 `mine`、`hr` 等小包。文件少可能说明边界收敛成功，不代表功能应回到一个“大杂烩”目录。

### 子功能包

只有出现稳定的子流程、专用数据子系统，或一个可单独解释且持续增长的业务概念时才增加子包。例如 `resume/preference`、`report/generation`、`message/chat`。

不要新建 `adapter/`、`dialog/`、`viewholder/`、`viewModel/`、`vm/`、`bean/` 等技术角色目录。Adapter、Dialog、ViewModel 和 UiModel 默认与使用它们的页面或子流程同包；仅被一个功能使用的实现不要上提为共享能力。

### 共享与平台能力

- 与具体业务无关、被多个 feature 使用的 UI 或辅助能力进入 `common/`、`core/ui/` 或既有 `widget/`，具体选择按依赖方向和现有同类代码决定。
- 网络、数据库、全局状态和共享管理器进入 `core/`。
- 厂商 SDK 的 Android 入口与回调适配进入 `platform/<integration>/`；业务页面不得直接散落第三方 SDK 调用。
- 名称看似通用但仍携带具体业务桥接的实现不能仅靠搬包下沉。例如 `web/` 当前会分发报告等业务动作，应先拆开通用容器与业务桥接，再评估迁移。

### 跨 Feature 依赖

新增代码优先通过 `router/`、事件或共享数据能力跨 feature，不共享另一个页面的 ViewModel，也不要让公共层依赖具体页面实现。

本轮按顶层包统计 import，仍存在 10 组双向引用。部分来自 `main` 组装页面与业务返回主界面，部分来自页面间直接启动；`mine` 与 `order/shrimp` 共享 `MineViewModel` 是更明确的边界债务。它们不影响本次包级导航，但意味着当前目录不能一比一升级为独立 Gradle 模块。真正模块化前，应先抽取导航契约和共享数据所有者，再消除双向依赖，而不是继续搬包。

## 变更判断顺序

新增或移动代码时按以下顺序判断：

1. 先找最接近的用户能力顶层包。
2. 再判断是否属于该能力下已有的稳定子流程。
3. 只有被多个业务复用且不携带具体页面知识时，才进入共享层。
4. 只有需要可见性隔离、独立复用或构建收益时，才讨论 Gradle 模块化；包名相似不是拆模块依据。

## 实现位置与验证

| 关注点 | 代码锚点 | 验证方式 |
| --- | --- | --- |
| 业务包结构 | `app/src/main/java/com/shiyuan/fastclaw/feature/` | 检查包声明与目录一致、无技术角色子目录 |
| 共享图片预览 | `app/src/main/java/com/shiyuan/fastclaw/core/ui/imagepreview/` | 检查业务和公共控件只引用共享实现 |
| 厂商推送入口 | `app/src/main/java/com/shiyuan/fastclaw/platform/push/` | 检查 Manifest 和 SDK 回调入口一致 |
| 应用壳与导航 | `feature/main/`、`router/` | 检查主 Tab 组装和跨功能跳转 |
| 依赖风险 | `feature/` 下 import | 统计跨顶层包引用和双向依赖 |

本轮只进行了包声明、引用、Manifest、DataBinding 类型和跨包依赖的静态检查，没有运行 Gradle。

## 官方参考

- [Android Developers：Guide to Android app modularization](https://developer.android.com/topic/modularization)
- [Android Developers：Common modularization patterns](https://developer.android.com/topic/modularization/patterns)
- [Android 官方仓库：Now in Android modularization learning journey](https://github.com/android/nowinandroid/blob/main/docs/ModularizationLearningJourney.md)
- [Android 官方仓库：Architecture Samples](https://github.com/android/architecture-samples)
