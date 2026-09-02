---
title: 在线简历
type: feature
status: active
implementation: in-progress
applies_to: Android app 模块的候选人在线简历
last_verified: 2026-09-02
---

# 在线简历

## 目标

在线简历集中维护小虾理解候选人所需的资料。客户端分别展示和编辑结构化简历、求职偏好与附件简历解析任务，并在成功修改后重新查询服务端结果和理解度，避免把页面缓存当作最终事实。

## 范围与非目标

- 包含：理解度、简历信息与求职偏好双页签、结构化字段和可重复经历编辑、附件上传/替换/删除、小程序回传及解析状态轮询。
- 不包含：本地草稿与历史版本、离线补交、整份简历导出、客户端计算理解度、附件解析内容预览、服务端字段完整性规则。

## 当前事实与约束

- 在线简历包含三类独立数据：结构化简历、求职偏好、附件简历任务。它们分别查询，不能因共享“简历”名称而合并为一个页面状态。
- `OnlineResumeActivity` 持有选中页签和理解度；`ResumeInfoFragment`、`JobPreferencesFragment` 分别渲染两类 section；`InformationViewModel` 发起查询与保存。
- 服务端以 section、attribute group、field tree 和 field item 描述结构化内容。客户端只适配已识别分组；`instanceIndex = -1` 或空 index 表示新增占位，不是已保存经历。
- 普通属性走属性更新；可重复经历按 `attributeGroupCode + instanceIndex` 走 CREATE、UPDATE、DELETE。成功后通过 `Events.CommonAttributeArrayQueryData` 重新查询页面数据和理解度。
- 附件简历由 `MyResumeActivity` 管理。`parseStatus` 为 0/1 时每 2 秒查询一次，2 表示解析成功，其余状态按失败处理；Activity 销毁时停止 Handler 轮询。
- 页面不在本地数据库持久化在线简历。理解度、字段展示值、选项、校验状态和附件解析状态均以服务端响应为准。

## 目标设计

| 领域 | 状态所有者 | 不变量 |
| --- | --- | --- |
| 页签与理解度 | `OnlineResumeActivity` / `InformationViewModel` | 入口索引限制为 0/1；分数显示服务端值 |
| 结构化简历 | `ResumeInfoFragment` / `InformationViewModel` | 保存成功后重新查询；占位模板不作为真实经历展示 |
| 求职偏好 | `JobPreferencesFragment` / `InformationViewModel` | 独立选择页的结果必须匹配明确事件类型和字段编码 |
| 附件任务 | `MyResumeActivity` / `HomeReportViewModel` | 同一页面只运行一个轮询；任务离开解析中状态后停止 |

客户端不维护另一套完整字段 Schema。新增服务端分组时，应显式决定是否需要专用展示/编辑入口；未识别分组不得被静默写入错误字段。

## 关键决策

- 页面入口统一通过 `OnlineResumeActivity.start()` 并裁剪页签索引，避免不同入口维护自己的参数语义。
- 基础字段优先使用服务端 `displayValue`，复杂经历沿用 field tree；新增经历复用服务端下发的占位模板，避免客户端复制字段定义。
- 技能、证书和奖项类长列表保留页面级展开集合；服务端数据刷新后清空展开态，避免旧分组状态污染新结果。
- 附件上传先完成安全文件上传或小程序 code 换取文件信息，再提交解析任务；解析结果通过查询确认，不以提交成功代表解析完成。

## 关键流程与状态

进入页面时，Activity 查询理解度，两个 Fragment 分别查询结构化简历和求职偏好。普通字段或经历保存成功后发送公共更新事件，Activity 刷新理解度，两个 Fragment 各自重新查询。

附件链路为：选择本地文件或接收小程序 code → 获得文件地址 → 提交附件任务 → 查询任务列表 → 存在 0/1 状态时每 2 秒轮询 → 全部任务成功或失败后停止。上传提交和解析完成是两个不同状态。

## 失败与恢复

