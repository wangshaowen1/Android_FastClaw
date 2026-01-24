package com.kgmedia.fastfilm.feature.creator.character

import android.graphics.Color
import androidx.databinding.ObservableField
import com.kgmedia.fastfilm.R
import com.kgmedia.fastfilm.base.RecyclerViewVM
import com.kgmedia.fastfilm.base.viewmodel.AppRepositoryViewModel
import com.kgmedia.fastfilm.base.viewmodel.ToolbarViewModel
import com.kgmedia.fastfilm.common.Constants
import com.kgmedia.fastfilm.common.ext.formatNumber
import com.kgmedia.fastfilm.core.ext.requestWithLoading
import com.kgmedia.fastfilm.core.network.apis.entity.CharacterInfo
import com.kgmedia.fastfilm.core.network.apis.request.RoleIdBody
import com.kgmedia.fastfilm.core.network.apis.request.RoleSwitchBody
import com.kgmedia.fastfilm.feature.creator.data.CreationRepository
import com.kgmedia.fastfilm.widget.decoration.LinearLayoutManagerSpacingDecoration
import com.nhsd.basic.ext.getDrawable
import com.nhsd.basic.ext.getParcelableValue
import com.nhsd.basic.ext.getResColor
import com.nhsd.basic.item.BaseItemEventsViewModel

class CharacterHistoryViewModel : AppRepositoryViewModel<CreationRepository>() {

    val mToolbarVM = ToolbarViewModel(
        navigationAction = { back() },
        navigationDrawable = R.drawable.ic_nav_back.getDrawable(),
        title = "",
        titleColor = R.color.white.getResColor(),
        background = Color.TRANSPARENT,
        menuButtonText = "确认使用",
        menuAction = {
            confirmCharacter(currentRoleInfo.get())
        }
    )

    val dramaId by lazy { mBundle.getString(Constants.Extra.ID) }
    val currentRoleInfo by lazy { ObservableField(mBundle.getParcelableValue(Constants.Extra.DATA, CharacterInfo::class.java)) }
    var originalRoleId: String? = null

    val characterVms = arrayListOf<ItemCharacterHistoryVm>()
    val mAdapterVm =
        RecyclerViewVM().Builder().setQuickAdapter(R.layout.item_character_history, characterVms).setItemDecoration(
            LinearLayoutManagerSpacingDecoration(16, 10, LinearLayoutManagerSpacingDecoration.Orientation.Horizontal)
        ).build()

    val eventVm by lazy {
        BaseItemEventsViewModel<CharacterEvent, String>()
    }
    var switchRoleEvent: (() -> Unit)? = null

    override fun onCreate() {
        super.onCreate()
        originalRoleId = currentRoleInfo.get()?.roleId
        mToolbarVM.mTitle.set(currentRoleInfo.get()?.roleName)
        if (currentRoleInfo.get()?.isNewRole() == true) {
            getCharacterList()
        }
    }

    var lastRoleInfoVM: ItemCharacterHistoryVm? = null

    fun getCharacterList() {
        requestWithLoading({ mRepo.getHistoryRoles(RoleIdBody(currentRoleInfo.get()?.roleId)) }) {
            data?.items?.forEachIndexed { i, info ->
                characterVms.add(ItemCharacterHistoryVm(info).apply {
                    index.set((i + 1).formatNumber())
                    if (currentRoleInfo.get()?.roleId == info.roleId) {
                        sel.set(true)
                        lastRoleInfoVM = this
                    }
                    onItemClick.value = {
                        if (lastRoleInfoVM != this) {
                            lastRoleInfoVM?.sel?.set(false)
                            currentRoleInfo.set(info)
                            lastRoleInfoVM = this
                            lastRoleInfoVM?.sel?.set(true)
                        }
                    }
                })
            }
            mAdapterVm.notifyDataSetChanged()
        }
    }

    fun confirmCharacter(character: CharacterInfo?) {
        requestWithLoading({ mRepo.historyRoleSwitch(RoleSwitchBody(character?.roleId, originalRoleId)) }) {
            switchRoleEvent?.invoke()
        }
    }

}