---
name: android-api-request
description: "Android Retrofit 接口链路接入规范。仅在新增接口，或本次新增、修改、审查 ApiService、request/response model、Repository、ViewModel 中至少两层，或调整 response/request/requestWithLoading 错误语义时使用。当前分支已有完整同类链路且只补明确字段、参数或单一调用点时走 quick rules 局部接口快路径；只改 Toast、loading 文案或普通 UI 状态时不要使用。"
---

# Android 接口请求

分页、新页面或长任务只在本次确实修改对应领域时追加相应 skill，不因接口词出现在上下文中连锁加载。

## 契约证据

接口路径、方法、字段、类型、是否可空和枚举值必须来自可追溯证据，不能从 UI 文案或相似旧接口猜测：

- 用户本次提供的接口截图、字段表、OpenAPI 或明确路径是目标契约；当前代码用于判断兼容现状和改动范围，两者冲突时明确指出，不静默覆盖。
- 本地证据已经足够时直接实现。证据不完整且当前项目已配置接口文档查询工具时，只查询目标接口，不全量扫描接口平台。
- 工具不可用或目标接口不存在时，只落地已确认字段，并把缺口列为待确认；不要借用相似业务模型补齐未知字段。
- 不读取、输出或写回接口平台凭据、token、secret；日志也不得打印完整请求、响应或敏感参数。

## 核心原则

沿用当前项目网络链路，不引入通用模板：

- API 路径常量写在 `core/network/apis/ApiConstants.kt`。
- Retrofit 方法写在 `core/network/apis/ApiService.kt`。
- 请求体写在 `core/network/apis/request/`。
- 聚合响应模型写在 `core/network/apis/response/`。
- 接口实体写在 `core/network/apis/entity/`。
- 返回类型保持 `ApiResponse<T>`。
- ViewModel 通过 `mRepo.api` 或 feature Repository 调接口。
- 错误处理优先使用 `response(...)`、`request(...)`、`requestWithLoading(...)`。
- 新增完整接口、请求体、响应模型或实体模型时，按接口文档补中文业务注释。局部调用已有接口、只补字段或只接已有 body 时，优先沿用当前文件风格；缺少接口文档时不要编造注释。

不要修改 `ApiResponse.java`、`ApiException.java`、`ApiFactory.kt`，除非用户明确要求改全局网络层。

## 接入流程

1. 先搜索是否已有相同或相近接口：

```powershell
rg -n "业务关键词|接口路径|ApiConstants名|方法名" app/src/main/java/<主业务包路径>/core/network/apis app/src/main/java/<主业务包路径>/feature -g "!**/OssManager.kt"
```

2. 在 `ApiConstants.kt` 添加路径常量，放到业务相近区域。

```kotlin
/**
 * 绑定手机号
 */
const val BIND_PHONE = "user/phone/action/bind"
```

3. 按需新增请求体。新增完整请求体时，类和字段按接口文档补中文业务注释；局部补字段时沿用当前文件风格，不确定含义时标明需要确认，不编造注释。

```kotlin
/**
 * 绑定手机号
 * @param telNum 手机号
 * @param smsValidateCode 验证码
 */
data class BindPhoneBody(
    val telNum: String?,
    val smsValidateCode: String?
) : UserTokenBody()
```

4. 按需新增响应模型。接口直接返回业务实体时优先复用已有 `Info/Data`，不要为了形式新增一层 UiModel。新增完整模型时按接口文档补中文业务注释；局部补字段时沿用当前文件风格。

5. 在 `ApiService.kt` 增加 Retrofit 方法。

```kotlin
/**
 * 绑定手机号
 */
@POST(ApiConstants.BIND_PHONE)
suspend fun bindPhone(@Body body: BindPhoneBody): ApiResponse<Any?>
```

6. 在 ViewModel 或 feature Repository 调用接口。

## ViewModel 调用规则

### 无 loading 的静默请求

用于页面初始化、后台刷新、多个接口并行中的普通请求。

```kotlin
request(
    apiBlock = { mRepo.api.getAppBanner(BannerBody(BannerBody.LOCATION_HOME)) },
    onSuccess = {
        val data = data ?: return@request
        // 更新页面状态
    }
)
```

### 用户触发且需要 loading 的请求

用于保存、提交、删除、支付前置、重试等用户操作。

```kotlin
requestWithLoading({
    mRepo.api.bindPhone(BindPhoneBody(telNum = mobile.value, smsValidateCode = code.value))
}) {
    onSuccess?.invoke()
}
```

需要自定义失败处理：

```kotlin
requestWithLoading(
    apiBlock = { mRepo.api.logOff(UserTokenBody()) },
    showToastError = true,
    onError = {
        // 恢复按钮状态、刷新状态页等
    }
) {
    // 成功逻辑
}
```

### 直接使用 response(...)

项目已有代码里 `launch { response(...) }` 很常见。新增或修改时优先沿用目标文件附近写法：无 loading 的简单请求可用 `request(...)` 或 `launch { response(...) }`，用户触发且需要 loading 的请求用 `requestWithLoading(...)`。当你已经在 `launch` / `launchWithLoading` / 自定义协程中，或需要组合多个接口时，可以使用：

