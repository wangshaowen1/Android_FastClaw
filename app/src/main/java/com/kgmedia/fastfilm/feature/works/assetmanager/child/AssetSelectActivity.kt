package com.kgmedia.fastfilm.feature.works.assetmanager.child

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.kgmedia.fastfilm.common.Constants
import com.kgmedia.fastfilm.common.ext.startRefreshWithAdapter
import com.kgmedia.fastfilm.core.network.apis.entity.SimpleAsset
import com.kgmedia.fastfilm.core.network.apis.request.QueryAssetBody
import com.kgmedia.fastfilm.databinding.ActivityAssetSelectBinding
import com.kgmedia.fastfilm.widget.decoration.GridSpacingItemDecoration
import com.kgmedia.fastfilm.widget.decoration.SpaceDecoration
import com.kgmedia.fastfilm.widget.stateful.applyGridSpanSizeLookup
import com.kgmedia.fastfilm.widget.stateful.paging.MyMultiStatePagingFooterAdapter
import com.nhsd.basic.BaseViewModelActivity
import com.nhsd.basic.ext.dp
import com.nhsd.basic.ext.setOnSingleClickListener
import com.nhsd.basic.item.BaseItemEventsViewModel
import kotlinx.coroutines.launch

/**
 * 角色，场景，道具图片选择
 */
class AssetSelectActivity : BaseViewModelActivity<ActivityAssetSelectBinding, AssetSelectViewModel>() {


    companion object {
        const val TAG = "AssetSelectActivity"

        @JvmStatic
        fun getIntent(context: Context?, bizType: Int, workId: String?, appearanceId: String?): Intent? {
            if (context == null) return null
            val starter = Intent(context, AssetSelectActivity::class.java)
            starter.putExtra(Constants.Extra.TYPE, bizType)
            starter.putExtra(Constants.Extra.WORK_ID, workId)
            starter.putExtra(Constants.Extra.APPEARANCE_ID, appearanceId)
            return starter
        }
    }

    val appearanceId by lazy { intent.getStringExtra(Constants.Extra.APPEARANCE_ID) }
    val itemClickVm by lazy { BaseItemEventsViewModel<AssetsEvent, SimpleAsset>() }
    val assetsChildAdapter by lazy { AssetsChildAdapter(true, itemClickVm, appearanceId) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initView()
        dataUpdate()
        initListener()
    }

    fun initListener() {
        binding.btnCancel.setOnSingleClickListener {
            onBackPressed()
        }
        binding.btnConfirm.setOnSingleClickListener {
            if (mRealVM.selectedItem != null) {
                setResult(RESULT_OK, Intent().apply {
                    putExtra(Constants.Extra.DATA, mRealVM.selectedItem)
                })
            }
            finish()
        }
    }

    private fun initView() {
        binding.recyclerView.apply {
            addItemDecoration(SpaceDecoration(14.dp))

            isFocusable = false
            if (mRealVM.bizType == QueryAssetBody.BIZ_TYPE_SCENE) {
                layoutManager = GridLayoutManager(this@AssetSelectActivity, 2)
                addItemDecoration(GridSpacingItemDecoration(2, 12.dp, 0))
            } else {
                addItemDecoration(GridSpacingItemDecoration(3, 12.dp, 0))
            }
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
        lifecycleScope.launch {
            mRealVM.getAssetsPage().collect {
                assetsChildAdapter.submitData(it)
            }
        }

        lifecycleScope.launch {
            itemClickVm.events.collect {
                when (it) {
                    is AssetsEvent.Click -> {

                    }

                    is AssetsEvent.Select -> {
                        mRealVM.selectedItem = it.item?.data?.get()
                    }
                }
            }
        }
    }
}