package com.kgmedia.fastfilm.feature.works.assetmanager

import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.kgmedia.fastfilm.common.ext.startRefreshWithAdapter
import com.kgmedia.fastfilm.databinding.FragmentAssetFolderBinding
import com.kgmedia.fastfilm.widget.decoration.GridSpacingItemDecoration
import com.kgmedia.fastfilm.widget.decoration.SpaceDecoration
import com.kgmedia.fastfilm.widget.decoration.TopSpaceDecoration
import com.kgmedia.fastfilm.widget.stateful.applyGridSpanSizeLookup
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

    val assetFolderAdapter by lazy { AssetFolderAdapter() }

    override fun onPrepareView() {
        super.onPrepareView()
        initView()
        dataUpdate()
    }


    private fun initView() {
        binding.recyclerView.apply {
            addItemDecoration(SpaceDecoration(12.dp))
            addItemDecoration(GridSpacingItemDecoration(3, 0, 23.dp))
            isFocusable = false
            adapter =
                assetFolderAdapter.withLoadStateFooter(
                    MyMultiStatePagingFooterAdapter(
                        hostAdapter = assetFolderAdapter,
                        noMoreThreshold = 9
                    ) { assetFolderAdapter.retry() })
            binding.recyclerView.applyGridSpanSizeLookup()
        }
        binding.srl.startRefreshWithAdapter(
            pagingDataAdapter = assetFolderAdapter
        )
    }

    private fun dataUpdate() {
        lifecycleScope.launch {
            mRealVM.getAssetFolderPage().collect {
                assetFolderAdapter.submitData(it)
            }
        }
    }
}