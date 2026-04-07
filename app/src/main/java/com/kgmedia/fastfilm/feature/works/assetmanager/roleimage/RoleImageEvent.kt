package com.kgmedia.fastfilm.feature.works.assetmanager.roleimage

import com.kgmedia.fastfilm.feature.works.assetmanager.child.ItemAssetsChildVm


sealed class RoleImageEvent {
    class Click(val item: ItemRoleImageVm) : RoleImageEvent()

    class Select(val item: ItemRoleImageVm?) : RoleImageEvent()
}