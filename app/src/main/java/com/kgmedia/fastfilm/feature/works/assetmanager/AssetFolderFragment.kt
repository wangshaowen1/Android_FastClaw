package com.kgmedia.fastfilm.feature.works.assetmanager

import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.kgmedia.fastfilm.common.ext.startRefreshWithAdapter
import com.kgmedia.fastfilm.databinding.FragmentAssetFolderBinding
import com.kgmedia.fastfilm.widget.decoration.TopSpaceDecoration
import com.kgmedia.fastfilm.widget.stateful.paging.MyMultiStatePagingFooterAdapter
import com.nhsd.basic.BaseViewModelFragment
import com.nhsd.basic.ext.dp
import kotlinx.coroutines.launch

/**
 * 资产管理文件夹
 */
class AssetFolderFragment : BaseViewModelFragment<FragmentAssetFolderBinding, AssetFolderViewModel>() {


    companion object {
        const val TAG = "AssetFolderFragment"
        fun newInstance(): AssetFolderFragment {
            val args = Bundle()
            val fragment = AssetFolderFragment()
            fragment.arguments = args
            return fragment
        }
    }

    override fun onPrepareView() {
        super.onPrepareView()
    }


    private fun initView() {
//        binding.recyclerView.apply {
//            addItemDecoration(TopSpaceDecoration(14.dp))
//            isFocusable = false
//            adapter =
//                computingPowerDetailsAdapter.withLoadStateFooter(
//                    MyMultiStatePagingFooterAdapter(
//                        hostAdapter = computingPowerDetailsAdapter,
//                        noMoreThreshold = 5
//                    ) { computingPowerDetailsAdapter.retry() })
//        }
//        binding.srl.startRefreshWithAdapter(
//            pagingDataAdapter = computingPowerDetailsAdapter
//        )
    }


    private fun dataUpdate() {
        lifecycleScope.launch {
//            mRealVM.getComputingPowerDetailsPage().collect {
//                computingPowerDetailsAdapter.submitData(it)
//            }
        }
    }
}