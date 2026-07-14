---
name: decisions
description: 版本决策、踩坑与验证结论（追加式情节记忆，按需手动维护）
metadata:
  type: project
---

> 情节记忆：记录「发生了什么」——版本决策、踩坑、验证结论。**只追加，不删除、不改写历史条目。**
> 由对话中按需判断后手动追加（用户说“本轮有没有需要更新的 memory？”时触发）。
> **新建项目从模板派生后，请清空下方示例区，重新开始记录本项目历史。**

## 条目格式

```
## v<版本或"本轮"> <变更主题>

### 变更了什么
<字段 / 文件> → <新增 / 删除 / 修改>：<一句话>

### 为什么
<具体症状：哪个版本、哪一处、出现了什么问题>
<根因分析 + 决策推导>

### 验证结论
<验证方式 + 结果，含具体数据> 或 PENDING（说明待验证项）
```

---
<!-- 模板初始无历史条目。新项目的第一条决策从这里开始追加。 -->

## v本轮 U-Link 邀请归因字段收口

### 变更了什么
登录请求链路 → 删除旧 `installData` / `inviteData`：手机号登录、闪验登录、第三方登录统一只传 `inviteAttribution(inviteCode, inviteScene)`。

### 为什么
友盟 U-Link/H5 落地页归因只需要 `inviteCode` 和 `inviteScene` 两个业务字段；继续保留旧 `installData` 会造成请求体字段并存、后端归因口径不清晰。

### 验证结论
静态检查：`rg -n "installData|INSTALL_DATA|inviteData" app\src\main\java app\build.gradle` 无残留；按项目规则未运行 Gradle。

## v本轮 面试报告列表接口独立接入

### 变更了什么
面试报告列表 → 新增独立接口链路：`InterviewReportActivity` 使用独立 `InterviewReportViewModel`、`InterviewReportPagingSource`、`InterviewReportListItem` 和 `INTERVIEW_REPORT_LIST_QUERY`，不复用或改写旧历史报告列表链路。

### 为什么
Apikit 路径 `/api/candidate/app/myReport/report/list/query` 虽然包含 `myReport`，但本次页面语义是面试报告；直接把旧 `GET_MY_REPORT_LIST` / `getMyReportList` / `ReportPagingSource` 改成新接口会污染旧历史报告逻辑。决策是保留旧链路，新增面试报告专属链路。

### 验证结论
静态检查：`rg -n "GET_MY_REPORT_LIST|getMyReportList|INTERVIEW_REPORT_LIST_QUERY|getInterviewReportList|InterviewReportPagingSource"` 确认旧链路仍走旧接口，新页面走新接口；按项目规则未运行 Gradle。

## v本轮 面试通关包报告卡片状态字段收口

### 变更了什么
求职 Agent 消息卡片 → 新增 `INTERVIEW_PASS_PACKAGE_REPORT` 三态卡片时，只按 `payloadJson.reportStatus` 分发生成中 / 失败 / 成功卡片，不读取 `packageStatus`。

### 为什么
联调样例曾包含 `packageStatus`，但用户明确要求“就用 reportStatus”。为避免状态来源分叉，端侧卡片渲染统一以 `reportStatus` 为唯一状态入口，未知或缺失时兜底为生成中。

### 验证结论
静态检查：`rg -n "packageStatus" app\src\main\java\com\shiyuan\fastclaw\feature\clawagent app\src\main\java\com\shiyuan\fastclaw\core\network\apis\response\ClawAgentCardPayload.kt` 无使用；按项目规则未运行 Gradle。

## v本轮 DataNexus/广点通转化 SDK 合规接入

### 变更了什么
第三方归因 SDK → 新增 `DataNexusManager` 收口 SDK 调用；官方 `GDTActionSDK.min.1.9.7.aar` 放入 `app/libs`；DataNexus 数据源 ID 和 secret 通过 `local.properties` 注入 BuildConfig。

### 为什么
DataNexus 官方文档要求在用户同意隐私协议后尽早调用 `GDTAction.init`，并在 `init` 后调用 `GDTAction.start`；项目红线要求第三方 SDK 隔离在 `core/manager/`，且不能把密钥写入仓库。当前工程已有 `App.initSdkAfterAgreeLicense()` 隐私同意后统一入口，因此 SDK 初始化接入该入口，不提前到 `Application.onCreate()`。

