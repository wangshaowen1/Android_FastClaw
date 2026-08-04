---
name: android-paging-state-ui
description: "Android Paging3 分页加载与状态 UI 实现规范。仅在本次实际涉及分页列表、Paging3、PagingSource、PagingDataAdapter、RemoteMediator、SmartRefreshLayout 分页刷新、LoadStateViewHolder、MyMultiStatePagingFooterAdapter、分页 empty/error/loading/footer/loadState UI、分页 retry 或 Room 缓存分页时使用。普通非分页列表状态 UI 不触发本 skill。"
---

# Android 分页状态 UI

多 Tab + ViewPager2 Fragment 容器同时读取 `android-tab-viewpager`；本 skill 只负责每个子 Fragment 内的分页链路。

## 核心规则

默认使用 `PagingSource + PagingDataAdapter + LoadStateViewHolder`。

只有用户明确需要 Room 数据库缓存、离线缓存、本地优先展示或跨会话列表持久化时，才加入 `RemoteMediator`。`VideoRemoteMediator` 和其他 `RemoteMediator` 示例属于数据库缓存分支，不是默认分支。

实现或审查分页页面时，先查当前 feature 或相近页面的现有分页实现。只有找不到同类实现、不确定项目分页组件用法、需要新增完整分页页面，或发现分页状态/缓存/生命周期问题时，才读取 [project-examples.md](references/project-examples.md) 对齐当前项目示例和已知坑点。

## 默认结构

使用现有项目栈：

- 页面：`BaseViewModelActivity<VB, VM>` 或 `BaseViewModelFragment<VB, VM>`。
- ViewModel：需要 API 访问时使用 `AppRepositoryViewModel<AppRepository>`。
- XML：`<layout>` 根节点，页面变量名为 `layout`。
- 刷新和状态容器：新页面默认 `SmartRefreshLayout` -> `LoadStateViewHolder` -> `RecyclerView`；当前 feature 已使用 `CoolRefreshView` 等项目容器时，沿用对应现有扩展，同一页面不要混用两套刷新状态链路。
- Adapter：`PagingDataAdapter<T, RecyclerView.ViewHolder>`。
- Footer：`MyMultiStatePagingFooterAdapter`。

## XML 模式

除非附近 feature 已有更强局部模式，否则使用以下结构：

```xml
<com.scwang.smart.refresh.layout.SmartRefreshLayout
    android:id="@+id/srl"
    android:layout_width="match_parent"
    android:layout_height="0dp"
    app:srlAccentColor="@color/white"
    app:srlPrimaryColor="@color/transparent">

    <<主业务包名>.widget.LoadStateViewHolder
        android:id="@+id/lsv"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        app:noData="@string/no_data">

        <androidx.recyclerview.widget.RecyclerView
            android:id="@+id/recycler_view"
            android:layout_width="match_parent"
            android:layout_height="match_parent"
            android:clipToPadding="false"
            android:overScrollMode="never"
            android:scrollbars="none" />
    </<主业务包名>.widget.LoadStateViewHolder>
</com.scwang.smart.refresh.layout.SmartRefreshLayout>
```

全页 `LoadStateViewHolder` 内的 `RecyclerView` 优先使用 `match_parent`。只有列表刻意嵌套在可测量父容器中，并且外层布局明确处理滚动时，才使用 `wrap_content`。

## ViewModel 模式

在 ViewModel 中创建 pager，并使用 `viewModelScope` 缓存。

```kotlin
fun getXxxPage(): Flow<PagingData<XxxInfo>> {
    return Pager(
        config = PagingConfig(
            initialLoadSize = Constant.Paged.PAGE_SIZE,
            pageSize = Constant.Paged.PAGE_SIZE,
            prefetchDistance = Constant.Paged.PREFETCH_DISTANCE,
        ),
        pagingSourceFactory = {
            XxxPagingSource(
                api = mRepo.api,
                queryType = queryType
            )
        }
    ).flow.cachedIn(viewModelScope)
}
```

如果查询条件会变化，使用 `StateFlow.distinctUntilChanged().flatMapLatest { Pager(...) }` 或当前项目已有 reload trigger，再接 `cachedIn(viewModelScope)`。不要用 getter 每次访问都创建新 Pager，也不要让一个固定 Pager 捕获之后会变化的普通字段。

## PagingSource 规则

