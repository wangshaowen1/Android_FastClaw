---
title: FastClaw 开发者导览
type: overview
status: active
last_verified: 2026-09-04
---

# FastClaw 开发者导览

## 项目是做什么的

当前分支的 `app` 模块是 FastClaw 候选人端 Android 应用，也保留了模板工程的公共基础能力。根目录 `README.md` 说明工程初始化和构建配置；这里帮助开发者快速找到业务入口、公共导航和需要长期维护的设计说明。

## 主要功能在哪里

| 领域 | 负责什么 | 先看哪里 | 详细说明 |
| --- | --- | --- | --- |
| 小虾对话 | 会话恢复、SSE 消息、卡片动作、历史合并与报告状态 | `app/src/main/java/com/shiyuan/fastclaw/feature/clawagent/` | [聊天对话](features/chat-conversation.md) |
| 消息沟通 | 招聘会话列表、P2P 沟通、简历/职位消息卡片与未读草稿 | `app/src/main/java/com/shiyuan/fastclaw/feature/message/` | [消息沟通](features/message.md) |
| 模拟面试 | 面试创建、房间状态、语音作答、SSE 文本流、恢复与报告轮询 | `app/src/main/java/com/shiyuan/fastclaw/feature/interview/`、`app/src/main/java/com/shiyuan/fastclaw/feature/clawagent/` | [模拟面试](features/mock-interview.md) |
| 在线简历 | 结构化简历、求职偏好、理解度与附件解析任务 | `app/src/main/java/com/shiyuan/fastclaw/feature/resume/` | [在线简历](features/online-resume.md) |
| 机会与求职宝典 | 机会匹配、已处理机会、岗位/公司详情与求职工具入口 | `app/src/main/java/com/shiyuan/fastclaw/feature/opportunity/` | [机会与求职宝典](features/opportunity.md) |
| IMSDK | 私有依赖、账号凭据、登录登出、会话生命周期与离线推送边界 | `app/src/main/java/com/shiyuan/fastclaw/App.kt`、`app/src/main/java/com/shiyuan/fastclaw/core/User.kt`、`app/src/main/java/com/shiyuan/fastclaw/core/im/` | 当前没有独立功能文档，按任务读取代码 |
| 网络与流式传输 | Retrofit 接口、统一响应处理与原始 SSE 分发 | `app/src/main/java/com/shiyuan/fastclaw/core/network/` | [App 网络接口清单](contracts/app-api-inventory.md) |
| 公共应用基础 | Base 类、事件、导航、资源与通用组件 | `app/src/main/java/com/shiyuan/fastclaw/` | [App 分层架构](architecture/app-layering.md)、[Feature 包结构](architecture/feature-package-structure.md) |

## 应用怎么启动和打开页面

| 入口或容器 | 职责 | 代码入口 |
| --- | --- | --- |
| App 启动与邀请归因 | 展示启动页、处理隐私同意、在同意后初始化能力；收到 URI 时由 ULink 保存邀请归因 | `feature/splash/SplashActivity`、`core/manager/ULinkManager` |
| 主界面 | 承载“小虾、机会/宝典、消息、我的”四个主 Tab；调用方传入 scheme 时再交给公共路由 | `feature/main/MainActivity` |
| 小虾 Tab | 对话与智能体主入口 | `feature/clawagent/ClawAgentMainFragment` |
| 机会/宝典 Tab | 机会、面试报告和求职工具入口 | `feature/opportunity/HomeOpportunityFragment` |
| 消息 Tab | 招聘沟通会话入口 | `feature/message/MessageFragment` |
| 我的 Tab | 账号、简历和个人功能入口 | `feature/mine/MineFragment` |
| 公共 scheme 路由 | 解析当前 App scheme，并把已支持的 host 分发到业务入口 | `router/AppRouter` |
| 推送跳转 | 接收推送 `jumpUri`，处理冷启动或切回前台后的延迟分发 | `core/manager/PushManager`、`router/AppLaunchJump`、`router/BringForegroundActivity` |
| 登录拦截 | 判断游客状态并统一拉起登录流程；强制退出时保留已存在的首页、关闭其他页面并打开手机号登录页 | `router/AppNavigator` |

