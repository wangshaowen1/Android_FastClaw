package com.kgmedia.fastfilm.feature.mine.drafts

import androidx.lifecycle.lifecycleScope
import com.kgmedia.fastfilm.common.ext.startRefreshWithAdapter
import com.kgmedia.fastfilm.core.Events
import com.kgmedia.fastfilm.core.User
import com.kgmedia.fastfilm.core.ext.onUserStatusChanged
import com.kgmedia.fastfilm.core.network.apis.entity.ShortDramaInfo.Companion.STATUS_GENERATED_SUCCESS
import com.kgmedia.fastfilm.databinding.FragmentUserShortsBinding
import com.kgmedia.fastfilm.feature.creator.CreationRootActivity
import com.kgmedia.fastfilm.feature.creator.generate.StoryEditorActivity
import com.kgmedia.fastfilm.feature.editor.ShortDramaEditorActivity
import com.kgmedia.fastfilm.feature.mine.MineViewModel
import com.kgmedia.fastfilm.feature.play.publish.ShortDramaPublishActivity
import com.kgmedia.fastfilm.widget.decoration.SpaceDecoration
import com.kgmedia.fastfilm.widget.stateful.paging.MyMultiStatePagingFooterAdapter
import com.nhsd.basic.BaseViewModelFragment
import com.nhsd.basic.bus.observeEvents
import com.nhsd.basic.ext.dp
import com.nhsd.basic.ext.getViewModelOfParentFragment
import com.nhsd.basic.ext.isActive
import com.nhsd.basic.item.ItemEventsViewModel
import kotlinx.coroutines.launch

/**
 * *****************************************
 * 项目名称: KidsTales
 * @Author: SZYMN
 * @Introduce:绘本作品
 * @Data: 2025/4/29  9:38
 * @Version: 1.0
 * *****************************************
 */
class UserShortDramaDraftsFragment :
    BaseViewModelFragment<FragmentUserShortsBinding, ShotDramaDraftsViewModel>() {

    companion object {
        //绘本
        const val PICTURE_BOOK = 0

        //草稿
        const val DRAFT = 1

        fun newInstance(): UserShortDramaDraftsFragment {
            val fragment = UserShortDramaDraftsFragment()
            return fragment
        }
    }

    private var userId: String? = null
    val hostVm by lazy { getViewModelOfParentFragment<MineViewModel>() }

    private val pictureBookWorksAdapter by lazy {
        ShortDramaDraftsAdapter(
            eventVm = eventVm
        )
    }

    private val eventVm by lazy {
        ItemEventsViewModel<ShortDramaDraftEvent>()
    }

    override fun onPrepareView() {
        super.onPrepareView()
        userId = User.get().id

        initView()
        //数据更新
        dataUpdate()
    }

    private fun initView() {
        val noDataButtonTextAs = "去创作"
        val noDataAs = "当前还没有作品哦\n快去创作吧~"
        binding.recyclerView.apply {
            addItemDecoration(SpaceDecoration(12.dp))
            itemAnimator = null
            adapter = pictureBookWorksAdapter.withLoadStateFooter(
                MyMultiStatePagingFooterAdapter(
                    hostAdapter = pictureBookWorksAdapter,
                    noData = noDataAs,
                    noDataButtonDisplay = false,
                    requestButtonDisplay = false,
                    noDataButtonText = noDataButtonTextAs,
                    displayedHeight = 400.dp,
                    refresh = {
                        notDataHandle()
                    },
                    requestButtonTextAndImg = false,
                )
            )
        }
        binding.lsv.apply {
            makeScrollable()
            setRetryListener {
                //刷新
                refresh()
            }
            setNoDataButton(noDataButtonTextAs, {
                notDataHandle()
            })
            setNoDataPrompt(noDataAs)
        }

        binding.srl.startRefreshWithAdapter(pictureBookWorksAdapter, onRefreshListener = {
            hostVm?.updateInformation()
        })
    }

    private fun notDataHandle() {
        //没有数据，去创作
        CreationRootActivity.start(mActivity)
    }

    override fun onStart() {
        super.onStart()

        refresh()
    }

    override fun onStop() {
        super.onStop()
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (isShowReception()) {

            refresh()
        }
    }


    private fun dataUpdate() {
        lifecycleScope.launch {
            eventVm.events.collect { event ->
                when (event) {
                    is ShortDramaDraftEvent.Complete -> {
                        event.shortDramaInfo?.let {
                            if (it.workStatus == STATUS_GENERATED_SUCCESS) {
                                ShortDramaPublishActivity.start(event.shortDramaInfo.dramasId)
                            } else if (it.isPrepareForEditor()) {
                                ShortDramaEditorActivity.start(it.dramasId)
                            } else {
                                StoryEditorActivity.start(
                                    mActivity,
                                    step = 2,
                                    it.dramasId,
                                    title = it.dramasName,
                                )
                            }
                        }
                    }

                    is ShortDramaDraftEvent.ItemClick -> {
                        if (event.shortDramaInfo?.workStatus == STATUS_GENERATED_SUCCESS) {
                            ShortDramaPublishActivity.start(event.shortDramaInfo.dramasId)
                        }

                    }
                }
            }

        }

        lifecycleScope.launch {
            mRealVM.shortDramas().collect {
                pictureBookWorksAdapter.submitData(it)
            }
        }
        lifecycleScope.launch {
            mRealVM.totalCount.collect {
                getViewModelOfParentFragment<MineViewModel>()?.shortCount?.value = it
            }
        }
        observeEvents {


            /**
             * 登录状态变化
             */
            onUserStatusChanged {
                if (userId == User.get().id) return@onUserStatusChanged
                userId = User.get().id
                refresh()
            }

            /**
             * 绘本发布成功
             */
            on<Events.PictureBookPublishEvent> {
                refresh()
            }

            on<Events.ShortChangeEvent> { event ->
                mRealVM.update(event.shortDramaInfo)
            }


            on<Events.ShortDeletionEvent> { event ->
                //有绘本被删除了
                mRealVM.delete(event.shortDramaInfo)
            }

            on<Events.MainTabDoubleClickEvent> { event ->
                if (event.tabType == Events.MainTabDoubleClickEvent.TAB_MINE) {
                    if (isActive) {
                        pictureBookWorksAdapter.refresh()
                        binding.recyclerView.smoothScrollToPosition(0)
                    }
                }
            }
        }
    }

    private fun refresh() {
        pictureBookWorksAdapter.refresh()
    }

}