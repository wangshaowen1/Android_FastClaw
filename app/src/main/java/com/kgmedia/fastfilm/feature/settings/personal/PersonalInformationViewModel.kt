package com.kgmedia.fastfilm.feature.settings.personal

import android.graphics.Color
import android.view.View
import androidx.databinding.ObservableField
import androidx.databinding.ObservableInt
import androidx.lifecycle.MutableLiveData
import com.kgmedia.fastfilm.App
import com.kgmedia.fastfilm.R
import com.kgmedia.fastfilm.base.viewmodel.AppRepositoryViewModel
import com.kgmedia.fastfilm.base.viewmodel.ToolbarViewModel
import com.kgmedia.fastfilm.core.AppRepository
import com.kgmedia.fastfilm.core.User
import com.kgmedia.fastfilm.core.ext.launchWithLoading
import com.kgmedia.fastfilm.core.network.apis.ApiException
import com.kgmedia.fastfilm.core.network.apis.request.CompleteUserDataInfoBody
import com.kgmedia.fastfilm.core.response
import com.kuaige.oss.OnUploadListener
import com.kuaige.oss.OssClient
import com.nhsd.basic.ext.getResString
import com.nhsd.basic.ext.showToast

/**
 * *****************************************
 * 项目名称: KidsTales
 * @Author: SZYMN
 * @Introduce:个人资料
 * @Data: 2025/4/21  16:45
 * @Version: 1.0
 * *****************************************
 */
class PersonalInformationViewModel : AppRepositoryViewModel<AppRepository>() {

    var mToolbarVM = ToolbarViewModel(
        navigationAction = { finish() },
        title = R.string.app_personal_information.getResString(),
        background = Color.TRANSPARENT,
    )

    private val user
        get() = User.get()

    /**
     * 头像
     */
    val avatar by lazy { ObservableField(user.avatarUrl) }

    //uniqueId
    val uniqueId by lazy { user.userName }

    /**
     * 昵称
     */
    //昵称
    val nickname by lazy { ObservableField(user.nickName) }
    val editNickname by lazy { MutableLiveData(user.nickName) }

    /**
     * 签名
     */
    val signature by lazy { ObservableField("") }
    val editSignature by lazy { MutableLiveData("") }

    /**
     * 性别
     */
    val gender by lazy { ObservableInt(user.gender?:0) }

    val editGender by lazy { ObservableInt() }

    /**
     * id
     */
    val id by lazy { user.userId }



    fun clearNickname(view: View) {
        editNickname.value = ""
    }

    /**
     * 修改头像
     * @param path 本地路径
     */
    fun changeAvatar(path: String) {
        avatar.set(path)
        setLoading(true)
        val ossClient = OssClient.newInstance(App.instance)
        ossClient.uploadFile(path, OssClient.TYPE_USER_AVATAR_IMG, object : OnUploadListener() {
            override fun onUploadSuccess(uploadSuccess: List<String?>) {
                val url = uploadSuccess.firstOrNull()
                if (url.isNullOrEmpty()) {
                    onUploadFail(null)
                } else {
                    commit(avatar = url)
                }
            }

            override fun onUploadFail(uploadFail: MutableList<String>?) {
                super.onUploadFail(uploadFail)
                setLoading(false)
                showToast(R.string.upload_failed)
            }
        })
    }


    /**
     * 提交
     * @param avatar 头像url
     */
    fun commit(
        avatar: String? = null,
        nickname: String? = null,
        gender: Int? = null,
        birthday: Long? = null,
        prov: String? = null,
        city: String? = null,
        signature: String? = null,
        onSuccess: (() -> Unit)? = null
    ) {
        launchWithLoading(uiState.value.isLoading) {
            val body = CompleteUserDataInfoBody(
                nickname,
                avatar,
                gender,
                birthday,
                prov,
                city,
                signature
            )
            response(mRepo.api.modifyUserInfo(body), {
                if (uiState.value.isLoading) {
                    "保存成功".showToast()
                }
                User.get().updateFromCompleteUserBody(body)
                nickname?.let { this@PersonalInformationViewModel.nickname.set(it) }
                signature?.let { this@PersonalInformationViewModel.signature.set(it) }
                gender?.let { this@PersonalInformationViewModel.gender.set(it) }

                onSuccess?.invoke()
            }, {
                this?.msg?.showToast()
                when (this?.code) {
                    ApiException.IMAGE_ILLEGAL -> {
                        this@PersonalInformationViewModel.avatar.set(user.avatarUrl)
                    }

                    ApiException.TEXT_ILLEGAL -> {
                        this@PersonalInformationViewModel.editNickname.value = user.nickName
                    }
                }
            })
        }
    }
}