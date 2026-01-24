package com.kgmedia.fastfilm.feature.invitation.person

import com.kgmedia.fastfilm.base.item.AppItemViewModel
import com.kgmedia.fastfilm.common.utils.DateUtil
import com.kgmedia.fastfilm.core.network.apis.response.InviteUserPageInfo

/**
 * *****************************************
 * 项目名称: KidsTales
 * @Author: SZYMN
 * @Introduce:邀请人列表
 * @Data: 2025/10/13/周一  14:54
 * @Version: 1.0
 * *****************************************
 */
class ItemInviteeListVm(data: InviteUserPageInfo?) : AppItemViewModel<InviteUserPageInfo>(data) {
    fun remarks(): String {
        return DateUtil.format(data.get()?.registerTime ?: 0L, DateUtil.FORMAT_DATETIME_BIAS) + " 注册成功"
    }
}