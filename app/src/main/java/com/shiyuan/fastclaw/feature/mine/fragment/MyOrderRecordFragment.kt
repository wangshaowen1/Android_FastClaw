package com.shiyuan.fastclaw.feature.mine.fragment

import android.graphics.Typeface
import android.text.SpannableString
import android.text.Spanned
import android.text.style.UnderlineSpan
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.nhsd.basic.BaseViewModelFragment
import com.nhsd.basic.ext.dp
import com.nhsd.basic.ext.setOnSingleClickListener
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.base.item.AppItemViewModel
import com.shiyuan.fastclaw.base.viewmodel.AppRepositoryViewModel
import com.shiyuan.fastclaw.common.adapter.QuickAdapter
import com.shiyuan.fastclaw.common.ext.gone
import com.shiyuan.fastclaw.common.ext.visible
import com.shiyuan.fastclaw.common.utils.WXUtils
import com.shiyuan.fastclaw.core.AppRepository
import com.shiyuan.fastclaw.core.ext.request
import com.shiyuan.fastclaw.core.network.apis.request.PagedBody
import com.shiyuan.fastclaw.core.network.apis.response.PayOrderInfo
import com.shiyuan.fastclaw.databinding.FragmentMyOrderRecordBinding
import com.shiyuan.fastclaw.feature.mine.dialog.MyOrderCustomerServiceDialog
import com.shiyuan.fastclaw.widget.decoration.SpaceDecoration

class MyOrderRecordFragment :
    BaseViewModelFragment<FragmentMyOrderRecordBinding, MyOrderRecordViewModel>() {

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
        setProblemTipText()
        binding.tvProblemTip.setOnSingleClickListener {
            MyOrderCustomerServiceDialog.show(childFragmentManager) {
                WXUtils.customerServiceChatWeb(requireActivity(),"https://work.weixin.qq.com/kfid/kfc40a9a48f69a47f52")
            }
        }
        // 配置空视图
        binding.lsvOrderRecord.setDefaultEmptyImage(R.drawable.ic_empty)
        binding.lsvOrderRecord.setNoDataPrompt("暂无订单")
        binding.lsvOrderRecord.setNoDataPromptSty(6f,14f, Typeface.NORMAL,requireActivity().getColor(R.color.color_85_3c3c43))
        binding.tvProblemTip.gone()
        binding.lsvOrderRecord.showLoading()
        mRealVM.orderRecords.observe(viewLifecycleOwner) { records ->
            updateOrderRecords(records.orEmpty())
        }
        mRealVM.loadOrderRecords()
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

class MyOrderRecordViewModel : AppRepositoryViewModel<AppRepository>() {
    val orderRecords = MutableLiveData<List<MyOrderRecord>>()

    fun loadOrderRecords() {
        request(
            apiBlock = {
                mRepo.api.queryPayOrderList(
                    PagedBody(ORDER_RECORD_FIRST_PAGE, ORDER_RECORD_PAGE_SIZE)
                )
            },
            showToast = true,
            onError = {
                orderRecords.value = emptyList()
            },
            onSuccess = {
                orderRecords.value = data?.items.orEmpty().map { it.toOrderRecord() }
            }
        )
    }

    private fun PayOrderInfo.toOrderRecord(): MyOrderRecord {
        return MyOrderRecord(
            orderNo = orderNo.orEmpty(),
            title = goodsName.orEmpty(),
            amount = payAmountText.orEmpty(),
            time = createTime.orEmpty()
        )
    }

    companion object {
        private const val ORDER_RECORD_FIRST_PAGE = 1
        private const val ORDER_RECORD_PAGE_SIZE = 20
    }
}

data class MyOrderRecord(
    val orderNo: String,
    val title: String,
    val amount: String,
    val time: String
)

class ItemMyOrderRecordVM(data: MyOrderRecord) : AppItemViewModel<MyOrderRecord>(data)
