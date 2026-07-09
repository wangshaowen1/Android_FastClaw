package com.shiyuan.fastclaw.feature.information.viewModel

import android.graphics.Color
import androidx.lifecycle.MutableLiveData
import com.nhsd.basic.ext.showToast
import com.shiyuan.fastclaw.base.viewmodel.ToolbarViewModel
import com.shiyuan.fastclaw.core.AppRepository
import com.shiyuan.fastclaw.core.CommonRepositoryViewModel
import com.shiyuan.fastclaw.core.User
import com.shiyuan.fastclaw.core.ext.launch
import com.shiyuan.fastclaw.core.ext.launchWithLoading
import com.shiyuan.fastclaw.core.ext.parseMessage
import com.shiyuan.fastclaw.core.ext.request
import com.shiyuan.fastclaw.core.ext.requestWithLoading
import com.shiyuan.fastclaw.core.network.apis.ApiFactory
import com.shiyuan.fastclaw.core.network.apis.request.AttributeArrayQueryBody
import com.shiyuan.fastclaw.core.network.apis.request.DictIndustryQueryBody
import com.shiyuan.fastclaw.core.network.apis.request.DictPositionQueryBody
import com.shiyuan.fastclaw.core.network.apis.request.OnlineResumeAttributeUpdateBody
import com.shiyuan.fastclaw.core.network.apis.request.ResumeUploadSubmitMyBody
import com.shiyuan.fastclaw.core.network.apis.request.UpdateAvatarBody
import com.shiyuan.fastclaw.core.network.apis.response.AttributeArrayQueryData
import com.shiyuan.fastclaw.core.network.apis.response.AttributeFieldItem
import com.shiyuan.fastclaw.core.network.apis.response.AttributeOption
import com.shiyuan.fastclaw.core.network.apis.response.AttributeSave
import com.shiyuan.fastclaw.core.network.apis.response.CommonEnumItem
import com.shiyuan.fastclaw.core.network.apis.response.OnlineResumeInfoSection
import com.shiyuan.fastclaw.core.network.apis.response.UnderstandingScore
import com.shiyuan.fastclaw.core.response
import java.util.UUID
import kotlin.collections.forEach

/**
 * *****************************************
 * 项目名称: KidsTales
 * @Author: SZYMN
 * @Introduce:我的
 * @Data: 2025/4/18  14:10
 * @Version: 1.0
 * *****************************************
 */
/**
 * 求职信息填写相关页面共享的 ViewModel：
 * 行业 / 职位字典查询、字段保存等能力。
 */
class InformationViewModel : CommonRepositoryViewModel<AppRepository>() {

    // 顶部导航 ViewModel（占位，未实际绑定 Toolbar）
    val mToolbarVM = ToolbarViewModel(
        navigationAction = { finish() },
        title = "",
        background = Color.TRANSPARENT,
    )
    // 行业字典：完整一级 + 二级树
    var allData = MutableLiveData<List<CommonEnumItem>>()
    // 行业搜索结果
    var searchData = MutableLiveData<List<CommonEnumItem>>()
    // 多选模式下用户已选行业
    var selectData = MutableLiveData<List<CommonEnumItem>>()
    // 上次选中的一级行业索引（用于恢复选中态）
    var lastSelectedPosition = MutableLiveData<Int>(1)

    /**
     * 请求行业字典（无关键词，带 Loading）。
     */
    fun queryDictIndustryQuery(sessionId: String? = null, conversationId: String? = null) {
        launchWithLoading(true, block = {
            val requestBody =
                DictIndustryQueryBody(sessionId = sessionId, conversationId = conversationId)
            response(ApiFactory.apiService.queryDictIndustryQuery(requestBody), result = {
                allData.postValue(this.data?.items)
                // 首次进入时同步服务端反显的已选项
                if (selectData.value.isNullOrEmpty()) {
                    selectData.postValue(this.data?.selectedItems)
                }
            }, error = { this?.msg?.showToast() })
        }, onError = { it.message.toString().showToast() })

    }

    /**
     * 请求行业字典（带关键词，用于搜索）。
     */
    fun queryDictIndustryQuery(
        keyword: String? = null,
        sessionId: String? = null,
        conversationId: String? = null
    ) {
        launchWithLoading(false, block = {
            val requestBody = DictIndustryQueryBody(
                sessionId = sessionId,
                conversationId = conversationId,
                keyword = keyword
            )
            response(ApiFactory.apiService.queryDictIndustryQuery(requestBody), result = {
                // 搜索结果取所有一级节点下的二级节点并扁平化
                searchData.postValue(this.data?.items?.flatMap { it.children ?: emptyList() }
                    ?: emptyList())
            }, error = { this?.msg?.showToast() })
        }, onError = { it.message.toString().showToast() })

    }

