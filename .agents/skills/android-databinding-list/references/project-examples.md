# 项目示例

> 使用前必须确认示例路径在当前分支真实存在；不存在时，用本文件的搜索命令查找当前分支等价实现，不要机械照搬复制来源项目的路径。

## 核心基础文件

- `app/src/main/java/<主业务包路径>/common/adapter/QuickAdapter.kt`
- `app/src/main/java/<主业务包路径>/common/adapter/MultiTypeAdapter.kt`
- `app/src/main/java/<主业务包路径>/common/adapter/LoopQuickAdapter.kt`
- `app/src/main/java/<主业务包路径>/base/item/AppItemViewModel.kt`
- `app/src/main/java/<主业务包路径>/base/RecyclerViewVM.kt`
- `app/src/main/java/<主业务包路径>/common/ext/ItemViewModelExtensions.kt`

要点：

- `QuickAdapter<T : QuickItemViewModel>` 内部通过 `QuickViewHolder.bind(item)` 绑定 DataBinding。
- `AppItemViewModel<T>.id()` 固定返回 `BR.item`，所以 item XML 变量名必须是 `item`。
- `RecyclerViewVM.Builder().setQuickAdapter(...)` 适合已有 XML 绑定 `RecyclerViewVM` 的静态/配置型列表。
- `ItemViewModelExtensions.kt` 中基于 position 的 `getVm(...)` 属于历史兼容；新分页/复用场景优先按当前 Adapter 同类写法处理。

## QuickAdapter + submitList 示例

支付方式选择弹窗：

- `app/src/main/java/<主业务包路径>/feature/payment/PaymentMethodDialog.kt`
- `app/src/main/java/<主业务包路径>/feature/payment/ItemPaymentMethodVm.kt`
- `app/src/main/res/layout/dialog_payment_method.xml`
- `app/src/main/res/layout/item_payment_method.xml`

可参考写法：

- Dialog 持有 `QuickAdapter<ItemPaymentMethodVm>(R.layout.item_payment_method, emptyList())`。
- 组装 `ItemPaymentMethodVm` 时注入 `onItemClick.value`，item VM 只保存展示状态 `select`。
- 列表数据使用 `wayAdapter.submitList(data)`。
- item XML 变量名为 `item`，点击使用 `singleClick="@{(v)->item.onItemClick.invoke(v)}"`。

注意：

- Adapter 和 item VM 不直接请求接口。
- 支付、确认、关闭等行为由弹窗本身或 ViewModel 处理。

## PagingDataAdapter + item VM 示例

首页推荐列表是分页场景，不属于普通列表主流程，但可参考 item VM 复用：

- `app/src/main/java/<主业务包路径>/feature/home/HomeAdapter.kt`
- `app/src/main/java/<主业务包路径>/feature/home/ItemHomeDramaVm.kt`
- `app/src/main/res/layout/item_home_drama.xml`

可参考写法：

- `HomeAdapter` 继承 `PagingDataAdapter<WorkInfo, RecyclerView.ViewHolder>`。
- ViewHolder 通过 `binding.getVm(position, item, ItemHomeDramaVm::class.java)` 复用 item VM。
- item XML 变量名为 `item`，点击通过 `item.onItemClick` 转发。

分页链路本身要读 `android-paging-state-ui`，不要用普通列表 skill 生成分页流程。

## MultiTypeAdapter 分支

当前工程存在通用 `common/adapter/MultiTypeAdapter.kt`，但未发现稳定业务侧混排模板。确实需要多类型时：

- 先用 `rg -n "MultiTypeAdapter|ViewType<"` 搜当前分支。
- 确认当前 BaseQuickAdapter 版本和 `QuickViewHolder` 创建方式。
- 只有真实混排需求时才新增，不要把普通同构列表做成多类型。

## RecyclerViewVM 分支

当前工程保留 `RecyclerViewVM.Builder().setQuickAdapter(...)` 能力，但业务侧使用较少。只有目标 XML 已绑定 `RecyclerViewVM` 或同类页面已采用时再沿用。

## 搜索建议

```powershell
rg -n "QuickAdapter<|submitList\\(|setQuickAdapter\\(" app/src/main/java/<主业务包路径> -g "!**/OssManager.kt"
rg -n "MultiTypeAdapter|ViewType<|DefaultViewType" app/src/main/java/<主业务包路径> -g "!**/OssManager.kt"
rg -n "name=\"item\"|singleClick=\"@\\{.*item" app/src/main/res/layout
```
