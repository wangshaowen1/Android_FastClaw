package com.shiyuan.fastclaw.feature.opportunity


import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.animation.Animation
import android.view.animation.LinearInterpolator
import android.view.animation.RotateAnimation
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.nhsd.basic.BaseViewModelFragment
import com.nhsd.basic.bus.FlowEventBus
import com.nhsd.basic.bus.observeEvents
import com.nhsd.basic.ext.dp
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
import com.shiyuan.fastclaw.databinding.FragmentNotOpportunityBinding
import com.shiyuan.fastclaw.feature.mine.OnlineResumeActivity
import com.shiyuan.fastclaw.feature.opportunity.vm.OpportunityMatchState
import com.shiyuan.fastclaw.feature.opportunity.vm.OpportunityViewModel
import com.shiyuan.fastclaw.feature.report.UploadResumeDialog
import com.shiyuan.fastclaw.router.AppNavigator
import com.shiyuan.fastclaw.widget.CommonLoadingDialog
import com.shiyuan.fastclaw.widget.decoration.SpaceDecoration
import com.shiyuan.fastclaw.widget.shape.internal.BOLD
import kotlin.coroutines.resume

class OpportunityFragment :
    BaseViewModelFragment<FragmentNotOpportunityBinding, OpportunityViewModel>() {

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
        binding.rvProcessedOpportunity.apply {
            layoutManager = LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false)
            if (itemDecorationCount == 0) {
                addItemDecoration(SpaceDecoration(10.dp, SpaceDecoration.Direction.MIDDLE))
            }
            adapter = opportunityAdapter
        }
        binding.rvMatchOpportunity.apply {
            layoutManager = LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false)
            if (itemDecorationCount == 0) {
                addItemDecoration(SpaceDecoration(12.dp, SpaceDecoration.Direction.MIDDLE))
            }
            adapter = matchOpportunityAdapter
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
                if (!isResumePolling) {
                    startResumePolling()
                }
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
                stopResumePolling()  // ✅ 停止轮询
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
                stopResumePolling()  // ✅ 停止轮询
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
                stopResumePolling()  // ✅ 停止轮询
                stopRotation(binding.ivState)
            }
        }
        mRealVM.resumeUploadResult.observe(this) { data -> //上传成功
            //解析简成功发送消息
            FlowEventBus.postSync(Events.ResumeEvent(true))
            startResumePolling()
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
                    binding.v.gone()
                    binding.cardUnderstandUser.gone()
                    binding.cardNoChance.gone()
                    binding.llMatchOpportunity.visible()
                    binding.llProcessedOpportunity.visible()
                }
                OpportunityMatchState.NORMAL_MATCHING -> {
                    binding.v.visible()
                    binding.cardUnderstandUser.gone()
                    binding.cardNoChance.visible()
                    binding.llMatchOpportunity.gone()
                    binding.llProcessedOpportunity.visible()
                }
                else -> {
                    binding.v.visible()
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
            matchOpportunityAdapter.submitList(opportunityItems.map(::ItemMatchOpportunityVM))
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

                binding.tvMatchOpportunity.text = getColoredSpanText(
                    getString(R.string.not_opportunity_match_count, opportunityItems.size),
                    opportunityItems.size.toString(),
                    resources.getColor(R.color.color_2f6bff)
                )
            }
        }
        mRealVM.matchingData.observe(this) { data ->
            val opportunityItems = data.orEmpty()
            matchOpportunityAdapter.submitList(opportunityItems.map(::ItemMatchOpportunityVM))
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
        stopResumePolling()
        mRealVM.resetUserState()
        if (!isVisitor) {
            mRealVM.isJobPreferenceCompleted.value = true
            mRealVM.chanceData.value = mRealVM.createFakeChanceData()
            mRealVM.matchingData.value = mRealVM.createFakeChanceData()
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
        syncUserState()
    }

    private val handler = Handler(Looper.getMainLooper())
    private val POLLING_INTERVAL = 2000L // 2秒
    private var resumePollingRunnable: Runnable? = null
    private var isResumePolling = false
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

    private fun startResumePolling() {
        if (User.get().isVisitor || isResumePolling) return  // ✅ 防止游客或重复启动

        isResumePolling = true
        resumePollingRunnable = object : Runnable {
            override fun run() {
                if (User.get().isVisitor) {
                    stopResumePolling()
                    return
                }
                val activity = activity
                if (activity != null && !activity.isFinishing && !activity.isDestroyed && isResumePolling) {
                    Log.d("Polling", "轮询检查状态...")
                    mRealVM.queryResumeList()
                    handler.postDelayed(this, POLLING_INTERVAL)
                }
            }
        }
        handler.postDelayed(resumePollingRunnable!!, POLLING_INTERVAL)
        Log.d("Polling", "开始轮询")
    }

    private fun stopResumePolling() {
        if (!isResumePolling) return  // ✅ 停止前检查

        isResumePolling = false
        resumePollingRunnable?.let {
            handler.removeCallbacks(it)
            resumePollingRunnable = null
        }
        Log.d("Polling", "停止轮询")
    }

    override fun onDestroy() {
        super.onDestroy()
        stopResumePolling()
    }

}
data class ProcessedOpportunityItem(
    val title: String,
    val company: String,
    val requirements: String,
    val salary: String,
    val actionTime: String,
    val matchPercent: Int,
    val isRead: Boolean
)

class ItemProcessedOpportunityVM(data: ProcessedOpportunityItem) : AppItemViewModel<ProcessedOpportunityItem>(data)
class ItemMatchOpportunityVM(data: ProcessedOpportunityItem) : AppItemViewModel<ProcessedOpportunityItem>(data)
