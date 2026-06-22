package com.shiyuan.fastclaw.feature.clawagent.viewholder

import android.animation.ObjectAnimator
import android.view.animation.LinearInterpolator
import androidx.recyclerview.widget.RecyclerView
import com.shiyuan.fastclaw.databinding.ItemMsgAgentMockInterviewReportGeneratingBinding
import com.shiyuan.fastclaw.feature.clawagent.bean.ClawAgentMessageInfo

/**
 * 面试复盘报告 - 生成中
 */
class AgentMockInterviewReportGeneratingViewHolder(private val binding: ItemMsgAgentMockInterviewReportGeneratingBinding) : RecyclerView.ViewHolder(binding.root) {

    private var rotateAnimator: ObjectAnimator? = null

    fun bind(item: ClawAgentMessageInfo) = Unit

    fun startRotate() {
        if (rotateAnimator?.isStarted == true) return
        rotateAnimator = ObjectAnimator.ofFloat(binding.tvStep3Icon, "rotation", 0f, 360f).apply {
            duration = 1000L
            interpolator = LinearInterpolator()
            repeatCount = ObjectAnimator.INFINITE
            repeatMode = ObjectAnimator.RESTART
            start()
        }
    }

    fun stopRotate() {
        rotateAnimator?.apply {
            cancel()
            removeAllListeners()
            removeAllUpdateListeners()
        }
        rotateAnimator = null
        binding.tvStep3Icon.rotation = 0f
    }
}
