package com.shiyuan.fastclaw.feature.opportunity


import android.graphics.Rect
import android.util.Log
import android.view.View
import android.view.animation.Animation
import android.view.animation.LinearInterpolator
import android.view.animation.RotateAnimation
import androidx.core.view.doOnPreDraw
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.nhsd.basic.BaseViewModelFragment
import com.nhsd.basic.bus.FlowEventBus
import com.nhsd.basic.bus.observeEvents
import com.nhsd.basic.ext.dp
import com.nhsd.basic.ext.getResString
import com.nhsd.basic.ext.setOnSingleClickListener
import com.nhsd.basic.ext.showToast
import com.nhsd.permisson.PermissionX
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.base.item.AppItemViewModel
import com.shiyuan.fastclaw.common.adapter.QuickAdapter
import com.shiyuan.fastclaw.common.ext.getColoredSpanText
import com.shiyuan.fastclaw.common.ext.gone
import com.shiyuan.fastclaw.common.ext.visible
import com.shiyuan.fastclaw.core.Events
import com.shiyuan.fastclaw.core.User
import com.shiyuan.fastclaw.core.ext.onUserStatusChanged
import com.shiyuan.fastclaw.core.network.apis.request.FileUploadCredentialItemBody.Companion.BIZ_TYPE_CANDIDATE_ONLINE_RESUME_A
import com.shiyuan.fastclaw.core.network.apis.response.OpportunityNewItemData
import com.shiyuan.fastclaw.core.network.apis.response.OpportunityProcessedItemData
import com.shiyuan.fastclaw.databinding.FragmentNotOpportunityBinding
import com.shiyuan.fastclaw.feature.mine.OnlineResumeActivity
import com.shiyuan.fastclaw.feature.opportunity.vm.OpportunityMatchState
import com.shiyuan.fastclaw.feature.opportunity.vm.OpportunityViewModel
import com.shiyuan.fastclaw.feature.report.UploadResumeDialog
import com.shiyuan.fastclaw.router.AppNavigator
import com.shiyuan.fastclaw.widget.CommonLoadingDialog
import com.shiyuan.fastclaw.widget.decoration.SpaceDecoration
import com.shiyuan.fastclaw.widget.shape.internal.BOLD
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.coroutines.resume

