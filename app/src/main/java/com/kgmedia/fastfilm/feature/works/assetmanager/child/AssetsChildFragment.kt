package com.kgmedia.fastfilm.feature.works.assetmanager.child

import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.kgmedia.fastfilm.common.Constants
import com.kgmedia.fastfilm.common.ext.startRefreshWithAdapter
import com.kgmedia.fastfilm.core.network.apis.entity.SimpleAsset
import com.kgmedia.fastfilm.core.network.apis.entity.SimpleRoleInfo
import com.kgmedia.fastfilm.core.network.apis.request.QueryAssetBody
import com.kgmedia.fastfilm.databinding.FragmentAssetsChildBinding
import com.kgmedia.fastfilm.feature.works.assetmanager.AssetDetailActivity
import com.kgmedia.fastfilm.feature.works.assetmanager.AssetsViewModel
import com.kgmedia.fastfilm.feature.works.assetmanager.roleimage.RoleImageSelectActivity
import com.kgmedia.fastfilm.widget.decoration.GridSpacingItemDecoration
import com.kgmedia.fastfilm.widget.decoration.SpaceDecoration
import com.kgmedia.fastfilm.widget.stateful.applyGridSpanSizeLookup
import com.kgmedia.fastfilm.widget.stateful.paging.MyMultiStatePagingFooterAdapter
import com.nhsd.basic.BaseViewModelFragment
import com.nhsd.basic.ext.dp
import com.nhsd.basic.ext.getViewModelOfActivity
import com.nhsd.basic.item.BaseItemEventsViewModel
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

    val itemClickVm by lazy { BaseItemEventsViewModel<AssetsEvent, SimpleAsset>() }
    val assetsChildAdapter by lazy { AssetsChildAdapter(itemClickVm = itemClickVm) }
    val parentVm by lazy { getViewModelOfActivity<AssetsViewModel>() }

    override fun onPrepareView() {
        super.onPrepareView()
        initView()
        dataUpdate()
    }

    private fun initView() {
        binding.recyclerView.apply {
            addItemDecoration(SpaceDecoration(12.dp))
            addItemDecoration(GridSpacingItemDecoration(3, 12.dp, 0))
            if (mRealVM.bizType == QueryAssetBody.BIZ_TYPE_SCENE) {
                layoutManager = GridLayoutManager(mActivity, 2)
            }
            isFocusable = false
            adapter =
                assetsChildAdapter.withLoadStateFooter(
                    MyMultiStatePagingFooterAdapter(
                        hostAdapter = assetsChildAdapter,
                        noMoreThreshold = 5
                    ) { assetsChildAdapter.retry() })
            applyGridSpanSizeLookup()
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
        lifecycleScope.launch {
            itemClickVm.events.collect {
                when (it) {
                    is AssetsEvent.Click -> {
                        val assetData = it.item.data.get()
                        if (assetData is SimpleRoleInfo) {
                            RoleImageSelectActivity.start(mActivity, mRealVM.workId, assetData)
                        } else if (assetData is SimpleAsset) {
                            AssetDetailActivity.start(mActivity, mRealVM.workId, assetData)
                        }
                    }

                    is AssetsEvent.Select -> {
                    }
                }
            }
        }
    }
}