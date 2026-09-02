---
title: 消息沟通
type: feature
status: active
implementation: in-progress
applies_to: Android app 模块的招聘消息列表、P2P 沟通与业务消息卡片
last_verified: 2026-09-02
---

# 消息沟通

## 目标

消息模块承接主 Tab 的招聘沟通：展示云信 P2P 会话列表，进入单聊后加载历史和实时消息，支持文本沟通、职位卡片、HR 索要简历和求职者投递简历，并保持未读、草稿和岗位上下文一致。

## 范围与非目标

- 包含：会话列表、已标记不合适会话入口、P2P 单聊、历史分页、实时消息监听、文本消息、简历请求/投递卡片、职位卡片、会话草稿、未读清理、沟通职位切换和 IM 业务通知。
- 不包含：小虾智能体消息流与 SSE（见[聊天对话](chat-conversation.md)）、机会匹配和岗位详情本身（见[机会与求职宝典](opportunity.md)）、在线简历字段编辑和附件解析（见[在线简历](online-resume.md)）、IM SDK 的宿主初始化和推送底层实现。

## 当前事实与边界

- MainActivity 的消息 Tab 只负责承载 MessageFragment；云信账号和会话同步由 App、MainViewModel 及 core/im 初始化。用户信息成功后查询 IM 账号并调用 AccountHelper.login()，主 ViewModel 持续收集 SDK 聚合未读数，退出登录时清零。
- 消息列表不通过业务 REST 接口分页，而是从 SDK ConversationManager 查询并监听会话变化。BaseConversationViewModel 只保留 P2P 且 contactId 非空的会话，并按最后消息时间倒序维护内存缓存。
- 普通消息页过滤 serverExtension["hr"]["isInappropriate"] == true 的会话，将它们汇总为一行“已标记不合适”入口；UnableMessageActivity 使用同一套会话缓存规则只展示这些会话。
- 会话副标题来自 HR 公司和职位；名称、头像、公司等资料优先使用会话扩展中的 hr 节点，缺失时回退 SDK 会话资料或 contactId。末条摘要由本地草稿、业务自定义附件协议或 SDK 普通消息类型生成。
- 聊天消息只保存在 MessageChatViewModel 的内存 messageCache，key 为 SDK messageClientId；本地不使用数据库作为历史事实来源。消息模型和列表 Adapter 与 SDK MessageWrapper 解耦。
- 输入草稿保存在 MMKV 实例 MESSAGE_DRAFT，以会话 ID 作为 key 后缀；非空输入保存，发送或清空输入时删除，回到会话页时恢复。

## 页面职责与状态所有者

| 页面/状态 | 所有者 | 关键规则 |
| --- | --- | --- |
| 消息主列表 | MessageFragment / MessageViewModel | 只展示普通 P2P 会话，并追加不合适会话汇总入口 |
| 不合适会话列表 | UnableMessageActivity / UnableMessageViewModel | 只筛选 isInappropriate == true 的会话；点击仍进入同一单聊页 |
| 会话缓存与 SDK 监听 | BaseConversationViewModel | 监听加载、变更、删除和状态变化；页面销毁时解除监听并取消加载任务 |
| 单聊上下文和消息缓存 | MessageChatViewModel | 管理会话打开、历史、实时消息、发送、未读和 ViewModel 生命周期 |
| 单聊页面交互 | MessageChatActivity | 负责输入、滚动、键盘 Insets、草稿、弹框和消息事件分发 |
| 消息类型渲染 | MessageAdapter、MessageChatAdapter 与各 ViewHolder | Adapter 只做多类型映射和点击转发，不直接访问网络或 SDK 业务状态 |

## 会话列表流程

页面或登录态变化时调用 getConversationList()。未登录或 SDK 查询异常时发布空列表；已登录时查询会话，过滤群聊和空对端账号，写入缓存并按会话时间倒序输出。SDK 后续的会话新增、更新、删除和进入聊天后的未读清零通过 ConversationListener 增量刷新。

MessageViewModel 将不合适会话从普通列表剔除并追加一个聚合入口；MessageAdapter 以 id + itemType 判断同一列表行，使用 DiffUtil 做差量更新。下拉刷新重新查询 SDK 会话，UserChangedEvent 触发同样的重新查询；主列表重新可见时会重建行模型以读取最新草稿。

## P2P 单聊流程

进入聊天时由 MessageChatParams 携带 sessionId、contactId、联系人信息以及当前沟通岗位。页面以 imAccessId|1|contactId 生成 P2P 会话 ID，ViewModel 打开 Chat、注册发送结果监听、激活会话、清零未读、补查对方头像，然后同时订阅实时消息/修改消息并查询最近历史。

