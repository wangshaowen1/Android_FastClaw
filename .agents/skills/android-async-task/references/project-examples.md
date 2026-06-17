# 项目示例索引

> 使用前必须确认示例路径在当前分支真实存在；不存在时，用本文件的搜索命令查找当前分支等价实现，不要机械照搬复制来源项目的路径。

本文件记录异步长任务 skill 的当前项目依据。生成轮询、SSE、下载、进度模拟时优先对齐这些写法。

## 核心扩展

- `app/src/main/java/<主业务包路径>/core/ext/ViewModelExt.kt`
  - `request(...)`：无 loading 的接口请求。
  - `requestWithLoading(...)`：接口请求 + loading + toast/error 回调。
  - `launchWithLoading(...)`：自定义协程 + loading。
  - `PollingState`：`SUCCESS`、`FAIL`、`CONTINUE`。
  - `launchPolling(...)`：后端任务状态轮询；内部使用 `viewModelScope`、IO 切换、`isActive`，并放行 `CancellationException`。

## 支付结果短轮询

- `app/src/main/java/<主业务包路径>/feature/payment/pay/PaymentViewModel.kt`
  - `pollTask(...)` 使用 `repeat(maxRetry)` + `delay(delayMs)` 做有限次数短轮询。
  - `checkBaoFuResult(...)` 通过 `AtomicBoolean.compareAndSet(...)` 防重复消费状态。
  - 轮询接口为 `mRepo.api.getOrderStatus(OrderStatusBody(orderNo = currentOrderId))`。
  - 成功/失败通过回调交给调用方，不能只相信客户端支付回调。

新增后端长任务时优先考虑 `launchPolling(...)`；支付这种短小、局部、固定次数检查可以参考 `pollTask(...)`。

## SSE 基础能力

- `app/src/main/java/<主业务包路径>/core/network/sse/SSEStreamHandler.kt`
  - 内部保存 `private var job: Job? = null`。
  - `start(flow)` 开始前 `cancel()`。
  - `AtomicBoolean` 防止重复完成/错误回调。
  - `CancellationException` 作为正常取消处理。
  - `cancel()` 中设置结束状态、`_isStreaming=false`、取消 job 并置空。
- `app/src/main/java/<主业务包路径>/core/network/SseCallAdapter.kt`
  - Retrofit SSE 返回 `Flow<SSEvent>`。
  - `awaitClose` 中取消底层 `EventSource`。
- `app/src/main/java/<主业务包路径>/core/network/sse/processor/PlainTextSseProcessor.kt`
- `app/src/main/java/<主业务包路径>/core/network/sse/processor/StorySseProcessor.kt`
- `app/src/main/java/<主业务包路径>/core/network/sse/processor/EpisodeSseProcessor.kt`

当前 `ApiService.kt` 未发现已声明的 SSE 接口。新增 SSE 功能时先确认后端接口、返回格式和 processor 是否复用。

## 下载示例

- `app/src/main/java/<主业务包路径>/service/DownloadService.java`
  - 使用系统 `DownloadManager` 下载。
  - 通过 `Events.DownloadProgressEvent` 发布进度。
  - 完成后按下载类型发布 `DownloadApkEvent`、`InstallApkEvent` 或 `DownloadContractEvent`。
  - `onDestroy()` 取消 receiver 和 handler callback。
- `app/src/main/java/<主业务包路径>/core/manager/UpgradeManager.kt`
  - APK 更新下载入口。
  - 下载前检查服务是否运行、MD5、通知和权限。
- `app/src/main/java/<主业务包路径>/common/ext/Context.kt`
- `app/src/main/java/<主业务包路径>/common/utils/IntentUtils.java`
  - 普通下载入口，向 `DownloadService` 传入 URL、文件名和类型。
- `app/src/main/java/<主业务包路径>/core/network/HttpDownload.kt`
  - 网络层下载拦截辅助。

## 本地进度模拟

- `app/src/main/java/<主业务包路径>/common/utils/ProgressSimulator.kt`
- `app/src/main/java/<主业务包路径>/common/utils/AutoProgressSimulator.kt`
- `app/src/main/java/<主业务包路径>/kit/media/source/MediaSourceManager.kt`

可参考写法：

- 保存 `Job?` 或内部任务状态。
- 启动前取消旧任务或检查是否运行中。
- delay 循环必须有退出条件。
- 停止时复位状态并取消 job。

## 倒计时示例

- `app/src/main/java/<主业务包路径>/feature/settings/logoff/LogoffViewModel.kt`
- `app/src/main/java/<主业务包路径>/feature/login/bind/BindPhoneViewModel.kt`
- `app/src/main/java/<主业务包路径>/widget/CountDownTextView.java`

可参考写法：

- `CountDownTimer` 或协程倒计时必须有取消入口。
- ViewModel 中的倒计时在 `onCleared()` 或状态结束时取消。
- 短信验证码倒计时要用 id 或状态防止旧倒计时继续更新 UI。

## 搜索命令

```powershell
rg -n "launchPolling|PollingState|Job\\?|onCleared|SSEStreamHandler|DownloadService|DownloadProgressEvent|pollTask|CountDownTimer" app/src/main/java/<主业务包路径> -g "*.kt" -g "*.java" -g "!**/OssManager.kt"
rg -n "SSEStreamHandler|PlainTextSseProcessor|StorySseProcessor|EpisodeSseProcessor|SseCallAdapter" app/src/main/java/<主业务包路径> -g "*.kt"
rg -n "DownloadService|HttpDownload|DownloadApkEvent|InstallApkEvent|DownloadContractEvent" app/src/main/java/<主业务包路径> -g "*.kt" -g "*.java"
```
