package com.kgmedia.fastfilm.feature.creator.generate

import android.os.Bundle
import android.view.View
import androidx.core.widget.doAfterTextChanged
import com.kgmedia.fastfilm.common.Constants
import com.kgmedia.fastfilm.core.User
import com.kgmedia.fastfilm.core.manager.AppEvent
import com.kgmedia.fastfilm.core.manager.EventManager
import com.kgmedia.fastfilm.core.manager.GrowingIO
import com.kgmedia.fastfilm.core.network.apis.request.DramaGenerateBody
import com.kgmedia.fastfilm.databinding.FragmentStoryBoardBinding
import com.kgmedia.fastfilm.feature.creator.generate.edit.StoryOutlineEditorDialog
import com.kgmedia.fastfilm.feature.creator.generate.edit.StoryboardEditorDialog
import com.kgmedia.fastfilm.widget.decoration.BottomSpaceDecoration
import com.nhsd.basic.BaseViewModelFragment
import com.nhsd.basic.ext.dp
import com.nhsd.basic.ext.getViewModelOfActivity
import com.nhsd.basic.ext.setOnSingleClickListener
import com.nhsd.basic.ext.showToast

class StoryBoardFragment :
    BaseViewModelFragment<FragmentStoryBoardBinding, StoryBoardViewModel>() {

    companion object {
        fun newInstance(): StoryBoardFragment {
            val fragment = StoryBoardFragment()
            val bundle = Bundle()
            fragment.arguments = bundle
            return fragment
        }
    }

    val hostVm by lazy { getViewModelOfActivity<StoryEditorViewModel>() }

    override fun onPrepareView() {
        super.onPrepareView()
        EventManager.onAppTrack(AppEvent.JG_VIEW_GENERATE_STORYBOARD_PAGE)
        activity?.intent?.let {
            val dramasId = it.getStringExtra(Constants.Extra.ID)
            if (dramasId.isNullOrEmpty()) {
                mRealVM.otherExtraId = it.getStringExtra(Constants.Extra.EXTRA_ID)
                mRealVM.content = it.getStringExtra(Constants.Extra.CONTENT)
                mRealVM.model = it.getIntExtra(Constants.Extra.MODE, DramaGenerateBody.GEN_WAY_INSPIRATION)
                mRealVM.dramaStyleId = it.getStringExtra(Constants.Extra.STYLE_ID)
                mRealVM.videoRatio = it.getIntExtra(Constants.Extra.VIDEO_RATIO, 0)
                mRealVM.sseGenerate()
                hostVm?.backWarning = true
                return@let
            } else {// 有dramasId的情况下走恢复生成（目前只有恢复生成会在步骤1传入dramasId）
                mRealVM.dramaId.value = dramasId
                mRealVM.mContent.set(it.getStringExtra(Constants.Extra.CONTENT))
                mRealVM.restoreDramaGenerate()
            }
        }
        // 在文本更新时自动滚动到底部
        binding.tvSSEContent.doAfterTextChanged {
            binding.nsv.postDelayed({
                bindingNullable?.nsv?.fullScroll(View.FOCUS_DOWN)
            }, 100)
        }
        binding.recyclerView.addItemDecoration(BottomSpaceDecoration(80.dp))
        mRealVM.mRoleList.observe(this) {
            binding.tagLayout.tags = it?.mapNotNull { role -> role.roleName }
        }
        mRealVM.dramaId.observe(this) {
            hostVm?.backWarning = false
            hostVm?.dramaId = it
        }
        mRealVM.mDramaName.observe(this) {
            hostVm?.title?.value = it
        }
        mRealVM.loadingState.observe(this) {
            binding.tvLoadingStateText.text = when (it) {
                0 -> "队列等待中..."
                1 -> "故事分析中..."
                2 -> "撰写故事中..."
                else -> "分镜加载中..."
            }
        }

        binding.expandTextView.setOnSingleClickListener {
            mRealVM.mContent.get()?.let {
                StoryOutlineEditorDialog.newInstance(mRealVM.dramaId.value, mRealVM.mContent.get()).apply {
                    onSuccess = { content ->
                        this@StoryBoardFragment.mRealVM.refreshDramaContent(content)
                    }
                }.show(childFragmentManager)
            }
        }

        binding.btnDramaInfoRegenerate.setOnSingleClickListener {
            mRealVM.sseGenerate()
        }

        binding.btnGenerate.setOnSingleClickListener {
            if (mRealVM.loadingStoryboardState.value == StoryBoardViewModel.DISPLAY_STORYBOARD_FAIL) {//分镜生成失败
                mRealVM.retryStoryboardGeneration()
                return@setOnSingleClickListener
            }
            if (binding.btnGenerate.alpha < 1f) {//分镜里面的内容没有填充满
                "请完善所有分镜内容".showToast()
                return@setOnSingleClickListener
            }
            if (mRealVM.mRoleList.value.isNullOrEmpty()) {
                "请等待角色生成".showToast()
                return@setOnSingleClickListener
            }
            if (mRealVM.nextStepEnabled.get()) {//分镜生成完毕
                EventManager.onAppTrack(
                    AppEvent.JG_CLICK_GENERATE_STORYBOARD_PAGE_CONFIRM_BUTTON,
                    GrowingIO.KEY_ALL_COUNT_INT to mRealVM.totalStoryBoardCount.get().toString()
                )
                if (mRealVM.isFirstGenerate.get()) {//新用户首次生成
                    mRealVM.confirmGenerate {
                        hostVm?.step?.value = 2
                    }
                    return@setOnSingleClickListener
                }
                mRealVM.calculatePrice {
                    val cost = it ?: 0
                    ConfirmCostDialog.show(childFragmentManager, cost) {
                        mRealVM.confirmGenerate {
                            //User.get().storeScoreBalance(User.get().scoreBalance - cost)
                            hostVm?.step?.value = 2
                        }
                    }
                }
            }
        }

        mRealVM.showStoryboardEditor = { index, vM ->
            StoryboardEditorDialog.newInstance(mRealVM.dramaId.value, vM.data.get()) {
                if (!it.isNullOrEmpty()) {

                    vM.data.get()?.sceneContent = it
                    vM.showText.set(it)

                    mRealVM.inputAllComplete.set(!mRealVM.adapter.items.any { item -> item.isInputNotComplete() })
                    mRealVM.adapter.notifyItemChanged(index)
                }
            }.show(childFragmentManager)
        }
    }
}