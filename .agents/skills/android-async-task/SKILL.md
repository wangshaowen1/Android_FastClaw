---
name: android-async-task
description: "Android 异步长任务规范。仅在本次实际涉及轮询、polling、launchPolling、SSE、流式接口、支付状态检查、任务状态检查、下载任务、Job 保存、onCleared 取消、重复启动保护、取消逻辑、Coroutine 长任务、DownloadService、HttpDownload 或 SSEStreamHandler 时使用。普通 loading/error 文案或一次性接口请求不要触发本 skill。"
---

# Android 异步任务

## 快速决策

按任务类型选择项目已有能力：

- 一次性接口请求：使用 `request(...)` 或 `requestWithLoading(...)`，不需要读取本 skill。
- 用户点击触发且需要 loading：使用 `requestWithLoading(...)` 或 `launchWithLoading(...)`。
- 后端任务状态轮询：优先使用 `launchPolling(...)`，保存返回的 `Job`；固定次数的轻量短轮询先搜索当前 feature 或相邻业务的同类实现，不能凭空复制其他项目的方法名。
- SSE 流式文本生成：使用 `SSEStreamHandler` + 对应 `SseChunkProcessor`，不要手写 OkHttp SSE。
- APK 或文件下载：优先复用 `DownloadService`、`HttpDownload`、`DownloadProgressEvent` 和当前调用入口。
- 本地进度模拟、支付结果短轮询、媒体数据源等任务：可以保存普通 `Job`，用 `while (isActive)` 或延迟递归，但必须有取消入口。

只有找不到同类实现、不确定项目异步封装用法、需要新增完整长任务，或发现轮询/SSE/下载生命周期问题时，才读取 [project-examples.md](references/project-examples.md) 对齐项目示例。

## Job 管理

长任务必须有明确生命周期。

- ViewModel 内保存为 `private var xxxJob: Job? = null`。
- 启动前先判断重复：持续任务通常 `if (xxxJob?.isActive == true) return`；互斥任务通常先 `xxxJob?.cancel()` 再重新启动。
- 页面 ViewModel 自己创建并拥有的 `Job`、SSE handler 和临时任务必须在 `onCleared()` 取消。由 `Service`、系统 `DownloadManager`、应用级 Manager 或持久队列拥有的任务遵循其自身生命周期，页面销毁只解绑观察，不得误取消后台任务。
- View 自己拥有的播放器、动画或 item 内部任务可以留在 UI 层，但必须在当前基类、View 缓存和附近同类实现所对应的真实 View 生命周期释放。
- 取消后把字段置空，并复位 `loading`、`downloading`、`generating`、按钮可用状态等 UI 状态。
- `CancellationException` 必须放行，不要当成普通错误吞掉。
- 一次性简单接口请求沿用附近 `request(...)`、`requestWithLoading(...)` 或 `launch { response(...) }`，不需要为了统一格式升级成本 skill。
- 需要状态回滚、自定义异常提示或 `savingIds`、loading/progress 等必然清理时，使用 `launch(block = { response(...) }, onError = { ... }, onComplete = { ... })`。
- 不要在 `launch` 内手写 `try/catch/finally` 包网络请求，除非有明确特殊原因；接口业务失败放 `response.error`，网络/协程异常放 `launch.onError`，临时状态清理放 `launch.onComplete`。
- 不在 Activity/Fragment 里持有长任务逻辑；UI 只展示 loading、progress、toast、dialog、状态页。

## 轮询任务

后端返回任务状态时，优先使用 `launchPolling(...)`。

```kotlin
private var scriptPollingJob: Job? = null

fun pollScript() {
    if (scriptPollingJob?.isActive == true) return
    isScriptLoading.set(true)

    scriptPollingJob = launchPolling(
        intervalMs = 3000L,
        retryOnNetworkError = true,
        apiBlock = { mRepo.api.pollScriptResult(PollScriptBody(workId.value, storyId)) },
        checkState = { response ->
            when {
                response?.data?.isSuccess() == true -> PollingState.SUCCESS
                response?.data?.isFail() == true -> PollingState.FAIL
                else -> PollingState.CONTINUE
            }
        },
        onSuccess = { response -> handlePollingSuccess(response?.data) },
        onFail = { response, _ -> handlePollingFail(response?.data) },
        onPolling = { updatePollingUi(it?.data) }
    )
}

override fun onCleared() {
    scriptPollingJob?.cancel()
    super.onCleared()
}
```

轮询规则：

- `PollingState.SUCCESS`：停止轮询，写入结果，关闭 loading，开放下一步。
- `PollingState.FAIL`：停止轮询，关闭 loading，展示失败状态或重试入口。
- `PollingState.CONTINUE`：更新进度/文案后等待下一次。
- `retryOnNetworkError = true` 只用于可以继续等待的后台任务；会导致网络异常后继续轮询，不要误用于必须立即失败的提交。
- 任务创建接口和轮询接口分开：创建任务用 `requestWithLoading(...)`，创建成功后重置状态并启动轮询。

