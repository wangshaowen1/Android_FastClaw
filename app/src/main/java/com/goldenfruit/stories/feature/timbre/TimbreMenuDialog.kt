package com.kgmedia.fastfilm.feature.timbre

import android.os.Bundle
import androidx.fragment.app.FragmentManager
import com.kgmedia.fastfilm.R
import com.kgmedia.fastfilm.common.Constants
import com.kgmedia.fastfilm.common.adapter.QuickAdapter
import com.kgmedia.fastfilm.common.ext.showConfirmPrompt
import com.kgmedia.fastfilm.core.network.apis.entity.TimbreInfo
import com.kgmedia.fastfilm.databinding.DialogTimbreMenuBinding
import com.nhsd.basic.BaseBottomPushVMDialogFragment
import com.nhsd.basic.ext.getResString
import com.nhsd.basic.ext.showToast

class TimbreMenuDialog : BaseBottomPushVMDialogFragment<DialogTimbreMenuBinding, TimbreMenuViewModel>() {
    companion object {
        fun show(
            manager: FragmentManager,
            timbre: TimbreInfo?,
            callback: Callback? = null
        ): TimbreMenuDialog {
            val dialog = TimbreMenuDialog()
            dialog.arguments = Bundle().apply {
                putParcelable(Constants.Extra.TIMBRE, timbre)
            }
            dialog.callback = callback
            dialog.show(manager)
            return dialog
        }
    }

    private lateinit var songMenu: ArrayList<SongMenuData>
    private val menuAdapter by lazy { QuickAdapter(R.layout.item_song_menu, arrayListOf()) }

    //点击菜单回调
    var onClick: ((SongMenuEnum) -> Unit)? = null

    interface Callback {
        fun onNameChange(info: TimbreInfo?, name: String?)
        fun onDelete(info: TimbreInfo?)
    }

    var callback: Callback? = null
    override fun onPrepareView() {
        super.onPrepareView()

        songMenu = arrayListOf(
            SongMenuData(
                "修改名称",
                R.drawable.ic_modify_the_name,
                SongMenuEnum.MODIFY_NAME
            ),
            SongMenuData(
                "删除音色",
                R.drawable.ic_delete_the_song,
                SongMenuEnum.DELETE
            )
        )

        binding.rvMenu.adapter = menuAdapter
        val vm = arrayListOf<ItemSongMenuVm>()
        songMenu.forEach { song ->
            vm.add(ItemSongMenuVm(song).apply {
                onItemClick.value = onItemClick@{
                    onClickType(song.type)
                }
            })
        }
        menuAdapter.submitList(vm)
    }


    private fun onClickType(songMenuEnum: SongMenuEnum) {
        when (songMenuEnum) {
            SongMenuEnum.SHARE -> {
                dismissAllowingStateLoss()
            }


            SongMenuEnum.MODIFY_NAME -> {
                CommonEditorDialogFragment.show(
                    childFragmentManager,
                    "修改名称",
                    "请输入自定义名称",
                    8,
                    id = mRealVM.timbreInfo?.timbreId,
                    type = CommonEditorDialogFragment.TYPE_TIMBRE_NAME
                ).apply {
                    onModifySuccess = {
                        callback?.onNameChange(this@TimbreMenuDialog.mRealVM.timbreInfo, it.second)
                        this@TimbreMenuDialog.dismissAllowingStateLoss()
                    }
                }
            }


            SongMenuEnum.DELETE -> {
                //删除
                activity?.showConfirmPrompt(
                    title = "删除音色",
                    tip = "删除音色后，不可恢复，请谨慎操作",
                    confirmText = "删除",
                    cancelText = getString(R.string.cancel),
                ) {
                    mRealVM.deleteTimbre(userTimbreId = mRealVM.timbreInfo?.timbreId) {
                        R.string.delete_success.getResString().showToast()
                        callback?.onDelete(mRealVM.timbreInfo)
                        dismissAllowingStateLoss()
                    }
                }
            }
        }
    }
}