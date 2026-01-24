package com.kgmedia.fastfilm.feature.creator.style

import androidx.core.os.bundleOf
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.GridLayoutManager
import com.kgmedia.fastfilm.common.Constants
import com.kgmedia.fastfilm.common.ext.getAdapterByItemPosition
import com.kgmedia.fastfilm.common.ext.startRefreshWithAdapter
import com.kgmedia.fastfilm.core.network.apis.entity.CreationStyleInfo
import com.kgmedia.fastfilm.databinding.DialogCreationStyleChooseBinding
import com.kgmedia.fastfilm.widget.decoration.BottomSpaceDecoration
import com.kgmedia.fastfilm.widget.decoration.GridSpacingItemDecoration
import com.kgmedia.fastfilm.widget.stateful.paging.MyMultiStatePagingFooterAdapter
import com.nhsd.basic.BaseBottomPushVMDialogFragment
import com.nhsd.basic.ext.dp
import kotlinx.coroutines.launch


class CreationStyleChooseDialog : BaseBottomPushVMDialogFragment<DialogCreationStyleChooseBinding, CreationStyleChooseViewModel>() {

    companion object {
        fun newInstance(lastChooseStyleId: String? = null): CreationStyleChooseDialog {
            val fragment = CreationStyleChooseDialog()
            fragment.arguments = bundleOf(
                Constants.Extra.ID to lastChooseStyleId
            )
            return fragment
        }
    }

    var onSuccess: (CreationStyleInfo?) -> Unit = {}
    val adapter by lazy { AllCreationStyleAdapter(mRealVM.lastStyleId) }

    override fun onPrepareView() {
        super.onPrepareView()
        binding.recyclerView.addItemDecoration(
            GridSpacingItemDecoration(
                4,
                10.dp,
                12.dp
            )
        )
        binding.recyclerView.layoutManager = GridLayoutManager(mActivity, 4).apply {
            spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                override fun getSpanSize(position: Int): Int {
                    val adapter = binding.recyclerView.adapter
                    if (adapter is ConcatAdapter) {
                        if (adapter.getAdapterByItemPosition(position) is MyMultiStatePagingFooterAdapter) {
                            return spanCount
                        }
                    }
                    return 1
                }
            }
        }
        binding.recyclerView.addItemDecoration(BottomSpaceDecoration(40.dp))
        binding.recyclerView.adapter = adapter.withLoadStateFooter(
            MyMultiStatePagingFooterAdapter(
                hostAdapter = adapter,
                loadMoreLoadEnd = "",
                refresh = {
                    adapter.retry()
                },
            )
        )
        binding.loadState.startRefreshWithAdapter(adapter)
        binding.btnConfirm.setOnClickListener {
            onSuccess.invoke(adapter.itemVm?.data?.get())
            dismissAllowingStateLoss()
        }

        lifecycleScope.launch {
            mRealVM.stylesGet.collect {
                adapter.submitData(it)
            }
        }
    }
}