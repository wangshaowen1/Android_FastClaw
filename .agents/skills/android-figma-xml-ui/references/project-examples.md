# 项目资源索引

> 使用前必须确认示例路径在当前分支真实存在；不存在时，用本文件的搜索命令查找当前分支等价实现，不要机械照搬复制来源项目的路径。

本文件记录 Figma/截图到 Android XML skill 的项目依据。只有不清楚项目常用 style/token/资源示例、候选资源查询不足，或需要新增资源时才读取；不要每次 UI 还原前默认读取。

## 主题与样式

- `app/src/main/res/values/attrs.xml`
  - 主题色：`colorBrand`、`colorBrandLight`、`colorBrandSubtle`。
  - 功能色：`funcColorSuccess`、`funcColorWarning`、`funcColorFailure`、`funcColorLink`、`funcColorSpecial`。
  - 填充色：`fillPrimary`、`fillSecondary`、`fillTertiary`、`fillQuaternary`。
  - 文本色：`textColorPrimary`、`textColorTitle`、`textColorSecondary`、`textColorHint`、`textColorDisabled`。
  - 背景色：`bgPrimary`、`bgSurface`、`bgSurfaceVariant`、`bgSurfaceContainer`、`bgSurfaceContainerHigh`、`bgSecondary`。
  - 描边色：`outlineSubtle`、`outlineDefault`、`outlineVariant`、`outlineMedium`。
- `app/src/main/res/values/styles.xml`
  - `AppTheme.Base` 中绑定了上述语义 token；不要新增 `themeColor1`、`textColor2`、`bgColor3` 这类数字别名。
  - 字重 style：`Text.Bold`、`Text.Medium`，以及对应语义字号样式自带的加粗配置。
  - 字号 style：`Text.Display`、`Text.TitleLarge`、`Text.Title`、`Text.SubtitleLarge`、`Text.Subtitle`、`Text.Body`、`Text.BodySmall`、`Text.Caption`、`Text.CaptionSmall`、`Text.Label`。
  - 常见按钮 style：`Button.Medium`、`Button.Bold`、`Button.Bold.Gradient`。
- `app/src/main/res/values/dimens.xml`
  - 通用间距：`spacing_xs`、`spacing_s`、`spacing_m`、`spacing_l`、`spacing_xl`。
  - 通用圆角：`radius_s`、`radius_m`、`radius_l`、`radius_xl`。
  - 主导航高度：`main_bottom_nav_height`。

## XML 示例

- `app/src/main/res/layout/view_model_toolbar.xml`
  - Toolbar 标题和按钮文本使用 `@style/Text.Subtitle`、`@style/Text.Medium`、`?attr/textColorPrimary`。
- `app/src/main/res/layout/fragment_home.xml`
  - 首页区域使用 `@style/Text.SubtitleLarge`、`@style/Text.Subtitle`、`?attr/textColorPrimary` 和 `@dimen/spacing_*`。
- `app/src/main/res/layout/item_home_drama.xml`
  - item 使用 `@style/TextAppearance.AIChip`、`@style/Text.Label`、`@style/Text.Caption`、`?attr/textColorPrimary`、`@dimen/spacing_s` 和 `loadThumbnailUri`。
- `app/src/main/res/layout/dialog_payment_method.xml`
  - 支付方式底部弹窗和 item 可作为简单列表弹窗参考。
- `app/src/main/res/layout/activity_settings.xml`
  - 设置项大量使用 `singleClick="@{layout.clickHandler::onClick}"`、`@style/Text.Body`、`?attr/textColorSecondary` 和通用 spacing。
- `app/src/main/res/layout/fragment_mine.xml`
  - 我的页头像、文案、设置入口使用 `loadUri`、`singleClick`、项目 Text style 和 attr 色值。
- `app/src/main/res/layout/multi_state_adapter_view.xml`
  - 状态 UI 文案使用项目 Text style。

## BindingAdapter

- `app/src/main/java/<主业务包路径>/core/databinding/BindingAdapters.kt`
  - 图片：`loadUri`、`loadThumbnailUri`、`loadHQThumbnailUri`、`loadVideoUrl`。
  - 尺寸/边距：`setLayoutWidth`、`setLayoutHeightDp`、`layoutHeight`、`layoutWidth`、`marginTopDp`、`marginBottomDp`。
  - 点击：`singleClick`、`navigationClick`。
  - 状态/可见性：`setVisible`、`animateVisibility`、`setLoadState`。

## Drawable 命名示例

优先复用已有通用资源：

- 胶囊：`bg_10_ffffff_r50.xml`、`bg_07c160_r50.xml`、`bg_ffffff_r100.xml`、`bg_ececf0_r100.xml`。
- 圆角背景：`bg_06_ffffff_r12.xml`、`bg_06_ffffff_r8.xml`、`bg_2a2a2b_r20.xml`、`bg_ffffff_r16.xml`。
- 顶部圆角：`bg_121212_tr16.xml`、`bg_141414_tr20.xml`、`bg_f5f5f5_tr16.xml`。
- 渐变：`gradient_left_73b0f8_right_7567f0_r100.xml`、`gradient_left_50_73b0f8_right_50_7567f0_r100.xml`、`gradient_top_00_000000_bottom_60_000000.xml`。
- 功能语义背景：当前需先搜索确认；不要沿用复制来源项目的 `shape_bg_publish_task.xml`、`shape_bg_story_upload.xml` 等旧资源名。
- 主题色背景优先复用已有 selector/button 资源；新增通用背景按色值/圆角命名，不再新增 `theme1` 这类数字语义文件名。

新增 drawable 时，如果是通用资源，按值命名；如果只服务某个界面且语义强，按 `shape_bg_<功能>_<状态或角色>.xml`。

## 搜索命令

```powershell
rg -n "colorBrand|textColorPrimary|bgPrimary|fillPrimary|outlineDefault" app/src/main/res/values
rg -n "Text\\.Display|Text\\.Title|Text\\.Subtitle|Text\\.Body|Text\\.Caption|Text\\.Label" app/src/main/res/values/styles.xml
rg --files app/src/main/res/drawable -g "bg_*.xml" -g "gradient_*.xml" -g "shape_bg_*.xml" -g "selector_*.xml"
rg -n "loadUri|loadThumbnailUri|loadHQThumbnailUri|loadVideoUrl|singleClick" app/src/main/java/<主业务包路径>/core/databinding/BindingAdapters.kt app/src/main/res/layout
```
