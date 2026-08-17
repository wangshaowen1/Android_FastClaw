package com.shiyuan.fastclaw.feature.clawagent.dialog

import android.content.Context
import com.lxj.xpopup.XPopup
import com.lxj.xpopup.core.BasePopupView
import com.lxj.xpopup.core.CenterPopupView
import com.lxj.xpopup.interfaces.SimpleCallback
import com.nhsd.basic.ext.setOnSingleClickListener
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.databinding.DialogClawAgentJobRecommendationBinding

/** 小虾推荐工作提示弹窗 */
class ClawAgentJobRecommendationDialog(context: Context) : CenterPopupView(context) {

    private lateinit var binding: DialogClawAgentJobRecommendationBinding
    var onDismissListener: (() -> Unit)? = null

    override fun getImplLayoutId(): Int = R.layout.dialog_claw_agent_job_recommendation

    override fun onCreate() {
        binding = DialogClawAgentJobRecommendationBinding.bind(popupImplView)
        binding.sbConfirm.setOnSingleClickListener {
            dismiss()
        }
    }

    fun showDialog(): ClawAgentJobRecommendationDialog {
        XPopup.Builder(activity)
            .isDestroyOnDismiss(false)
            .dismissOnBackPressed(false)
            .dismissOnTouchOutside(true)
            .setPopupCallback(object : SimpleCallback() {
                override fun onDismiss(popupView: BasePopupView) {
                    onDismissListener?.invoke()
                }
            })
            .asCustom(this)
            .show()
        return this
    }
}