## SSE 流式生成

项目已有 `SSEStreamHandler`，新流式生成优先复用它。

- ViewModel 保存 `private var sseHandler: SSEStreamHandler? = null`。
- 开启新流前先 `sseHandler?.cancel()`，防止旧流继续回调。
- 使用 `viewModelScope` 作为 `SSEStreamHandler` 的 scope。
- 文本拼接、节流、字段解析放到 `PlainTextSseProcessor`、`StorySseProcessor`、`EpisodeSseProcessor` 或新增 processor。
- `onStart` 清空旧内容并设置生成中状态。
- `onUpdate` 只更新 XML 或观察者实际消费的 `LiveData` / `ObservableField`；仅供内部拼接、判断或缓存的中间状态使用普通字段。
- `onComplete` 关闭生成中状态并开放后续按钮。
- `onErrorCallback` 关闭生成中状态，使用 `parseMessage(e)?.showToast()` 或页面错误态。
- `onCleared()` 中调用 `sseHandler?.cancel()`。

不要在页面里直接 new `EventSource` 或手写 callbackFlow；底层已经由 `SseCallAdapter` 和 `SSEStreamHandler` 收口。

## 下载任务

APK 或文件下载优先按当前项目下载模式实现。

- APK 更新参考 `core/manager/UpgradeManager.kt` + `service/DownloadService.java`。
- H5 或外部文件下载参考 `common/ext/Context.kt`、`common/utils/IntentUtils.java` 对 `DownloadService` 的调用。
- 下载进度通过 `Events.DownloadProgressEvent` 或 `DownloadService.DownloadApkEvent` 通知，不要在页面重复实现 DownloadManager 轮询。
- 下载前检查权限、服务是否已运行、目标文件是否已存在和 MD5 校验。
- 如新增 ViewModel 侧下载状态，保存 `Job` 或任务对象，取消时停止任务并复位状态。

```kotlin
private var downloadJob: Job? = null
val downloading = MutableLiveData(false)
override val downloadProgress = ObservableInt()

override fun cancelDownload() {
    downloadJob?.cancel()
    downloadJob = null
    downloading.value = false
}
```

## 本地循环与进度模拟

无法直接套 `launchPolling(...)` 的本地任务，也要保存和取消。

- 播放器进度刷新使用 `while (isActive) { ... delay(...) }`，保存 `progressJob`，暂停/销毁时取消。
- 进度模拟参考 `common/utils/ProgressSimulator.kt`、`common/utils/AutoProgressSimulator.kt`。
- 固定次数的短轮询必须限制最大重试次数，并先搜索当前业务的同类实现；找不到时再基于项目现有协程封装实现最小闭环。
- 递归 delay 轮询必须有退出条件，不能无限递归；优先用 `isActive` 或任务状态判断。
- 任务失败要关闭 Dialog/Loading，并展示 `reason` 或可理解错误文案。
- 页面销毁时释放页面拥有的播放器、SSE handler、进度模拟器或 adapter item 内部 Job；持久下载等后台任务只解绑观察，除非用户明确执行取消。

## 状态与错误

- 只有需要被 XML 或观察者自动刷新时，loading/error/content 才使用项目已有 `ObservableBoolean`、`ObservableInt`、`LoadStateViewHolder`、`MutableLiveData` 或轻量状态对象；内部状态使用普通字段。
- 每个长任务至少明确这几种状态：未开始、进行中、成功、失败、取消。
- 失败时保留重试入口，重试前重置失败状态和旧 `Job`。
- 涉及支付、订单状态或会员权益时，最终结果以服务端状态为准，不只依赖客户端支付回调。
- 跨页面生成完成通知使用 `Events` + `FlowEventBus`，但页面内部进度不要用全局事件。

## 审查清单

完成异步任务前检查：

- 是否使用了项目已有 `requestWithLoading`、`launchPolling`、`SSEStreamHandler`、`DownloadService`、`HttpDownload`，没有重复造轮询/SSE/下载框架。
- 是否先明确了任务所有者；页面 ViewModel 拥有的 `Job` / handler 是否在 `onCleared()` 取消，Service、系统下载或应用级 Manager 拥有的任务是否只解绑页面观察。
- 是否有重复启动保护：`isActive` return、先 cancel、或 `downloading/generating` 标记。
- 成功、失败、异常、取消路径是否都复位 loading/progress/generating。
- `CancellationException` 是否放行。
- 后台任务是否定义了未开始、进行中、成功、失败、取消等终态，以及重试和重复启动规则。
- Activity/Fragment 是否只负责展示，不直接持有网络轮询或下载业务。
- Adapter item 内部启动的任务是否有 `clear()` 并由宿主 ViewModel 清理。
