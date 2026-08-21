package com.shiyuan.fastclaw.feature.mine.fragment

import android.graphics.Typeface
import android.text.SpannableString
import android.text.Spanned
import android.text.style.UnderlineSpan
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewModelScope
import androidx.paging.LoadState
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.blankj.utilcode.util.ClipboardUtils
import com.nhsd.basic.BaseViewModelFragment
import com.nhsd.basic.ext.dp
import com.nhsd.basic.ext.setOnSingleClickListener
import com.nhsd.basic.ext.showToast
import com.shiyuan.fastclaw.BuildConfig
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.base.item.AppItemViewModel
import com.shiyuan.fastclaw.base.viewmodel.AppRepositoryViewModel
import com.shiyuan.fastclaw.common.ext.gone
import com.shiyuan.fastclaw.common.ext.startRefreshWithAdapter
import com.shiyuan.fastclaw.common.ext.visible
import com.shiyuan.fastclaw.common.utils.WXUtils
import com.shiyuan.fastclaw.core.AppRepository
import com.shiyuan.fastclaw.databinding.FragmentMyOrderRecordBinding
import com.shiyuan.fastclaw.feature.mine.MyOrderRecordPagingAdapter
import com.shiyuan.fastclaw.feature.mine.MyOrderRecordPagingSource
import com.shiyuan.fastclaw.feature.mine.dialog.MyOrderCustomerServiceDialog
import com.shiyuan.fastclaw.widget.decoration.SpaceDecoration
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MyOrderRecordFragment :
    BaseViewModelFragment<FragmentMyOrderRecordBinding, MyOrderRecordViewModel>() {

    companion object {
        fun newInstance(): MyOrderRecordFragment = MyOrderRecordFragment()
    }

    private val orderAdapter by lazy {
        MyOrderRecordPagingAdapter(::copyOrderNo)
    }

    override fun onPrepareView() {
        super.onPrepareView()
        initView()
        initObservers()
    }

    private fun initView() {
        binding.rvOrderRecord.apply {
            layoutManager = LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false)
            if (itemDecorationCount == 0) {
                addItemDecoration(SpaceDecoration(16.dp, SpaceDecoration.Direction.MIDDLE))
            }
            adapter = orderAdapter
        }
        setProblemTipText()
        binding.tvProblemTip.setOnSingleClickListener {
            MyOrderCustomerServiceDialog.show(childFragmentManager) {
                WXUtils.customerServiceChat(requireActivity(),BuildConfig.WX_KF_URL,BuildConfig.WX_KF_CORPID)
            }
        }
        // 配置空视图
        binding.lsvOrderRecord.setDefaultEmptyImage(R.drawable.ic_empty)
        binding.lsvOrderRecord.setNoDataPrompt("暂无订单")
        binding.lsvOrderRecord.setNoDataPromptSty(6f,14f, Typeface.NORMAL,requireActivity().getColor(R.color.color_85_3c3c43))
        binding.tvProblemTip.gone()
        binding.srl.startRefreshWithAdapter(
            pagingDataAdapter = orderAdapter,
            loadStateHolder = binding.lsvOrderRecord,
            addExtraLoadStateListener = { loadStates ->
                if (loadStates.refresh is LoadState.NotLoading) {
                    if (orderAdapter.itemCount == 0) {
                        binding.tvProblemTip.gone()
                    } else {
                        binding.tvProblemTip.visible()
                    }
                }
            }
        )
    }

    private fun initObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                mRealVM.orderRecordPagingFlow.collectLatest { pagingData ->
                    orderAdapter.submitData(pagingData)
                }
            }
        }
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

    private fun copyOrderNo(orderNo: String) {
        if (orderNo.isEmpty()) return
        ClipboardUtils.copyText(orderNo)
        getString(R.string.toast_copied).showToast()
    }
}

class MyOrderRecordViewModel : AppRepositoryViewModel<AppRepository>() {
    val orderRecordPagingFlow = Pager(
        config = PagingConfig(
            pageSize = ORDER_RECORD_PAGE_SIZE,
            initialLoadSize = ORDER_RECORD_PAGE_SIZE,
            enablePlaceholders = false
        ),
        pagingSourceFactory = {
            MyOrderRecordPagingSource(mRepo.api)
        }
    ).flow.cachedIn(viewModelScope)

    companion object {
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