`PagingSource` 只专注加载分页数据，不要在 `load()` 中更新 `ObservableBoolean`、`LiveData`、Adapter 状态或其他 UI 状态。

优先把 `mRepo.api` 或小型 loader lambda 传入 `PagingSource`。除非正在编辑已经沿用该风格的旧类，新代码避免直接使用 `ApiFactory.apiService`。

```kotlin
class XxxPagingSource(
    private val api: ApiService,
    private val queryType: Int,
) : PagingSource<Int, XxxInfo>() {

    override fun getRefreshKey(state: PagingState<Int, XxxInfo>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            val anchorPage = state.closestPageToPosition(anchorPosition)
            anchorPage?.prevKey?.plus(1) ?: anchorPage?.nextKey?.minus(1)
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, XxxInfo> {
        return try {
            val pageNum = params.key ?: 1
            val response = api.xxxPage(XxxPagedBody(pageNum = pageNum, pageSize = params.loadSize))
            if (!response.isSuccess) {
                return LoadResult.Error(ApiException(response.code, response.msg))
            }

            val items = response.data?.items?.filterNotNull().orEmpty()
            val prevKey = if (pageNum > 1) pageNum - 1 else null
            val nextKey = if (items.isEmpty() || items.size < params.loadSize) null else pageNum + 1
            LoadResult.Page(data = items, prevKey = prevKey, nextKey = nextKey)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }
}
```

如果后端忽略 `params.loadSize` 并总是使用固定 page size，判断是否还有更多数据时要和实际请求 page size 对比。

## 页面初始化

Adapter、footer、状态容器和数据收集要一起初始化。

```kotlin
private val xxxAdapter by lazy { XxxAdapter(eventVm) }

private fun initView() {
    binding.recyclerView.apply {
        itemAnimator = null
        adapter = xxxAdapter.withLoadStateFooter(
            MyMultiStatePagingFooterAdapter(
                hostAdapter = xxxAdapter,
                noData = getString(R.string.no_data),
                noMoreThreshold = 12
            ) {
                xxxAdapter.retry()
            }
        )
        applyGridSpanSizeLookup()
    }

    binding.srl.startRefreshWithAdapter(
        pagingDataAdapter = xxxAdapter,
        loadStateHolder = binding.lsv
    )
}

private fun dataUpdate() {
    lifecycleScope.launch {
        repeatOnLifecycle(Lifecycle.State.STARTED) {
            mRealVM.getXxxPage().collectLatest { pagingData ->
                xxxAdapter.submitData(pagingData)
            }
        }
    }
}
```

Fragment 收集 Paging/Flow 数据时可以使用 `viewLifecycleOwner.lifecycleScope` 和 `viewLifecycleOwner.repeatOnLifecycle(...)`。这只约束数据收集生命周期；`onPrepareView()` 中手动注册的 AdapterDataObserver、RecyclerView/ViewPager callback、键盘监听等，先检查当前 baselibrary、基类 View 缓存和附近同类实现，再在真实配对生命周期解除并清空引用。FastClaw 不少页面使用 `onDestroy()`，不要将其硬套到其他基线。

不要把 `onRefreshListener = { adapter.refresh() }` 传给 `startRefreshWithAdapter`；该扩展已经会调用 `pagingDataAdapter.refresh()`。`onRefreshListener` 只用于重启轮询、刷新头部数据等额外工作。

## Adapter 规则

Adapter 只负责展示：

- 继承 `PagingDataAdapter<T, RecyclerView.ViewHolder>`。
- ViewHolder 优先继承 `BindingViewHolder<VB>`，不要手写 `RecyclerView.ViewHolder(binding.root)`，除非同类旧代码已明确采用该写法。
- `areItemsTheSame` 使用稳定业务 id。
- `areContentsTheSame` 默认使用 `oldItem == newItem`，除非有明确理由只比较特定字段。
- 现有 item layout 使用 DataBinding 时，XML `item` 变量优先绑定 `ItemXxxVM`，通过 `binding.getVm(...)` 获取或复用 item VM，不直接绑定接口实体或临时 UI data class。
- `binding.getVm(...)` 使用稳定、非空、唯一的业务 key，不使用 position、可变展示字段或可能为 null 的 id；项目已废弃 position 重载，新代码不得继续调用。
- XML 可以直接读取 `item.data.xxx` 的简单字段、空值兜底、颜色或可见性表达式，不要在 item VM 增加无意义的透传 getter / Observable 属性。
- 分页 item 的复杂点击/选择/操作、多选、跨页面刷新或批量操作优先通过当前 feature 已有事件 VM、callback 或 ViewModel 方法转发，由 Fragment/Activity/ViewModel 处理业务；简单局部 item 操作可以沿用当前 feature 的 item VM callback 或同类 `onItemClick` 写法。Adapter 不直接请求网络、跳转或执行业务。

