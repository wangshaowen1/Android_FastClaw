package com.kgmedia.fastfilm.feature.creator.generate.edit

import com.kgmedia.fastfilm.R
import com.kgmedia.fastfilm.base.RecyclerViewVM
import com.kgmedia.fastfilm.base.viewmodel.AppRepositoryViewModel
import com.kgmedia.fastfilm.common.Constants
import com.kgmedia.fastfilm.core.data.CommonRepository
import com.kgmedia.fastfilm.core.network.apis.response.Role
import com.nhsd.basic.ext.getParcelableArrayListValue

class CharacterChooseViewModel : AppRepositoryViewModel<CommonRepository>() {

    val roles by lazy { mBundle.getParcelableArrayListValue(Constants.Extra.CONTENT, Role::class.java) }

    val characterVms = arrayListOf<ItemCharacterChooseVm>()
    val mAdapterVm =
        RecyclerViewVM().Builder().setQuickAdapter(R.layout.item_character_choose, characterVms).build()

    var onRoleChoose: ((Role) -> Unit)? = null

    override fun onCreate() {
        super.onCreate()
        roles?.forEach { role ->
            characterVms.add(ItemCharacterChooseVm(role).apply {
                onItemClick.value = {
                    onRoleChoose?.invoke(role)
                }
            })
        }
        mAdapterVm.notifyDataSetChanged()
    }
}