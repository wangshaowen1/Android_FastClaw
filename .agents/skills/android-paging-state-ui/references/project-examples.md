# 项目示例与已知问题

> 使用前必须确认示例路径在当前分支真实存在；不存在时，用本文件的搜索命令查找当前分支等价实现，不要机械照搬复制来源项目的路径。

## 首页分页示例

当前项目可参考的分页页面：

- `app/src/main/java/<主业务包路径>/feature/home/HomeFragment.kt`
- `app/src/main/java/<主业务包路径>/feature/home/HomeViewModel.kt`
- `app/src/main/java/<主业务包路径>/feature/home/HomeAdapter.kt`
- `app/src/main/res/layout/fragment_home.xml`
- `app/src/main/res/layout/item_home_drama.xml`

可参考写法：

- `HomeAdapter` 继承 `PagingDataAdapter<WorkInfo, RecyclerView.ViewHolder>`。
- item 绑定通过 `BindingViewHolder` 和 `binding.getVm(...)` 复用 `ItemHomeDramaVm`。
- RecyclerView 通过 `withLoadStateFooter(MyMultiStatePagingFooterAdapter(...))` 添加分页 footer。
- `SmartRefreshLayout.startRefreshWithAdapter(homeAdapter)` 统一处理 refresh 状态、空态和错误态。
- 页面级重试走 `binding.lsv.setRetryListener { ... homeAdapter.refresh() }`。

需要注意：

- `HomeFragment` 当前使用 `lifecycleScope.launch` 收集 `mRealVM.feedFlow`。新 Fragment 收集 Flow 时可优先考虑 `viewLifecycleOwner.lifecycleScope` + `repeatOnLifecycle`，除非附近页面已有更强局部模式。
- `HomeAdapter.areItemsTheSame` 当前比较 `hashCode()`。新代码优先使用稳定 id；没有稳定 id 时再按附近同类取舍。

## Repository + RemoteMediator 示例

当前首页推荐流使用全局缓存和 Room：

- `app/src/main/java/<主业务包路径>/feature/play/data/FeedRepository.kt`
- `app/src/main/java/<主业务包路径>/feature/home/data/VideoRemoteMediator.kt`
- `app/src/main/java/<主业务包路径>/core/db/AppDatabase.kt`
- `app/src/main/java/<主业务包路径>/core/db/dao/WorkDao.kt`
- `app/src/main/java/<主业务包路径>/core/db/entity/WorkInfoEntity.kt`
- `app/src/main/java/<主业务包路径>/core/db/mapper/WorkInfoMapper.kt`

可参考写法：

- `Pager(config, remoteMediator, pagingSourceFactory = { appDatabase.workDao.getPagingData() })`。
- `RemoteMediator` 调用 `ApiService.recommendWorkList(PagedBody(...))`。
- Refresh 时清理缓存并插入第一页。
- Repository 层通过 `pagingData.map { it.toDomain() }` 转回业务模型。
- `cachedIn(repositoryScope)` 用于推荐流跨页面复用；普通页面默认仍优先 `cachedIn(viewModelScope)`。

风险点：

- `RemoteMediator` 改表结构或缓存策略时必须检查 Room migration。
- 如果同一张表未来支持多个列表，需要按 query/user/list type 限定缓存清理范围。
- 新 `RemoteMediator` 必须放行 `CancellationException`，不要把取消当普通错误吞掉。

## 普通 PagingSource 示例

当前项目还有非 Room 缓存的分页源：

- `app/src/main/java/<主业务包路径>/feature/play/data/VideoFeedPagingSource.kt`

已知取舍：

- 该类直接调用 `ApiFactory.apiService.recommendWorkList(...)`。
- 新代码优先把 `mRepo.api` 或 loader lambda 传入 `PagingSource`，除非正在编辑历史类并需要保持同类风格。
- `PagingSource.load()` 应只返回数据，不更新 `LiveData`、Adapter 或页面状态。

## 共享 Helper

- `app/src/main/java/<主业务包路径>/common/ext/PagingExtensions.kt`
- `app/src/main/java/<主业务包路径>/widget/LoadStateViewHolder.java`
- `app/src/main/java/<主业务包路径>/widget/stateful/paging/MyMultiStatePagingFooterAdapter.kt`
- `app/src/main/java/<主业务包路径>/widget/stateful/paging/MultiStatePagingFooterAdapter.kt`

重要行为：

- `SmartRefreshLayout.startRefreshWithAdapter` 内部会调用 `pagingDataAdapter.refresh()`。
- 初始状态会调用 `contentSwitcher.showLoading()`。
- Refresh `LoadState.Error` 且没有已加载 item 时展示页面重试；否则保留内容并展示 toast。
- Refresh `LoadState.NotLoading` 且 `pagingDataAdapter.itemCount == 0` 时展示空状态。
- Append 错误由 `withLoadStateFooter` 添加的 footer 处理。

## 搜索建议

```powershell
rg -n "Pager\\(|PagingDataAdapter|PagingSource|RemoteMediator|withLoadStateFooter|startRefreshWithAdapter" app/src/main/java/<主业务包路径> -g "!**/OssManager.kt"
rg -n "MyMultiStatePagingFooterAdapter|MultiStatePagingFooterAdapter|LoadStateViewHolder" app/src/main/java/<主业务包路径> app/src/main/res/layout
```
