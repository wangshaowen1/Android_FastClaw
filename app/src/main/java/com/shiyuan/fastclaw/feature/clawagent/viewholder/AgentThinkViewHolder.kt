package com.shiyuan.fastclaw.feature.clawagent.viewholder

import android.animation.ObjectAnimator
import android.view.animation.LinearInterpolator
import androidx.recyclerview.widget.RecyclerView
import com.shiyuan.fastclaw.databinding.ItemMsgAgentThinkBinding
import com.shiyuan.fastclaw.feature.clawagent.bean.ClawAgentMessageInfo

/** agent思考消息
 * @createtime 2026年05月25日 14:51
 * @author TooWhiteT
 */

class AgentThinkViewHolder(private val binding: ItemMsgAgentThinkBinding) : RecyclerView.ViewHolder(binding.root) {

    private var rotateAnimator: ObjectAnimator? = null

    fun bind(item: ClawAgentMessageInfo) {
        binding.tvThink.text = "${item.content}"
    }

    /** 启动 iv_think 旋转动画；附着到窗口时由 Adapter 调用 */
    fun startRotate() {
        if (rotateAnimator?.isStarted == true) return
        rotateAnimator = ObjectAnimator.ofFloat(binding.ivThink, "rotation", 0f, 360f).apply {
            duration = 1000L
            interpolator = LinearInterpolator()
            repeatCount = ObjectAnimator.INFINITE
            repeatMode = ObjectAnimator.RESTART
            start()
        }
    }

    /** 停止并释放动画；脱离窗口或回收时由 Adapter 调用，避免 Animator 持有 View 造成泄漏 */
    fun stopRotate() {
        rotateAnimator?.apply {
            cancel()
            removeAllListeners()
            removeAllUpdateListeners()
        }
        rotateAnimator = null
        binding.ivThink.rotation = 0f
    }

}