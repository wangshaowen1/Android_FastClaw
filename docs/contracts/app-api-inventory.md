---
title: App 网络接口清单
type: contract
status: active
last_verified: 2026-09-04
---

# App 网络接口清单

## 谁会使用、以什么为准

本文面向维护 FastClaw Android `app` 模块的开发者，回答“当前 App 实际调用了哪些网络接口”。接口声明以 [`ApiService`](../../app/src/main/java/com/shiyuan/fastclaw/core/network/apis/ApiService.kt) 和 [`ApiConstants`](../../app/src/main/java/com/shiyuan/fastclaw/core/network/apis/ApiConstants.kt) 为准，是否计入清单以 `app/src/main/java` 下的非注释调用点为准。请求字段和响应字段仍以 `request/`、`response/`、`entity/` 模型及服务端 Schema 为准，本文不复制完整 Schema。

本次静态核对得到：`ApiService` 声明 121 个方法名，其中 108 个存在调用点；去除同一路径重载后得到 107 条实际使用的后端路径，全部为 `POST`。其中求职 Agent 和模拟面试各有 1 条 `text/event-stream` SSE 接口。`EventService.eventTracking` 当前只有声明和工厂配置，没有非注释调用点，未计入实际使用清单。

## 共同规则

- 普通接口通过 [`ApiFactory`](../../app/src/main/java/com/shiyuan/fastclaw/core/network/apis/ApiFactory.kt) 创建，基础地址来自 `Config.baseUrlCustom`，为空时回退到 `BuildConfig.SERVER_URL`。不要在业务页面硬编码服务端地址。
- 当前接口均为 `POST`；`RetrofitClient` 为非 `GET` 请求统一补充 `Content-Type`、`Accept`、`userToken` 和 `appHeader` 请求头。
- 普通响应使用 [`ApiResponse<T>`](../../app/src/main/java/com/shiyuan/fastclaw/core/network/apis/ApiResponse.java)，业务成功码为 `200`；HTTP `401` 会发布 Token 过期事件。SSE 使用 `Flow<SSEvent>`，由 [`SseCallAdapter`](../../app/src/main/java/com/shiyuan/fastclaw/core/network/SseCallAdapter.kt) 转换。
- 分页接口通常由 Paging3 `PagingSource` 或 ViewModel 驱动；文件安全上传是“创建凭证 → OSS 上传 → 确认完成 → 轮询安全状态”的串行链路。
- 路径按源码原样记录（有些常量带 `/`，有些不带）；修改路径时应同步修改 `ApiConstants`、调用方和服务端契约。

## 接口清单

### 应用配置与公共能力（7）

| 方法 | 路径 | 用途 |
| --- | --- | --- |
| `appConfig` | `app/getAppConfigInfo` | 获取 App 配置 |
| `getAppShelfStatus` | `app/getVersionShelfStatus` | 查询当前版本上架状态 |
| `checkAppVersion` | `api/candidate/app/version/check` | 检测 App 版本更新 |
| `commonInit` | `/api/app/common/init` | App 公共初始化 |
| `daily` | `user/login/daily` | 上报用户日活 |
| `playEventReport` | `app/home/work/playCount` | 上报作品播放量 |
| `getScheme` | `/api/candidate/miniProgram/scheme` | 获取小程序/业务 Scheme |

### 登录、账号与用户资料（17）

| 方法 | 路径 | 用途 |
| --- | --- | --- |
| `loginByThird` | `api/candidate/auth/loginByThirdParty` | 第三方登录 |
| `bindThirdParty` | `/api/candidate/auth/bindThirdParty` | 绑定第三方账号 |
| `rebindThirdParty` | `/api/candidate/auth/rebindThirdParty` | 重新绑定第三方账号 |
| `unbindThirdParty` | `/api/candidate/auth/unbindThirdParty` | 解除第三方账号绑定 |
| `getUserHomeInfo` | `api/candidate/auth/user/info-detail` | 查询用户主页信息 |
| `updateUserAvatar` | `api/candidate/user/avatar/update` | 更新用户头像 |
| `logOff` | `/api/candidate/auth/applyCancelAccount` | 提交账号注销 |
| `sendSmsValidateCode` | `/api/candidate/auth/sendSmsValidateCode` | 发送短信验证码 |
| `checkMobileBindStatus` | `/api/candidate/auth/checkMobileBindStatus` | 手机号绑定前置校验 |
| `bindPhone` | `user/phone/action/bind` | 绑定手机号 |
| `bindMobile` | `/api/candidate/auth/bindMobile` | 三方登录后绑定手机号 |
| `changeMobile` | `/api/candidate/auth/changeMobile` | 换绑手机号 |
| `loginByFlashMobile` | `/api/candidate/auth/loginByFlashMobile` | 闪验登录 |
| `loginByPhone` | `/api/candidate/auth/loginByMobile` | 短信验证码登录 |
| `refreshToken` | `/api/candidate/auth/refreshToken` | 刷新登录 Token |
| `cancelLogOff` | `user/remove/cancel` | 取消注销申请 |
| `signOut` | `/candidate/auth/sign-out` | 退出登录并通知服务端 |

