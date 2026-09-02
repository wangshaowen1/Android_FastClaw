---
title: 机会与求职宝典
type: feature
status: active
implementation: in-progress
applies_to: Android app 模块的机会匹配、已处理机会、机会详情与求职宝典
last_verified: 2026-09-02
---

# 机会与求职宝典

## 目标

机会模块帮助候选人完成匹配前置资料后，持续接收服务端筛选的岗位机会，并在同一业务入口查看已处理机会、岗位详情、公司公开信息和求职工具。机会列表、统计、状态和可用操作均以服务端响应为准，客户端只负责展示、触发操作和在结果变化后重新查询。

## 范围与非目标

- 包含：机会/求职宝典双 Tab、简历与求职偏好完成引导、新机会轮询、机会统计、已处理机会预览与分页、岗位详情、公司主页、机会状态操作、企业 IM 会话入口和宝典工具分发。
- 不包含：简历字段编辑规则与附件任务本身（见[在线简历](online-resume.md)）、招聘沟通消息与会话生命周期（见[小虾聊天对话](chat-conversation.md)）、模拟面试房间（见[模拟面试](mock-interview.md)）、服务端推荐算法和完整接口 Schema。

## 当前事实与边界

- 主 Tab 的“机会/宝典”由 `HomeOpportunityFragment` 承载，固定使用 `ViewPager2` 的两个页面：`OpportunityFragment`（机会）和 `HomeTreasuryFragment`（求职宝典）。页面由缓存适配器维护，预加载两个页面，因此不可见的宝典页也可能已执行初始化查询。
- `OpportunityViewModel` 是机会首页和已处理机会页共享的状态所有者；它继承 `HomeReportViewModel`，复用了简历查询/上传、求职工具模块和面试直通车状态查询能力。详情和公司主页分别使用独立 ViewModel。
- 机会匹配资格只有两项同时满足才成立：最新简历首条记录 `parseStatus == 2`，以及 `checkJobAgentAttributeAnswers().result == true`。否则展示上传简历和确认求职偏好引导，不请求匹配首页数据。
- 游客不会发起机会导航数量、新机会单次查询或新机会轮询；退出/切换用户时机会列表、统计和完成状态会清空。主 Tab 数量由 `MainViewModel` 查询服务端 `unprocessedCount`，0 时隐藏，展示值超过 99 时按 99 显示。
- 新机会接口每次返回的是当前有效未处理批次，写入 `matchingData` 时整体替换，不在客户端合并或递减。已处理机会首页预览请求第一页 5 条；完整列表按 `COMMUNICATING`、`UNSUITABLE` 两个状态分页。

## 页面职责与状态所有者

| 页面/状态 | 所有者 | 关键规则 |
| --- | --- | --- |
| 机会/宝典 Tab 与横向切换 | `HomeOpportunityFragment` | 只保留两个固定页面；点击和滑动必须同步 Tab 选中态 |
| 匹配资格与首页数据 | `OpportunityFragment` / `OpportunityViewModel` | 简历、偏好完成后才查询统计、新机会和已处理预览 |
| 简历引导与解析状态 | `OpportunityFragment` / 继承的 `HomeReportViewModel` | 解析中持续查询；成功、失败和未上传对应不同操作区 |
| 已处理机会列表 | `ProcessedOpportunityActivity`、`ProcessedOpportunityListFragment` | 两个状态页共享聚合计数，但各自拥有 Paging3 列表流 |
| 岗位详情与底部操作 | `OpportunityDetailActivity` / `OpportunityDetailViewModel` | `jobClosed` 优先于 `status`；按钮文案和编码来自 `actions` |
| 公司公开信息 | `CompanyDetailActivity` / `CompanyDetailViewModel` | 企业 ID 必须为正数；企业名称为空时回退品牌名 |
| 求职宝典模块 | `HomeTreasuryFragment` / 继承的 `HomeReportViewModel` | 只展示客户端支持的服务端 `reportType`，点击前统一登录拦截 |

## 主要流程和状态变化

### 机会首页

进入或回到可见页面时，已登录用户补查简历和求职偏好完成状态。两项均完成后并行查询统计、最新未处理机会和最近已处理机会，并在页面可见且当前页停留在机会 Tab 时启动新机会轮询（当前间隔 15 秒）。

机会首页有两种业务展示状态：

- 未满足匹配资格：显示“先让小虾理解你”卡片。简历解析中每 3 秒轮询一次；解析成功后求职偏好卡片才可操作，未上传或解析失败时保留上传入口。
- 已满足匹配资格：新机会为空时显示“正在寻找新机会”和统计数据；新机会非空时显示新机会列表及匹配数量，同时保留已处理机会预览。新机会列表为空/非空只影响展示，不改变轮询的服务端事实。