```kotlin
launchWithLoading {
    response(mRepo.api.cancelLogOff(UserTokenBody())) {
        showToast(R.string.cancellation_success)
        finish()
    }
}
```

如果使用自定义 `launch`，简单请求可沿用附近的 `launch { response(...) }`。涉及状态回滚、`savingIds`、loading/progress 清理或自定义异常提示时，使用项目已有 `launch(block, onError, onComplete)`：

- `response.error` 处理接口业务失败，例如回滚状态和展示后端错误。
- `launch.onError` 处理网络/协程异常，例如异常回滚和 `parseMessage(e)?.showToast()`。
- `launch.onComplete` 清理必然执行的临时状态，例如移除 `savingIds`。
- 不要在 `launch` 内手写 `try/catch/finally` 包网络请求，除非有明确特殊原因。

不要在 Activity/Fragment 中直接请求网络。页面只负责观察 ViewModel、展示 Toast/Dialog/loading、跳转和回填 UI。

## Repository 使用规则

默认 ViewModel 可以直接用 `mRepo.api.xxx(...)`。

只有满足以下情况，才在 feature 下新增 Repository 方法：

- 同一接口组合被多个 ViewModel 复用。
- 需要组合多个接口、数据库、缓存或 MMKV。
- 需要统一 IO 切换、数据转换、缓存降级或第三方 SDK 隔离。
- ViewModel 中请求构造明显变复杂。

Repository 方法可以这样写：

```kotlin
class BindPhoneRepository : AppRepository() {
    suspend fun bindPhone(phoneNumber: String?, validationCode: String?) = withContext(Dispatchers.IO) {
        api.bindPhone(BindPhoneBody(phoneNumber, validationCode))
    }
}
```

Repository 不要随意吞异常。普通 API 失败仍交给 ViewModel 的 `response/request/requestWithLoading` 处理。

## 模型放置规则

- `XxxBody`：接口请求体，放 `core/network/apis/request/`。
- `XxxPagedBody`：分页请求体。
- `XxxInfo`：接口实体，放 `core/network/apis/entity/`。
- `XxxData`：聚合响应数据，放 `core/network/apis/response/`。
- `PagedList<T>`：分页列表返回。
- `ListData<T>`：普通列表返回。
- `Any?`：接口确实不关心返回体时可以使用。

字段名默认按后端 JSON 字段保持 lowerCamelCase。只有字段名和 Kotlin 冲突、或后端字段不符合命名时，再考虑注解映射；先参考项目已有模型写法。

## 错误处理规则

项目已有行为：

- `ApiResponse.isSuccess` 判断 `code == 200`。
- `ApiResponse.apiException` 会转成 `ApiException(code, msg)`。
- `response(...)` 会处理 token 过期、注销、拉黑等通用登录事件。
- `requestWithLoading(showToastError = true)` 默认展示接口错误文案。

新代码不要自己到处写：

```kotlin
if (response.code == 200) { ... }
```

优先写：

```kotlin
response(bean) {
    // success
}
```

或：

```kotlin
requestWithLoading({ mRepo.api.xxx(body) }) {
    // success
}
```

协程中捕获异常时必须放行 `CancellationException`。如果任务包含轮询/SSE/下载，优先走专门的异步任务规范，不要把长任务逻辑写进普通 API 接口接入。

## SSE 和分页分支

SSE 接口在 `ApiService` 中返回 `Flow<SSEvent>`，并需要 `@Headers("Accept: text/event-stream", ...)`。普通 API skill 只负责识别它，不展开生成完整 SSE 处理器。

分页接口返回 `ApiResponse<PagedList<T>?>` 时，页面实现必须走 `android-paging-state-ui`，不要用普通列表请求临时拼分页。

## 审查清单

完成前检查：

- 是否先搜索并复用了已有接口、请求体、响应模型。
- 接口路径、方法、字段、类型、可空性和枚举值是否都有用户资料、接口文档或当前代码证据；冲突和缺口是否已明确说明。
- `ApiConstants` 常量名和路径放在业务相近位置。
- `ApiService` 方法名是业务动作，不是泛名。
- 新增完整接口、request/response/entity 模型时有来自接口文档或业务说明的中文注释；局部小改沿用当前文件风格，没有编造注释。
- 请求体放在 `request/`，响应聚合放在 `response/`，接口实体放在 `entity/`。
- 返回类型保持当前项目的 `ApiResponse<T>`。
- ViewModel 通过 `mRepo.api` 或 feature Repository 调用，不在 Activity/Fragment 请求网络。
- 用户触发操作使用 `requestWithLoading(...)`，后台刷新使用 `request(...)` 或已有 `response(...)` 风格。
- API 失败没有被随意吞掉。
- 没有修改 `ApiResponse.java`、`ApiException.java`、`ApiFactory.kt`。
- 分页、SSE、轮询没有被误写成普通一次性请求。