首次历史成功后过滤已删除消息和协议专用的简历请求结果消息，按 messageClientId 重建缓存；向上滚动接近顶部时以最早消息为锚点继续查询更早历史，并对重复 ID 去重。消息转换为文本左右气泡、职位卡片、简历请求卡片或简历投递卡片后由 MessageChatAdapter 差量刷新。

消息时间在首条或相邻消息间隔超过 5 分钟时显示。新消息到达时，用户原本在列表底部才自动跟随滚动；用户正在查看历史时不强制跳到底部。用户主动发送后设置一次性贴底标记，连续发送时使用本地顺序时间避免 SDK 回填时间造成顺序抖动。

## 业务消息与操作

| SDK subtype | 协议类型 | 页面行为 | 后端衔接 |
| ---: | --- | --- | --- |
| 1001 | resumeRequest | 展示 HR 索要简历卡片；待处理时显示同意/拒绝 | /api/app/im/resume/send/agree，需要 sessionId、原消息 messageServerId 和处理状态 |
| 1002 | resumeDelivery | 展示求职者投递的简历文件及状态 | 先查询最新解析成功简历，发送文本提示和自定义附件 |
| 1003 | jobCard | 展示岗位快照；点击进入机会详情，isJobChanged 决定切换职位文案 | 创建机会 IM 会话或切换职位后由服务端发送/更新卡片 |
| 1004 | resumeRequestResult | 协议结果消息，不进入聊天列表展示 | 由后台更新原简历请求卡片，客户端通过修改消息监听刷新 |

普通文本按发送方向渲染左右气泡；暂不支持的消息类型回退为文本或“自定义消息”摘要。文本气泡长按复制。职位卡片点击使用其中的 jobId 打开机会详情，HR 头像点击使用当前会话岗位 ID 回到机会详情。

用户点击“发简历”后查询第一页最多 100 条简历，只选 parseStatus == 2 且上传时间最新的一条；确认弹框通过后发送固定提示文本和 resumeDelivery 附件。HR 索要简历时，同意/拒绝请求先通知后端，成功后由服务端修改原卡片；同意操作再发送“您好，我已投递简历”。

首次创建会话且历史加载完成后，ViewModel 自动发送一次开场白。用户第一条主动文本发送后调用首条消息通知接口。若进入聊天时参数标记为职位已切换，页面先展示二次确认，确认后调用职位切换接口，并发送 OpportunityListRefreshEvent(2) 通知机会模块刷新列表。

## 事件与跨模块依赖

- MessageChatAdapter 通过 ItemEventsViewModel<MessageChatItemEvent> 向 Activity 发送强类型事件；MessageChatActivity 统一处理职位卡片、HR 头像、简历请求同意/拒绝和简历文件点击。
- UserChangedEvent 由消息列表消费并重新查询会话；真正的 SDK 登录、账号同步和未读聚合仍由 App/MainViewModel 负责。
- OpportunityListRefreshEvent(2) 从聊天侧职位切换成功回传机会模块；消息模块不把该事件当作本地机会状态，也不直接修改机会列表。
- ViewModel 销毁时关闭当前 Chat，并依靠 viewModelScope 取消实时收集和头像查询；列表 ViewModel 同时移除 ConversationListener。

## 契约与依赖

- IM 宿主初始化与账号生命周期：app/src/main/java/com/shiyuan/fastclaw/App.kt、app/src/main/java/com/shiyuan/fastclaw/feature/main/MainViewModel.kt、app/src/main/java/com/shiyuan/fastclaw/core/im/。
- 会话/聊天业务代码：app/src/main/java/com/shiyuan/fastclaw/feature/message/，其中 data/ 保存会话参数、草稿和自定义附件协议，chat/ 保存单聊页面、ViewModel、Adapter 和 ViewHolder。
- 业务接口声明和请求体：app/src/main/java/com/shiyuan/fastclaw/core/network/apis/ApiService.kt、ApiConstants.kt 及 Im*、ResumeSendAgreeBody 模型。
- 当前使用的业务接口：/api/app/im/account/query、/api/app/im/token/refresh、/api/app/im/resume/send/agree、/api/app/im/job/switch、/api/app/im/session/firstMessage/notify。机会侧创建会话的 /api/app/im/session/create 由[机会与求职宝典](opportunity.md)负责。
- 自定义附件跨端字段以 ResumeRequestAttachment、ResumeDeliveryAttachment、MessageJobAttachment 的 attachment.raw 序列化/解析实现为准；本文不复制完整 JSON Schema。

## 失败与恢复

