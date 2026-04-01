package com.kgmedia.fastfilm.feature.works.assetmanager

import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.kgmedia.fastfilm.common.ext.startRefreshWithAdapter
import com.kgmedia.fastfilm.databinding.FragmentAssetFolderBinding
import com.kgmedia.fastfilm.databinding.FragmentAssetsBinding
import com.kgmedia.fastfilm.widget.decoration.TopSpaceDecoration
import com.kgmedia.fastfilm.widget.stateful.paging.MyMultiStatePagingFooterAdapter
import com.nhsd.basic.BaseViewModelFragment
import com.nhsd.basic.ext.dp
import kotlinx.coroutines.launch

/**
 * 资产管理子列表（角色，场景，道具）
 */
class AssetsFragment : BaseViewModelFragment<FragmentAssetsBinding, AssetsChildViewModel>() {


    companion object {
        const val TAG = "AssetFolderFragment"
        fun newInstance(): AssetsFragment {
            val args = Bundle()
            val fragment = AssetsFragment()
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