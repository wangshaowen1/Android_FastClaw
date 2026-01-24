package com.kgmedia.fastfilm.feature.invitation

import android.graphics.Color
import androidx.databinding.ObservableField
import com.kgmedia.fastfilm.base.viewmodel.AppRepositoryViewModel
import com.kgmedia.fastfilm.base.viewmodel.ToolbarViewModel
import com.kgmedia.fastfilm.core.User
import com.kgmedia.fastfilm.core.data.CommonRepository

/**
 * *****************************************
 * 项目名称: KidsTales
 * @Author: SZYMN
 * @Introduce:邀请明细
 * @Data: 2025/10/13/周一  10:15
 * @Version: 1.0
 * *****************************************
 */
class InvitationDetailsViewModel : AppRepositoryViewModel<CommonRepository>() {
    var mToolbarVM = ToolbarViewModel(
        navigationAction = { back() },
        title = "邀请明细",
        background = Color.TRANSPARENT,
    )


    //用户头像
    var mUserAvatar = ObservableField(User.get().avatar)
    //用户昵称
    var mUserNickname = ObservableField(User.get().nickname)


    //总收益
    var mTotalIncome = ObservableField("0.00")

}