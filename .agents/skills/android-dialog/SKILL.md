---
name: android-dialog
description: "Android 弹窗实现规范。仅在本次实际新增、重构或审查 Dialog/DialogFragment 的基类选择、参数传递、show/newInstance、按钮事件、回调结果、生命周期、键盘处理或关闭逻辑时使用。只改已有弹窗文案、Toast、字段展示、简单样式或局部绑定时，不触发完整弹窗 skill。"
---

# Android 弹窗

本次确实按 Figma/截图实现可见 UI 时再追加 Figma XML 规则；只有弹窗内部实际新增或重构列表时，才追加对应列表 skill。

## 基类选择

优先按内容复杂度和展示位置选择基类：

- 简单中心弹窗，无页面状态/接口/复杂业务：`BaseDialogFragment<VB>`。
- 中心弹窗，带倒计时、支付、下载、接口、复杂状态：`BaseVMDialogFragment<VB, VM>`。
- 简单底部弹窗，无 ViewModel，只做选项选择/输入回调：`BaseBottomPushDialogFragment<VB>`。
- 底部弹窗，带 ViewModel、接口提交、列表状态、复杂选择：`BaseBottomPushVMDialogFragment<VB, VM>`。
- 只是通用确认/提示，不需要自定义布局：优先使用 `showCommonDialog(...)`、`showConfirmDialog(...)` 或 `showConfirmPrompt(...)`。
- 极小的 ViewBinding-only 非 Fragment 弹窗可以使用 `CommonDialog` / `Dialog(context, style)`，但不要作为复杂弹窗默认方案。

不要为了“规范完整”给简单确认弹窗新增 Fragment + ViewModel。也不要在需要接口、倒计时、支付、列表状态的弹窗里硬塞无 VM 基类。

## 布局高度与适配

- 弹窗根布局默认 `wrap_content`，内部纵向约束必须闭合；设计稿总高度只用于视觉核对，不直接当成根布局固定高度。
- 只有局部控件确实固定时才写固定高度。长文案、输入内容、系统字号和多语言区域要允许伸缩、滚动或省略。
- 设置全屏宽度、底部安全区或键盘模式前，先看所选 Base Dialog 的 Window、insets 和 IME 处理，避免页面重复加高度或 padding。

## ViewModel 与 DataBinding 边界

- 简单确认、单次选择和本地输入回调不需要 ViewModel；接口、支付、下载、计时/轮询/SSE、复杂可复用状态或列表 item VM 才考虑 VM 基类。仅使用 DataBinding、输入框或 RecyclerView 不是新增 ViewModel 的理由。
- 无 VM 弹窗直接更新 binding 和局部字段，不伪造 `layout`、`dialog`、`entity` 等变量。VM 弹窗页面变量默认只用 `layout`，简单实体字段、空值兜底、颜色和可见性表达式可以直接绑定。
- 不为每个视觉值增加 `ObservableField` 或透传 getter；只有 XML/观察者需要自动刷新时才使用可观察状态。
- `onPrepareView()` 中的简单初始化直接写清楚；只有监听、观察、列表等职责确实独立时才拆 `init*()`，不生成空初始化函数或一两行的形式包装。

## 参数传递

Fragment 弹窗参数优先通过 `arguments` 传递。Key 优先使用 `Constants.Extra.*`，弹窗私有参数可以在 companion object 内定义 `ARG_XXX`。

无 VM 弹窗在 Dialog 内读取：

```kotlin
class PolicyDialog : BaseDialogFragment<DialogPolicyBinding>() {
    companion object {
        private const val ARG_SHOW_CLOSE = "showClose"

        fun newInstance(showClose: Boolean): PolicyDialog {
            return PolicyDialog().apply {
                arguments = Bundle().apply {
                    putBoolean(ARG_SHOW_CLOSE, showClose)
                }
            }
        }
    }

    override fun onPrepareView() {
        super.onPrepareView()
        val showClose = arguments?.getBoolean(ARG_SHOW_CLOSE, false) ?: false
    }
}
```

带 VM 弹窗让 ViewModel 从 `mBundle` 读取：

```kotlin
class ReportDialogViewModel : AppRepositoryViewModel<AppRepository>() {
    private val userId by lazy { mBundle.getString(ReportDialog.USER_ID) }
}
```

不要通过构造函数传参给 Fragment/DialogFragment。非序列化的短生命周期回调可以作为属性赋值，但不要放进 `arguments`。

## show / newInstance

只创建实例、不立即展示时用 `newInstance(...)`：

```kotlin
fun newInstance(data: XxxInfo?): XxxDialog {
    return XxxDialog().apply {
        arguments = bundleOf(Constants.Extra.DATA to data)
    }
}
```

内部自行找到 FragmentManager 并立即展示时用 `show(...)`：

```kotlin
fun show(manager: FragmentManager, amount: String?): PaymentMethodDialog {
    return PaymentMethodDialog().apply {
        arguments = Bundle().apply { putString(AMOUNT, amount) }
        show(manager)
    }
}
```

有调用方上下文、需要设置回调时，返回 dialog 实例，方便外部继续赋值：