class OpportunityFragment :
    BaseViewModelFragment<FragmentNotOpportunityBinding, OpportunityViewModel>() {

    private companion object {
        const val OPPORTUNITY_PAGE = 0
        const val LOTTIE_RESUME_DELAY_MS = 120L
    }

    private var lastVisitorState: Boolean? = null

    private val opportunityAdapter by lazy {
        QuickAdapter<ItemProcessedOpportunityVM>(
            R.layout.item_processed_opportunity,
            emptyList()
        )
    }

    private val matchOpportunityAdapter by lazy {
        QuickAdapter<ItemMatchOpportunityVM>(
            R.layout.item_match_opportunity,
            emptyList()
        )
    }

    override fun onPrepareView() {
        super.onPrepareView()
        initView()
        initObservers()
        initUserEvents()
        syncUserState(force = true)
    }

    private fun initView() {

        bindParentPagerLottieState()
        //滚动时立即暂停 Lottie。 连续 120ms 未滚动后，检查 Lottie 是否仍在可视区域。
        binding.scrollContent.setOnScrollChangeListener { _, _, _, _, _ ->
            onContentScrolled()
        }
        binding.rvProcessedOpportunity.apply {
            layoutManager = LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false)
            if (itemDecorationCount == 0) {
                addItemDecoration(SpaceDecoration(10.dp, SpaceDecoration.Direction.MIDDLE))
            }
            adapter = opportunityAdapter
        }
        opportunityAdapter.setOnItemClickListener { adapter, view, i ->
            OpportunityDetailActivity.start(this.requireActivity())
        }
        binding.rvMatchOpportunity.apply {
            layoutManager = LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false)
            if (itemDecorationCount == 0) {
                addItemDecoration(SpaceDecoration(12.dp, SpaceDecoration.Direction.MIDDLE))
            }
            adapter = matchOpportunityAdapter
        }
        matchOpportunityAdapter.setOnItemClickListener { adapter, view, i ->
            OpportunityDetailActivity.start(this.requireActivity())
        }
    }

    private fun initObservers() {
        mRealVM.resumeListData.observe(this) { data ->
            if (User.get().isVisitor && data.orEmpty().isNotEmpty()) {
                return@observe
            }
            val resume = data.orEmpty().maxByOrNull { it.uploadTime.orEmpty() }
            mRealVM.isJobUpCompleted.value = resume?.parseStatus == 2
            if (resume?.parseStatus == 0 || resume?.parseStatus == 1) {
                mRealVM.startResumePolling()
                binding.llState.visible()
                binding.vLine.gone()
                binding.ivState.setImageResource(R.drawable.ic_running2)
                startRotation(binding.ivState)
                binding.tvState.setTextColor(resources.getColor(R.color.color_2f6bff))
                binding.tvState.text = resume.parseStatusText
            } else if (resume?.parseStatus == 2) {
                binding.llState.gone()
                binding.vLine.visible()
                binding.cardUploadResumeAction.gone()
                binding.cardUploadAlready.visible()
                mRealVM.stopResumePolling()
                stopRotation(binding.ivState)
            } else if (resume?.parseStatus == 3) {
                binding.llState.visible()
                binding.vLine.gone()
                binding.cardUploadResumeAction.visible()
                binding.cardUploadResumeAction.setOnSingleClickListener {
                    if (!AppNavigator.checkLoginIntercepted(false)) {
                        uploadResume()
                    }
                }
                binding.cardUploadAlready.gone()
                mRealVM.stopResumePolling()
                binding.ivState.setImageResource(R.drawable.ic_running_fail)
                stopRotation(binding.ivState)
                binding.tvState.setTextColor(resources.getColor(R.color.color_fb415e))
                binding.tvState.text = resume.parseStatusText
            } else {
                binding.llState.gone()
                binding.vLine.visible()
                binding.cardUploadResumeAction.visible()
                binding.cardUploadResumeAction.setOnSingleClickListener {
                    if (!AppNavigator.checkLoginIntercepted(false)) {
                        uploadResume()
                    }
                }
                binding.cardUploadAlready.gone()
                mRealVM.stopResumePolling()
                stopRotation(binding.ivState)
            }
        }
        mRealVM.resumeUploadResult.observe(this) { data -> //上传成功
            //解析简成功发送消息
            FlowEventBus.postSync(Events.ResumeEvent(true))
            mRealVM.startResumePolling()
        }
        mRealVM.isJobUpCompleted.observe(this) { data ->
            if (data) {
                binding.clJobPreference.setBackgroundResource(R.drawable.bg_opportunity_card_bg3)
                binding.tvJobPreferenceTitle.setTextColor(resources.getColor(R.color.color_000000))
                binding.tvJobPreferenceDescription.setTextColor(resources.getColor(R.color.color_85_3c3c43))
                binding.cardJobPreferenceAction.setBackgroundResource(R.drawable.bg_upload_resume_btn)
                binding.cardJobPreferenceAction.setOnSingleClickListener {
                    if (!AppNavigator.checkLoginIntercepted(false)) {
                        OnlineResumeActivity.start(requireActivity(), 1)
                    }
                }
            } else {
                binding.clJobPreference.setBackgroundResource(R.drawable.bg_opportunity_card_bg2)
                binding.tvJobPreferenceTitle.setTextColor(resources.getColor(R.color.color_75_3c3c43))
                binding.tvJobPreferenceDescription.setTextColor(resources.getColor(R.color.color_55_3c3c43))
                binding.cardJobPreferenceAction.setBackgroundResource(R.drawable.bg_20_666666_r50)
                binding.cardJobPreferenceAction.setOnSingleClickListener {
                    if (!AppNavigator.checkLoginIntercepted(false)) {
                        "请先上传简历".showToast()
                    }
                }
            }
        }
        mRealVM.matchState.observe(this) { data ->
            when (data) {
                OpportunityMatchState.MATCHED -> {
                    renderLottie(false)
                    binding.cardUnderstandUser.gone()
                    binding.cardNoChance.gone()
                    binding.llMatchOpportunity.visible()
                    binding.llProcessedOpportunity.visible()
                    updateChanceData(lastUiVisible == true)
                }

                OpportunityMatchState.NORMAL_MATCHING -> {
                    renderLottie(true)
                    binding.cardUnderstandUser.gone()
                    binding.cardNoChance.visible()
                    binding.llMatchOpportunity.gone()
                    binding.llProcessedOpportunity.visible()
                    mRealVM.queryOpportunityStats()
                    updateNewOpportunityPolling(lastUiVisible == true)
                    updateChanceData(lastUiVisible == true)
                }
                else -> {
                    renderLottie(true)
                    binding.cardUnderstandUser.visible()
                    binding.cardNoChance.gone()
                    binding.llMatchOpportunity.gone()
                    binding.llProcessedOpportunity.gone()
                }
            }

        }
        mRealVM.chanceData.observe(this) { data ->
            val opportunityItems = data.orEmpty()
            opportunityAdapter.submitList(opportunityItems.map(::ItemProcessedOpportunityVM))
            val isEmpty = opportunityItems.isEmpty()
            if (isEmpty) {
                binding.llViewAllProcessedOpportunity.gone()
                binding.rlEmpty.visible()
                binding.rvProcessedOpportunity.gone()
            } else {
                binding.llViewAllProcessedOpportunity.visible()
                binding.llViewAllProcessedOpportunity.setOnSingleClickListener {
                    ProcessedOpportunityActivity.start(requireActivity())
                }
                binding.rlEmpty.gone()
                binding.rvProcessedOpportunity.visible()
            }
        }
        mRealVM.matchingData.observe(this) { data ->
            val opportunityItems = data.orEmpty()
            binding.tvMatchOpportunity.text = getColoredSpanText(
                getString(R.string.not_opportunity_match_count, opportunityItems.size),
                opportunityItems.size.toString(),
                resources.getColor(R.color.color_2f6bff)
            )
            matchOpportunityAdapter.submitList(opportunityItems.map(::ItemMatchOpportunityVM))
        }
        mRealVM.opportunityStats.observe(this) { data ->
            val viewedJobCount = data?.viewedJobCount ?: 0
            binding.tvBrowsedJobCount.text = getColoredSpanText(
                getString(R.string.not_opportunity_browsed_job_count_format, viewedJobCount),
                viewedJobCount.toString(),
                resources.getColor(R.color.color_90_000000),
                true
            )
            binding.tvRecommendedOpportunityCount.text =
                (data?.recommendedOpportunityCount ?: 0).toString()
            binding.tvPendingOpportunityCount.text =
                (data?.pendingOpportunityCount ?: 0).toString()
            binding.tvMismatchedOpportunityCount.text =
                (data?.notMatchedOpportunityCount ?: 0).toString()
        }
    }

    private fun initUserEvents() {
        observeEvents {
            onUserStatusChanged {
                syncUserState(force = true)
            }
        }
    }

    private fun syncUserState(force: Boolean = false) {
        val isVisitor = User.get().isVisitor
        if (!force && lastVisitorState == isVisitor) return

        lastVisitorState = isVisitor
        mRealVM.stopResumePolling()
        mRealVM.resetUserState()
        if (!isVisitor) {
            mRealVM.isJobPreferenceCompleted.value = true
            mRealVM.queryResumeList()
        }
    }


    private var commonLoadingDialog: CommonLoadingDialog? = null
    fun showLoading(text: String? = null) {
        if (commonLoadingDialog?.isAdded == true) return
        commonLoadingDialog = CommonLoadingDialog.newInstance(text).apply {
            show(this@OpportunityFragment.childFragmentManager)
        }
    }

    fun hideLoading() {
        commonLoadingDialog?.dismissAllowingStateLoss()
        commonLoadingDialog = null
    }

    private var uploadResumeDialog: UploadResumeDialog? = null
    fun uploadResume(replaceResumeTaskId: String? = null) {
        //上传简历
        uploadResumeDialog = UploadResumeDialog.show(
            manager = this@OpportunityFragment.childFragmentManager,
            false,
            bizType = BIZ_TYPE_CANDIDATE_ONLINE_RESUME_A,
            { fileInfo ->
                if (fileInfo != null) {
                    showLoading("上传中")
                    mRealVM.uploadFilesWithSecurityCheck(
                        listOf(fileInfo.uri), BIZ_TYPE_CANDIDATE_ONLINE_RESUME_A,
                        onSuccess = { successUrls ->
                            hideLoading()
                            mRealVM.submitResumeUpload(
                                successUrls.first(),
                                fileInfo.fileName,
                                replaceResumeTaskId,
                                false
                            ) {
                                hideLoading()
                            }
                        },
                        onFailure = {
                            hideLoading()
                            it.showToast()
                        })
                }
            },
            {
                mRealVM.code.value = it
            })
    }


    override fun onResume() {
        super.onResume()
        dispatchChanged()
    }

    private var lastUiVisible: Boolean? = null
    private fun dispatchChanged() {
        val isUiVisible = isVisible && isResumed && !isHidden
        if (lastUiVisible == isUiVisible) return
        lastUiVisible = isUiVisible
        Log.d("uploadResumeDialog", lastUiVisible.toString())
        onVisibilityChanged(isUiVisible)
    }

    private fun onVisibilityChanged(isVisible: Boolean) {
        updateNewOpportunityPolling(isVisible)
        updateChanceData(isVisible)
        if (isVisible) {
            scheduleLottiePlaybackUpdate()
            handleWechatResumeReturn()
            syncUserState()
        } else {
            stopLottiePlayback()
        }
    }

    /** 轮询最新机会**/
    private fun updateNewOpportunityPolling(isVisible: Boolean) {
        if (isVisible && mRealVM.matchState.value == OpportunityMatchState.NORMAL_MATCHING) {
            mRealVM.startNewOpportunityPolling()
        } else {
            mRealVM.stopNewOpportunityPolling()
        }
    }

    /** 请求已处理机会**/
    private fun updateChanceData(isVisible: Boolean) {
        if (isVisible && (mRealVM.matchState.value == OpportunityMatchState.NORMAL_MATCHING || mRealVM.matchState.value == OpportunityMatchState.NORMAL_MATCHING)) {
            mRealVM.queryProcessedOpportunityPreview()
        }
    }

    private fun handleWechatResumeReturn() {
        if (!mRealVM.code.value.isNullOrEmpty()) {
            Log.d("uploadResumeDialog", mRealVM.code.value.toString())
            uploadResumeDialog?.dismissAllowingStateLoss()
            mRealVM.queryCodeResumeBinding(mRealVM.code.value ?: "") { resume ->
                mRealVM.submitResumeUpload(
                    resume?.resumeUrl,
                    resume?.resumeFileName,
                    resume?.resumeTaskBizId
                )
            }
        }
    }
    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        dispatchChanged()
    }

    override fun onPause() {
        super.onPause()
        dispatchChanged()
    }

    private val lottieVisibleRect = Rect()
    private var isContentScrolling = false
    private var parentPager: ViewPager2? = null

    // 纵向滚动停止 120ms 后再判断，避免滚动过程中频繁暂停和恢复。
    private val resumeLottieAfterScroll = Runnable {
        updateLottiePlayback(isScrolling = false)
    }

    // ViewPager2 切页时 onPause 触发较晚，主动监听滑动状态以便立即暂停动画。
    private val parentPagerCallback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            updateLottiePlayback()
        }

        override fun onPageScrollStateChanged(state: Int) {
            updateLottiePlayback()
        }
    }

    private fun onContentScrolled() {
        updateLottiePlayback(isScrolling = true)
        binding.scrollContent.removeCallbacks(resumeLottieAfterScroll)
        binding.scrollContent.postDelayed(
            resumeLottieAfterScroll,
            LOTTIE_RESUME_DELAY_MS,
        )
    }

    // 回调与当前 View 生命周期绑定，并在 onDestroyView 中使用同一实例注销。
    private fun bindParentPagerLottieState() {
        parentPager?.unregisterOnPageChangeCallback(parentPagerCallback)
        parentPager = parentFragment?.view?.findViewById(R.id.vp_opportunity_pages)
        parentPager?.registerOnPageChangeCallback(parentPagerCallback)
        updateLottiePlayback()
    }

    private fun renderLottie(isVisible: Boolean) {
        if (isVisible) {
            binding.v.visible()
            if (viewLifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                scheduleLottiePlaybackUpdate()
            }
        } else {
            binding.v.gone()
            pauseLottiePlayback()
        }
    }

    // 首次显示需等待布局完成，否则可视区域判断可能拿不到正确结果。
    private fun scheduleLottiePlaybackUpdate() {
        binding.v.doOnPreDraw {
            updateLottiePlayback()
        }
    }

    private fun updateLottiePlayback(isScrolling: Boolean = isContentScrolling) {
        isContentScrolling = isScrolling
        val isLottieVisible = binding.v.visibility == View.VISIBLE &&
                binding.v.getLocalVisibleRect(lottieVisibleRect)
        // 父 Pager 停稳且当前为机会页时，才继续判断生命周期和页面内可见性。
        val isParentPagerIdleAndSelected = parentPager?.run {
            scrollState == ViewPager2.SCROLL_STATE_IDLE && currentItem == OPPORTUNITY_PAGE
        } ?: true
        val shouldPlayLottie = isParentPagerIdleAndSelected &&
                !isContentScrolling &&
                viewLifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) &&
                isLottieVisible
        if (shouldPlayLottie) {
            binding.v.resumeAnimation()
        } else {
            pauseLottiePlayback()
        }
    }

    private fun stopLottiePlayback() {
        binding.scrollContent.removeCallbacks(resumeLottieAfterScroll)
        isContentScrolling = false
        pauseLottiePlayback()
    }


    private fun pauseLottiePlayback() {
        binding.v.pauseAnimation()
    }

    private fun releaseLottie() {
        binding.v.cancelAnimation()
    }

    private var rotateAnim: RotateAnimation? = null
    private fun startRotation(view: View) {
        if (rotateAnim == null) {
            rotateAnim = RotateAnimation(
                0f, 360f,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f
            ).apply {
                duration = 1000L
                repeatCount = Animation.INFINITE
                interpolator = LinearInterpolator()
            }
        }
        // 如果动画已经在运行，不要重复启动
        if (rotateAnim != null && view.animation != rotateAnim) {
            view.startAnimation(rotateAnim)
        }
    }

    private fun stopRotation(view: View) {
        view.clearAnimation()
    }

    override fun onDestroyView() {
        parentPager?.unregisterOnPageChangeCallback(parentPagerCallback)
        parentPager = null
        binding.scrollContent.removeCallbacks(resumeLottieAfterScroll)
        isContentScrolling = false
        releaseLottie()
        mRealVM.stopResumePolling()
        mRealVM.stopNewOpportunityPolling()
        super.onDestroyView()
    }

}

