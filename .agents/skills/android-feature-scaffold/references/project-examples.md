# 项目示例

> 使用前必须确认示例路径在当前分支真实存在；不存在时，用本文件的搜索命令查找当前分支等价实现，不要机械照搬复制来源项目的路径。

这些示例用于对齐当前 `AIMarry` 页面骨架写法，只复制结构，不照搬业务逻辑。

## 带 Toolbar 的简单 Activity

- `app/src/main/java/<主业务包路径>/feature/mine/PhoneShowActivity.kt`
- `app/src/main/java/<主业务包路径>/feature/mine/PhoneShowViewModel.kt`
- `app/src/main/res/layout/activity_phone_show.xml`

可参考写法：

- `BaseViewModelActivity<ActivityPhoneShowBinding, PhoneShowViewModel>`。
- `companion object start(context)`。
- `ToolbarViewModel(navigationAction = { back() }, title = ...)`。
- XML 使用 `<layout>` 和页面变量 `layout`。
- Toolbar include 使用 `@layout/view_model_toolbar`。

## 表单 Activity

- `app/src/main/java/<主业务包路径>/feature/login/bind/BindPhoneActivity.kt`
- `app/src/main/java/<主业务包路径>/feature/login/bind/BindPhoneViewModel.kt`
- `app/src/main/res/layout/activity_bind_phone.xml`

可参考写法：

- Activity 负责输入框焦点、点击分发和观察 `LiveData`。
- ViewModel 负责手机号、验证码、按钮可用状态、倒计时和接口请求。
- 用户点击绑定/发送验证码时经 ViewModel 校验，不在 Activity 直接请求网络。
- 需要键盘控制时沿用当前页面的 `KeyboardUtils` 或同类写法。

## 设置类 Activity

- `app/src/main/java/<主业务包路径>/feature/settings/SettingsActivity.kt`
- `app/src/main/java/<主业务包路径>/feature/settings/SettingsViewModel.kt`
- `app/src/main/res/layout/activity_settings.xml`

可参考写法：

- `onCreate()` 只做 debug IP 等页面初始化。
- XML 使用 `singleClick="@{layout.clickHandler::onClick}"`，Activity 重写 `onClick(...)` 分发入口。
- 登录拦截走 `AppNavigator.checkLoginIntercepted(...)`。
- 普通确认使用 `showConfirmPrompt(...)`，明确页面跳转使用目标 Activity 的 `start(...)`。

## Fragment Tab 页面

- `app/src/main/java/<主业务包路径>/feature/home/HomeFragment.kt`
- `app/src/main/java/<主业务包路径>/feature/home/HomeViewModel.kt`
- `app/src/main/res/layout/fragment_home.xml`

可参考写法：

- `BaseViewModelFragment<FragmentHomeBinding, HomeViewModel>`。
- `companion object newInstance()`。
- `onPrepareView()` 中进入 `initView()` 和数据观察。
- Paging 列表、Banner、状态 UI 属于具体页面内容；新增普通页面骨架时不要默认引入这些复杂能力。

## 带参数 Fragment

- `app/src/main/java/<主业务包路径>/feature/web/BrowserFragment.kt`
- `app/src/main/java/<主业务包路径>/feature/web/BrowserViewModel.kt`
- `app/src/main/res/layout/fragment_browser.xml`

可参考写法：

- `newInstance(url)` 使用 `arguments` 传入 `Constants.Extra.URL`。
- ViewModel 从 `mBundle` 读取参数。
- Fragment 负责 WebView、JS bridge、权限回调和导航处理。
- 手动持有的 WebView/Callback 在对应生命周期中清理。

## Base 与共享文件

- `app/src/main/java/<主业务包路径>/base/viewmodel/AppViewModel.kt`
- `app/src/main/java/<主业务包路径>/base/viewmodel/AppRepositoryViewModel.kt`
- `app/src/main/java/<主业务包路径>/base/viewmodel/ToolbarViewModel.kt`
- `app/src/main/res/layout/view_model_toolbar.xml`
- `app/src/main/java/<主业务包路径>/common/Constants.java`
- `app/src/main/java/<主业务包路径>/core/AppRepository.kt`

搜索建议：

```powershell
rg -n "BaseViewModelActivity<|BaseViewModelFragment<|ToolbarViewModel\\(" app/src/main/java/<主业务包路径>/feature -g "!**/OssManager.kt"
rg -n "fun start\\(|fun getIntent\\(|fun newInstance\\(" app/src/main/java/<主业务包路径>/feature -g "!**/OssManager.kt"
rg -n "layout=\"@layout/view_model_toolbar\"|name=\"layout\"" app/src/main/res/layout
```
