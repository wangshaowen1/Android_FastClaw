package com.kgmedia.fastfilm.feature.creator.generate.edit

import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.lifecycle.MutableLiveData
import com.kgmedia.fastfilm.App
import com.kgmedia.fastfilm.base.viewmodel.AppRepositoryViewModel
import com.kgmedia.fastfilm.common.Constants
import com.kgmedia.fastfilm.core.Config
import com.kgmedia.fastfilm.core.ext.launch
import com.kgmedia.fastfilm.core.ext.launchWithLoading
import com.kgmedia.fastfilm.core.ext.parseMessage
import com.kgmedia.fastfilm.core.network.apis.entity.CharacterInfo
import com.kgmedia.fastfilm.core.network.apis.entity.RoleTagInfo
import com.kgmedia.fastfilm.core.network.apis.request.ImageUrlBody
import com.kgmedia.fastfilm.core.network.apis.request.OptimizeRoleImageDescBody
import com.kgmedia.fastfilm.core.network.apis.request.RoleImageRegenerateBody
import com.kgmedia.fastfilm.core.response
import com.kgmedia.fastfilm.feature.creator.data.CreationRepository
import com.kuaige.oss.OnUploadListener
import com.kuaige.oss.OssClient
import com.nhsd.basic.ext.getParcelableValue
import com.nhsd.basic.ext.showToast

class CharacterRegenerateViewModel : AppRepositoryViewModel<CreationRepository>() {

    val maxInputLength = Config.get().appConfig.shortDramas?.shortDramasGenerateRoleImageDescriptionLimitNum ?: 500
    val isRoleAddModel by lazy { mBundle.getBoolean(Constants.Extra.MODE) }
    val role by lazy { mBundle.getParcelableValue(Constants.Extra.DATA, CharacterInfo::class.java) }
    val dramasId by lazy { mBundle.getString(Constants.Extra.DRAMAS_ID) }
    val roleName = MutableLiveData("")
    val input = MutableLiveData<String?>()
    var originalTags: List<RoleTagInfo?>? = null
    val tags = MutableLiveData<List<RoleTagInfo>>()

    var mAgeTagInfo: RoleTagInfo? = null
    var mGenderTagInfo: RoleTagInfo? = null
    var mTypeTagInfo: RoleTagInfo? = null

    var isAnalysisLoading = MutableLiveData(false)
    val promptGenerating = MutableLiveData<Pair<Boolean, String>>()

    override fun onCreate() {
        super.onCreate()
        input.value = role?.roleImageDesc
        originalTags = role?.roleTagList
        getTagList()
    }

    fun getTagList(callback: (() -> Unit)? = null) {
        launchWithLoading {
            response(mRepo.roleTagList()) {
                tags.value = data?.items
                callback?.invoke()
            }
        }
    }

    fun optimizeRoleImageDesc(callback: ((String?) -> Unit)? = null) {
        promptGenerating.value = true to "正在优化，请稍候..."
        launch({
            val promptResponse =
                mRepo.optimizeRoleImageDesc(
                    OptimizeRoleImageDescBody(dramasId, input.value, mAgeTagInfo?.roleTagName, mGenderTagInfo?.roleTagName, mTypeTagInfo?.roleTagName)
                )
            promptGenerating.value = false to ""
            if (promptResponse.isSuccess) {
                input.value = promptResponse.data?.content
                callback?.invoke(promptResponse.data?.content)
            } else {
                promptResponse.msg?.showToast()
            }
        }, onError = {
            promptGenerating.value = false to ""
            parseMessage(it)?.showToast()
        })
    }

    var avatarPath = ObservableField<String>()
    val isImageValid = ObservableBoolean(false)

    /**
     * 上传到oss
     * @param pathImg 本地路径
     */
    fun uploadAvatar(pathImg: String?) {
        setLoading(true)
        val ossClient = OssClient.newInstance(App.instance)
        ossClient.uploadFile(pathImg, OssClient.TYPE_ASSET, object : OnUploadListener() {
            override fun onUploadSuccess(uploadSuccess: List<String?>) {
                if (uploadSuccess.isEmpty()) {
                    onUploadFail(null)
                } else {
                    setLoading(false)
                    avatarPath.set(uploadSuccess.first().toString())
                    imageCheck()
                }
            }

            override fun onUploadFail(uploadFail: MutableList<String>?) {
                super.onUploadFail(uploadFail)
                setLoading(false)
                "上传失败".showToast()
            }
        })
    }

    fun imageCheck() {
        isAnalysisLoading.value = true
        isImageValid.set(false)
        launch({
            val response = mRepo.api.imageCheck(ImageUrlBody(avatarPath.get()))
            isAnalysisLoading.postValue(false)
            if (response.isSuccess) {
                isImageValid.set(true)
            } else {
                response.msg?.showToast()
                avatarPath.set(null)
            }
        }, {
            isAnalysisLoading.postValue(false)
            avatarPath.set(null)
            parseMessage(it)?.showToast()
        })
    }

    fun roleImageRegenerate(callback: ((String?) -> Unit)? = null) {
        launchWithLoading {
            response(
                mRepo.roleImageRegenerate(
                    RoleImageRegenerateBody(
                        dramasId,
                        role?.roleId,
                        input.value,
                        listOf(mAgeTagInfo?.roleTagId, mGenderTagInfo?.roleTagId, mTypeTagInfo?.roleTagId),
                        avatarPath.get()
                    )
                )
            ) {
                callback?.invoke(role?.roleId)
            }
        }
    }

    fun roleAdd(callback: (() -> Unit)? = null) {
        launchWithLoading {
            response(
                mRepo.roleAdd(
                    RoleImageRegenerateBody(
                        dramasId,
                        role?.roleId,
                        input.value,
                        listOf(mAgeTagInfo?.roleTagId, mGenderTagInfo?.roleTagId, mTypeTagInfo?.roleTagId),
                        avatarPath.get(),
                        roleName.value
                    )
                )
            ) {
                callback?.invoke()
            }
        }
    }
}