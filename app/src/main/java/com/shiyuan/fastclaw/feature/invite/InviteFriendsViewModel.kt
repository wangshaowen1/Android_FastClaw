package com.shiyuan.fastclaw.feature.invite

import android.graphics.Color
import androidx.lifecycle.MutableLiveData
import com.nhsd.basic.ext.getDrawable
import com.nhsd.basic.ext.getResString
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.base.item.AppItemViewModel
import com.shiyuan.fastclaw.base.viewmodel.AppRepositoryViewModel
import com.shiyuan.fastclaw.base.viewmodel.ToolbarViewModel
import com.shiyuan.fastclaw.core.User
import com.shiyuan.fastclaw.core.AppRepository
import com.shiyuan.fastclaw.core.ext.request
import com.shiyuan.fastclaw.core.ext.requestWithLoading
import com.shiyuan.fastclaw.core.network.apis.request.InviteShareInfoBody
import com.shiyuan.fastclaw.core.network.apis.request.PagedBody
import com.shiyuan.fastclaw.core.network.apis.response.InviteRecordInfo

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

    val inviteRecordItems = MutableLiveData<List<ItemInviteRecordVM>>(emptyList())

    fun loadInviteRecords() {
        request(
            apiBlock = { mRepo.api.queryInviteRecordPage(PagedBody(INVITE_RECORD_FIRST_PAGE, INVITE_RECORD_PAGE_SIZE)) },
            onSuccess = {
                val records = data?.items.orEmpty()
                inviteRecordItems.value = records.map { item -> ItemInviteRecordVM(item.toInviteRecordItem()) }
            }
        )
    }

    fun queryInviteShareInfo(onSuccess: (String) -> Unit) {
        requestWithLoading(
            apiBlock = { mRepo.api.queryInviteShareInfo(InviteShareInfoBody()) },
            onSuccess = {
                data?.inviteCode?.let { inviteCode ->
                    User.get().storeInviteCode(inviteCode)
                    onSuccess(inviteCode)
                }
            }
        )
    }

    private fun InviteRecordInfo.toInviteRecordItem(): InviteRecordItem {
        val experienced = experienceStatus == EXPERIENCE_STATUS_DONE
        return InviteRecordItem(
            name = inviteeNickName.orEmpty(),
            date = inviteDate.toInviteRecordDate(),
            reward = if (experienced) INVITE_REWARD_FULL else INVITE_REWARD_REGISTER,
            registerStatusText = registerStatusText.orEmpty(),
            experienceStatusText = experienceStatusText.orEmpty(),
            experienceStatusIconRes = if (experienced) {
                R.drawable.ic_invite_status_experienced
            } else {
                R.drawable.ic_invite_status_unexperienced
            }
        )
    }

    private fun String?.toInviteRecordDate(): String {
        val value = this.orEmpty()
        return if (value.length >= 10) value.substring(5, 10) else value
    }

    companion object {
        private const val INVITE_RECORD_FIRST_PAGE = 1
        private const val INVITE_RECORD_PAGE_SIZE = 20
        private const val EXPERIENCE_STATUS_DONE = 1
        private const val INVITE_REWARD_REGISTER = "20"
        private const val INVITE_REWARD_FULL = "100"
    }
}

data class InviteRecordItem(
    val name: String,
    val date: String,
    val reward: String,
    val registerStatusText: String,
    val experienceStatusText: String,
    val experienceStatusIconRes: Int
)

class ItemInviteRecordVM(data: InviteRecordItem?) :
    AppItemViewModel<InviteRecordItem>(data)
