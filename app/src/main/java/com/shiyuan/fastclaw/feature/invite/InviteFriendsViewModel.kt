package com.shiyuan.fastclaw.feature.invite

import android.graphics.Color
import com.nhsd.basic.ext.getDrawable
import com.nhsd.basic.ext.getResString
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.base.item.AppItemViewModel
import com.shiyuan.fastclaw.base.viewmodel.AppRepositoryViewModel
import com.shiyuan.fastclaw.base.viewmodel.ToolbarViewModel
import com.shiyuan.fastclaw.core.AppRepository

/**
 * @Introduce: 邀请好友
 */
class InviteFriendsViewModel : AppRepositoryViewModel<AppRepository>() {
    val mToolbarVM = ToolbarViewModel(
        navigationDrawable = R.drawable.ic_invite_toolbar_back.getDrawable(),
        navigationAction = { finish() },
        title = R.string.invite_friends_title.getResString(),
        titleColor = Color.WHITE,
        menuIconRes = R.drawable.ic_invite_toolbar_action,
        background = Color.TRANSPARENT
    )

    val inviteRecordItems by lazy {
        listOf(
            InviteRecordItem(
                name = R.string.invite_record_name_one.getResString(),
                date = R.string.invite_record_date_one.getResString(),
                reward = "100"
            ),
            InviteRecordItem(
                name = R.string.invite_record_name_two.getResString(),
                date = R.string.invite_record_date_two.getResString(),
                reward = R.string.invite_record_reward_register.getResString()
            ),
            InviteRecordItem(
                name = R.string.invite_record_name_three.getResString(),
                date = R.string.invite_record_date_three.getResString(),
                reward = R.string.invite_record_reward_experience.getResString()
            )
        ).map { item -> ItemInviteRecordVM(item) }
    }
}

data class InviteRecordItem(
    val name: String,
    val date: String,
    val reward: String
)

class ItemInviteRecordVM(data: InviteRecordItem?) :
    AppItemViewModel<InviteRecordItem>(data)