    // 职位字典：完整一级分类
    var allPositioData = MutableLiveData<List<CommonEnumItem>>()
    // 职位搜索结果
    var searchPositionData = MutableLiveData<List<CommonEnumItem>>()
    // 多选模式下用户已选职位
    var selectPositionData = MutableLiveData<List<CommonEnumItem>>()
    // 上次选中的一级职位索引
    var lastSelectedPosition2 = MutableLiveData<Int>(1)

    /**
     * 请求职位字典（无关键词，带 Loading）。
     */
    fun queryDictPositionQuery(sessionId: String? = null, conversationId: String? = null) {
        launchWithLoading(true, block = {
            val requestBody =
                DictPositionQueryBody(sessionId = sessionId, conversationId = conversationId)
            response(ApiFactory.apiService.queryDictPositionQuery(requestBody), result = {
                allPositioData.postValue(this.data?.firstCategories)
                // 注意：此处用的是 selectData 而非 selectPositionData，沿用原实现
                if (selectData.value.isNullOrEmpty()) {
                    selectData.postValue(this.data?.selectedItems)
                }
            }, error = { this?.msg?.showToast() })
        }, onError = { it.message.toString().showToast() })

    }

    /**
     * 请求职位字典（带关键词，用于搜索）。
     */
    fun queryDictPositionQuery(
        keyword: String? = null,
        sessionId: String? = null,
        conversationId: String? = null
    ) {
        launchWithLoading(false, block = {
            val requestBody = DictPositionQueryBody(
                keyword = keyword,
                sessionId = sessionId,
                conversationId = conversationId
            )
            response(ApiFactory.apiService.queryDictPositionQuery(requestBody), result = {
                searchPositionData.postValue(this.data?.matchedPositions)
            }, error = { this?.msg?.showToast() })
        }, onError = { it.message.toString().showToast() })
    }

    // 字段保存接口的返回结果
    var attributeSave = MutableLiveData<AttributeSave>()

    /**
     * 调用字段保存接口。data 直接以 Any 透传给底层接口，便于支持多种数据类型。
     */
    fun attributeSave(data: Any) {
        launchWithLoading(true, block = {
            response(ApiFactory.apiService.attributeSave(data), result = {
                attributeSave.postValue(this.data)
            }, error = { this?.msg?.showToast() })
        }, onError = { it.message.toString().showToast() })
    }



    // 字段保存接口的返回结果
    var onResumeAttribute = MutableLiveData<Any>()
    /**
     * 在线简历保存
     */
    fun onResumeInfoAttributeSave(attributeData: AttributeArrayQueryData?, position: Int = 0, isEdit: Boolean = true) {
        launchWithLoading(true, block = {
            val sourceData = attributeData ?: run {
                "属性数据异常".showToast()
                return@launchWithLoading
            }
            val data = sourceData.fieldTree?.getOrNull(position) ?: run {
                "属性数据异常".showToast()
                return@launchWithLoading
            }
            data.attributeGroupCode = sourceData.attributeGroupCode
            data.operation = if (isEdit) "UPDATE" else "CREATE"
            data.instanceIndex =data.instanceIndex
            response(ApiFactory.apiService.onlineResumeAttributeSave(data), result = {
                onResumeAttribute.postValue(this.data)
            }, error = { this?.msg?.showToast() })
        }, onError = { it.message.toString().showToast() })
    }

    fun onlineResumeAttributeDelete(attributeData: AttributeArrayQueryData?, position: Int = 0) {
        val attributeGroupCode = attributeData?.attributeGroupCode
        if (attributeGroupCode.isNullOrBlank()) {
            "属性数据异常".showToast()
            return
        }
        val data = attributeData.fieldTree?.getOrNull(position) ?: run {
            "属性数据异常".showToast()
            return@onlineResumeAttributeDelete
        }
        launchWithLoading(true, block = {
            val body = mapOf(
                "attributeGroupCode" to attributeGroupCode,
                "instanceIndex" to data.instanceIndex
            )
            response(ApiFactory.apiService.onlineResumeAttributeDelete(body), result = {
                onResumeAttribute.postValue(this.data)
            }, error = { this?.msg?.showToast() })
        }, onError = { it.message.toString().showToast() })
    }