简历上传链路为：选择来源 → 文件安全检查并上传 → 提交简历 URL/文件名（可带替换任务 ID）→ `resumeUploadResult` → 重新查询解析状态。微信回传使用授权码换取绑定简历后走同一提交链路，授权码处理完成后清空。Lottie 空态动画仅在页面已恢复、机会页被选中、内容未滚动且视图实际可见时播放。

### 已处理机会

首页预览条目点击岗位详情；“查看全部”打开 `ProcessedOpportunityActivity`。完整页固定两个 Tab：

1. `COMMUNICATING`：沟通中；
2. `UNSUITABLE`：不合适。

每个 Tab 通过 `OpportunityProcessedPageBody(status, pageNum, pageSize)` 创建缓存的 Paging3 流，页面进入 `STARTED` 时收集。分页数据同时返回两个状态的聚合总数，页面用状态查询的结果更新两个 Tab 文案。下拉刷新会重新加载当前流并重新查询当前状态计数。

### 岗位详情与公司主页

详情页在 `onResume()` 以固定 `jobId` 重新查询，因此返回页面时仍以服务端状态为准。推荐理由和岗位描述使用 Markdown 渲染，岗位描述默认折叠到 20 行；`enterpriseId` 可打开公司主页，公司页动态渲染福利标签。

详情底部状态优先级为 `jobClosed == true → CLOSED`，否则按 `NEW`、`COMMUNICATING`、`UNSUITABLE` 映射。按钮按服务端 `actions` 顺序取前两个：关闭岗位不可操作；新机会和沟通中展示第二个按钮；缺少 `actionCode` 的按钮禁用。`MARK_COMMUNICATING` 先创建企业 IM 会话，成功后带岗位上下文进入消息页；其他操作直接更新机会状态。

## 跨页面刷新事件

`OpportunityListRefreshEvent` 是“重新查询”的通知，不携带新的机会事实：

| 事件类型 | 当前生产点 | 机会首页处理 |
| --- | --- | --- |
| `1` | 未读新机会首次成功打开详情 | 只重新查询新机会列表 |
| `2` | 详情状态操作、创建 IM 会话或消息侧切换职位完成 | 重新查询新机会和已处理预览 |

已处理列表对两种类型都执行 Paging3 refresh 和状态计数查询。详情页只在传入 `unread=true` 且第一次成功收到详情后发送类型 1，并立即把 Intent 标记改为已处理，避免同一页面重复通知。

## 失败与恢复

- 机会详情状态更新无论业务错误还是成功，都会静默重新查询详情；不能用本地点击结果直接覆盖 `status` 或 `actions`。
- 企业 IM 会话创建时 `jobId` 无法转换为正数时直接结束请求；服务端返回无效对端账号时不进入聊天。
- 已处理机会分页将非成功响应、空数据节点和普通异常转换为 Paging 错误；协程取消继续向上抛出，避免被误判为加载失败。
- 新机会轮询启用网络错误重试，页面不可见、用户退出或 ViewModel 销毁时取消；简历解析轮询同样重试且同一时间只允许一个任务，但当前在页面不可见时不会主动停止，会在解析进入终态、用户切换或 ViewModel/视图销毁时停止。
- 首页已处理预览请求失败时清空预览列表，避免继续展示旧数据；完整分页列表由 Paging3 的重试/空态控件承接。
- 页面入口对空 `jobId`、非法 `enterpriseId` 和越界 Tab 索引做拦截或裁剪，不创建无效页面。

## 契约与依赖

- 机会接口常量、Retrofit 声明、请求体和响应模型：`app/src/main/java/com/shiyuan/fastclaw/core/network/apis/ApiConstants.kt`、`ApiService.kt` 及其 `Opportunity*` 模型。
- 主要接口路径：`/api/candidate/app/opportunity/nav/count/query`、`/api/candidate/app/opportunity/stats/query`、`/api/candidate/app/opportunity/new/query`、`/api/candidate/app/opportunity/processed/page`、`/api/candidate/app/opportunity/detail/query`、`/api/candidate/app/opportunity/company/detail/query`、`/api/candidate/app/opportunity/status/update`；企业 IM 会话使用 `/api/app/im/session/create`。
- 机会状态刷新事件：`app/src/main/java/com/shiyuan/fastclaw/core/Events.java`；事件只触发重新查询，不能视为服务端状态缓存。
- 简历上传、解析轮询和求职宝典列表：`app/src/main/java/com/shiyuan/fastclaw/feature/report/HomeReportViewModel.kt`；简历字段编辑规则见[在线简历](online-resume.md)。
- 企业沟通承接：`app/src/main/java/com/shiyuan/fastclaw/feature/message/chat/MessageChatActivity.kt`；模拟面试与报告工具承接见[模拟面试](mock-interview.md)。