- 保存缺少属性分组或目标 field tree 时终止并提示“属性数据异常”，不发送不完整请求。
- 新增经历依赖服务端提供可复制的 field tree；模板缺失时当前页面无法构造新增表单。
- 小程序绑定查询完成后清空临时 code，避免 `onResume()` 重复提交。
- 查询结果为空时两个 Fragment 当前直接展示空态，首次加载与真实空数据尚未区分。
- 附件轮询使用防重复标记，并在列表不再包含解析中任务或 Activity 销毁时停止。

## 契约与依赖

- 在线简历接口与模型：`app/src/main/java/com/shiyuan/fastclaw/core/network/apis/`。
- 页面和附件入口：`app/src/main/java/com/shiyuan/fastclaw/feature/resume/`。
- 字段保存与理解度查询：`app/src/main/java/com/shiyuan/fastclaw/feature/resume/InformationViewModel.kt`。
- 跨页面刷新事件：`app/src/main/java/com/shiyuan/fastclaw/core/Events.java`。事件只表示需要重新查询，不携带新的服务端事实。

## 验收与验证

- 任何入口都只能打开简历信息或求职偏好，页签点击、滑动与 ViewModel 状态一致。
- 普通字段及经历 CREATE/UPDATE/DELETE 成功后，相关页面与理解度重新查询；失败时不伪造本地成功状态。
- 占位经历不作为真实内容展示，旧数据刷新后本地展开态不会串到其他分组。
- 选择页结果只更新对应字段；未知事件不应落入任意求职偏好字段。
- 附件任务同一时刻只有一个轮询，进入后台或离开页面后的行为符合确认的生命周期规则，解析完成后列表与结构化简历可刷新到最新状态。

当前验证证据为对 Activity、两个 Fragment、ViewModel、附件页面、事件消费者与 API 声明的静态核对；仓库未发现覆盖该功能的单元或仪器测试。本次未运行 Gradle。

## 实现对齐

| 关注点 | 稳定代码锚点 | 状态或偏差 |
| --- | --- | --- |
| 页签与理解度 | `app/src/main/java/com/shiyuan/fastclaw/feature/resume/OnlineResumeActivity.kt` | complete：入口索引、双向页签同步与字段更新后刷新已实现；0 分时波浪视觉值固定为 10 |
| 结构化简历 | `app/src/main/java/com/shiyuan/fastclaw/feature/resume/ResumeInfoFragment.kt`、`app/src/main/java/com/shiyuan/fastclaw/feature/resume/experience/`、`app/src/main/java/com/shiyuan/fastclaw/feature/resume/profile/` | in-progress：查询、常见分组和增删改已实现；新增依赖服务端模板 |
| 求职偏好 | `app/src/main/java/com/shiyuan/fastclaw/feature/resume/preference/JobPreferencesFragment.kt` | in-progress：专用选择器和通用单选已实现；未知选择事件会被当作福利偏好 |
| 附件简历 | `app/src/main/java/com/shiyuan/fastclaw/feature/resume/MyResumeActivity.kt`、`app/src/main/java/com/shiyuan/fastclaw/feature/report/HomeReportViewModel.kt` | in-progress：上传、删除与轮询已实现；刷新衔接和可见生命周期未闭合 |

## 风险与待确认问题

- `JobPreferencesFragment` 当前把所有未匹配的 `ClawAgentEvent` 当作福利偏好，并记录完整选择结果 JSON；需要改为明确事件白名单并确认线上隐私日志策略。
- 附件上传成功发送 `ResumeEvent`，但 `OnlineResumeActivity` 和两个 Fragment 没有消费该事件；返回在线简历后可能继续展示上传前的数据，需要确定统一刷新触发点。
- 附件查询使用带 Loading 的请求，轮询只在 `onDestroy()` 停止；需要确定进入后台是否继续轮询，以及周期请求是否允许反复影响 Loading。
- 页面文案声称重新上传会覆盖结构化简历但不影响求职偏好，该语义尚需服务端契约确认；确认前不能据此推导局部刷新范围。
- 理解度没有显式裁剪到 0～100，且 0 分文本与波浪值不同；需确认这是长期视觉规则还是临时兜底。
- 首次加载态与真实空态共用空列表，公共 sticky 更新事件又会让多个观察者分别查询；需要通过交互验收确认空态闪现和重复请求是否可接受。
