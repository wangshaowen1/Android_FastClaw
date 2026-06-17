# 项目示例

> 使用前必须确认示例路径在当前分支真实存在；不存在时，用本文件的搜索命令查找当前分支等价实现，不要机械照搬复制来源项目的路径。

## 基类和工具入口

常见外部基类：

- `com.nhsd.basic.BaseDialogFragment`
- `com.nhsd.basic.BaseVMDialogFragment`
- `com.nhsd.basic.BaseBottomPushDialogFragment`
- `com.nhsd.basic.BaseBottomPushVMDialogFragment`

项目内工具：

- `app/src/main/java/<主业务包路径>/widget/CommonDialog.kt`
- `app/src/main/java/<主业务包路径>/common/ext/DialogExt.kt`
- `app/src/main/java/<主业务包路径>/base/BaseBlurDialogFragment.kt`
- `app/src/main/java/<主业务包路径>/base/BaseBlurVMDialogFragment.kt`

说明：

- `CommonDialog` / `DialogExt` 适合普通确认和提示。
- `BaseBlurDialogFragment` / `BaseBlurVMDialogFragment` 是项目自定义 blur 分支，只有附近已有同类需求或设计明确要求 blur 背景时参考，不作为默认弹窗基类。

## BaseDialogFragment 示例

简单中心弹窗：

- `app/src/main/java/<主业务包路径>/feature/splash/PolicyDialog.kt`
- `app/src/main/java/<主业务包路径>/feature/login/PrivacyPolicyDialog.kt`
- `app/src/main/java/<主业务包路径>/feature/login/flash/AccountExclusionDialog.kt`
- `app/src/main/java/<主业务包路径>/widget/LoadingDialog2.java`

可参考写法：

- 无复杂页面状态和接口时使用 `BaseDialogFragment<VB>`。
- 参数通过 `arguments` 或属性 callback 传递，先跟随当前弹窗已有模式。
- 简单中心弹窗不要新增 ViewModel。
- 关闭按钮只关闭弹窗，不要误触发确认回调。

## BaseVMDialogFragment 示例

带 ViewModel 的中心弹窗：

- `app/src/main/java/<主业务包路径>/feature/upgrade/UpgradeDialogFragment.kt`
- `app/src/main/java/<主业务包路径>/feature/login/flash/FlashLoginDialogFragment.kt`
- `app/src/main/java/<主业务包路径>/feature/settings/customerservice/CustomerServiceDialog.kt`
- `app/src/main/java/<主业务包路径>/feature/settings/customerservice/CustomerServiceViewModel.kt`

可参考写法：

- 弹窗包含升级状态、登录流程、客服数据或接口请求时使用 `BaseVMDialogFragment<VB, VM>`。
- ViewModel 通过 `mBundle` 读取参数。
- Dialog 负责按钮、观察事件、关闭；支付/登录/下载/倒计时逻辑优先在 ViewModel。
- 长任务 `Job` 在 ViewModel 中保存并在 `onCleared()` 取消。

## BaseBottomPushDialogFragment 示例

无 ViewModel 的底部弹窗：

- `app/src/main/java/<主业务包路径>/feature/payment/PaymentMethodDialog.kt`
- `app/src/main/java/<主业务包路径>/widget/BottomDatePickerDialog.java`
- `app/src/main/java/<主业务包路径>/widget/BottomTimePickerDialog.java`
- `app/src/main/java/<主业务包路径>/widget/AreaPicker.java`
- `app/src/main/java/<主业务包路径>/widget/DataPicker.java`
- `app/src/main/java/<主业务包路径>/widget/actionsheet/ActionSheetDialog.java`

可参考写法：

- 底部选项、支付方式选择、日期/时间/地区选择使用 `BaseBottomPushDialogFragment<VB>`。
- `PaymentMethodDialog.show(...)` 返回 dialog 实例，调用方可设置 `onConfirmClick`。
- 简单列表使用 `QuickAdapter<ItemPaymentMethodVm>`，item 点击只更新选择状态。

## BaseBottomPushVMDialogFragment 示例

带 ViewModel 的底部弹窗：

- `app/src/main/java/<主业务包路径>/feature/login/phone/PhoneLoginDialog.kt`
- `app/src/main/java/<主业务包路径>/feature/login/unified/UnifiedLoginDialog.kt`
- `app/src/main/java/<主业务包路径>/feature/settings/report/ReportDialog.kt`
- `app/src/main/java/<主业务包路径>/feature/settings/report/ReportDialogViewModel.kt`

可参考写法：

- 底部弹窗包含接口提交、登录状态、复杂输入或列表选择时使用 `BaseBottomPushVMDialogFragment<VB, VM>`。
- 参数通过 `arguments` 传入，ViewModel 用 `mBundle` 读取。
- `ReportDialog` 点击提交后调用 `mRealVM.submit(...)`，观察 `isSubmit` 成功后 toast 并关闭。
- 登录弹窗按当前 `AppNavigator` 和登录 ViewModel 流程处理，不在弹窗里散落第三方登录细节。

## 通用确认弹窗示例

- `app/src/main/java/<主业务包路径>/common/ext/DialogExt.kt`
- `app/src/main/java/<主业务包路径>/widget/CommonDialog.kt`
- `app/src/main/java/<主业务包路径>/feature/settings/SettingsActivity.kt`
- `app/src/main/java/<主业务包路径>/feature/settings/logoff/LogoffActivity.kt`
- `app/src/main/java/<主业务包路径>/feature/settings/logoff/ApplyLogOffActivity.kt`

适合场景：

- 退出登录确认。
- 注销账号确认。
- 单按钮提示。
- 普通文案确认，无自定义复杂布局。

## 搜索建议

```powershell
rg -n "BaseDialogFragment|BaseVMDialogFragment|BaseBottomPushDialogFragment|BaseBottomPushVMDialogFragment" app/src/main/java/<主业务包路径> -g "!**/OssManager.kt"
rg -n "showCommonDialog\\(|showConfirmPrompt\\(|CommonDialog\\(" app/src/main/java/<主业务包路径> -g "!**/OssManager.kt"
rg -n "var on[A-Z].*\\(|callback:|onConfirm|onCancel|onSave|on.*Selected|isSubmit" app/src/main/java/<主业务包路径> -g "!**/OssManager.kt"
```
