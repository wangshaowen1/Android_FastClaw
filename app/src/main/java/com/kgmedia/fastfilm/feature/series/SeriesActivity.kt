package com.kgmedia.fastfilm.feature.series

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import com.kgmedia.fastfilm.R
import com.kgmedia.fastfilm.common.Constants
import com.kgmedia.fastfilm.common.ext.startRefreshWithAdapter
import com.kgmedia.fastfilm.common.ext.topActivity
import com.kgmedia.fastfilm.core.Events
import com.kgmedia.fastfilm.core.network.apis.entity.EpisodeInfo
import com.kgmedia.fastfilm.core.network.apis.entity.WorkInfo
import com.kgmedia.fastfilm.databinding.ActivitySeriesBinding
import com.kgmedia.fastfilm.feature.creator.continuewriting.ContinueWritingActivity
import com.kgmedia.fastfilm.feature.creator.data.DramaGenerateConstants
import com.kgmedia.fastfilm.feature.creator.generate.StoryEditorActivity
import com.kgmedia.fastfilm.feature.editor.info.DramaInfoEditActivity
import com.kgmedia.fastfilm.feature.play.DramasPlayPagerActivity
import com.kgmedia.fastfilm.feature.play.data.FeedRepository
import com.kgmedia.fastfilm.feature.series.management.WorksManagementActivity
import com.kgmedia.fastfilm.feature.web.SimpleBrowserActivity
import com.kgmedia.fastfilm.feature.works.ItemMultipleChoiceEventsViewModel
import com.kgmedia.fastfilm.router.AppRouter
import com.kgmedia.fastfilm.widget.Popup
import com.kgmedia.fastfilm.widget.SeriesMorePopupView
import com.kgmedia.fastfilm.widget.SeriesReleaseFailTipsPopupView
import com.kgmedia.fastfilm.widget.decoration.BottomSpaceDecoration
import com.kgmedia.fastfilm.widget.stateful.applyGridSpanSizeLookup
import com.kgmedia.fastfilm.widget.stateful.paging.MyMultiStatePagingFooterAdapter
import com.nhsd.basic.BaseViewModelActivity
import com.nhsd.basic.bus.observeEvents
import com.nhsd.basic.ext.dp
import com.nhsd.basic.ext.isActive
import com.nhsd.basic.ext.setOnSingleClickListener
import com.nhsd.basic.ext.showToast
import com.nhsd.basic.utils.Logger
import kotlinx.coroutines.launch

/**
 * 剧集详情页
 */
class SeriesActivity : BaseViewModelActivity<ActivitySeriesBinding, SeriesViewModel>() {
    companion object {
        fun start(workId: String?, newlyPublish: Boolean = false) {
            val activity = topActivity ?: return
            activity.startActivity(
                Intent(activity, SeriesActivity::class.java).apply {
                    putExtra(Constants.Extra.ID, workId)
                    putExtra(Constants.Extra.NEWLY_PUBLISH, newlyPublish)
                }
            )
        }
    }

    private val eventVm by lazy { ItemMultipleChoiceEventsViewModel<SeriesEpisodeEvent, String>() }
    private val episodeAdapter by lazy { EpisodeAdapter(eventVm) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding.toolbarLayout.minimumHeight = 32.dp
        initView()
        //数据更新
        observeData()
    }

    private fun initView() {
        val noDataButtonTextAs = "刷新"
        val noDataAs = "暂无数据"
        binding.recyclerView.apply {
            itemAnimator = null
            addItemDecoration(BottomSpaceDecoration(80.dp))
            adapter = episodeAdapter.withLoadStateFooter(
                MyMultiStatePagingFooterAdapter(
                    hostAdapter = episodeAdapter,
                    noData = noDataAs,
                    noDataButtonDisplay = false,
                    requestButtonDisplay = false,
                    noDataButtonText = noDataButtonTextAs,
                    displayedHeight = 400.dp,
                    refresh = { refresh() },
                    requestButtonTextAndImg = false,
                )
            )
            binding.recyclerView.applyGridSpanSizeLookup()
        }
        binding.loadStateViewHolder.apply {
            setGravityHorizontalTop(100.dp)
            setRetryListener { refresh() }
            setNoDataButton(noDataButtonTextAs, { refresh() })
            setNoDataPrompt(noDataAs)
        }
        binding.swipeRefreshLayout.startRefreshWithAdapter(pagingDataAdapter = episodeAdapter)
    }

    private fun refresh() {
        //作品详情
        mRealVM.getWorkDtl()
    }

