package com.shiyuan.fastclaw.feature.clawagent

import android.content.Context
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.lxj.xpopup.XPopup
import com.lxj.xpopup.core.AttachPopupView
import com.lxj.xpopup.core.BasePopupView
import com.lxj.xpopup.enums.PopupAnimation
import com.lxj.xpopup.enums.PopupPosition
import com.lxj.xpopup.interfaces.SimpleCallback
import com.nhsd.basic.ext.dp
import com.nhsd.basic.ext.setOnSingleClickListener
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.common.adapter.SingleBindingAdapter
import com.shiyuan.fastclaw.core.network.apis.response.OnlineResumeAttributeData
import com.shiyuan.fastclaw.core.network.apis.response.OnlineResumeAttributeOption
import com.shiyuan.fastclaw.databinding.ItemPopupClawAgentStatusBinding
import com.shiyuan.fastclaw.databinding.PopupClawAgentStatusBinding

class ClawAgentStatusDialog(context: Context, private val attributeData: OnlineResumeAttributeData) : AttachPopupView(context) {

    private lateinit var mBinding: PopupClawAgentStatusBinding
    var onDismissListener: (() -> Unit)? = null
    var onItemClickListener: ((OnlineResumeAttributeOption) -> Unit)? = null
    private val statusAdapter: SingleBindingAdapter<ItemPopupClawAgentStatusBinding, OnlineResumeAttributeOption> by lazy {
        SingleBindingAdapter(
            R.layout.item_popup_claw_agent_status,
            emptyList()
        ) { binding, position, item ->
            binding.tvStatus.text = item.name.orEmpty()
            binding.ivSelected.visibility = if (attributeData.displayValue == item.name) {
                View.VISIBLE
            } else {
                View.GONE
            }
            binding.vDivider.visibility = if (position == statusAdapter.itemCount - 1) {
                View.INVISIBLE
            } else {
                View.VISIBLE
            }
            binding.root.setOnSingleClickListener {
                onItemClickListener?.invoke(item)
                dismiss()
            }
        }
    }

    init {
        addInnerContent()
    }

    override fun getImplLayoutId(): Int = R.layout.popup_claw_agent_status

    override fun onCreate() {
        mBinding = PopupClawAgentStatusBinding.bind(popupImplView)
        mBinding.rvStatus.layoutManager = LinearLayoutManager(context)
        mBinding.rvStatus.adapter = statusAdapter
        statusAdapter.submitList(attributeData.options.orEmpty().sortedBy { it.sortOrder ?: Long.MAX_VALUE })
    }

    fun showDialog(anchorView: View): ClawAgentStatusDialog {
        XPopup.Builder(context)
            .isViewMode(true)
            .atView(anchorView)
            .popupPosition(PopupPosition.Bottom)
            .popupAnimation(PopupAnimation.ScrollAlphaFromTop)
            .popupWidth(151.dp)
            .borderRadius(20.dp.toFloat())
            .offsetY(4.dp)
            .hasShadowBg(false)
            .dismissOnTouchOutside(true)
            .dismissOnBackPressed(true)
            .isDestroyOnDismiss(true)
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
