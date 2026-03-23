package com.kgmedia.fastfilm.feature.works

import android.graphics.Color
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.View
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.blankj.utilcode.util.ActivityUtils
import com.kgmedia.fastfilm.R
import com.kgmedia.fastfilm.common.ext.showConfirmPrompt
import com.kgmedia.fastfilm.common.ext.startRefreshWithAdapter
import com.kgmedia.fastfilm.common.ext.topActivity
import com.kgmedia.fastfilm.core.Config
import com.kgmedia.fastfilm.core.Events
import com.kgmedia.fastfilm.core.User
import com.kgmedia.fastfilm.core.databinding.loadGifUrl
import com.kgmedia.fastfilm.core.db.entity.WorkInformationInfo
import com.kgmedia.fastfilm.core.ext.onUserStatusChanged
import com.kgmedia.fastfilm.core.network.apis.entity.EpisodeInfo
import com.kgmedia.fastfilm.core.network.apis.entity.WorkInfo
import com.kgmedia.fastfilm.core.network.apis.response.ActivityConfigData
import com.kgmedia.fastfilm.core.network.apis.response.FloatWindowInfo
import com.kgmedia.fastfilm.core.network.apis.response.PopupInfo
import com.kgmedia.fastfilm.databinding.FragmentMyWorksBinding
import com.kgmedia.fastfilm.feature.creator.generate.StoryEditorActivity
import com.kgmedia.fastfilm.feature.main.ActivityDialog
import com.kgmedia.fastfilm.feature.main.MainActivity
import com.kgmedia.fastfilm.feature.main.MainViewModel
import com.kgmedia.fastfilm.feature.play.DramasPlayPagerActivity
import com.kgmedia.fastfilm.feature.play.data.FeedRepository
import com.kgmedia.fastfilm.feature.series.SeriesActivity
import com.kgmedia.fastfilm.router.AppRouter
import com.kgmedia.fastfilm.widget.decoration.BottomSpaceDecoration
import com.kgmedia.fastfilm.widget.decoration.SpaceDecoration
import com.kgmedia.fastfilm.widget.stateful.applyGridSpanSizeLookup
import com.kgmedia.fastfilm.widget.stateful.paging.MyMultiStatePagingFooterAdapter
import com.nhsd.basic.BaseViewModelFragment
import com.nhsd.basic.bus.FlowEventBus
import com.nhsd.basic.bus.observeEvents
import com.nhsd.basic.ext.dp
import com.nhsd.basic.ext.getResDrawable
import com.nhsd.basic.ext.getViewModelOfActivity
import kotlinx.coroutines.launch

class MyWorksFragment : BaseViewModelFragment<FragmentMyWorksBinding, MyWorksViewModel>() {


    companion object{
        const val TAG = "MyWorksFragment"
        @JvmStatic
        fun start() {
            FlowEventBus.postSync(Events.MainTabEvent(Events.MainTabEvent.TAB_MY_WORKS))
            ActivityUtils.finishToActivity(MainActivity::class.java, false)
        }
    }

    //上一个的登录id
    private var userId: String? = null

    private val myWorksAdapter by lazy {
        MyWorksAdapter(eventVm = eventVm)
    }
    private val eventVm by lazy {
        ItemMultipleChoiceEventsViewModel<MyWorksEvent, String>()
    }
    private val parentVm by lazy {
        getViewModelOfActivity<MainViewModel>()
    }

    //当前点击的作品信息
    var workInfo: WorkInfo? = null

    override fun onPrepareView() {
        super.onPrepareView()
        initView()
        //数据更新
        dataUpdate()
    }

    override fun onStart() {
        super.onStart()
        mRealVM.isShow.set(true)
        mRealVM.startChecking()
        refresh()
    }

    override fun onStop() {
        super.onStop()
        mRealVM.isShow.set(false)
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (hidden) return
        if (isShowReception()) {
            mRealVM.isShow.set(true)
            mRealVM.startChecking()
            //更新必要信息
            refresh()
        } else {
            mRealVM.isShow.set(false)
        }
    }

    private fun refresh() {
        myWorksAdapter.refresh()
    }