    private fun observeData() {
        lifecycleScope.launch {
            eventVm.events.collect { event ->
                when (event) {
                    is SeriesEpisodeEvent.Click -> {
                        //点击查看
                        clickToView(event.item.data.get(), event.position)
                    }

                    else -> {}
                }
            }
        }

        lifecycleScope.launch {
            mRealVM.episodeList().collect {
                episodeAdapter.submitData(it)
            }
        }

        observeEvents {
            on<Events.SeriesUpdatedEvent> { event ->
                if (isActive && event.seriesId == mRealVM.workId) {
                    refresh()
                }
            }
            on<Events.ShortDramasEditInfoEvent> { event ->
                if (episodeAdapter.snapshot().any { it?.storyId == event.dramaId }) {
                    refresh()
                }
            }
            on<Events.DeleteTVSeriesEvent> { event ->
                if (isActive && event.workId == mRealVM.workId) {
                    refresh()
                }
            }
        }
        mRealVM.workInfo.observe(this) {
            episodeAdapter.refresh()
        }
        mRealVM.episodeInfo.observe(this) {
            if (it == null) return@observe
            mRealVM.episodeInfo.value = null
            val workInfo = mRealVM.workInfo.value ?: return@observe
            Logger.d("[SSE]", "上一集的剧集id = ${it.storyId}")
            Logger.d("[SSE]", "当前集的剧集id = ${it.sequelStoryId}")
            //灵感创作续写
            StoryEditorActivity.startContinue(
                this,
                workId = mRealVM.workId,
                workTitle = workInfo.workTitle,
                storyId = it.sequelStoryId,
                previousStoryId = it.storyId,
                chapterIndex = (it.sortNo ?: 0) + 1,
                model = DramaGenerateConstants.MODEL_CONTINUE_INSPIRATION,
                videoRatio = it.workRatio
            )
        }
    }

    /**
     * 点击查看
     */
    private fun clickToView(episodeInfo: EpisodeInfo?, position: Int) {
        if (episodeInfo == null) return
        //[10=剧本输入 15=剧本生成中 20=资产生成中 30=分镜生成中 32=分镜视频生成中 35=待合成 40=已完成]
        when (episodeInfo.stepProgress) {
            EpisodeInfo.PROGRESS_COMPLETED -> {
                val snapshot = episodeAdapter.snapshot().items
                onPlayEpisodeClicked(
                    mRealVM.workInfo.value,
                    snapshot,
                    position
                )
            }

            else -> {
                if (episodeInfo.storyId.isNullOrEmpty()) {
                    val lastEpisode = episodeAdapter.snapshot().lastOrNull { !it?.storyId.isNullOrEmpty() }
                    //仅当上一集内容已进入“分镜视频生成”环节，下一集才可开始创建。
                    //若上一集还未进入“分镜视频生成”环节，此时点击“创建下一集”，则toast提示“请先完成上一集的内容制作”；
                    if (lastEpisode == null) {
                        // TODO: 没有剧集的情况
                    } else if ((lastEpisode.stepProgress ?: 0) >= EpisodeInfo.PROGRESS_STORYBOARD_GENERATING) {
                        continueWriting()
                    } else {
                        "请先完成上一集的内容制作".showToast()
                    }
                } else {
                    AppRouter.startStory(mRealVM.workInfo.value, episodeInfo.storyId)
                }
            }
        }
    }

    fun onPlayEpisodeClicked(workInfo: WorkInfo?, episodes: List<EpisodeInfo>, clickPosition: Int) {
        // 1. [核心步骤] 过滤未完成的数据 + 转换为 Adapter 需要的 WorkInfo
        val validPlayList = episodes
            .filter { !it.videoUrl.isNullOrEmpty() } // 过滤条件
            .map { episode ->
                // 复用父作品信息，包装成 WorkInfo
                WorkInfo(
                    workId = episode.storyId ?: "", // 必须唯一
                    userId = workInfo?.userId,
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

    override fun <Data> onClick(view: View?, data: Data?) {
        super.onClick(view, data)
        when (view?.id) {
            R.id.btnMore -> {
                mRealVM.workInfo.value ?: return
                val popupView = SeriesMorePopupView(this)
                val popup = Popup(popupView)
                popupView.binding.btnEdit.setOnSingleClickListener {
                    popup.dismiss()
                    //编辑剧本信息
                    DramaInfoEditActivity.start(this, mRealVM.workInfo.value)
                }
                popupView.binding.btnManage.setOnSingleClickListener {
                    popup.dismiss()
                    WorksManagementActivity.start(this, workId = mRealVM.workId)
                }
                popup.showUnder(view)
            }

            R.id.llReleaseStatus -> {
                bindingNullable?.tvReleaseStatus?.let {
                    Popup(SeriesReleaseFailTipsPopupView(this)).showUnder(it)
                }
            }

            R.id.btnRelease -> {
                SimpleBrowserActivity.launch(this, "http://192.168.10.16:5173/pages/issue/issue?nav=1&darkStyle=1")
            }
        }
    }

    private fun continueWriting() {
        val workInfo = mRealVM.workInfo.value ?: return
        if (!workInfo.isInspirationGenerated()) {
            //小说改编续写
            ContinueWritingActivity.start(
                this,
                workId = mRealVM.workId,
                workTitle = workInfo.workTitle
            )
            return
        }
        //需要拿到上一集的剧集信息，所以先调用一下
        mRealVM.getEpisodeInfo()
    }
}