### 支付、积分与邀请（13 个路径；H5 支付由两个方法复用同一路径）

| 方法 | 路径 | 用途 |
| --- | --- | --- |
| `getOrderStatus` | `/api/candidate/app/trade/pay/order/status/query` | 查询支付订单状态 |
| `queryPayOrderList` | `/api/candidate/app/trade/pay/order/list/query` | 分页查询支付订单 |
| `queryWalletInfo` | `trade/user/wallet/current` | 查询用户钱包 |
| `h5PayWx` / `h5PayAlipay` | `/api/candidate/app/interviewPassPackage/pay/create` | H5 通关包支付下单（微信/支付宝） |
| `createAppWechatPayOrder` | `/api/candidate/app/trade/pay/wechat/create` | 创建 App 微信支付订单 |
| `createAppAlipayOrder` | `/api/candidate/app/trade/pay/alipay/create` | 创建 App 支付宝订单 |
| `queryTradePayWayList` | `/api/candidate/app/trade/pay/way/list` | 查询充值页支付方式 |
| `getUserPoints` | `api/candidate/points/info` | 查询虾条积分汇总 |
| `queryUserPointsPage` | `api/candidate/points/transaction/page` | 分页查询积分明细 |
| `queryInviteRecordPage` | `api/candidate/h5/invite/record/page` | 分页查询邀请记录 |
| `queryInviteStats` | `/api/candidate/app/invite/stats/query` | 查询邀请统计 |
| `queryInviteShareInfo` | `/api/candidate/app/invite/share/info/query` | 查询邀请分享信息 |
| `queryPointConfigList` | `/api/candidate/app/trade/point/config/list` | 查询虾条充值配置 |

### 文件上传、简历质量与报告（14）

| 方法 | 路径 | 用途 |
| --- | --- | --- |
| `createFileUploadCredential` | `/api/file/upload/credential/create` | 创建文件上传凭证 |
| `completeFileUpload` | `/api/file/upload/complete` | 确认文件已上传完成 |
| `queryFileSecurityStatus` | `/api/file/security/status/query` | 轮询文件安全审核状态 |
| `getMyReportList` | `/api/candidate/app/interviewReport/report/list` | 分页查询我的报告 |
| `getInterviewReportList` | `/api/candidate/app/myReport/report/list/query` | 分页查询面试报告 |
| `deleteInterviewReport` | `/api/candidate/app/myReport/report/delete` | 从报告列表隐藏报告 |
| `queryLatestResume` | `/api/candidate/app/interviewReport/resume/latest/query` | 查询最新简历 |
| `resumeMaterialQuality` | `/api/candidate/app/interviewReport/resumeMaterialQuality/check` | 发起简历材料质量检查 |
| `resumeMaterialQualityResult` | `/api/candidate/app/interviewReport/resumeMaterialQuality/result/query` | 查询简历材料质量结果 |
| `jdMaterialQuality` | `/api/candidate/app/interviewReport/jdMaterialQuality/check` | 发起 JD 材料质量检查 |
| `jdMaterialQualityResult` | `/api/candidate/app/interviewReport/jdMaterialQuality/result/query` | 查询 JD 材料质量结果 |
| `queryInterviewPassPackageResumeStatus` | `/api/candidate/app/interviewPassPackage/resume/status` | 查询通关包简历状态 |
| `interviewReport` | `/api/candidate/app/interviewReport/report/submit` | 提交面试报告生成任务 |
| `interviewReportResult` | `/api/candidate/app/interviewReport/report/status/query` | 查询面试报告生成状态 |

### 小虾对话与求职 Agent（18）

