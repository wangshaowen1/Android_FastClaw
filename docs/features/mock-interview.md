---
title: 模拟面试
type: feature
status: active
implementation: in-progress
applies_to: Android app 模块的模拟面试房间与报告生成
last_verified: 2026-08-07
---

# 模拟面试

## 目标

模拟面试让候选人从小虾对话入口进入一个独立面试房间：先确认目标岗位，再按服务端问题进行语音作答，面试官文本边到达边展示并按需播报；用户作答边识别边展示，点击结束后提交后台。面试结束后回到小虾对话，由报告卡承接生成进度、失败重试和详情入口。

## 范围与非目标

- 包含：面试创建、未结束面试恢复、目标岗位提交、面试间 SSE 动作、房间状态机、服务端文本流式展示与 TTS、用户 ASR 草稿与作答提交、违规回答替换、强制结束、报告生成轮询。
- 不包含：面试报告详情页内容、扣费权益规则本身、服务端问题生成逻辑、音频 SDK 内部实现、本地持久化面试记录。

## 当前事实与约束

- 小虾对话页负责创建和恢复入口。`ClawAgentViewModel` 发起 `startMockInterviewSession()`、`queryMockInterviewUnfinished()` 和强制结束未完成面试；存在未结束面试时由 `InterviewRecoverDialog` 决定继续或结束。
- 面试房间由 `InterviewActivity` 和 `InterviewViewModel` 管理。Activity 负责 UI、ASR、TTS 和事件观察；ViewModel 持有 `interviewId`、`conversationId`、`lastMessageSeq`、当前题目、提交态、消息缓存、倒计时和 SSE 连接。
- 面试间所有业务动作复用 `mockInterviewStream()`，通过 `actionType` 区分 `ENTER_ROOM`、`SESSION_RECOVER`、`TARGET_JOB_SUBMIT`、`ANSWER_START` 和 `ANSWER_SUBMIT`。
- `InterviewSseProcessor` 只按 SSE event 名解析并回调业务消息，不操作 UI；`InterviewViewModel.sseInsertMessages()` 再按 `messageType` 分发到状态、文本、违规、结束或错误处理。
- 面试房间消息只保存在 ViewModel 内存，恢复现场以服务端返回的 `messages`、`currentQuestion` 和 `latestMessageSeq` 为准。
- 用户语音作答在端侧先以草稿消息展示，只有点击结束或倒计时结束后才提交后台；提交时生成 `answerSubmitId`，用于后续 `TEXT_BLOCKED` 替换本地历史回答。

## 目标设计

| 职责 | 所有者 | 不变量 |
| --- | --- | --- |
| 面试入口与权益结果 | `ClawAgentViewModel` / `ClawAgentMainFragment` | 扣费成功才进入房间；恢复弹窗不直接伪造房间状态 |
| 房间上下文与状态机 | `InterviewViewModel` | 所有服务端状态先进入 ViewModel，再驱动 Activity 底部区域 |
| SSE 事件解析 | `InterviewSseProcessor` | 解析层只做事件类型分发和 JSON 转换，不理解业务状态 |
| 消息身份与展示 | `messageStore` / `InterviewRoomMessageAdapter` | 同一条流式 Agent 文本保持一个气泡；同一次用户提交可被违规消息定位替换 |
| 语音输入与播报 | `InterviewActivity` | 用户开始作答前必须停止或屏蔽面试官 TTS，避免 ASR 录入播报内容 |
| 报告生成状态 | `ClawAgentViewModel` | 报告卡轮询到终态后拉取增量消息刷新卡片，不在本地直接拼报告结果 |

## 关键决策

- 面试房间不是小虾主聊天列表的普通卡片展开态，而是独立 Activity。创建、恢复和报告卡仍归小虾对话域，实时问答归 `feature/interview` 域。
- `lastMessageSeq` 只作为下一次动作请求水位，不用于在 `sseInsertMessages()` 中拦截同序号业务消息；meta 或 payload 可能先推进水位，同序号消息仍需要处理。
- 服务端有 `TEXT_DELTA` 时，`TEXT_DONE` 只覆盖当前流式气泡的最终内容；服务端只返回完整文本时，端侧拆成小块模拟流式展示并分批送入 TTS。
- 用户一次作答只允许提交一次。提交后进入 `ANSWER_SUBMITTED`，必须等待服务端返回新 `questionId` 或违规可重答状态后才能再次作答。
- `TEXT_BLOCKED` 优先按 `answerSubmitId` 替换对应用户回答，而不是追加一条新消息；这样用户看到的是“本次回答被拦截后的结果”，并能继续重答当前题。
- 面试结束不立即关闭页面；`INTERVIEW_FINISHED` 的结束文本展示和 TTS 播完后，才通知 Activity 关闭。

## 关键流程与状态

进入流程为：小虾入口创建面试 → 扣费成功打开 `InterviewActivity` → 查询音频 token → `ENTER_ROOM` 或 `SESSION_RECOVER` → 服务端返回房间状态和消息 → Activity 根据 `InterviewRoomState` 切换上传、等待、作答或结束区域。

目标岗位流程为：等待目标岗位 → 上传岗位截图或选择岗位 → `TARGET_JOB_SUBMIT` → 目标岗位解析中 → 计划生成中 → 等待用户作答。解析失败回到上传区，计划生成和回答提交期间展示等待态。

作答流程为：面试官题目流式展示和按需播报 → TTS 完成后自动开始录音或等待用户点击 → `ANSWER_START` → ASR partial/final 更新本地用户草稿 → 用户点击结束或倒计时结束 → `ANSWER_SUBMIT` → 等待追问判断或下一题。`WAITING_FOLLOW_UP_DECISION` 只驱动等待态，不作为文本气泡展示或播报。

