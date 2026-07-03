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
