---
name: android-databinding-ui
description: Android 页面状态 DataBinding 规范。仅在新增、重构或审查 Activity/Fragment/Dialog/XML 的稳定页面状态绑定、多个互斥区域、Observer 中批量写 View、ViewAnimator/ViewSwitcher、Observable/LiveData 到 text/visibility/enabled/selected 映射时使用。手势逐帧、动画过程、录音按压、播放器进度或一次性简单 View 赋值不触发。
---

# Android 页面状态 DataBinding

让 ViewModel 成为稳定页面状态的单一可信源，由 XML DataBinding 完成展示映射；Activity/Fragment 只负责生命周期、导航、弹窗、权限、动画和必须依赖 View 的交互。

## 判断边界

- `binding.xxx` 只是类型安全地访问 View，不等于页面状态已使用 DataBinding。
- 接口结果、关系状态、数量、文案、红点、选中、启用和稳定显隐优先绑定。
- 手势坐标、录音按压、拖动、动画逐帧、播放器进度、ViewPager 当前下标和第三方控件回调可以保留命令式更新。
- 不为消灭一行赋值把瞬时 UI 状态塞进接口实体；状态放 ViewModel 的 `Observable*`、`LiveData` 或确有必要的专用 UiModel。
- XML 只写简单映射；组合业务判断先在 ViewModel 收敛为语义字段或单一状态。
- 无 ViewModel 且参数创建后不再变化的多模式 Dialog，可以由 Dialog 暴露只读语义展示属性供 XML 绑定；不要只为绑定新增空壳 ViewModel。

## 实现流程

1. 用 `rg -n "binding\\..*\\.(text|visibility|isVisible|isEnabled|isSelected)\\s*="` 定位直接写 View 的代码。
2. 按稳定状态与瞬时交互分类，不机械迁移。
3. 找出同一 View 被几个方法或 Observer 修改；两个以上来源先合并状态再绑定。
4. 在 ViewModel 暴露语义状态，并在所有业务变更入口统一更新。
5. 在 XML 绑定 `text`、`visibility`、`enabled`、`selected` 或项目已有 BindingAdapter。
6. 删除失效 Observer、重复显隐和已无入口的历史 View。
7. 静态检查 XML、表达式类型、资源引用和旧赋值残留。

## 表达式类型

- `backgroundRes`、`textColorRes`、`imageResource` 等 `*Res` BindingAdapter 接收资源 ID；不要把条件 drawable 或条件颜色直接传给它们，DataBinding 可能解析成 `Drawable` 或实际颜色值。
- 条件背景使用接收 `Drawable` 的 `backgroundDrawable`；条件文字颜色使用 `android:textColor`。确实需要资源 ID 时，在 ViewModel 暴露明确的 `@DrawableRes/@ColorRes Int` 状态。
- 不在 XML 中导入 `java.lang.String` 再调用 `String.valueOf(LiveData)`；数量文本使用字符串资源或参数类型明确的 BindingAdapter。
- `@drawable/x : 0` 会形成 `Drawable/Int` 混合分支；改为 ViewModel/Dialog 暴露明确的 drawable resource ID，再交给接收 `Int` 的适配器。
- 会主动发送 `null` 清空事件的状态声明为 `LiveData<T?>`，观察端显式处理空值；不要用非空 `LiveData<T>` 包装可空的 `MutableLiveData.value`。

## 互斥区域

- 同一位置三个及以上互斥布局，优先使用 `ViewAnimator`；两个轻量子 View 可以使用 `ViewSwitcher` 或简单绑定。
- ViewModel 暴露单一 `ObservableInt`/`LiveData<Int>` 状态，XML 绑定 `android:displayedChild`。
- 状态索引使用有业务名称的常量，不在 Activity/Fragment 散落数字。
- 需要“不显示任何内容”时增加零尺寸空 child，不同时控制容器和所有 child 的显隐。
- 不允许多个 Observer 分别控制互斥 child；键盘关闭、停止录音等副作用可以继续监听状态执行。

## 收敛阈值

发现以下任一情况时暂停追加局部 `isVisible`，先收敛状态：

- 同一 View 的属性在两个以上方法中被修改。
- 一个渲染方法连续修改三个以上 View 的稳定状态。
- 两个 Observer 共同决定同一区域。
- 新状态加入后需要先隐藏所有 View，再显示其中一个。
- XML 已有 `<layout>`，但页面状态仍主要由 Observer 手动赋值。
- 已迁移到 Dialog/新页面的旧布局仍保留且只被隐藏。

## 静态检查

- Base 类已经把 `layout` 变量和 lifecycle owner 绑定到 ViewModel。
- 所有业务变更入口都会更新同一语义状态。
- 互斥容器始终只有一个 child 可见。
- 命令式代码只剩副作用和瞬时交互。
- 默认不跑 Gradle；执行 XML 解析、表达式引用和残留写法检查。
