package com.kgmedia.fastfilm.feature.works.assetmanager

import android.os.Bundle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.RecyclerView
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
            addItemDecoration(GridSpacingItemDecoration(3, 12.dp, 0))
            isFocusable = false
            itemAnimator = null
            assetFolderAdapter.stateRestorationPolicy =
                RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
            adapter =
                assetFolderAdapter.withLoadStateFooter(
                    MyMultiStatePagingFooterAdapter(
                        hostAdapter = assetFolderAdapter,
                        noMoreThreshold = 12
                    ) { assetFolderAdapter.retry() })
            binding.recyclerView.applyGridSpanSizeLookup()
        }
        binding.srl.startRefreshWithAdapter(
            pagingDataAdapter = assetFolderAdapter
        )
    }

    private fun dataUpdate() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                mRealVM.getAssetFolderPage().collect {
                    assetFolderAdapter.submitData(it)
                }
            }
        }
    }
}