    private fun initView() {
        binding.recyclerView.apply {
            addItemDecoration(SpaceDecoration(12.dp))
            addItemDecoration(BottomSpaceDecoration(80.dp))
            val noMoreThreshold = 2
            itemAnimator = null
            val myMultiStatePagingFooterAdapter = MyMultiStatePagingFooterAdapter(
                myWorksAdapter,
                noMoreThreshold = noMoreThreshold,
                noData = getString(R.string.empty_creation_history)
            ) {
                myWorksAdapter.retry()
            }
            adapter = myWorksAdapter.withLoadStateFooter(myMultiStatePagingFooterAdapter)
            binding.recyclerView.applyGridSpanSizeLookup()
        }
        binding.srl.startRefreshWithAdapter(
            pagingDataAdapter = myWorksAdapter,
            loadStateHolder = binding.lsv,
            onRefreshListener = {
                mRealVM.startChecking()
            }
        )
    }

    private fun dataUpdate() {
        parentVm?.activityConfigCache?.observe(this) {
            handlePopup(it.getPopupInfo(ActivityConfigData.TAB_MY_WORKS))
            handleFloatWindow(it.getFloatWindowInfo(ActivityConfigData.TAB_MY_WORKS))
        }
        observeEvents {
            //登录状态变化
            onUserStatusChanged {
                if (userId == User.get().id) return@onUserStatusChanged
                userId = User.get().id
                if (userId.isNullOrEmpty()) {
                    mRealVM.deleteAll()
                } else {
                    myWorksAdapter.refresh()
                }
            }
        }

        lifecycleScope.launch {
            mRealVM.getWorkInformation().collect {
                myWorksAdapter.submitData(it)
            }
        }

        lifecycleScope.launch {
            eventVm.events.collect { event ->
                when (event) {
                    is MyWorksEvent.ViewDetails -> {
                        if (event.workInfo?.isNotCreatedEpisode() == true) {
                            //作品类型：3-小说上传 且是草稿状态
                            mRealVM.getWorkDtl(workId = event.workInfo.workId)
                            return@collect
                        }
                        if (event.workInfo?.totalEpisodeCnt == 1) {
                            //这个是单集的
                            if (event.workInfo.workStatus == WorkInfo.COMPLETED_STATE) {
                                //已经完成了，直接播放
                                workInfo = event.workInfo
                                mRealVM.getEpisodes(event.workInfo.workId)
                                return@collect
                            }
                            AppRouter.singleEpisodeSeriesDetails(event.workInfo)
                            return@collect
                        }
                        //查看详情
                        SeriesActivity.start(workId = event.workInfo?.workId)
                    }

                    is MyWorksEvent.ComposeState -> {
                        //编辑下选择
                        mRealVM.selectedCount.value = eventVm.getSelectedItems().size
                    }
                }
            }
        }

        mRealVM.workDtl.observe(this) {
            if (it == null) return@observe
            mRealVM.workDtl.value = null
            if (it.isInspirationUploadMode()) {
                //查看详情
                SeriesActivity.start(workId = it.workId)
                return@observe
            }
            StoryEditorActivity.restore(
                context = topActivity,
                step = 1,
                workId = it.workId,
                storyId = null,
                workTitle = it.workTitle,
                chapterIndex = 1,
                workType = it.workType,
                model = AppRouter.getModel(workType = it.workType, sortNo = 1),
                videoRatio = it.workRatio,
                from = StoryEditorActivity.FROM_MY_WORKS
            )
        }

        getViewModelOfActivity<MainViewModel>()?.setIsEditing = { status ->
            mRealVM.isView.value = true
        }

        mRealVM.isView.observe(this) {
            eventVm.switchStatus(it)
            getViewModelOfActivity<MainViewModel>()?.isEditing?.set(!it)
            if (it) {
                //是否启用下拉刷新
                binding.srl.setEnableRefresh(true)
                binding.groupView.visibility = View.VISIBLE
                binding.groudEdit.visibility = View.GONE
                eventVm.clearAllSelection()
                getViewModelOfActivity<MainViewModel>()?.showBottomBar?.set(true)
                return@observe
            }
            binding.srl.setEnableRefresh(false)
            getViewModelOfActivity<MainViewModel>()?.showBottomBar?.set(false)
            binding.groupView.visibility = View.GONE
            binding.groudEdit.visibility = View.VISIBLE
        }

        mRealVM.selectedCount.observe(this) {count ->
            if (count <= 0) {
                binding.tvSelectCount.text = getString(R.string.selected_items_format, "0")
                    return@observe
            }
            val spannableString = SpannableString(getString(R.string.selected_items_format, count.toString()))
            spannableString.setSpan(
                ForegroundColorSpan(
                    Color.WHITE
                ), 3, spannableString.length - 1,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            binding.tvSelectCount.text = spannableString
        }

        mRealVM.episodeList.observe(this) {
            if (it.isNullOrEmpty()) return@observe
            mRealVM.episodeList.value = null
            onPlayEpisodeClicked(workInfo = workInfo, episodes = it, clickPosition = 0)
        }
    }

    fun onPlayEpisodeClicked(workInfo: WorkInfo?, episodes: List<EpisodeInfo>, clickPosition: Int) {
        // 1. [核心步骤] 过滤未完成的数据 + 转换为 Adapter 需要的 WorkInfo
        val validPlayList = episodes
            .filter { !it.videoUrl.isNullOrEmpty() } // 过滤条件
            .map { episode ->
                episode.workId = workInfo?.workId
                // 复用父作品信息，包装成 WorkInfo
                WorkInfo(
                    workId = episode.storyId ?: "", // 必须唯一
                    userId = User.get().userId,
                    workTitle = workInfo?.workTitle,
                    workCoverUrl = workInfo?.workCoverUrl,
                    workDesc = workInfo?.workDesc,
                    workStoryItemList = listOf(episode), // 这一集的数据
                    lastEpisodeNo = episode.sortNo,
                    totalEpisodeCnt = workInfo?.totalEpisodeCnt,
                    playCount = episode.playCount,
                )
            }

        // 2. 计算修正后的点击位置
        // 因为过滤了数据，原来的 position 可能变了，需要找到点击的那个 episode 在新列表里的位置
        val clickedEpisodeId = episodes[clickPosition].storyId
        val newPosition =
            validPlayList.indexOfFirst { it.workId == clickedEpisodeId }.coerceAtLeast(0)
        // 3. 存入 Repository
        FeedRepository.setPreloadedSeries(workInfo?.workId ?: "", validPlayList)

        // 4. 跳转播放页
        DramasPlayPagerActivity.start(
            workId = workInfo?.workId,
            // 传入修正后的位置
            position = newPosition
        )
    }


    override fun <Data> onClick(view: View, data: Data?) {
        super.onClick(view, data)
        when (view.id) {
            R.id.btnEdit -> {
                //编辑
                mRealVM.isView.value = false
            }

            R.id.btnSelectAll -> {
                //全选
                val ids = hashSetOf<String>()
                val items: List<WorkInformationInfo> = myWorksAdapter.snapshot().items
                items.forEach { w -> ids.add(w.work.workId ?: "") }
                eventVm.setSelectedItems(ids)
                mRealVM.selectedCount.value = ids.size
            }

            R.id.btnDelete -> {
                //删除
                if (eventVm.getSelectedItems().isEmpty()) {
                    mRealVM.isView.value = true
                    return
                }
                topActivity?.showConfirmPrompt(
                    title = getString(R.string.confirm_delete_work),
                    cancelText = getString(R.string.cancel),
                    confirmText = getString(R.string.confirm_ok)
                ) {
                    mRealVM.delete(eventVm.getSelectedItems().toList())
                }
            }

            R.id.btnComplete -> {
                //完成
                mRealVM.isView.value = true
            }
        }
    }

    private fun handlePopup(popup: PopupInfo?) {
        popup?.let {
            if (popup.isShow()) {
                ActivityDialog.show(popup)
                Config.get().activityPopupShowMark(popup.id)
            }
        }
    }

    private fun handleFloatWindow(float: FloatWindowInfo?) {
        if (float != null && float.isShow()) {
            binding.floatingBall.isVisible = true
            loadGifUrl(
                binding.ivFloating,
                float.imageUrl,
                R.drawable.img_default_cover_1_1.getResDrawable()
            )
            Config.get().activityFloatShowMark(float.id)
            binding.floatingBall.setOnMfClickListener {
                AppRouter.jump(float.jumpUrl)
            }
        } else {
            binding.floatingBall.visibility = View.GONE
        }
    }
}