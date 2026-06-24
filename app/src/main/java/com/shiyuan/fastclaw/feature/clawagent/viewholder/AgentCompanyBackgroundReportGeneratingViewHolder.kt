package com.shiyuan.fastclaw.feature.clawagent.viewholder

import android.animation.ObjectAnimator
import android.view.animation.LinearInterpolator
import androidx.recyclerview.widget.RecyclerView
import com.shiyuan.fastclaw.databinding.ItemMsgAgentCompanyBackgroundReportGeneratingBinding
import com.shiyuan.fastclaw.feature.clawagent.bean.ClawAgentMessageInfo

/**
 * 公司背调报告 - 生成中
 */
class AgentCompanyBackgroundReportGeneratingViewHolder(private val binding: ItemMsgAgentCompanyBackgroundReportGeneratingBinding) : RecyclerView.ViewHolder(binding.root) {

    private var rotateStepAnimator: ObjectAnimator? = null
    private var rotateGenerateAnimator: ObjectAnimator? = null

    fun bind(item: ClawAgentMessageInfo) {
        var data = item.metaData
    }

    fun startRotate() {
        if (null == rotateStepAnimator || rotateStepAnimator?.isStarted == false) {
            rotateStepAnimator = ObjectAnimator.ofFloat(binding.tvStep3Icon, "rotation", 0f, 360f).apply {
                    duration = 1000L
                    interpolator = LinearInterpolator()
                    repeatCount = ObjectAnimator.INFINITE
                    repeatMode = ObjectAnimator.RESTART
                    start()
                }
        }
        if (null == rotateGenerateAnimator || rotateGenerateAnimator?.isStarted == false) {
            rotateGenerateAnimator = ObjectAnimator.ofFloat(binding.ivGenerating, "rotation", 0f, 360f).apply {
                duration = 1000L
                interpolator = LinearInterpolator()
                repeatCount = ObjectAnimator.INFINITE
                repeatMode = ObjectAnimator.RESTART
                start()
            }
        }
    }

    fun stopRotate() {
        rotateStepAnimator?.apply {
            cancel()
            removeAllListeners()
            removeAllUpdateListeners()
        }
        rotateGenerateAnimator?.apply {
            cancel()
            removeAllListeners()
            removeAllUpdateListeners()
        }
        rotateStepAnimator = null
        rotateGenerateAnimator = null
        binding.tvStep3Icon.rotation = 0f
        binding.ivGenerating.rotation = 0f
    }
}
