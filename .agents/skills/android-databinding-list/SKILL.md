---
name: android-databinding-list
description: "Android 非分页 DataBinding 列表实现规范。仅在本次实际新增、重构或审查非分页 RecyclerView/DataBinding 列表实现，包括 Adapter、ItemVM、item layout、列表数据更新和点击转发时使用。只排查列表 bug、只改 item 文案/样式或只改一小段刷新逻辑时，优先读目标代码和 android-check-quick.md，不触发完整列表 skill。"
---

# Android 非分页 DataBinding 列表

实际涉及分页链路时改读 `android-paging-state-ui`；本次还新增页面骨架时再追加 `android-feature-scaffold`。

## 核心规则

普通 DataBinding 列表优先使用项目现有组合：

- 单类型普通列表：`QuickAdapter<ItemXxxVM>(R.layout.item_xxx, emptyList())`。
- item VM：继承 `AppItemViewModel<T>`，命名为 `ItemXxxVM`。
- item XML：使用 `<layout>`，DataBinding 变量名固定为 `item`，类型优先绑定 `ItemXxxVM`。
- 列表更新：优先 `adapter.submitList(newItems)`。
- 自定义 Adapter 或 PagingDataAdapter 的 ViewHolder 优先继承 `BindingViewHolder<VB>`，并通过 `binding.getVm(...)` 获取或复用 item VM。
- `binding.getVm(...)` 必须传稳定、非空、唯一的业务 key；不要使用 position、可变展示字段或可能为 null 的 id。项目已废弃 position 重载，新代码不得继续调用。
- item VM 只承载实体、本地 UI 状态和点击回调。XML 可以直接写 `item.data.xxx` 的简单字段、颜色或可见性表达式，不要再包一层无意义的 `getXxxText()`、`isXxxVisible()` 或同名透传属性。
- 复杂列表、多选、批量操作、跨页面刷新或需要统一收敛的 item 点击/选择/操作，优先查当前 feature 是否已有事件 VM 或 callback 模式；简单选择、弹窗列表、静态配置项、局部 item 操作可以使用 item VM callback、`onItemClick.value`、ViewModel 方法或同类 `setOnItemClickListener`。
- 多类型列表：当前工程有通用 `common/adapter/MultiTypeAdapter.kt`，业务侧未发现稳定混排模板；只有真实混排需求且附近已有同类时才采用，不作为普通列表默认方案。
- 静态/配置型列表可以使用 `RecyclerViewVM().Builder().setQuickAdapter(...)`。

不要为了普通列表引入 `PagingSource`、`PagingDataAdapter`、`RemoteMediator` 或新的 BaseAdapter；除非当前代码已有同类模式或用户明确要求，不手写原生 `ListAdapter`、`DiffUtil`、`ItemDecoration` 或额外宽度计算。Adapter 只负责渲染和转发事件，不直接请求网络或访问数据库。

## 单类型列表

ViewModel 或 Dialog 中持有 Adapter，符合当前项目风格：

```kotlin
val paymentAdapter = QuickAdapter<ItemPaymentMethodVm>(R.layout.item_payment_method, emptyList())

fun loadList() {
    request(
        apiBlock = { mRepo.api.xxx(body) },
        onSuccess = {
            paymentAdapter.submitList(data?.map { item ->
                ItemPaymentMethodVm(item).apply {
                    onItemClick.value = {
                        // 页面跳转、弹窗或回调放在 ViewModel 中处理
                    }
                }
            } ?: emptyList())
        }
    )
}
```

item VM 保持轻量，持有展示状态和回调入口：

```kotlin
class ItemPaymentMethodVm(data: PaymentMethod?) : AppItemViewModel<PaymentMethod>(data) {
    val select = ObservableField(false)
}
```

`AppItemViewModel.id()` 返回的是 DataBinding 变量 id，不是列表项业务唯一键；复用 item VM 时仍需显式传入实体的稳定业务 id。

item XML 变量固定为 `item`：

```xml
<layout xmlns:android="http://schemas.android.com/apk/res/android">
    <data>
        <variable
            name="item"
            type="<主业务包名>.feature.xxx.ItemXxxVM" />
    </data>

    <androidx.constraintlayout.widget.ConstraintLayout
        singleClick="@{(v) -> item.onItemClick.invoke(v)}"
        android:layout_width="match_parent"
        android:layout_height="wrap_content">
        ...
    </androidx.constraintlayout.widget.ConstraintLayout>
</layout>
```

## 点击事件

优先让 ViewModel 或 Dialog 组装 item VM 时注入回调。需要跨页面统一处理、多选状态、批量操作或分页列表操作时，再搜索当前 feature 是否已有事件 VM 或 callback 模式：

