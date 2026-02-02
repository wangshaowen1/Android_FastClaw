package com.kgmedia.fastfilm.feature.creator.assets

import android.view.View
import com.kgmedia.fastfilm.R
import com.kgmedia.fastfilm.databinding.FragmentAssetManagementBinding
import com.nhsd.basic.BaseViewModelFragment

/**
 * 资产管理
 */
class AssetManagementFragment : BaseViewModelFragment<FragmentAssetManagementBinding, AssetManagementViewModel>() {

    override fun onPrepareView() {
        super.onPrepareView()
        binding.rgAssetType.setOnCheckedChangeListener { group, checkedId ->

        }
    }

    override fun <Data> onClick(view: View, data: Data?) {
        super.onClick(view, data)
        when (view.id) {
            R.id.rbRoles -> {
            }

            R.id.rbScenes -> {

            }

            R.id.rbProps -> {

            }

            R.id.btnOneKeyGenerate -> {

            }

            R.id.btnNext -> {

            }
        }
    }

}