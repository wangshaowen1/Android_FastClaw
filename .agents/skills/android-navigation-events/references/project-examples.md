# 项目示例索引

> 使用前必须确认示例路径在当前分支真实存在；不存在时，用本文件的搜索命令查找当前分支等价实现，不要机械照搬复制来源项目的路径。

本文件记录导航与事件通信 skill 的当前项目依据。生成代码时按这些文件的现有写法对齐，不要照搬通用 Android 模板。

## 核心文件

- `app/src/main/java/<主业务包路径>/router/AppRouter.kt`
  - `openFromPush(context, uri)`：Push URI 进入 `AppLaunchJump.launchJump(...)`。
  - `navigate(context, path)`：IRouter 入口，转到 `jump(path)`。
  - `jump(context, schemeUri, openBrowser)`：HTTP 且要求外部浏览器时走 `openBrowser(...)`，否则继续项目内分发。
  - `jump(schemeUri)`：处理空 URI、项目 scheme、apk 下载、HTTP 内置浏览器。
  - `jump(uri)`：当前只确认 `wechatMini` host，用于拉起微信小程序。
- `app/src/main/java/<主业务包路径>/router/AppNavigator.kt`
  - `checkLoginIntercepted(...)`：访客拦截。
  - `startLogin(...)`：按微信开关选择手机号登录或统一登录、全屏页或弹窗。
  - `forceLogoutAndRestart()`：Token 失效后的统一退出重启。
- `app/src/main/java/<主业务包路径>/core/Events.java`
  - 项目全局事件集中定义处，事件类实现 `AppEvent`。
- `app/src/main/java/<主业务包路径>/core/ext/EventScopeExt.kt`
  - `onUserSignIn(...)`、`onUserSignOut(...)`、`onUserStatusChanged(...)` 是登录态监听的优先封装。

## 主 Tab 与登录态

- `app/src/main/java/<主业务包路径>/feature/main/MainActivity.kt`
  - `KEY_SCHEME` 启动参数会调用 `AppRouter.jump(schemeUri)`。
  - `binding.mrg.setOnCheckedChangeListener` 中对作品、我的等入口调用 `AppNavigator.checkLoginIntercepted(true)`。
  - tab 连续点击发布 `Events.MainTabDoubleClickEvent`。
  - `observeEvents { on<Events.MainTabEvent> { ... } }` 中统一切换 tab。
  - `observeEvents { on<Events.UserChangedEvent> { ... } }` 中处理 `TOKEN_EXPIRED` 和用户信息刷新。
- `app/src/main/java/<主业务包路径>/kit/SignUtils.kt`
  - `signIn()` 发布 `Events.UserChangedEvent.SIGN_IN`。
  - `signOut()` 发布 `Events.UserChangedEvent.SIGN_OUT`。
- `app/src/main/java/<主业务包路径>/core/Response.kt`
  - 通用 `response(...)` 遇到授权错误时发布 `Events.UserChangedEvent.TOKEN_EXPIRED`。

## 直接页面跳转

当前工程明确目标页跳转优先使用目标入口：

- `SettingsActivity.start(...)`
- `FeedbackActivity.start(...)`
- `AboutUsActivity.start(...)`
- `LogoffActivity.start(...)`
- `PhoneShowActivity.start(...)`
- `SimpleBrowserActivity.launch(...)`
- `MainActivity.start(...)`

普通按钮跳转、设置页入口、H5 页面入口不要强行绕 `AppRouter` 或全局事件。

## 事件监听与过滤

当前可参考事件：

- `Events.BindingSuccessfulEvent`
  - 发送：`BindPhoneViewModel`、手机号登录/绑定相关页面。
  - 用途：手机号绑定成功后通知相关页面刷新。
- `Events.AgreePolicyEvent`
  - 发送/监听：登录弹窗和登录页之间同步协议勾选。
- `Events.DownloadProgressEvent`
  - 发送：`DownloadService`。
  - 监听：`UpgradeDialogFragment`。
- `Events.ConfigChangeEvent`
  - 发送：`Config.kt`。
  - 用途：配置变化通知。
- `Events.PushEvent`、`Events.TrackCreatedEvent`
  - 发送：`PushManager`。

新增事件必须放到 `Events.java`，字段要带足过滤条件，接收方按页面生命周期和业务 id 过滤。Dialog 结果、Adapter item 点击、单页面按钮反馈优先用 callback 或 ViewModel 状态。

## 现有事件类型

常见可复用事件：

- `Events.MainTabEvent`：主 tab 切换，常量 `TAB_HOME`、`TAB_MY_WORKS`、`TAB_MINE`。
- `Events.MainTabDoubleClickEvent`：主 tab 连续点击刷新。
- `Events.UserChangedEvent`：登录成功、退出登录、Token 过期。
- `Events.UserDataEvent`：用户数据变更。
- `Events.ConfigChangeEvent`：配置变化。
- `Events.PushEvent`：Push 消息。
- `Events.TrackCreatedEvent`：歌曲/内容创建事件，沿用现有命名。
- `Events.DownloadProgressEvent`、`Events.DownloadContractEvent`：下载进度和合同下载。
- `Events.BindingSuccessfulEvent`：手机号绑定成功。
- `Events.AgreePolicyEvent`：登录协议同意状态。

## 搜索命令

需要补充示例时，优先使用：

```powershell
rg -n "AppRouter|AppNavigator|FlowEventBus|observeEvents|Events\\." app/src/main/java/<主业务包路径>
rg -n "MainTabEvent|MainTabDoubleClickEvent|UserChangedEvent|BindingSuccessfulEvent|DownloadProgressEvent" app/src/main/java/<主业务包路径>
rg -n "fun start\\(|launch\\(" app/src/main/java/<主业务包路径>/feature app/src/main/java/<主业务包路径>/router
```
