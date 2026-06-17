# 项目示例

> 使用前必须确认示例路径在当前分支真实存在；不存在时，用本文件的搜索命令查找当前分支等价实现，不要机械照搬复制来源项目的路径。

这些文件体现当前项目 API 接入方式。只复制结构，不要照搬业务逻辑。

## 核心网络文件

- `app/src/main/java/<主业务包路径>/core/network/apis/ApiConstants.kt`
- `app/src/main/java/<主业务包路径>/core/network/apis/ApiService.kt`
- `app/src/main/java/<主业务包路径>/core/network/apis/ApiResponse.java`
- `app/src/main/java/<主业务包路径>/core/network/apis/ApiException.java`
- `app/src/main/java/<主业务包路径>/core/network/apis/ApiFactory.kt`
- `app/src/main/java/<主业务包路径>/core/Response.kt`
- `app/src/main/java/<主业务包路径>/core/ext/ViewModelExt.kt`
- `app/src/main/java/<主业务包路径>/core/AppRepository.kt`

注意：

- 不要修改 `ApiResponse.java` 和 `ApiException.java`。
- `ApiFactory.apiService` 已统一创建 Retrofit Service；业务 ViewModel 默认通过 `mRepo.api` 使用它。

## 请求体和接口方法

参考：

- `app/src/main/java/<主业务包路径>/core/network/apis/request/UserTokenBody.java`
- `app/src/main/java/<主业务包路径>/core/network/apis/request/BindPhoneBody.kt`
- `app/src/main/java/<主业务包路径>/core/network/apis/request/FeedbackBody.kt`
- `app/src/main/java/<主业务包路径>/core/network/apis/request/ReportSubmissionBody.kt`
- `app/src/main/java/<主业务包路径>/core/network/apis/request/PagedBody.java`

接口方法参考：

- `ApiService.bindPhone(...)`
- `ApiService.feedback(...)`
- `ApiService.logOff(...)`
- `ApiService.cancelLogOff(...)`
- `ApiService.recommendWorkList(...)`
- `ApiService.queryWalletInfo(...)`

## feature Repository 示例

参考：

- `app/src/main/java/<主业务包路径>/feature/login/data/BindPhoneRepository.kt`
- `app/src/main/java/<主业务包路径>/feature/login/data/PhoneLoginRepository.kt`
- `app/src/main/java/<主业务包路径>/feature/login/data/ThirdLoginRepository.kt`
- `app/src/main/java/<主业务包路径>/feature/web/data/BrowserRepository.kt`

可复制点：

- Repository 继承 `AppRepository`。
- 需要 IO 切换时使用 `withContext(Dispatchers.IO)`。
- 登录、绑定手机号等接口组合和第三方 SDK 上报可以收敛在 feature Repository。
- 普通 API 失败仍交给 ViewModel 的 `response/request/requestWithLoading` 处理，不在 Repository 随意吞异常。

## ViewModel 中 launchWithLoading + response

参考：

- `app/src/main/java/<主业务包路径>/feature/login/bind/BindPhoneViewModel.kt`
- `app/src/main/java/<主业务包路径>/feature/login/phone/PhoneLoginViewModel.kt`
- `app/src/main/java/<主业务包路径>/feature/settings/logoff/LogoffViewModel.kt`
- `app/src/main/java/<主业务包路径>/feature/settings/report/ReportDialogViewModel.kt`

可复制点：

- 用户点击提交、绑定、注销、举报等需要 loading 的操作，使用 `launchWithLoading { response(...) { ... } }`。
- 接口成功后更新页面状态、发事件或关闭页面。
- 发送验证码这类需要失败回滚按钮状态的流程，使用 `launch(block = ..., onError = ...)` 并在错误分支恢复状态。

## ViewModel 中普通 launch + response

参考：

- `app/src/main/java/<主业务包路径>/feature/home/HomeViewModel.kt`
- `app/src/main/java/<主业务包路径>/feature/main/MainViewModel.kt`
- `app/src/main/java/<主业务包路径>/feature/mine/MineViewModel.kt`
- `app/src/main/java/<主业务包路径>/feature/settings/SettingsViewModel.kt`

适用：

- 页面初始化、静默刷新或后台记录。
- API 成功后更新 `MutableLiveData`、`ObservableField` 或缓存。
- 周边代码已使用 `launch { response(...) }`。

## 自定义组合请求示例

参考：

- `app/src/main/java/<主业务包路径>/feature/settings/feedback/FeedbackViewModel.kt`

可复制点：

- 上传图片、敏感图片校验、提交反馈是组合流程。
- 需要明确处理 loading 关闭、校验失败返回和用户可理解 toast。
- 组合逻辑较复杂时保留必要中文注释，说明业务原因和边界。

## 分页接口示例

参考：

- `ApiService.recommendWorkList(...)`
- `app/src/main/java/<主业务包路径>/feature/play/data/VideoFeedPagingSource.kt`
- `app/src/main/java/<主业务包路径>/feature/play/data/FeedRepository.kt`
- `app/src/main/java/<主业务包路径>/feature/home/data/VideoRemoteMediator.kt`

规则：

- 返回 `ApiResponse<PagedList<T>?>`。
- 页面层实现必须走 `android-paging-state-ui`。
- 新 `PagingSource` 优先把 `mRepo.api` 或 loader lambda 传入；编辑历史类时可先沿用附近风格，但要在输出中说明取舍。

## SSE 基础能力

当前项目存在 SSE 基础类：

- `app/src/main/java/<主业务包路径>/core/network/SseCallAdapter.kt`
- `app/src/main/java/<主业务包路径>/core/network/sse/SSEStreamHandler.kt`
- `app/src/main/java/<主业务包路径>/core/network/sse/processor/PlainTextSseProcessor.kt`
- `app/src/main/java/<主业务包路径>/core/network/sse/processor/StorySseProcessor.kt`
- `app/src/main/java/<主业务包路径>/core/network/sse/processor/EpisodeSseProcessor.kt`

当前 `ApiService.kt` 未发现已声明的 SSE Retrofit 方法。新增 SSE 接口时才按 `android-async-task` 读取完整处理规则，不要把 SSE 当普通一次性接口写。

## 常用搜索命令

```powershell
rg -n "接口路径|常量名|方法名|Body名|Data名|Info名" app/src/main/java/<主业务包路径>/core/network/apis app/src/main/java/<主业务包路径>/feature -g "!**/OssManager.kt"
rg -n "requestWithLoading\\(|request\\(|response\\(|mRepo\\.api\\.|launchWithLoading" app/src/main/java/<主业务包路径>/feature -g "!**/OssManager.kt"
rg -n "@POST\\(ApiConstants\\.|ApiResponse<|PagedList<|Flow<SSEvent>" app/src/main/java/<主业务包路径>/core/network/apis app/src/main/java/<主业务包路径>/core/network
```
