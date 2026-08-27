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
import com.shiyuan.fastclaw.feature.main.MainActivity
import com.shiyuan.fastclaw.feature.mine.OnlineResumeActivity
import com.shiyuan.fastclaw.feature.opportunity.vm.OpportunityIsCanMatchState
import com.shiyuan.fastclaw.feature.opportunity.vm.OpportunityMatchState
import com.shiyuan.fastclaw.feature.opportunity.vm.OpportunityViewModel
import com.shiyuan.fastclaw.feature.report.UploadResumeDialog
import com.shiyuan.fastclaw.router.AppNavigator
import com.shiyuan.fastclaw.widget.CommonLoadingDialog
import com.shiyuan.fastclaw.widget.decoration.SpaceDecoration
import com.shiyuan.fastclaw.widget.shape.internal.BOLD
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.collections.orEmpty
import kotlin.coroutines.resume

/**
 * 机会首页。
 *
 * 未满足匹配条件时展示简历和求职偏好引导；满足条件后展示新机会、已处理机会和统计数据，
 * 同时根据页面可见性管理简历解析轮询、新机会轮询及 Lottie 播放。
 */
class OpportunityFragment :
    BaseViewModelFragment<FragmentNotOpportunityBinding, OpportunityViewModel>() {

    private companion object {
        const val OPPORTUNITY_PAGE = 0
        const val LOTTIE_RESUME_DELAY_MS = 120L
    }

    /** 首页最多展示 5 条已处理机会。 */
    private val opportunityAdapter by lazy {
        QuickAdapter<ItemProcessedOpportunityVM>(
            R.layout.item_processed_opportunity,
            emptyList()
        )
    }

    /** 当前最新一批未处理机会列表。 */
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
    }

    private fun initView() {

        bindParentPagerLottieState()
        // 滚动时立即暂停 Lottie；连续 120ms 未滚动后，再检查动画是否仍在可视区域。
        binding.scrollContent.setOnScrollChangeListener { _, _, _, _, _ ->
            onContentScrolled()
        }
        // 根据匹配资格刷新两组数据：匹配完成后查统计/新机会/已处理机会，否则查简历和求职偏好。
        binding.srl.setOnRefreshListener {
            if (User.get().isVisitor) {
                binding.srl.finishRefresh()
                return@setOnRefreshListener
            }
            if (mRealVM.isMatchState.value == OpportunityIsCanMatchState.CANMATCHED) {
                mRealVM.queryOpportunityStats()
                mRealVM.queryNewOpportunities()
                mRealVM.queryProcessedOpportunityPreview()
            } else {
                mRealVM.queryResumeList()
                mRealVM.queryJobPreferenceCompletion()
            }
            binding.srl.finishRefresh(1000)
        }
        binding.rvProcessedOpportunity.apply {
            layoutManager = LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false)
            if (itemDecorationCount == 0) {
                addItemDecoration(SpaceDecoration(10.dp, SpaceDecoration.Direction.MIDDLE))
            }
            adapter = opportunityAdapter
        }
        opportunityAdapter.setOnItemClickListener { _, _, i ->
            mRealVM.chanceData.value.orEmpty().getOrNull(i)?.let { opportunity ->
                OpportunityDetailActivity.start(requireActivity(), opportunity.jobId)
            }
        }
        binding.rvMatchOpportunity.apply {
            layoutManager = LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false)
            if (itemDecorationCount == 0) {
                addItemDecoration(SpaceDecoration(12.dp, SpaceDecoration.Direction.MIDDLE))
            }
            adapter = matchOpportunityAdapter
        }
        matchOpportunityAdapter.setOnItemClickListener { _, _, i ->
            mRealVM.matchingData.value.orEmpty().getOrNull(i)?.let { opportunity ->
                OpportunityDetailActivity.start(
                    requireActivity(),
                    opportunity.jobId,
                    opportunity.unread,
                )
            }
        }
    }

    private fun initObservers() {
        // 返回简历列表后取上传时间最新的一份，并按解析状态切换上传、解析中、成功或失败 UI。
        mRealVM.resumeListData.observe(this) { data ->
            val resume = data.orEmpty().maxByOrNull { it.uploadTime.orEmpty() }
            mRealVM.isJobUpCompleted.value = resume?.parseStatus == 2
            if (resume?.parseStatus == 0 || resume?.parseStatus == 1) {
                mRealVM.startResumePolling()
                binding.llState.visible()
                binding.vLine.gone()
                binding.cardUploadResumeAction.gone()
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
        // 简历上传提交成功后通知其他页面，并开始轮询解析结果。
        mRealVM.resumeUploadResult.observe(this) { data ->
            FlowEventBus.postSync(Events.ResumeEvent(true))
            mRealVM.startResumePolling()
        }
        // 简历解析结果决定求职偏好卡片是否可操作。
        mRealVM.isJobUpCompleted.observe(this) { data ->
            if (data) {
                binding.clJobPreference.setBackgroundResource(R.drawable.bg_opportunity_card_bg3)
                binding.tvJobPreferenceTitle.setTextColor(resources.getColor(R.color.color_000000))
                binding.tvJobPreferenceDescription.setTextColor(resources.getColor(R.color.color_85_3c3c43))
                binding.cardJobPreferenceAction.setBackgroundResource(R.drawable.bg_upload_resume_btn)
                binding.cardJobPreferenceAction.setOnSingleClickListener {
                    if (!AppNavigator.checkLoginIntercepted(false)) {
                        val mainActivity = activity as? MainActivity
                        mainActivity?.switchToTab(0)
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
        // 简历和求职偏好均完成后，开始请求机会首页所需的三组数据。
        mRealVM.isMatchState.observe(this) { data ->
            when (data) {
                OpportunityIsCanMatchState.UNMATCHED -> {
                    renderLottie(true)
                    binding.cardUnderstandUser.visible()
                    binding.cardNoChance.gone()
                    binding.llMatchOpportunity.gone()
                    binding.llProcessedOpportunity.gone()
                    binding.vTopBg.visible()
                }
                OpportunityIsCanMatchState.CANMATCHED -> {
                    mRealVM.queryOpportunityStats() // 查询统计数据。
                    updateNewOpportunityPolling(lastUiVisible == true)// 查询最新未处理机会。
                    mRealVM.queryProcessedOpportunityPreview() // 查询最近 5 条已处理机会。

                }
            }
        }

        // 返回最新未处理机会列表：空列表展示匹配中状态，非空列表展示机会卡片。
        mRealVM.matchingData.observe(this) { data ->
            if(mRealVM.isMatchState.value != OpportunityIsCanMatchState.CANMATCHED) return@observe
            val opportunityItems = data.orEmpty()
            data.orEmpty().let { list ->
                if (list.isEmpty()) {
                    // 暂无新机会时维持轮询，并展示匹配中状态。
                    renderLottie(true)
                    binding.cardUnderstandUser.gone()
                    binding.cardNoChance.visible()
                    binding.llMatchOpportunity.gone()
                    binding.llProcessedOpportunity.visible()
                    binding.vTopBg.gone()
                } else {
                    // 已匹配到机会时停止空态动画，展示本批机会。
                    renderLottie(false)
                    binding.cardUnderstandUser.gone()
                    binding.cardNoChance.gone()
                    binding.llMatchOpportunity.visible()
                    binding.llProcessedOpportunity.visible()
                    binding.vTopBg.gone()

                    binding.tvMatchOpportunity.text = getColoredSpanText(getString(R.string.not_opportunity_match_count, opportunityItems.size), opportunityItems.size.toString(), resources.getColor(R.color.color_2f6bff))
                    matchOpportunityAdapter.submitList(list.map(::ItemMatchOpportunityVM))
                }
            }
        }

        // 返回最近 5 条已处理机会，用于首页预览和空态切换。
        mRealVM.chanceData.observe(this) { data ->
            if(mRealVM.isMatchState.value != OpportunityIsCanMatchState.CANMATCHED) return@observe
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

        // 返回机会统计响应，依次展示浏览、推荐、待定和不匹配数量。
        mRealVM.opportunityStats.observe(this) { data ->
            if(mRealVM.isMatchState.value != OpportunityIsCanMatchState.CANMATCHED) return@observe
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

    /** 监听登录态和详情操作事件，按事件范围重新请求对应机会数据。 */
    private fun initUserEvents() {
        observeEvents {
            onUserStatusChanged {
                mRealVM.stopResumePolling()
                mRealVM.resetUserState()
            }
            on<Events.OpportunityListRefreshEvent> { event ->
                if(event.type == 1){
                    mRealVM.queryNewOpportunities()
                }else{
                    mRealVM.queryNewOpportunities()
                    mRealVM.queryProcessedOpportunityPreview()
                }
            }
            on<Events.OpportunityJobUpCompletedEvent> { event ->
                mRealVM.isJobUpCompleted.value = true
                mRealVM.isJobPreferenceCompleted.value = true
            }
        }
    }

    /** 文件上传期间展示的加载弹窗。 */
    private var commonLoadingDialog: CommonLoadingDialog? = null
    /** 展示加载弹窗，已有实例显示时不重复创建。 */
    fun showLoading(text: String? = null) {
        if (commonLoadingDialog?.isAdded == true) return
        commonLoadingDialog = CommonLoadingDialog.newInstance(text).apply {
            show(this@OpportunityFragment.childFragmentManager)
        }
    }

    /** 关闭并释放加载弹窗引用。 */
    fun hideLoading() {
        commonLoadingDialog?.dismissAllowingStateLoss()
        commonLoadingDialog = null
    }

    /** 简历来源选择弹窗。 */
    private var uploadResumeDialog: UploadResumeDialog? = null
    /**
     * 选择并上传简历。
     *
     * 请求链路：先申请上传凭证并完成文件安全检查，再提交简历 URL、文件名和可选替换任务 ID。
     * 返回：文件上传成功地址用于提交简历；提交结果由 resumeUploadResult 统一处理。
     */
    fun uploadResume(replaceResumeTaskId: String? = null) {
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

    /** 上一次分发的页面可见状态，避免重复启动请求、轮询或动画。 */
    private var lastUiVisible: Boolean? = null
    /** 合并 Fragment 可见、RESUMED 和非隐藏状态后分发一次变化。 */
    private fun dispatchChanged() {
        val isUiVisible = isVisible && isResumed && !isHidden
        if (lastUiVisible == isUiVisible) return
        lastUiVisible = isUiVisible
        Log.d("uploadResumeDialog", lastUiVisible.toString())
        onVisibilityChanged(isUiVisible)
    }

    /** 页面可见时恢复必要请求和动画，不可见时停止轮询与动画。 */
    private fun onVisibilityChanged(isVisible: Boolean) {
        updateJobResumeState(isVisible)
        updateNewOpportunityPolling(isVisible)
        if (isVisible) {
            scheduleLottiePlaybackUpdate() // 开始动画。
            handleWechatResumeReturn() // 处理微信上传简历后的回跳。
        } else {
            stopLottiePlayback() // 停止动画。
        }
    }

    /** 页面可见时补查简历解析结果和求职偏好完成状态。 */
    private fun updateJobResumeState(isVisible: Boolean) {
        if (!isVisible || User.get().isVisitor) return

        if (mRealVM.isJobUpCompleted.value != true) {
            mRealVM.queryResumeList()
        }
        if (mRealVM.isJobPreferenceCompleted.value != true) {
            mRealVM.queryJobPreferenceCompletion()
        }
    }

    /** 仅在页面可见、具备匹配资格且当前无新机会时维持轮询。 */
    private fun updateNewOpportunityPolling(isVisible: Boolean) {
        if (User.get().isVisitor) return
        //可见可匹配就轮询
        if (isVisible && mRealVM.isMatchState.value == OpportunityIsCanMatchState.CANMATCHED) {
            mRealVM.startNewOpportunityPolling()
        } else {
            mRealVM.stopNewOpportunityPolling()
        }
    }

    /**
     * 处理微信侧上传简历后的授权码回跳。
     *
     * 请求：使用授权码查询已绑定的简历信息。
     * 返回：简历 URL、文件名和业务任务 ID，随后提交为当前用户简历。
     */
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

    /** Lottie 当前可见区域，复用对象以减少滚动期间分配。 */
    private val lottieVisibleRect = Rect()
    /** 页面纵向内容是否正在滚动。 */
    private var isContentScrolling = false
    /** 父级机会/宝典 ViewPager2，用于感知横向切页。 */
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

    /** 根据页面状态控制 Lottie 区域显示或隐藏。 */
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

    /**
     * 统一判断 Lottie 是否允许播放。
     *
     * 需要同时满足父 Pager 停稳且选中机会页、页面未纵向滚动、生命周期已 RESUMED、动画视图可见。
     */
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

/** 已处理机会条目的页面展示模型。 */
class ItemProcessedOpportunityVM(data: OpportunityProcessedItemData) :
    AppItemViewModel<OpportunityProcessedItemData>(data) {

    /** 工作城市、经验和学历要求组合后的展示文案。 */
    val requirements = listOf(data.workCity, data.experienceText, data.degreeText)
        .filter(String::isNotBlank)
        .joinToString("·")

    /** 根据处理状态补充语义后的处理时间文案。 */
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

/** 新机会条目的页面展示模型。 */
class ItemMatchOpportunityVM(data: OpportunityNewItemData) :
    AppItemViewModel<OpportunityNewItemData>(data) {

    /** 工作城市、经验和学历要求组合后的展示文案。 */
    val requirements = listOf(data.workCity, data.experienceText, data.degreeText)
        .filter(String::isNotBlank)
        .joinToString("·")
}