### 验证结论
静态检查：`rg -n "import com\\.qq\\.gdt\\.action|GDTAction\\.|DataNexusManager\\.initAfterPrivacyAgreed|DATANEXUS_"` 确认 SDK 直接调用只在 Manager 内、初始化只接入隐私同意后入口；Manifest 所需权限已存在；按项目规则未运行 Gradle。

## v本轮 隐藏 Agent Fragment 错误 Toast 串页

### 变更了什么
`ClawAgentMainFragment.streamError` → 增加当前 Fragment 可见性判断；隐藏 tab 收到 SSE 错误时只消费事件，不在 `MineFragment` 页面弹 Toast。

### 为什么
在求职偏好页选择意向城市、意向岗位、期望行业后返回我的页时，日志显示 `SseCallAdapter` 因 Agent SSE `/jobAgent/stream` 返回 400 生成 `IOException("Unknown error")`，隐藏的 `ClawAgentMainFragment` 仍处于 resumed/observer 可用状态并无条件 `showMessage(e.message)`，导致错误 Toast 串到 `MineFragment`。

### 验证结论
静态检查：确认 `MainActivity` 使用 `switchFragment` hide/show tab；`ClawAgentMainFragment` 仅在 `isAdded && isResumed && !isHidden && view != null` 时显示 `streamError`。`git diff --check` 无空白错误；按项目规则未运行 Gradle。

## v本轮 Agent 错误 Toast 可见性条件收窄

### 变更了什么
`ClawAgentMainFragment.streamError` → 可见性判断从 `isResumed && !isHidden` 收窄为只拦截 `isHidden` 场景：`isAdded && !isHidden && view != null` 才显示 Toast。

### 为什么
`ClawAgentMainFragment` 自己也会打开意向城市、意向岗位、期望行业等选择 Activity。选择 Activity 覆盖时宿主 Fragment 会暂停但并不是主 tab 隐藏；如果用 `isResumed` 判断，会误吞 Agent 自己流程里的 `streamError` Toast。真正需要拦截的是主 tab 切到 `MineFragment` 后 `ClawAgentMainFragment.isHidden == true` 的串页错误。

### 验证结论
静态检查：`rg -n "JobCitySelectorActivity\\.start|JobSelectionActivity\\.start|IndustrySelectionActivity\\.start|streamError\\.observe|canShowStreamErrorToast"` 确认 Agent 页自身存在外部选择页入口，Toast 门禁已改为仅依赖 `!isHidden`；按项目规则未运行 Gradle。

## v本轮 H5 支付 payInfo 数字保真

### 变更了什么
`H5PayInfo.payInfo` → 从 `Any?` 改为 `JsonElement?`，`BrowserViewModel.startPay()` 下单前通过 `payInfoBody()` 取请求体，保留 H5 传入的 JSON 数字原文。

### 为什么
H5 `webPay` 入参里的 `payInfo` 如果被 Gson 按 `Any/Map` 解析，未加引号的 `goodsCode`、`resumeCheckId`、`jdCheckId` 会先变成 `Double`，Retrofit 再序列化时可能输出 `9.0001001E7` 一类科学计数法，导致下单参数变形。改用 Gson JSON 树后，数字节点按原始十进制文本写出；历史上若 `payInfo` 传的是 JSON 字符串，也在发送前尝试解析成 JSON。

### 验证结论
静态检查：`git diff --check` 未发现空白错误；`rg -n "payInfoBody|val payInfo|h5PayAlipay\\(|h5PayWx\\("` 确认请求链路改为使用 `payInfoBody()`。JShell 使用本地 Gson 2.10.1 验证：`Any/Map` 会输出 `9.0001001E7`，`JsonElement/JsonParser` 会输出 `90001001`；按项目规则未运行 Gradle。

## v本轮 ViewModel 扩展函数导包检查

### 变更了什么
接口调用代码使用 `requestWithLoading` 时，必须显式导入 `com.shiyuan.fastclaw.core.ext.requestWithLoading`。

### 为什么
`requestWithLoading` 定义在 `core/ext/ViewModelExt.kt`，是 `BaseViewModel` 的扩展函数。即使 `AppRepositoryViewModel.kt` 自身导入并使用了该扩展，子类文件也不会继承 import；漏导会导致目标 ViewModel 无法解析该符号。

### 验证结论
以后新增或修改 `request`、`requestWithLoading` 等扩展调用时，先对照同类 ViewModel 的 import，并用 `rg -n "^import .*requestWithLoading|requestWithLoading\\("` 静态确认定义与调用文件都已接入。