| 方法 | 路径 | 用途 |
| --- | --- | --- |
| `enterSession` | `/api/candidate/app/jobAgent/session/enter` | 进入或恢复对话会话 |
| `sendMsgToAgent`（SSE） | `/api/candidate/app/jobAgent/stream` | 流式发送用户消息并接收 Agent 事件 |
| `cancelStreamGenerate` | `/api/candidate/app/jobAgent/cancel` | 取消 Agent 流式生成 |
| `queryConversation` | `/api/candidate/app/jobAgent/conversation/query` | 查询会话状态 |
| `queryJobAgentModuleList` | `/api/candidate/app/jobAgent/module/list/query` | 查询求职 Agent 功能模块 |
| `queryActivityStatus` | `/api/candidate/app/config/activity/status/query` | 查询活动状态 |
| `submitAction` | `/api/candidate/app/jobAgent/action/submit` | 提交卡片或按钮动作 |
| `updateJobStatus` | `/api/candidate/app/jobAgent/jobStatus/update` | 更新顶部求职状态 |
| `queryMessagePage` | `/api/candidate/app/jobAgent/message/page` | 分页查询历史消息 |
| `pullMessages` | `/api/candidate/app/jobAgent/message/pull` | 拉取增量消息 |
| `queryCompanyBackgroundCompanySearch` | `/api/candidate/app/jobAgent/companyBackground/company/search/query` | 搜索公司背调目标公司 |
| `submitCompanyBackgroundPreCheck` | `/api/candidate/app/jobAgent/companyBackground/report/preCheck/submit` | 提交公司背调生成前置校验 |
| `queryCompanyBackgroundGenerateResult` | `/api/candidate/app/jobAgent/companyBackground/report/generate/result/query` | 查询公司背调生成结果 |
| `regenerateCompanyBackgroundReport` | `/api/candidate/app/jobAgent/companyBackground/report/regenerate/submit` | 重新生成公司背调失败报告 |
| `queryCurrentResume` | `/api/candidate/app/jobAgent/resume/current/query` | 查询待确认简历 |
| `confirmCurrentResume` | `/api/candidate/app/jobAgent/resume/current/confirm` | 确认或否认当前简历 |
| `submitResumeUpload` | `/api/candidate/app/jobAgent/resume/upload/submit` | 提交 Agent 简历上传结果 |
| `checkJobAgentAttributeAnswers` | `/api/candidate/app/jobAgent/attribute/answer/check` | 检查重要属性是否已回答 |

### 模拟面试与通关包（9）

| 方法 | 路径 | 用途 |
| --- | --- | --- |
| `startMockInterviewSession` | `/api/candidate/app/interviewReport/mockInterview/session/start` | 创建模拟面试会话 |
| `queryMockInterviewUnfinished` | `/api/candidate/app/interviewReport/mockInterview/unfinished/query` | 查询未完成的模拟面试 |
| `mockInterviewStream`（SSE） | `/api/candidate/app/interviewReport/mockInterview/stream` | 接收模拟面试流式事件 |
| `forceCancelMockInterview` | `/api/candidate/app/interviewReport/mockInterview/forceCancel` | 强制取消模拟面试 |
| `queryMockInterviewReportProgress` | `/api/candidate/app/interviewReport/mockInterview/report/progress/query` | 查询模拟面试报告进度 |
| `retryMockInterviewReport` | `/api/candidate/app/interviewReport/mockInterview/report/retry` | 重试模拟面试报告生成 |
| `queryPassPackageProgress` | `/api/candidate/app/interviewPassPackage/status/query` | 查询通关包报告进度 |
| `regeneratePassPackageReport` | `/api/candidate/app/interviewPassPackage/regenerate` | 重新生成通关包报告 |
| `queryCodeResumeBinding` | `/api/candidate/miniProgram/codeResumeBinding/query` | 查询小程序码绑定的简历 |

### 在线简历、求职偏好与字典（17）

| 方法 | 路径 | 用途 |
| --- | --- | --- |
| `queryDictIndustryQuery` | `/api/candidate/app/jobAgent/dict/industry/query` | 查询行业字典 |
| `queryDictPositionQuery` | `/api/candidate/app/jobAgent/dict/position/query` | 查询岗位字典 |
| `queryDictWelfareQuery` | `/api/candidate/app/jobAgent/dict/welfare/query` | 查询福利偏好字典 |
| `understandingScore` | `/api/candidate/app/jobAgent/understanding/score/query` | 查询“小虾对你的理解”分数 |
| `understandingDetail` | `/api/candidate/app/jobAgent/understanding/detail/query` | 查询理解度详情 |
| `resumeQuery` | `/api/candidate/app/jobAgent/resume/list/query` | 分页查询简历列表 |
| `deleteResume` | `/api/candidate/app/jobAgent/resume/list/delete` | 删除简历列表项 |
| `submitResumeMyUpload` | `/api/candidate/app/jobAgent/resume/my/upload/submit` | 提交“我的简历”上传结果 |
| `queryOnlineResumeInfo` | `/api/candidate/app/jobAgent/onlineResume/info/query` | 查询在线简历信息 |
| `updateOnlineResumeAttribute` | `/api/candidate/app/jobAgent/onlineResume/attribute/update` | 更新在线简历属性 |
| `queryOnlineResumeAttribute` | `/api/candidate/app/jobAgent/onlineResume/attribute/query` | 查询在线简历单项属性 |
| `queryJobPreferencesInfo` | `/api/candidate/app/jobAgent/onlineResume/preference/query` | 查询求职偏好 |
| `queryAttributeArray` | `/api/candidate/app/jobAgent/attribute/array/query` | 查询属性数组候选值 |
| `queryCityList` | `/api/candidate/app/jobAgent/dict/region/query` | 查询城市选择器数据 |
| `attributeSave` | `/api/candidate/app/jobAgent/attribute/array/save` | 保存 Agent 属性数组 |
| `onlineResumeAttributeSave` | `/api/candidate/app/jobAgent/onlineResume/attribute/array/item/save` | 保存在线简历数组项 |
| `onlineResumeAttributeDelete` | `/api/candidate/app/jobAgent/onlineResume/attribute/array/item/delete` | 删除在线简历数组项 |

