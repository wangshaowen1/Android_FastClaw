package com.shiyuan.fastclaw.feature.mine.fragment

import android.text.SpannableString
import android.text.Spanned
import android.text.style.UnderlineSpan
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.nhsd.basic.BaseViewModelFragment
import com.nhsd.basic.ext.dp
import com.nhsd.basic.ext.getResString
import com.nhsd.basic.ext.setOnSingleClickListener
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.base.item.AppItemViewModel
import com.shiyuan.fastclaw.base.viewmodel.EmptyViewModel
import com.shiyuan.fastclaw.common.adapter.QuickAdapter
import com.shiyuan.fastclaw.common.ext.gone
import com.shiyuan.fastclaw.common.ext.visible
import com.shiyuan.fastclaw.core.ext.customerServiceTransfer
import com.shiyuan.fastclaw.databinding.FragmentMyOrderRecordBinding
import com.shiyuan.fastclaw.feature.mine.dialog.MyOrderCustomerServiceDialog
import com.shiyuan.fastclaw.widget.decoration.SpaceDecoration

class MyOrderRecordFragment :
    BaseViewModelFragment<FragmentMyOrderRecordBinding, EmptyViewModel>() {

    companion object {
        fun newInstance(): MyOrderRecordFragment = MyOrderRecordFragment()
    }

    private val orderAdapter by lazy {
        QuickAdapter<ItemMyOrderRecordVM>(
            R.layout.item_my_order_record,
            emptyList()
        )
    }

    override fun onPrepareView() {
        super.onPrepareView()
        initView()
    }

    private fun initView() {
        binding.rvOrderRecord.apply {
            layoutManager = LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false)
            isNestedScrollingEnabled = false
            if (itemDecorationCount == 0) {
                addItemDecoration(SpaceDecoration(16.dp, SpaceDecoration.Direction.MIDDLE))
            }
            adapter = orderAdapter
        }
        updateOrderRecords(emptyList())
        setProblemTipText()
        binding.tvProblemTip.setOnSingleClickListener {
            MyOrderCustomerServiceDialog.show(childFragmentManager) {
                requireActivity().customerServiceTransfer()
            }
        }
        // 配置空视图
        binding.lsvOrderRecord.setDefaultEmptyImage(R.drawable.ic_empty)
        binding.lsvOrderRecord.setNoDataPrompt("暂无订单")
    }

    private fun setProblemTipText() {
        val tip = getString(R.string.my_order_problem_tip)
        val service = getString(R.string.app_contact_customer_service)
        val start = tip.indexOf(service)
        if (start < 0) {
            binding.tvProblemTip.text = tip
            return
        }
        binding.tvProblemTip.text = SpannableString(tip).apply {
            setSpan(
                UnderlineSpan(),
                start,
                start + service.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
    }

    private fun updateOrderRecords(data: List<MyOrderRecord>) {
        orderAdapter.submitList(data.map(::ItemMyOrderRecordVM))
        if (data.isEmpty()) {
            binding.lsvOrderRecord.showEmpty()
            binding.tvProblemTip.gone()
        } else {
            binding.lsvOrderRecord.showContent()
            binding.tvProblemTip.visible()
        }
    }
}

data class MyOrderRecord(
    val orderNo: String,
    val title: String,
    val amount: String,
    val time: String
)

class ItemMyOrderRecordVM(data: MyOrderRecord) : AppItemViewModel<MyOrderRecord>(data)
