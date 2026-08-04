---
name: android-feature-scaffold
description: "Android 页面骨架生成规范。仅在本次实际新增页面、创建 Activity/Fragment/ViewModel/XML 骨架、重构页面基础结构，或判断 Base 类、包路径、start/newInstance、mBundle 模式时使用。修改已有页面的局部逻辑、局部 XML、字段绑定、Toast、点击事件或状态同步时不要触发。"
---

# Android 页面骨架

## 核心规则

生成能满足当前功能的最小项目原生 MVVM 页面骨架。

使用当前项目模式，不使用通用 Android 模板：

- Activity 使用 `BaseViewModelActivity<VB, VM>`。
- Fragment 使用 `BaseViewModelFragment<VB, VM>`。
- 页面需要 `mRepo`、API、数据库或项目仓库能力时，使用 `AppRepositoryViewModel<AppRepository>` 或已有 feature Repository。
- 只需要页面状态、Toolbar、点击事件或 `mBundle` 参数读取时，优先使用 `AppViewModel`，并参考当前 feature 附近同类页面。
- XML 使用 `<layout>` 根节点，页面变量名为 `layout`。
- 页面需要标准标题/返回栏时，Toolbar 使用 `ToolbarViewModel` 和 `@layout/view_model_toolbar`。

## 生成前判断

1. 先在相同 `feature/<domain>` 包下搜索附近已有页面。
2. 明确选择 Activity、Fragment 或 Dialog：
   - Activity：独立全屏入口、外部启动、需要返回结果或作为导航目标。
   - Fragment：Tab 页、子页面、ViewPager 页面或由现有 Activity 承载的页面。
   - Dialog：弹窗 UI，使用项目弹窗专项规则，不使用本页面骨架规则。
3. 判断页面是否需要仓库访问。需要时使用 `AppRepositoryViewModel<AppRepository>` 或已有 feature Repository；仅有 `mBundle` 参数不算仓库访问。
4. 只新增当前确实需要的文件。除非功能已经需要，否则不要创建 Repository、Adapter、UiState 或 mapper。
5. 只为真实初始化职责生成 `onCreate()`、`onPrepareView()` 和 `init*()`；不生成空函数，也不把一两行代码机械拆成形式包装。

## 文件放置

新业务页面放在最接近的现有包下：

```text
app/src/main/java/<主业务包路径>/feature/<domain>/
```

典型文件：

- Activity 页面：`XxxActivity.kt`、`XxxViewModel.kt`、`activity_xxx.xml`。
- Fragment 页面：`XxxFragment.kt`、`XxxViewModel.kt`、`fragment_xxx.xml`。
- 只有需要列表时才增加：`XxxAdapter.kt`、`ItemXxxVM.kt`、`item_xxx.xml`、`XxxEvent.kt`。
- 只有现有 `AppRepository` 不足时才增加 feature Repository：`XxxRepository.kt`。

字符串资源放入项目允许的 string 文件，不要在 XML 或 Kotlin 中硬编码面向用户的文案。

## Activity 模式

独立页面和需要返回结果的页面使用 Activity。

```kotlin
class XxxActivity : BaseViewModelActivity<ActivityXxxBinding, XxxViewModel>() {

    companion object {
        @JvmStatic
        fun start(context: Context?, workId: String?) {
            if (context == null) return
            val starter = Intent(context, XxxActivity::class.java)
            starter.putExtra(Constants.Extra.WORK_ID, workId)
            context.startActivity(starter)
        }
    }

}
```

只有确实需要初始化 View、监听或观察时才重写 `onCreate()`；简单代码直接写清楚，监听、观察、列表等职责独立时再拆函数。如果需要返回结果，参考附近选择页补充 `getIntent(...)` 和 `setResult(...)`。网络和业务逻辑仍放在 ViewModel。

## Fragment 模式

Tab 页、子页面以及由其他页面承载的页面使用 Fragment。

```kotlin
class XxxFragment : BaseViewModelFragment<FragmentXxxBinding, XxxViewModel>() {

    companion object {
        fun newInstance(workId: String?): XxxFragment {
            return XxxFragment().apply {
                arguments = Bundle().apply {
                    putString(Constants.Extra.WORK_ID, workId)
                }
            }
        }
    }

}
```

只有需要初始化时才重写 `onPrepareView()`，不要生成空实现。本项目 `onPrepareView()` 不是普通模板里的 `onViewCreated()`；在其中注册 ViewPager2 callback、AdapterDataObserver、键盘监听或事件监听时，先检查当前 baselibrary 版本、基类是否缓存 View，以及附近同类页面的配对清理点，再在真实生命周期解除并清空引用。FastClaw 不少页面使用 `onDestroy()`，但不要把这个结论硬套到其他基线。

