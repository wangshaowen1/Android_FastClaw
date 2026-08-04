# FastClaw 项目索引

只在需要确认复用点或生命周期边界时读取。索引用于定位当前实现，不替代目标文件和附近同类代码。

## Fragment 容器 Adapter

- `app/src/main/java/com/shiyuan/fastclaw/common/adapter/FragmentStateCacheAdapter.kt`：保存预创建的 Fragment 列表；`setFragments()` 不发通知，itemId 来自 Fragment 实例 `hashCode()`。安全默认是初始化时设置一次稳定列表，不支持未经设计的动态增删或重排。
- `app/src/main/java/com/shiyuan/fastclaw/widget/NeedUpdateFragmentStateAdapter.kt`：以位置处理同位置刷新和尾部区间增删，不是任意动态 Tab 的现成替代。

## 现有业务页

- `feature/mine/MyOrderActivity.kt`：直接由 Tab 点击切换 `currentItem`，page callback 更新视觉选中态；callback 在 `onDestroy()` 成对注销。
- `feature/mine/OnlineResumeActivity.kt`：ViewModel 保存当前下标，Tab 点击、Observer 和 page callback 双向同步；写入前比较下标，避免循环更新。
- 上述页面都在初始化时一次性设置两个 Fragment。新页面先选择最接近需求的一种同步方式，不混合两套状态源。

## 指示器与绑定入口

- `common/utils/CommonNavigatorUtils.kt#getCommonNavigatorVp2`：MagicIndicator 与 ViewPager2 的标题点击封装。
- `widget/PillIndicatorView.kt#attachToViewPager2`、`magicindicator/.../ViewPagerHelper.java`：内部注册匿名 callback，未暴露 detach。只适合生命周期与 ViewPager2 一致且不会重复绑定的容器；可复用 Fragment View 或重复 attach 时不要直接套用。
- `core/databinding/BindingAdapters.kt` 中存在 ViewPager2 绑定入口；使用前检查是否会重复注册 callback，以及是否已有对应解绑路径。

## 定位命令

```bash
rg -n "FragmentStateCacheAdapter|NeedUpdateFragmentStateAdapter|ViewPager2|FragmentStateAdapter|registerOnPageChangeCallback|getCommonNavigatorVp2|attachToViewPager2" app magicindicator
```