### 机会匹配（7）

| 方法 | 路径 | 用途 |
| --- | --- | --- |
| `queryOpportunityNavCount` | `/api/candidate/app/opportunity/nav/count/query` | 查询机会导航未处理数量 |
| `queryOpportunityStats` | `/api/candidate/app/opportunity/stats/query` | 查询机会匹配统计 |
| `queryNewOpportunities` | `/api/candidate/app/opportunity/new/query` | 查询最新未处理机会 |
| `queryProcessedOpportunityPage` | `/api/candidate/app/opportunity/processed/page` | 分页查询已处理机会（按状态重载） |
| `queryOpportunityDetail` | `/api/candidate/app/opportunity/detail/query` | 查询机会详情 |
| `queryOpportunityCompanyDetail` | `/api/candidate/app/opportunity/company/detail/query` | 查询关联公司公开信息 |
| `updateOpportunityStatus` | `/api/candidate/app/opportunity/status/update` | 更新机会处理状态 |

### IM 业务接口（5）

| 方法 | 路径 | 用途 |
| --- | --- | --- |
| `createEnterpriseImSession` | `/api/app/im/session/create` | 创建企业侧 IM 会话 |
| `queryImAccount` | `/api/app/im/account/query` | 查询 App 端云信账号 |
| `agreeResumeSend` | `/api/app/im/resume/send/agree` | 同意发送简历 |
| `switchImJob` | `/api/app/im/job/switch` | 切换会话当前沟通职位 |
| `notifyFirstMessageSent` | `/api/app/im/session/firstMessage/notify` | 通知首条聊天消息已发送 |

## 非 ApiService 的网络入口

- 面试和小虾语音功能通过 `DashFunAsrSpeechTranscriber`、`DashCosyVoiceStreamTtsPlayer` 使用 DashScope NUI SDK 的 WebSocket 地址（`BuildConfig.DASHSCOPE_SPEECH_URL`，默认地址为 `wss://dashscope.aliyuncs.com/api-ws/v1/inference`）。这不是后端 `ApiService` 接口；鉴权值来自运行时配置，文档不记录具体密钥。
- 安全文件上传的对象存储上传由 `OssClient` 执行，上传地址和凭证由上表的文件接口返回，不能从源码归纳成固定业务路径。
- WebView 页面、图片加载、媒体下载和第三方 SDK（IM、推送、统计、微信/支付宝）使用运行时 URL 或 SDK 内部请求，本文只记录 App 自己声明且有调用点的业务接口。

## 当前未计入的声明

以下 `ApiService` 方法在本次静态扫描中未找到非注释调用点，不能据此判断服务端已下线：`tipOff`、`logoutApp`、`feedback`、`getAppBanner`、`recommendWorkList`、`reportSubmission`、`sensitiveImageCheck`、`uploadResume`、`queryCompanyBackgroundPreCheckResult`、`confirmAttribute`、`rejectAttribute`、`queryRegionDict`、`refreshImToken`。另有 `EventService.eventTracking` 仅声明未调用。后续新增调用时，应把接口移入上面的业务域并更新 `last_verified`。

## 怎么联调和验收

- 修改接口声明或路径后，先检查 `ApiConstants → ApiService → request/response model → ViewModel/Repository` 链路；页面不应直接请求网络。
- 对分页接口核对页码、页大小、服务端总页数和空数据行为；对 SSE 核对连接取消、业务结束事件和页面生命周期；对文件接口核对凭证、OSS 上传、完成确认和安全状态轮询四步顺序。
- 本文只完成源码静态核对，未运行 Gradle、设备测试、服务端联调或抓包验证；因此不等同于所有接口在当前环境可用。