`AndroidManifest.xml` 是启动 Activity 和外部 scheme 声明的当前依据，`MainActivity` 是主 Tab 与页面归属的当前依据。当前启动链路只保存邀请归因，`SplashViewModel` 进入主页时没有把普通业务 scheme 继续交给 `AppRouter`；新增外部业务跳转时需要补齐这段分发。修改 Manifest 中的公共入口、`AppRouter` 或 `AppLaunchJump` 的公开跳转、`AppNavigator` 的登录拦截、主 Tab 或上表页面职责时，应同时更新本节；普通页面内部跳转不需要登记。

## 三条主要业务流程

- 小虾对话：进入或恢复会话后，历史分页、增量补拉、SSE 与本地乐观消息统一进入内存消息仓库，再映射为多类型列表。
- 模拟面试：小虾入口创建或恢复面试房间，房间内通过 SSE 动作推进目标岗位、问题、语音作答和结束状态，报告生成回到小虾对话卡片轮询。
- 在线简历：结构化简历、求职偏好和理解度分别查询；字段保存成功后通过公共事件重新查询，附件简历由独立页面上传并轮询解析状态。

## 开发时先守住这些约束

- 当前包名和命名空间是 `com.shiyuan.fastclaw`；根 `README.md` 中的旧模板包路径不是当前代码事实。
- 页面只协调交互和渲染，接口调用归 ViewModel/Repository；Adapter 不发起业务请求。
- 聊天消息只在 ViewModel 内存中合并，不以本地数据库作为会话事实来源。
- 在线简历、求职偏好、理解度和附件解析状态均以服务端查询结果为准。
- 代码生成红线、验证档位和敏感信息要求统一以 `AGENTS.md` 为准，这里不重复维护。

## 按任务查文档

| 要做什么 | 先看什么 | 当前情况 |
| --- | --- | --- |
| 修改 AGENTS、规则路由、Skill、校验器或 AI 文档读取方式 | [AI Skill 与规则治理架构](architecture/ai-skill-governance.md) | active |
| 调整 `baselibrary` 与 App 共享代码的职责或依赖 | [App 分层架构](architecture/app-layering.md) | active |
| 新增、移动或审查 `feature` 包和子包 | [Feature 包结构与边界](architecture/feature-package-structure.md) | active |
| 处理聊天、通用卡片、SSE 或消息列表 | [聊天对话](features/chat-conversation.md) | active；实现中 |
| 处理招聘消息列表、P2P 沟通或 IM 业务卡片 | [消息沟通](features/message.md) | active；实现中 |
| 处理面试房间、语音作答、恢复或面试报告轮询 | [模拟面试](features/mock-interview.md) | active；实现中 |
| 处理简历、求职偏好、理解度或附件上传 | [在线简历](features/online-resume.md) | active；实现中 |
| 处理机会匹配、已处理机会、岗位/公司详情或求职宝典 | [机会与求职宝典](features/opportunity.md) | active；实现中 |
| 查询或维护 App 后端接口、请求域和 SSE 入口 | [App 网络接口清单](contracts/app-api-inventory.md) | active；以当前源码静态核对为准 |
| 处理 IM 私有依赖、账号凭据、登录登出、消息、会话生命周期或离线推送 | `App.kt`、`core/User.kt` 和 `core/im/` | 当前没有独立功能文档 |

新开发者先读根 `README.md`、`AGENTS.md` 和本页，再选一篇与负责功能直接相关的文档。进入代码后，以表格中的稳定目录或入口继续定位，不需要从头阅读全部 docs。

AI 做普通代码修改时不预读 docs。任务确实需要项目背景时，先从本页选择一篇直接相关的当前文档；证据不足再继续查代码或其他文档。版本归档标记为 `history-only`，只在版本比较、回归、迁移、回滚或追溯决定时读取。链接表示可继续查找，不表示需要全部打开。
