package com.kgmedia.fastfilm.feature.creator.assets

import android.view.View
import androidx.databinding.Observable
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.kgmedia.fastfilm.R
import com.kgmedia.fastfilm.common.ext.applySmartFocusManager
import com.kgmedia.fastfilm.core.Global
import com.kgmedia.fastfilm.core.manager.AppEvent
import com.kgmedia.fastfilm.core.manager.EventManager
import com.kgmedia.fastfilm.databinding.FragmentAssetManagementBinding
import com.kgmedia.fastfilm.feature.creator.generate.StoryEditorViewModel
import com.nhsd.basic.BaseViewModelFragment
import com.nhsd.basic.ext.getParentViewModel

/**
 * 资产管理
 */
class AssetManagementFragment : BaseViewModelFragment<FragmentAssetManagementBinding, AssetManagementViewModel>() {

    companion object {
        fun newInstance() = AssetManagementFragment()
    }

    private var firstItem: Any? = null
        set(value) {
            field = value
            when (value) {
                is ItemRoleVM -> bindingNullable?.rgAssetType?.check(R.id.rbRoles)
                is ItemSceneVM -> bindingNullable?.rgAssetType?.check(R.id.rbScenes)
                is ItemPropVM -> bindingNullable?.rgAssetType?.check(R.id.rbProps)
            }
        }
    private val layoutManager get() = bindingNullable?.rvAssets?.layoutManager as? LinearLayoutManager
    private val hostVm by lazy { getParentViewModel<StoryEditorViewModel>() }

    override fun onPrepareView() {
        super.onPrepareView()
        binding.root.applySmartFocusManager()
        mRealVM.workId = hostVm?.workId
        mRealVM.storyId = hostVm?.storyId
        mRealVM.taskId = hostVm?.taskId
        mRealVM.step.addOnPropertyChangedCallback(object : Observable.OnPropertyChangedCallback() {
            override fun onPropertyChanged(sender: Observable?, propertyId: Int) {
                Global.refreshBalance()
            }
        })
        mRealVM.retry()
        binding.rvAssets.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                layoutManager?.findFirstVisibleItemPosition()?.let { position ->
                    val item = mRealVM.adapter.getItem(position)
                    if (item?.javaClass != firstItem?.javaClass) {
                        firstItem = item
                    }
                }
            }
        })
        EventManager.onAppTrack(AppEvent.SF_VIEW_ASSET_MANAGEMENT_PAGE)
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
                mRealVM.clickNext { workId, storyId, taskId ->
                    hostVm?.nextStep(4, workId, storyId, taskId)
                }
            }
        }
    }

    private fun scrollToRoles() {
        mRealVM.adapter.items.indexOfFirst { it is ItemRoleVM }.takeIf { it > -1 }?.let {
            layoutManager?.scrollToPositionWithOffset(it, 0)
        }
    }

    private fun scrollToScenes() {
        mRealVM.adapter.items.indexOfFirst { it is ItemSceneVM }.takeIf { it > -1 }?.let {
            layoutManager?.scrollToPositionWithOffset(it, 0)
        }
    }

    private fun scrollToProps() {
        mRealVM.adapter.items.indexOfFirst { it is ItemPropVM }.takeIf { it > -1 }?.let {
            layoutManager?.scrollToPositionWithOffset(it, 0)
        }
    }

}