## 状态 UI 规则

使用项目 helper，不要手写 loading/empty/error 切换：

- `SmartRefreshLayout.startRefreshWithAdapter(...)`
- `LoadStateViewHolder.showLoading()`
- `LoadStateViewHolder.showContent()`
- `LoadStateViewHolder.showEmpty()`
- `LoadStateViewHolder.showRetry(...)`
- `MyMultiStatePagingFooterAdapter`

页面级状态由 `startRefreshWithAdapter` 中的 `adapter.loadState.refresh` 驱动。

Footer 状态通过 `withLoadStateFooter(...)` 由 `append` 驱动。

网格列表设置带 footer 的 Adapter 后，调用 `applyGridSpanSizeLookup()`，确保 footer/空状态横跨所有列。

如果页面通过 `ConcatAdapter` 添加非分页 header，需要明确验证空状态行为。`startRefreshWithAdapter` 使用 paging adapter 的 item count，不是 concat adapter 总数。

## 多 Tab 分页边界

容器、Fragment 列表稳定性和 Tab/页面同步由 `android-tab-viewpager` 负责。分页侧只保留：每个子 Fragment 独自持有 ViewModel、Pager、PagingSource、Adapter 和 LoadState；Tab 参数从自身 `arguments` / `mBundle` 读取，容器不共享 `PagingData` 或可变查询参数。多 Tab 本身不是新增 Repository 的理由。

## RemoteMediator 分支

只有需求包含数据库缓存时，才使用 `RemoteMediator`。

使用时：

- API、DAO 和 database 依赖保持从 `mRepo` 传入。
- 使用 `database.withTransaction`。
- `LoadType.REFRESH` 只清理当前作用域缓存。
- 存储 page key 或 remote key，保证 `APPEND` 可靠。
- `ApiResponse` 失败时返回 `MediatorResult.Error(ApiException(...))`。
- 重新抛出 `CancellationException`。
- 在 Adapter 外把 Room entity 映射成当前页面实际消费的已有模型。只有项目真实存在对应层时才使用 `toDomain()`；默认不为分页新增 Domain/UiModel 层。

不要为了让普通网络列表看起来更“架构化”而新增 Room entity、DAO、mapper 或 `RemoteMediator`。

## 审查清单

分页页面完成前检查：

- `startRefreshWithAdapter` 只调用一次，并且没有重复调用 `adapter.refresh()`。
- Fragment 收集数据使用合适的 view 生命周期；手动回调按当前 baselibrary、基类 View 缓存和附近同类实现选择真实清理点。
- `PagingSource.load()` 没有 UI 副作用。
- 除非为了保留附近历史风格，新代码避免直接使用 `ApiFactory.apiService`。
- 没有吞掉 `CancellationException`。
- API 失败转成 `LoadResult.Error(ApiException(...))`。
- 全页列表中的 `RecyclerView` 没有误用 `wrap_content`。
- DataBinding item XML 绑定的是 `ItemXxxVM`，Adapter ViewHolder 使用 `BindingViewHolder` 和 `binding.getVm(...)`。
- `binding.getVm(...)` 使用稳定、非空、唯一的业务 key，没有使用 position、可变字段或废弃重载，也没有增加无意义 item VM 透传属性。
- 可变查询条件使用 `distinctUntilChanged + flatMapLatest` 或当前 reload trigger，没有反复创建 Pager 或捕获失效字段。
- 复杂 item 点击/选择/操作没有直接落在 Adapter lambda 中；多选、跨页面刷新、批量操作或分页业务动作优先通过当前 feature 已有事件 VM、callback 或 ViewModel 方法转发，简单局部 item 操作可沿用当前 feature 的 callback / `onItemClick` 写法。
- Footer 重试使用 `adapter.retry()`，页面重试使用 `adapter.refresh()`。
- 网格/瀑布流 footer 横跨全宽。
- 空/错/加载文案和图标使用 string/drawable 资源与项目 style。
- 多 Tab 分页时，每个子页独立持有分页链路；容器规则已按需读取 `android-tab-viewpager`。
