package com.kgmedia.fastfilm.feature.creator.character

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.kgmedia.fastfilm.R
import com.kgmedia.fastfilm.common.Constants
import com.kgmedia.fastfilm.common.ext.showCommonDialog
import com.kgmedia.fastfilm.core.manager.AppEvent
import com.kgmedia.fastfilm.core.manager.EventManager
import com.kgmedia.fastfilm.core.network.apis.entity.CharacterInfo
import com.kgmedia.fastfilm.databinding.FragmentConfirmDramaCharacterBinding
import com.kgmedia.fastfilm.feature.creator.generate.edit.CharacterRegenerateDialog
import com.kgmedia.fastfilm.feature.editor.ShortDramaEditorActivity
import com.kgmedia.fastfilm.widget.decoration.BottomSpaceDecoration
import com.kgmedia.fastfilm.widget.decoration.GridSpacingItemDecoration
import com.nhsd.basic.BaseViewModelFragment
import com.nhsd.basic.ext.dp
import com.nhsd.basic.ext.showToast
import kotlinx.coroutines.launch


class CharacterFragment :
    BaseViewModelFragment<FragmentConfirmDramaCharacterBinding, CharactersViewModel>() {

    companion object {

        fun newInstance(dramaId: String?): CharacterFragment {
            val args = Bundle()
            args.putString(Constants.Extra.ID, dramaId)
            val fragment = CharacterFragment()
            fragment.arguments = args
            return fragment
        }
    }

    override fun onPrepareView() {
        super.onPrepareView()
        initView()
        //数据更新
        dataUpdate()
        EventManager.onAppTrack(AppEvent.JG_VIEW_CHARACTER_CONFIRM_PAGE)
    }

    private fun initView() {
        binding.recyclerView.apply {
            layoutAnimation = null
            itemAnimator = null
            isFocusable = false
            layoutManager = GridLayoutManager(mActivity, 3)
            addItemDecoration(GridSpacingItemDecoration(16.dp, 16.dp))
            addItemDecoration(BottomSpaceDecoration(70.dp))
        }
    }

    val switchRoleLauncher: ActivityResultLauncher<Intent> = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == RESULT_OK) {
            mRealVM.getRoleList()
        }
    }


    private fun dataUpdate() {
        mRealVM.getRoleList()
        lifecycleScope.launch {
            mRealVM.eventVm.events.collect { event ->
                if (event is CharacterEvent.Regenerate) {
                    regenerate(event.character)
                    return@collect
                }
                if (event is CharacterEvent.HistoryRoles) {
                    CharacterHistoryActivity.startForResult(switchRoleLauncher, mActivity, mRealVM.dramaId, event.character)
                    return@collect
                }
                if (event is CharacterEvent.DelRole) {
                    if (mRealVM.characterVms.size <= 1) {
                        "至少保留一个角色".showToast()
                        return@collect
                    }
                    mActivity.showCommonDialog("删除后生成画面将不会出现该角色，请谨慎操作", "确认要删除角色吗？", "删除") {
                        mRealVM.roleDel(event.character?.roleId) {
                            "已删除".showToast()
                            mRealVM.getRoleList()
                        }
                    }
                    return@collect
                }
            }
        }
    }

    override fun <Data> onClick(view: View, data: Data?) {
        super.onClick(view, data)
        when (view.id) {
            R.id.generating -> {
                if (mRealVM.generationSuccess.get() != true) {
                    "角色形象生成中，请稍候".showToast()
                    return
                }
                EventManager.onAppTrack(AppEvent.JG_CLICK_CHARACTER_CONFIRM_PAGE_GENERATE_IMAGE_BUTTON)
                mRealVM.generatingSegmentImage {
                    mActivity.finish()
                    ShortDramaEditorActivity.start(it)
                }
            }

            R.id.btnAddRole -> {
                EventManager.onAppTrack(AppEvent.JG_CLICK_ADD_ROLE_PAGE_ADD_BUTTON)
//                CharacterHistoryActivity.start(mActivity,mRealVM.dramaId, mRealVM.characterVms[0].data.get())
                regenerate(null, true)
            }
        }
    }

    fun regenerate(character: CharacterInfo?, isRoleAdd: Boolean = false) {
        CharacterRegenerateDialog.newInstance(mRealVM.dramaId, character, isRoleAdd).apply {
            onSuccess = {
                //charactersAdapter.refresh()
                this@CharacterFragment.mRealVM.getRoleList()
            }
        }.show(childFragmentManager)
    }
}