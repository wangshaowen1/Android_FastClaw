package com.kgmedia.fastfilm.core

import com.kgmedia.fastfilm.common.ext.ioScope
import com.kgmedia.fastfilm.core.network.apis.ApiFactory
import com.kgmedia.fastfilm.core.network.apis.request.UserTokenBody
import com.kgmedia.fastfilm.core.network.apis.response.SimpleActivityInfo
import com.nhsd.basic.ext.launchNoCrash

/**
 * 全部变量
 */
object Global {
    /**
     * 首次注册用户，需要弹出完善资料
     */
    var needCompleteProfile = false

    /**
     * 续写的内容，生命周期内保持，退出登录，退出app或者使用后清除
     */
    val continueWritingContent: MutableMap<String?, String?> by lazy { mutableMapOf() }

    /**
     * 灵感生成的内容，生命周期内保持，退出登录，退出app或者使用后清除
     */
    var inspirationContent: String? = null

    /**
     * 自己写故事内容，生命周期内保持，退出登录，退出app或者使用后清除
     */
    var customizedContent: String? = null

    var guideActivityInfoList: List<SimpleActivityInfo>? = null

    fun refreshUserInfo() {
        ioScope.launchNoCrash {
            response(ApiFactory.apiService.getUserHomeInfo(UserTokenBody())) {
                User.get().updateFrom(data)
            }
        }
    }

    fun refreshBalance() {
        ioScope.launchNoCrash {
            val resp = ApiFactory.apiService.queryWalletInfo(UserTokenBody())
            if (resp.isSuccess) {
                resp.data?.pointNum?.let { User.get().storeComputingPower(it) }
            }
        }
    }

    fun getGuideLinkCustomerInfo(callback: (List<SimpleActivityInfo>?) -> Unit) {
        // 如果有缓存，直接回调并 return
        guideActivityInfoList?.let {
            callback(it)
            return
        }
        ioScope.launchNoCrash {
            val resp = ApiFactory.apiService.guideLinkCustomerPopupInfo()
            if (resp.isSuccess) {
                guideActivityInfoList = resp.data
                callback(guideActivityInfoList) // 移除多余的 ?.invoke
            } else {
                // 建议：如果请求失败，回调 null，防止调用方如果在做等待动画时永远挂起
                callback(null)
            }
        }
    }

    fun clearContinueInputContent(dramasSeriesId: String?) {
        continueWritingContent.remove(dramasSeriesId)
    }

    fun clearInputContent() {
        inspirationContent = null
        customizedContent = null
    }
}