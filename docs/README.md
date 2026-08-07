---
title: FastClaw 项目文档
type: overview
status: active
last_verified: 2026-08-04
---

# FastClaw 项目文档

## 项目职责

当前分支的 `app` 模块承载 FastClaw 候选人端 Android 实现，同时保留模板工程的公共基础能力。根目录 `README.md` 是模板初始化与构建配置的事实源；本目录只记录无法从单个类直接恢复的业务边界、状态归属和关键取舍。

## 系统地图

| 领域 | 职责 | 稳定代码锚点 | 详细文档 |
| --- | --- | --- | --- |
| 小虾对话 | 会话恢复、SSE 消息、卡片动作、历史合并与报告状态 | `app/src/main/java/com/shiyuan/fastclaw/feature/clawagent/` | [聊天对话](features/chat-conversation.md) |
| 模拟面试 | 面试创建、房间状态、语音作答、SSE 文本流、恢复与报告轮询 | `app/src/main/java/com/shiyuan/fastclaw/feature/interview/`、`app/src/main/java/com/shiyuan/fastclaw/feature/clawagent/` | [模拟面试](features/mock-interview.md) |
| 在线简历 | 结构化简历、求职偏好、理解度与附件解析任务 | `app/src/main/java/com/shiyuan/fastclaw/feature/mine/`、`app/src/main/java/com/shiyuan/fastclaw/feature/information/` | [在线简历](features/online-resume.md) |
| 网络与流式传输 | Retrofit 接口、统一响应处理与原始 SSE 分发 | `app/src/main/java/com/shiyuan/fastclaw/core/network/` | 按任务读取代码；当前没有独立架构文档 |
| 公共应用基础 | Base 类、事件、导航、资源与通用组件 | `app/src/main/java/com/shiyuan/fastclaw/` | [App 分层架构](architecture/app-layering.md) |

## 关键流程

- 小虾对话：进入或恢复会话后，历史分页、增量补拉、SSE 与本地乐观消息统一进入内存消息仓库，再映射为多类型列表。
- 模拟面试：小虾入口创建或恢复面试房间，房间内通过 SSE 动作推进目标岗位、问题、语音作答和结束状态，报告生成回到小虾对话卡片轮询。
- 在线简历：结构化简历、求职偏好和理解度分别查询；字段保存成功后通过公共事件重新查询，附件简历由独立页面上传并轮询解析状态。

## 核心约束

- 当前包名和命名空间是 `com.shiyuan.fastclaw`；根 `README.md` 中的旧模板包路径不是当前代码事实。
- 页面只协调交互和渲染，接口调用归 ViewModel/Repository；Adapter 不发起业务请求。
- 聊天消息只在 ViewModel 内存中合并，不以本地数据库作为会话事实源。
- 在线简历、求职偏好、理解度和附件解析状态均以服务端查询结果为准。
- 代码生成红线、验证档位和敏感信息要求统一以 `AGENTS.md` 为准，本目录不重复维护。

## 有效文档

| 文档 | 用途 | 状态 | 实现状态或适用范围 |
| --- | --- | --- | --- |
| [AI Skill 与规则治理架构](architecture/ai-skill-governance.md) | 说明 AGENTS、quick rules、scenarios、skills 与 references 的职责和维护不变量 | active | AI 规则与项目文档治理 |
| [App 分层架构](architecture/app-layering.md) | 指导 `baselibrary` 与 App 内共享代码的职责、依赖和渐进治理 | active | Android `app` 模块 |
| [聊天对话](features/chat-conversation.md) | 解释会话、消息合并、SSE 和列表状态边界 | active | in-progress；Android `app` 模块 |
| [模拟面试](features/mock-interview.md) | 解释面试房间状态、语音/TTS、恢复、违规替换和报告轮询边界 | active | in-progress；Android `app` 模块 |
| [在线简历](features/online-resume.md) | 解释三类简历数据、编辑刷新和附件解析边界 | active | in-progress；Android `app` 模块 |

## 推荐阅读路径

- 新开发者：先读根 `README.md` 与 `AGENTS.md`，再按负责领域选择一篇功能文档，最后进入对应稳定代码锚点。
- 任务型 AI：规则/Skill 维护读取 [AI Skill 与规则治理架构](architecture/ai-skill-governance.md)；聊天、通用卡片、SSE 或消息列表任务只加载[聊天对话](features/chat-conversation.md)；模拟面试房间、语音作答、恢复或面试报告轮询任务只加载[模拟面试](features/mock-interview.md)；简历、求职偏好、理解度或附件上传任务只加载[在线简历](features/online-resume.md)。局部修改仍以目标代码和 `AGENTS.md` 路由的快速规则为准。