```kotlin
val zfb = ItemPaymentMethodVm(alipayMethod)
val wx = ItemPaymentMethodVm(wechatMethod)
zfb.onItemClick.value = {
    zfb.select.set(true)
    wx.select.set(false)
}
```

item VM 只暴露动作方法或 `onItemClick`：

```kotlin
class ItemHomeDramaVm(data: WorkInfo?) : AppItemViewModel<WorkInfo>(data)
```

不要在 Adapter 中写业务请求。item VM 也不要直接持有 Activity/Fragment/View；需要跳转、Toast、弹窗、接口请求时，优先通过 ViewModel 注入 callback。

## 数据更新

- 接口返回后把数据映射成 `ItemXxxVM`，再 `submitList(...)`。
- 空列表使用 `emptyList()`，不要传可变的 null。
- 只有强制整体刷新、且本地已有代码明确需要时，才使用 `adapter.refresh()` 或 `notifyDataSetChanged()`。
- 需要局部新增/删除时，可以使用 BaseQuickAdapter 的 `add(...)`、`remove(...)`，但要确保当前列表确实是本地编辑型列表。
- 不要复用同一个可变 `MutableList` 反复改完再提交，优先提交新列表，减少 Diff/刷新异常。

## 多类型列表

当前工程未发现稳定业务混排模板。确实需要混排时，优先搜索 `common/adapter/MultiTypeAdapter.kt`、`QuickViewHolder` 和附近业务实现，再按当前 BaseQuickAdapter 版本补齐：

```kotlin
val adapter = MultiTypeAdapter(object : ViewType<AppItemViewModel<*>> {
    override fun getViewType(position: Int, item: AppItemViewModel<*>?): Int {
        return when (item) {
            is ItemHeaderVM -> 1
            else -> 2
        }
    }

    override fun getLayoutId(viewType: Int): Int {
        return when (viewType) {
            1 -> R.layout.item_header
            else -> R.layout.item_content
        }
    }
})
```

多类型列表适合标题 + 内容 + 操作项这类真实混排。普通同构列表不要为了“可扩展”提前做成多类型。

## RecyclerViewVM 分支

静态选项、配置项、简单横向列表可以沿用 `RecyclerViewVM.Builder().setQuickAdapter(...)`：

```kotlin
val mStyleAdapterVm = RecyclerViewVM()
    .Builder()
    .setQuickAdapter(R.layout.item_selected_tag, tagVms)
    .build()
```

该模式适合 XML 已绑定 `RecyclerViewVM` 的页面。不要为了一个普通页面强行引入 `RecyclerViewVM`，优先跟随附近代码。

## 与分页列表区分

以下情况不要使用本 skill 作为主流程，应使用 `android-paging-state-ui`：

- 接口返回 `PagedList<T>`，或页面有 page/pageSize。
- 需要上拉加载更多、分页 footer、`LoadStateViewHolder`、`SmartRefreshLayout.startRefreshWithAdapter(...)`。
- 使用 `PagingSource`、`Pager(...).flow.cachedIn(viewModelScope)`、`PagingDataAdapter`。

## 审查清单

普通列表完成前检查：

- 单类型列表使用 `QuickAdapter<ItemXxxVM>`，多类型列表先确认当前工程可用的 `MultiTypeAdapter` 或附近同类 Adapter。
- item VM 继承 `AppItemViewModel<T>`，命名符合 `ItemXxxVM`。
- item XML 使用 `<layout>`，变量名是 `item`，类型是 `ItemXxxVM`。
- 自定义 Adapter / PagingDataAdapter 的 ViewHolder 使用 `BindingViewHolder`，并通过 `binding.getVm(...)` 复用 item VM。
- `binding.getVm(...)` 使用稳定、非空、唯一的业务 key，没有使用 position、可变字段或已废弃重载。
- item VM 没有为 XML 可直接读取的实体字段增加无意义透传 getter / Observable 属性。
- 列表更新优先使用 `submitList(newItems)`。
- Adapter 没有网络、数据库、跳转和复杂业务逻辑。
- 复杂列表、多选、批量操作、跨页面刷新或分页列表操作优先通过当前 feature 已有事件 VM 或 callback 转发；简单选择、弹窗列表、静态配置项、局部 item 操作可以使用 item VM callback、`onItemClick`、ViewModel 方法或同类 `setOnItemClickListener`。
- 用户可见文案使用 string 资源，文本样式和颜色优先使用项目 style/attr。
- 普通列表没有误引入 Paging3、RemoteMediator 或新 Base 类。