- 未登录或 IM 尚未完成账号登录时，会话列表按空列表展示；单聊入口必须同时具备非空 contactId 和可用的 SDK 登录态。当前 MessageChatActivity/MessageChatViewModel 没有显式校验或等待登录完成，失败表现需通过运行时验证确认。
- 首次历史查询失败或异常时结束 loading，但当前页面没有单独的历史重试状态；实时消息与历史查询并发期间，历史成功回调会清空并重建缓存，可能覆盖查询期间已到达的消息。
- 历史和实时消息以 messageClientId 去重；当前 mergeMessages() 对后来收到的已删除消息只过滤、不主动从已有缓存移除，删除事件后的 UI 一致性需要验证。
- 简历请求缺少 messageServerId 时不发送请求并提示失败；查询不到解析成功简历时不打开确认弹框。简历文件点击事件目前由 Activity 分支 Unit 处理，尚未打开预览或下载。
- 发送文本使用本地即时上屏，再由 SDK 发送成功/失败回调回填同一消息状态；首条消息通知在本地创建发送消息后即调用，是否必须等待 SDK 发送成功由后端契约确认。
- 登录切换或查询异常只将当前页面发布为空列表，但 BaseConversationViewModel 的私有 conversationCache 当前不会在未登录分支显式清空；后续监听或刷新可能重新暴露旧账号缓存，需补充账号切换验证。

## 怎么验收

- 已登录、未登录、SDK 未登录、登录切换和退出登录时，消息列表不会展示错误账号的会话；P2P 会话、群聊和空 contactId 的过滤符合约定。
- 普通会话、草稿、未读数、公司/职位副标题和不合适会话聚合入口在 SDK 加载、变更、删除及未读清零后正确更新。
- 进入单聊后历史、实时新消息、修改消息、上拉历史、重复消息、消息时间和滚动跟随行为稳定；离开页面后 Chat 和监听均释放。
- 文本、职位卡片、简历请求、简历投递和未知消息类型映射到正确布局；同意/拒绝简历请求和发送简历的业务状态能在原卡片上刷新。
- 首次创建会话只发送一次开场白，首条主动文本只触发一次通知；职位切换确认成功后机会列表收到类型 2 刷新事件。
- 草稿按会话恢复，发送后清除；键盘展开/收起、发简历面板、返回键和长按复制不破坏消息列表滚动。

当前验证证据为对消息列表、单聊 Activity/ViewModel、Adapter、会话/消息数据模型、IM 宿主和 API 声明的静态核对；仓库未发现覆盖该功能的单元或仪器测试。本次未运行 Gradle，也未进行真实 IM 账号、设备或网络断连验证。

## 代码与方案是否一致

| 内容 | 代码位置 | 当前状态或差异 |
| --- | --- | --- |
| 会话列表与不合适入口 | MessageFragment.kt、MessageViewModel.kt、UnableMessageActivity.kt、BaseConversationViewModel.kt | in-progress：P2P 过滤、SDK 监听、DiffUtil、草稿摘要和不合适聚合已实现；登录切换后的缓存清理未运行时确认 |
| 单聊历史、实时监听与消息缓存 | chat/MessageChatViewModel.kt、chat/MessageChatActivity.kt | in-progress：历史上拉、实时/修改消息、去重、滚动和生命周期释放已实现；登录竞态、历史并发覆盖和删除消息清理存在待确认风险 |
| 文本与业务卡片渲染 | chat/MessageChatAdapter.kt、data/*Attachment.kt、各 ViewHolder | in-progress：文本、职位、简历请求/投递协议和状态展示已实现；简历文件点击尚未接入预览/下载 |
| 简历请求、投递与岗位切换 | chat/MessageChatViewModel.kt、chat/MessageChatActivity.kt | in-progress：接口请求和事件回传已实现；首条消息通知时机、服务端修改卡片的端到端结果未验证 |
| IM 账号、未读和云端会话同步 | App.kt、feature/main/MainViewModel.kt、core/im/ | in-progress：账号查询、SDK 初始化、未读收集和退出清零已接入；真实账号登录完成后再打开聊天的时序需设备验证 |

## 风险和待确认事项

- IM 登录是异步过程，当前单聊初始化直接 openChat(Contact(contactId))；如果主界面尚未完成 AccountHelper.login()，可能无法生成有效 P2P 会话。需要确认统一的登录等待或入口拦截策略。
- MessageViewModel、BaseConversationViewModel 和 MessageChatViewModel 当前存在完整会话/消息 JSON 调试输出；这些内容可能包含用户输入、公司、岗位和简历信息，线上日志必须脱敏或关闭。
- 历史查询成功时清空缓存、删除消息仅过滤不移除、分页结束条件使用过滤后的消息数量，这些边界可能造成消息丢失、残留或提前停止分页，需要用真实 SDK 回调顺序验证。
- 草稿只由会话 ID 区分，当前实现依赖 SDK 会话 ID 隔离不同用户；账号切换和同一对端账号复用场景需要确认是否要加入用户维度。
- ResumeFile 点击目前没有业务处理；如果产品要求查看或下载已投递简历，需要补充文件权限、URL 有效期和离线失败规则后再实现。