Fragment 收集协程数据时，可以使用 `viewLifecycleOwner.lifecycleScope` 和 `viewLifecycleOwner.repeatOnLifecycle(...)`，除非附近代码已有更强的局部模式。这个规则只约束 LiveData/Flow 观察，不自动决定手动回调的清理点。

## ViewModel 模式

标准 Toolbar 使用 `ToolbarViewModel`，参数从 `mBundle` 读取。只读参数和维护本地页面状态时，可以使用 `AppViewModel`。

```kotlin
class XxxViewModel : AppViewModel() {

    val toolbar = ToolbarViewModel(
        navigationAction = { back() },
        title = R.string.xxx_title.getResString()
    )

    val workId by lazy { mBundle.getString(Constants.Extra.WORK_ID) }
}
```

需要接口、数据库或仓库能力时，再使用 `AppRepositoryViewModel`：

```kotlin
class XxxViewModel : AppRepositoryViewModel<AppRepository>() {

    val toolbar = ToolbarViewModel(
        navigationAction = { back() },
        title = R.string.xxx_title.getResString()
    )

}
```

使用与附近代码一致的项目状态类型：只有 XML 或观察者需要自动刷新时，才使用 `MutableLiveData`、`ObservableField`、`ObservableBoolean`、`ObservableInt`；仅供内部计算、临时缓存或选中判断的状态使用普通字段。只有功能已经需要分页、SSE、轮询或连续流时，才使用 `Flow`。

轮询、SSE handler、临时下载、媒体等由页面 ViewModel 创建和拥有的长任务必须保存引用，并在 `onCleared()` 中取消。由 Service、系统下载或应用级 Manager 拥有的后台任务遵循自身生命周期，页面只解绑观察，不得在 `onCleared()` 误取消。

## XML 模式

页面 XML 使用 DataBinding：

```xml
<layout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools">

    <data>
        <variable
            name="layout"
            type="<主业务包名>.feature.xxx.XxxViewModel" />
    </data>

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:orientation="vertical">

        <include
            android:id="@+id/toolbar"
            layout="@layout/view_model_toolbar"
            app:toolbar="@{layout.toolbar}" />

    </LinearLayout>
</layout>
```

优先使用项目已有 style 和 attr：`@style/Text.Body`、`@style/Text.Subtitle`、`@style/Button.Bold`、`?attr/textColorPrimary`、`?attr/textColorHint`、`@dimen/spacing_l` 和已有 drawable 资源。

点击处理：

- 保存、提交、支付、生成、跳转、登录等用户高频动作，Kotlin 命令式绑定优先使用 `setOnSingleClickListener`。
- 页面通过 `onClick(view, data)` 集中处理点击时，XML 使用 `singleClick="@{layout.clickHandler::onClick}"`。
- 简单关闭、toggle、播放器点击、adapter retry、widget 内部回调、Java 旧控件或同类代码已采用的简单监听，可以沿用 `setOnClickListener`。

## 观察与事件

Activity 可以使用 `observe(this)`。

Fragment 观察 LiveData/Flow 时可以使用 `viewLifecycleOwner`：

```kotlin
mRealVM.xxxData.observe(viewLifecycleOwner) { data ->
}
```

不要因为使用 `viewLifecycleOwner` 观察数据，就猜测 `onPrepareView()` 中手动注册的清理点；以当前 baselibrary、基类 View 缓存行为和附近同类实现为准。

`observeEvents { ... }` 和 `FlowEventBus` 仅用于跨页面应用事件。父子页面通信如果可以用直接回调或附近已有 shared ViewModel 处理，不要使用事件总线。

## 审查清单

页面骨架完成前检查：

- 文件位于最接近的现有 `feature/<domain>` 包下。
- Activity 继承 `BaseViewModelActivity<VB, VM>`，Fragment 继承 `BaseViewModelFragment<VB, VM>`。
- XML 根节点是 `<layout>`，页面变量名是 `layout`。
- 标准 toolbar 使用 `ToolbarViewModel` 和 `view_model_toolbar`。
- 跨页面参数优先用 `Constants.Extra.*` 或局部 `private const val KEY_XXX`，在 ViewModel 中从 `mBundle` 读取；不要为风格重构无关的历史 key。
- Activity/Fragment 只初始化 View、监听、观察者、导航、Toast/Dialog/loading，不直接请求网络或访问数据库。
- ViewModel 负责页面状态和请求调用。
- 没有空 `onCreate()`、`onPrepareView()`、`init*()`，简单初始化没有被过度拆分。
- 只有 UI/观察者消费的状态使用可观察类型，内部临时状态保持普通字段。
- 长任务所有者明确；页面 ViewModel 拥有的任务在 `onCleared()` 取消，Service/系统/应用级任务只解绑页面观察。
