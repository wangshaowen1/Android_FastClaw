package com.shiyuan.fastclaw.feature.clawagent.dialog

import android.content.Context
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.lxj.xpopup.XPopup
import com.lxj.xpopup.core.AttachPopupView
import com.lxj.xpopup.core.BasePopupView
import com.lxj.xpopup.enums.PopupPosition
import com.lxj.xpopup.interfaces.SimpleCallback
import com.nhsd.basic.ext.dp
import com.nhsd.basic.ext.setOnSingleClickListener
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.common.adapter.SingleBindingAdapter
import com.shiyuan.fastclaw.core.network.apis.response.JobSearchStatusOption
import com.shiyuan.fastclaw.databinding.ItemPopupClawAgentStatusBinding
import com.shiyuan.fastclaw.databinding.PopupClawAgentStatusBinding

class ClawAgentStatusPopup(
    context: Context,
    private val options: List<JobSearchStatusOption> = emptyList()
) : AttachPopupView(context) {

    private lateinit var mBinding: PopupClawAgentStatusBinding
    var onDismissListener: (() -> Unit)? = null
    var onItemClickListener: ((JobSearchStatusOption) -> Unit)? = null
    private val statusAdapter: SingleBindingAdapter<ItemPopupClawAgentStatusBinding, JobSearchStatusOption> by lazy {
        SingleBindingAdapter(
            R.layout.item_popup_claw_agent_status,
            emptyList()
        ) { binding, position, item ->
            binding.tvStatus.text = item.statusText.orEmpty()
            binding.ivSelected.visibility = if (item.selected == true) View.VISIBLE else View.GONE
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
        statusAdapter.submitList(options.sortedBy { it.sortOrder ?: Int.MAX_VALUE })
    }

    fun showDialog(anchorView: View): ClawAgentStatusPopup {
        XPopup.Builder(context)
            .atView(anchorView)
            .popupPosition(PopupPosition.Bottom)
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
