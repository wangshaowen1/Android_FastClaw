package com.kgmedia.fastfilm.feature.creator.assets

import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.kgmedia.fastfilm.R
import com.kgmedia.fastfilm.core.network.apis.entity.SimpleAsset
import com.kgmedia.fastfilm.core.network.apis.entity.SimplePropInfo
import com.kgmedia.fastfilm.core.network.apis.entity.SimpleRoleInfo
import com.kgmedia.fastfilm.core.network.apis.entity.SimpleSceneInfo
import com.kgmedia.fastfilm.databinding.FragmentAssetManagementBinding
import com.nhsd.basic.BaseViewModelFragment

/**
 * 资产管理
 */
class AssetManagementFragment : BaseViewModelFragment<FragmentAssetManagementBinding, AssetManagementViewModel>() {

    private var firstItem: SimpleAsset? = null
        set(value) {
            field = value
            when (value) {
                is SimpleRoleInfo -> bindingNullable?.rgAssetType?.check(R.id.rbRoles)
                is SimpleSceneInfo -> bindingNullable?.rgAssetType?.check(R.id.rbScenes)
                is SimplePropInfo -> bindingNullable?.rgAssetType?.check(R.id.rbProps)
            }
        }
    private val layoutManager get() = bindingNullable?.rvAssets?.layoutManager as? LinearLayoutManager

    override fun onPrepareView() {
        super.onPrepareView()
        binding.rvAssets.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                layoutManager?.findFirstVisibleItemPosition()?.let { position ->
                    val item = mRealVM.adapter.getItem(position)?.data?.get()
                    if (item is SimpleAsset && item.javaClass != firstItem?.javaClass) {
                        firstItem = item
                    }
                }
            }
        })
    }

    override fun <Data> onClick(view: View, data: Data?) {
        super.onClick(view, data)
        when (view.id) {
            R.id.rbRoles -> {
                scrollToRoles()
            }

            R.id.rbScenes -> {
                scrollToScenes()
            }

            R.id.rbProps -> {
                scrollToProps()
            }

            R.id.btnNext -> {
                // TODO: 分镜管理
            }
        }
    }

    private fun scrollToRoles() {
        mRealVM.adapter.items.indexOfFirst { it.data.get() is SimpleRoleInfo }.takeIf { it > -1 }?.let {
            layoutManager?.scrollToPositionWithOffset(it, 0)
        }
    }

    private fun scrollToScenes() {
        mRealVM.adapter.items.indexOfFirst { it.data.get() is SimpleSceneInfo }.takeIf { it > -1 }?.let {
            layoutManager?.scrollToPositionWithOffset(it, 0)
        }
    }

    private fun scrollToProps() {
        mRealVM.adapter.items.indexOfFirst { it.data.get() is SimplePropInfo }.takeIf { it > -1 }?.let {
            layoutManager?.scrollToPositionWithOffset(it, 0)
        }
    }

}