class ItemProcessedOpportunityVM(data: OpportunityProcessedItemData) :
    AppItemViewModel<OpportunityProcessedItemData>(data) {

    val requirements = listOf(data.workCity, data.experienceText, data.degreeText)
        .filter(String::isNotBlank)
        .joinToString("·")

    val actionTime = buildProcessedActionTime(data.statusTime, data.status)

    private fun buildProcessedActionTime(statusTime: String, status: String): String {
        val formattedTime = runCatching {
            val date = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA).parse(statusTime)
            date?.let {
                SimpleDateFormat("M月d日H时m分", Locale.CHINA).format(it)
            }
        }.getOrNull() ?: statusTime

        return when (status) {
            OpportunityProcessedItemData.STATUS_COMMUNICATING ->
                R.string.processed_opportunity_communicating_action_time.getResString(formattedTime)

            OpportunityProcessedItemData.STATUS_UNSUITABLE ->
                R.string.processed_opportunity_unsuitable_action_time.getResString(formattedTime)

            else -> formattedTime
        }
    }
}

class ItemMatchOpportunityVM(data: OpportunityNewItemData) :
    AppItemViewModel<OpportunityNewItemData>(data) {

    val requirements = listOf(data.workCity, data.experienceText, data.degreeText)
        .filter(String::isNotBlank)
        .joinToString("·")
}