```kotlin
val dialog = PaymentMethodDialog.show(supportFragmentManager, amount = "18.00")
dialog.onConfirmClick = { isWechat -> viewModel.pay(isWechat) }
```

## 按钮事件

- 确认、提交、保存、支付、删除、生成、跳转等用户高频或有副作用操作优先使用 `setOnSingleClickListener`。
- 关闭、取消、简单 toggle、只切 UI 状态的按钮，或同类弹窗已采用的简单监听，可以沿用 `setOnClickListener`。
- 点击确认后需要回调时，先校验输入/状态，再触发回调，最后按当前生命周期安全关闭。
- 取消、关闭按钮只做关闭或取消回调，不触发确认逻辑。
- 弹窗内需要接口请求时，优先交给 ViewModel 方法，Dialog 只负责收集输入、调用 VM、观察结果。

```kotlin
binding.btnConfirm.setOnSingleClickListener {
    val content = binding.xet.text.toString()
    if (content.length < 10) {
        getString(R.string.toast_inspiration_input, 10).showToast()
        return@setOnSingleClickListener
    }
    mRealVM.submit(content, phoneNumber)
}
```

## 结果返回

简单一次性结果可以用属性 callback：

```kotlin
var onConfirm: () -> Unit = {}
var onSave: ((String) -> Unit)? = null
var onEpisodeSelected: (EpisodeInfo?) -> Unit = {}
```

更复杂的结果或异步状态放在 ViewModel 的 `LiveData` / `Flow` 中，由 Dialog 观察后关闭：

```kotlin
mRealVM.isSubmit.observe(this) {
    if (!it) return@observe
    getString(R.string.report_success).showToast()
    dismiss()
}
```

属性 callback 只适合进程内、短生命周期且可以接受配置变更/进程重建丢失的一次性结果。需要跨配置或恢复时，使用项目已有 Fragment result、ActivityResult 或共享状态机制；支付结果、任务 id 等关键状态必须持久化到合适所有者。

跨页面或长期状态不建议用 Dialog 属性 callback 承担。支付、倒计时、轮询等由弹窗 ViewModel 创建的长任务必须保存 `Job` 并在 `onCleared()` 取消；由 Service、系统下载或应用级 Manager 拥有的任务，弹窗关闭时只解绑观察，不得误取消后台任务。

## 关闭与状态保存

- 正常交互优先 `dismiss()`，或沿用附近同类封装。
- 只有 FragmentManager 已保存状态、结果允许丢失，并且当前项目附近同类确实采用该方式时，才使用 `dismissAllowingStateLoss()`；不要把它当作默认关闭方法。
- 关闭路径要避免重复回调，并清理弹窗自己拥有的键盘、监听器和临时任务。

## 输入和键盘

底部输入或表单弹窗优先参考当前存在的 `ReportDialog`、`PhoneLoginDialog` 或同类 picker/dialog：

- 输入框初始化放在 `onPrepareView()`。
- 有未保存内容时，返回键/关闭按钮走确认离开逻辑。
- 关闭、dismiss、destroy 时清理软键盘。
- 敏感词检查、保存接口等异步逻辑交给 ViewModel 或已有扩展，Dialog 只负责 UI 流程。

## 通用确认弹窗

只是展示标题/内容/确认/取消，不需要自定义布局时使用快捷扩展：

```kotlin
showCommonDialog(title = getString(R.string.delete_work_warning)) {
    viewModel.delete()
}
```

或：

```kotlin
showConfirmPrompt(
    tip = getString(R.string.confirm_exit_without_saving),
    cancelText = getString(R.string.exit_directly),
    confirmText = getString(R.string.save_and_exit),
    cancel = { dismiss() },
    confirm = { save() }
)
```

不要为普通确认文案新建一个 DialogFragment，除非设计稿有明显自定义视觉结构或复杂交互。

## 审查清单

弹窗完成前检查：

- 基类选择符合展示位置和复杂度：中心/底部、有无 VM、有无接口/长任务。
- 根布局高度、纵向约束、安全区和 IME 处理能适应长文案、系统字号与多语言，没有照抄设计稿总高度。
- 简单弹窗没有滥加 ViewModel/DataBinding 状态或空 `init*()`，复杂状态才进入 VM。
- Fragment 参数通过 `arguments`，带 VM 的参数优先由 ViewModel 从 `mBundle` 读取。
- 没有使用 DialogFragment 构造函数传参。
- 确认、提交、保存、支付、删除、生成、跳转等操作使用 `setOnSingleClickListener` 或项目已有 click binding；关闭、取消、简单 toggle 可沿用当前代码的 `setOnClickListener`。
- 确认、取消、关闭语义清晰，不误触发回调。
- 接口、支付、下载、倒计时、轮询等复杂逻辑在 ViewModel，不在 Dialog 里堆业务。
- 回调只用于短生命周期结果返回，不持有 Activity/Fragment/View。
- 正常关闭使用 `dismiss()` 或附近安全封装；`dismissAllowingStateLoss()` 只在状态已保存且结果允许丢失时使用。需要不可取消时显式设置 `isCancelable = false`。
- 用户可见文案走 string 资源，UI 样式优先使用项目 style/attr/dimen/drawable。