## 怎么验收

- 游客、未上传简历、解析中、解析成功、解析失败、偏好未完成和两项均完成时，机会首页展示和可点击入口与匹配资格一致。
- 已匹配用户在进入页面、回到前台、下拉刷新和轮询过程中，统计、新机会和已处理预览来自最新服务端响应；新机会列表不发生客户端拼接或本地递减。
- 已处理机会两个 Tab 的分页、空态、失败重试、下拉刷新和服务端总数都与对应 `status` 一致，事件刷新后不会遗留旧列表。
- 详情页对 `NEW`、`COMMUNICATING`、`UNSUITABLE`、关闭岗位和缺少操作编码的响应渲染正确；状态操作后详情与列表均能重新查询，立即沟通能带正确岗位上下文进入聊天。
- 未读详情只触发一次类型 1 刷新；状态变化触发类型 2 刷新；登录切换后旧用户的轮询和页面数据均停止/清空。
- 求职宝典只显示当前客户端支持的五类工具，并分别完成登录拦截、H5 地址或首页事件分发。

当前验证证据为对机会包页面、ViewModel、分页源/适配器、事件生产与消费方、主 Tab 数量和 API 声明的静态核对；仓库未发现覆盖该功能的单元或仪器测试。本次未运行 Gradle。

## 代码与方案是否一致

| 内容 | 代码位置 | 当前状态或差异 |
| --- | --- | --- |
| 机会首页匹配资格、状态展示和轮询 | `app/src/main/java/com/shiyuan/fastclaw/feature/opportunity/OpportunityFragment.kt`、`OpportunityViewModel.kt` | in-progress：资格判断、三类首页展示、简历/新机会轮询已实现；仍需真机确认前后台和 ViewPager2 可见性边界 |
| 已处理机会分页与计数 | `ProcessedOpportunityActivity.kt`、`ProcessedOpportunityListFragment.kt`、`ProcessedOpportunityPagingSource.kt` | complete：两个状态、分页键、错误转换、计数刷新和事件刷新已实现；未执行运行时验证 |
| 岗位详情、状态操作和 IM 入口 | `OpportunityDetailActivity.kt`、`OpportunityDetailViewModel.kt` | in-progress：服务端 actions 驱动、关闭岗位优先级、详情重查和 IM 会话入口已实现；端到端状态与聊天联调未在本次验证 |
| 公司主页 | `CompanyDetailActivity.kt`、`CompanyDetailViewModel.kt` | complete：企业 ID 校验、名称回退、摘要和福利标签渲染已实现；缺少接口异常/空数据的设备证据 |
| 求职宝典分发 | `HomeTreasuryFragment.kt`、`HomeReportViewModel.kt` | in-progress：五类 reportType 过滤和入口分发已实现；服务端新增类型会被隐藏，游客预加载请求是否允许需确认 |
| 主 Tab 未处理数量 | `feature/main/MainActivity.kt`、`MainViewModel.kt` | in-progress：登录恢复时查询、游客/退出时清零和 99 上限已实现；当前代码只找到事件消费者，机会状态变化后没有明确的事件生产点，可能要等主页面恢复才刷新 |

## 风险和待确认事项

- `MainActivity` 消费 `OpportunityNavCountRefreshEvent`，但当前仓库没有对应的发送点；若要求详情已读或状态操作后立即更新底部红点，需要补齐生产点并确认刷新时机。
- `HomeOpportunityFragment` 将宝典页预加载到 `offscreenPageLimit = 2`，`HomeTreasuryFragment` 会独立请求模块列表；需确认游客是否允许该请求，或由服务端保证游客响应安全。
- `OpportunityFragment` 的新机会轮询当前代码为 15 秒，KDoc 中仍有“每 5 秒”的旧描述；后续调整周期时应以产品/服务端限流约定为准并同步文档与代码注释。
- 简历解析轮询在 `onVisibilityChanged(false)` 路径不会停止，切到宝典或其它主 Tab 后可能继续请求；需要确认是否应与新机会轮询保持一致的可见性策略。
- 详情页使用 `actions` 前两个元素决定按钮位置，协议若不保证顺序或返回超过两个操作，需先确认新的映射规则。
- 当前所有验证均为静态检查，未证明轮询取消、Lottie 播放、分页加载、登录切换和真实 IM 会话在设备上的生命周期表现。
