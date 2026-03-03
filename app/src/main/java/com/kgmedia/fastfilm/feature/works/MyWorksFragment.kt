package com.kgmedia.fastfilm.feature.works

import android.graphics.Color
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.View
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.kgmedia.fastfilm.R
import com.kgmedia.fastfilm.common.ext.showConfirmPrompt
import com.kgmedia.fastfilm.common.ext.startRefreshWithAdapter
import com.kgmedia.fastfilm.common.ext.topActivity
import com.kgmedia.fastfilm.core.Config
import com.kgmedia.fastfilm.core.User
import com.kgmedia.fastfilm.core.databinding.loadGifUrl
import com.kgmedia.fastfilm.core.db.entity.WorkInformationInfo
import com.kgmedia.fastfilm.core.ext.onUserStatusChanged
import com.kgmedia.fastfilm.core.network.apis.entity.WorkInfo
import com.kgmedia.fastfilm.core.network.apis.response.ActivityConfigData
import com.kgmedia.fastfilm.core.network.apis.response.FloatWindowInfo
import com.kgmedia.fastfilm.core.network.apis.response.PopupInfo
import com.kgmedia.fastfilm.databinding.FragmentMyWorksBinding
import com.kgmedia.fastfilm.feature.creator.generate.StoryEditorActivity
import com.kgmedia.fastfilm.feature.main.ActivityDialog
import com.kgmedia.fastfilm.feature.main.MainViewModel
import com.kgmedia.fastfilm.feature.series.SeriesActivity
import com.kgmedia.fastfilm.router.AppRouter
import com.kgmedia.fastfilm.widget.decoration.BottomSpaceDecoration
import com.kgmedia.fastfilm.widget.decoration.SpaceDecoration
import com.kgmedia.fastfilm.widget.stateful.paging.MyMultiStatePagingFooterAdapter
import com.kgmedia.fastfilm.widget.stateful.applyGridSpanSizeLookup
import com.nhsd.basic.BaseViewModelFragment
import com.nhsd.basic.bus.observeEvents
import com.nhsd.basic.ext.dp
import com.nhsd.basic.ext.getResDrawable
import com.nhsd.basic.ext.getViewModelOfActivity
import kotlinx.coroutines.launch

class MyWorksFragment : BaseViewModelFragment<FragmentMyWorksBinding, MyWorksViewModel>() {


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
                noData = "暂无作品记录，快去创作你的作品吧",
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
                        if (event.workInfo?.workType == WorkInfo.NOVEL_UPLOAD && event.workInfo.isDraftStatus()) {
                            //作品类型：3-小说上传 且是草稿状态
                            mRealVM.getWorkDtl(workId = event.workInfo.workId)
                            return@collect
                        }
                        if (event.workInfo?.totalEpisodeCnt == 1) {
                            //这个是单集的
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
                model = AppRouter.getModel(workType = it.workType, sortNo = 1),
                videoRatio = it.workRatio,
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

        mRealVM.selectedCount.observe(this) {
            if (it <= 0) {
                binding.tvSelectCount.text = "已选择 0 个"
                return@observe
            }
            val spannableString = SpannableString("已选择${it}个")
            spannableString.setSpan(
                ForegroundColorSpan(
                    Color.WHITE
                ), 3, spannableString.length - 1,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            binding.tvSelectCount.text = spannableString
        }
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
                    title = "是否删除该作品",
                    cancelText = "取消",
                    confirmText = "确定"
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