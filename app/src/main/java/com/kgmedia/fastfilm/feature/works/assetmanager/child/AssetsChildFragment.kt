package com.kgmedia.fastfilm.feature.works.assetmanager.child

import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.kgmedia.fastfilm.common.Constants
import com.kgmedia.fastfilm.common.ext.startRefreshWithAdapter
import com.kgmedia.fastfilm.core.network.apis.request.QueryAssetBody
import com.kgmedia.fastfilm.databinding.FragmentAssetsChildBinding
import com.kgmedia.fastfilm.feature.works.assetmanager.AssetsViewModel
import com.kgmedia.fastfilm.widget.decoration.TopSpaceDecoration
import com.kgmedia.fastfilm.widget.stateful.paging.MyMultiStatePagingFooterAdapter
import com.nhsd.basic.BaseViewModelFragment
import com.nhsd.basic.ext.dp
import com.nhsd.basic.ext.getViewModelOfActivity
import kotlinx.coroutines.launch

/**
 * 资产管理子列表（角色，场景，道具）
 */
class AssetsChildFragment : BaseViewModelFragment<FragmentAssetsChildBinding, AssetsChildViewModel>() {


    companion object {
        const val TAG = "AssetsChildFragment"
        fun newInstance(
            bizType: Int,
            workId: String? = null,
            storyId: String? = null,
            assetId: String? = null,
            appearanceId: String? = null
        ): AssetsChildFragment {
            val args = Bundle()
            args.putInt(Constants.Extra.TYPE, bizType)
            args.putString(Constants.Extra.WORK_ID, workId)
            args.putString(Constants.Extra.STORY_ID, storyId)
            args.putString(Constants.Extra.ASSET_ID, assetId)
            args.putString(Constants.Extra.APPEARANCE_ID, appearanceId)
            val fragment = AssetsChildFragment()
            fragment.arguments = args
            return fragment
        }
    }

    val assetsChildAdapter by lazy { AssetsChildAdapter() }
    val parentVm by lazy { getViewModelOfActivity<AssetsViewModel>() }

    override fun onPrepareView() {
        super.onPrepareView()
        initView()
        dataUpdate()
    }

    private fun initView() {
        binding.recyclerView.apply {
            addItemDecoration(TopSpaceDecoration(14.dp))
            isFocusable = false
            adapter =
                assetsChildAdapter.withLoadStateFooter(
                    MyMultiStatePagingFooterAdapter(
                        hostAdapter = assetsChildAdapter,
                        noMoreThreshold = 5
                    ) { assetsChildAdapter.retry() })
        }
        binding.srl.startRefreshWithAdapter(
            pagingDataAdapter = assetsChildAdapter
        )
    }

    private fun dataUpdate() {
        mRealVM.totalCountBack = {
            when (mRealVM.bizType) {
                QueryAssetBody.BIZ_TYPE_ROLE -> {
                    parentVm?.roleCount?.value = it
                }

                QueryAssetBody.BIZ_TYPE_PROP -> {
                    parentVm?.propCount?.value = it
                }

                QueryAssetBody.BIZ_TYPE_SCENE -> {
                    parentVm?.sceneCount?.value = it
                }
            }
        }
        lifecycleScope.launch {
            mRealVM.getAssetsPage().collect {
                assetsChildAdapter.submitData(it)
            }
        }
    }
}