    fun updateAvatar(avatarUrl: String, onSuccess: (String) -> Unit) {
        requestWithLoading(
            apiBlock = { mRepo.api.updateUserAvatar(UpdateAvatarBody(avatarUrl)) },
            onSuccess = {
                val updatedAvatarUrl = data?.avatarUrl ?: avatarUrl
                User.get().updateFrom(data)
                if (data?.avatarUrl.isNullOrBlank()) {
                    User.get().storeAvatar(updatedAvatarUrl)
                    User.get().notifyChange(User.AVATAR_URL)
                }
                onSuccess(updatedAvatarUrl)
            }
        )
    }

    fun updateOnlineResumeAttribute(
        attributeGroupCode: String?,
        inputMode: String?,
        value: Any?,
        onSuccess: () -> Unit
    ) {
        requestWithLoading(
            apiBlock = {
                mRepo.api.updateOnlineResumeAttribute(
                    OnlineResumeAttributeUpdateBody(
                        attributeGroupCode = attributeGroupCode,
                        inputMode = inputMode,
                        value = value
                    )
                )
            },
            onSuccess = {
                onSuccess()
            }
        )
    }


    fun queryAttributeArray(
        questionQueueId: String?,
        attributeGroupCode: String?,
        sessionId: String?,
        conversationId: String?,
        clientRequestId: String?,
        onResult: (AttributeArrayQueryData?) -> Unit
    ) {
        request(
            apiBlock = {
                mRepo.api.queryAttributeArray(
                    AttributeArrayQueryBody(
                        sessionId = sessionId,
                        conversationId = conversationId,
                        questionQueueId = questionQueueId,
                        attributeGroupCode = attributeGroupCode,
                    )
                )
            }
        ) {
            this.data?.clientRequestId = clientRequestId
            onResult(this.data)
        }
    }

    var onlineResumeBaseInfo = MutableLiveData<List<AttributeArrayQueryData>>()
    var onlineResumeInfo = MutableLiveData<List<AttributeArrayQueryData>>()

    var underStandingScore = MutableLiveData<UnderstandingScore>()

    fun understandingScore() {
        launch(block = {
            response(
                ApiFactory.apiService.understandingScore(),
                result = {
                    underStandingScore.postValue(this.data)
                },
                error = {
                    this?.msg?.showToast()
                })
        }, onError = {
            parseMessage(it)?.showToast()
        }, onComplete = {
            setLoading(false)
        })
    }

    fun queryOnlineResumeInfo() {
        requestWithLoading(
            apiBlock = {
                mRepo.api.queryOnlineResumeInfo(mapOf("clientRequestId" to UUID.randomUUID().toString()))
            },
            showLoading = true,
            showToastError = true,
        ) {
            val sections = this.data?.sections.orEmpty()
            val baseInfo = mutableListOf<AttributeArrayQueryData>()
            val resumeInfo = mutableListOf<AttributeArrayQueryData>()
            sections.forEach { section ->
                if (section.sectionCode == SECTION_BASE_INFO) {
                    baseInfo.addAll(section.fields)
                } else {
                    resumeInfo.addAll(section.fields)
                }
            }
            onlineResumeBaseInfo.postValue(baseInfo)
            onlineResumeInfo.postValue(resumeInfo)
        }
    }

    var jobPreferencesInfo = MutableLiveData<List<OnlineResumeInfoSection>>()
    fun queryJobPreferencesInfo(onCallback: (() -> Unit)? = null) {
        requestWithLoading(
            apiBlock = {
                mRepo.api.queryJobPreferencesInfo(mapOf("clientRequestId" to UUID.randomUUID().toString()))
            },
            showLoading = true,
            showToastError = true,
        ) {
            val sections = this.data?.sections.orEmpty()
            jobPreferencesInfo.postValue(sections)
            onCallback?.invoke()
        }
    }




    /*
        fun queryOnlineResumeInfo() {
            postOnlineResumeSections(mockOnlineResumeInfoSections())
        }

        private fun postOnlineResumeSections(sections: List<OnlineResumeInfoSection>) {
            val baseInfo = mutableListOf<AttributeArrayQueryData>()
            val resumeInfo = mutableListOf<AttributeArrayQueryData>()
            sections.forEach { section ->
                if (section.sectionCode == SECTION_BASE_INFO) {
                    baseInfo.addAll(section.fields)
                } else {
                    resumeInfo.addAll(section.fields)
                }
            }
            onlineResumeBaseInfo.postValue(baseInfo)
            onlineResumeInfo.postValue(resumeInfo)
        }
    */




    val mToolbarVMOnlineResume = ToolbarViewModel(
        navigationAction = { finish() },
        title = "",
        background = Color.TRANSPARENT,
    )

    var tableOnlineResumeIndex = MutableLiveData(0)

    companion object {
        private const val SECTION_BASE_INFO = "baseInfo"
    }
}