恢复流程为：继续未完成面试 → `SESSION_RECOVER` → 服务端以 `RESTORE_ROOM` 返回历史消息、当前题、最新序号和房间状态 → 端侧清空旧缓存后重建列表。恢复消息按 `messageSeq` 排序；目标岗位确认欢迎语可能出现多个不同 `messageId`，端侧只按文案保留一条，其他消息按 `messageId` 或 `messageSeq` 保留，避免相同回答内容被误合并。

报告流程为：房间进入报告生成或结束状态后返回小虾对话 → 生成中的模拟面试报告卡可见时启动 `pollMockInterviewReportProgress()` → 成功或失败终态后拉取增量消息刷新卡片 → 失败重试调用 `retryGenerateMockInterviewReport()` 后只重启最后一个报告轮询。

## 失败与恢复

- 创建面试扣费失败时留在小虾对话并展示虾条不足或服务端提示，不进入面试房间。
- 目标岗位上传或解析失败时回到上传区；上传成功不代表解析成功，必须等待房间状态推进。
- `ANSWER_START` 缺少题目 ID 时端侧使用当前题目，仍为空则生成 UUID 兜底，但最终题目事实应以服务端新消息为准。
- ASR 启动失败或权限缺失时不进入作答态；取消录音会清掉用户草稿，不提交后台。
- TTS 失败在普通问题场景下提示并继续允许作答；结束流程中 TTS 失败也关闭页面，避免卡在结束等待态。
- `TEXT_BLOCKED` 如果无法定位本地用户回答，会兜底插入为面试官提示；只有服务端状态允许等待作答时才清除提交态并允许重答。
- ViewModel 清理时取消倒计时、本地流式任务和 SSE 连接；报告轮询在小虾对话 ViewModel 中受独立 Job 管理。

## 契约与依赖

- 面试 API 和模型：`app/src/main/java/com/shiyuan/fastclaw/core/network/apis/`。
- 面试房间：`app/src/main/java/com/shiyuan/fastclaw/feature/interview/`。
- SSE 原始流和解析：`app/src/main/java/com/shiyuan/fastclaw/core/network/sse/`。
- 小虾入口、恢复弹窗触发和报告卡轮询：`app/src/main/java/com/shiyuan/fastclaw/feature/clawagent/`。
- 文档只记录端侧长期约束；完整请求、响应和房间状态枚举以联调文档与 `MockInterviewBody`、`InterviewAgentMessage` 为事实源。

## 验收与验证

- 从小虾入口创建面试时，只有扣费成功进入房间；杀进程后可恢复未完成面试，并且历史消息不出现重复欢迎语。
- 目标岗位上传、选择岗位、解析失败和计划生成状态能切换到正确底部区域。
- 服务端连续多段或多条 Agent 文本不会互相打断播报；最终 `TEXT_DONE` 不额外生成重复气泡。
- 用户语音作答过程中文本边识别边展示，点击结束后只提交一次；等待新题期间不能重复开始作答。
- `TEXT_BLOCKED` 能替换对应用户回答并允许重答；普通违规提示遵循 `ttsEnabled`。
- 面试结束语播报完成后关闭房间；报告生成卡轮询到成功或失败后通过增量消息刷新。

当前验证证据为对小虾入口、面试 Activity、ViewModel、Adapter、SSE processor、API 声明和报告轮询代码的静态核对；仓库未发现覆盖该功能的单元或仪器测试。本次未运行 Gradle。

## 实现对齐

| 关注点 | 稳定代码锚点 | 状态或偏差 |
| --- | --- | --- |
| 创建、恢复入口与报告卡 | `app/src/main/java/com/shiyuan/fastclaw/feature/clawagent/` | in-progress：创建、未完成查询、恢复弹窗、强制结束和报告轮询已实现；报告终态依赖增量消息刷新 |
| 房间状态与消息合并 | `app/src/main/java/com/shiyuan/fastclaw/feature/interview/InterviewViewModel.kt` | in-progress：SSE 分发、恢复、流式文本、违规替换和倒计时已实现；消息只保存在内存 |
| 语音输入与 TTS | `app/src/main/java/com/shiyuan/fastclaw/feature/interview/InterviewActivity.kt` | in-progress：ASR 草稿、TTS 队列、自动作答和结束后关闭已实现；需要真机验证音频抢占和权限边界 |
| SSE 事件解析 | `app/src/main/java/com/shiyuan/fastclaw/core/network/sse/processor/InterviewSseProcessor.kt` | complete：按文档事件名解析 meta、业务消息、done 和心跳 |
| 消息列表展示 | `app/src/main/java/com/shiyuan/fastclaw/feature/interview/InterviewRoomMessageAdapter.kt` | complete：AI/用户双类型、DiffUtil 局部刷新已实现 |

## 风险与待确认问题

- `InterviewViewModel.restoreRoom()` 在 Debug 下会打印完整恢复消息；需要确认其中是否包含用户回答、岗位信息或其它敏感内容，线上日志必须避免完整输出。
- `sendAction()` 每次启动新的 SSE 流时未在文档中确认是否允许并发动作；如果用户快速操作或网络慢，需用服务端幂等和端侧状态共同验证不会交叉。
- 作答倒计时为端侧 180 秒，服务端 payload 也存在 `remainingSeconds` 和 `answerDeadlineAt`；两者权威关系需要服务端契约确认。
- `ttsEnabled` 为空时端侧默认播报；如果后端希望默认不播，需要显式返回 `false`。
