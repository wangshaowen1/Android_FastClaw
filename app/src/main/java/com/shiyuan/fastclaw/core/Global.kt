package com.shiyuan.fastclaw.core

import com.shiyuan.fastclaw.common.ext.ioScope
import com.shiyuan.fastclaw.core.network.apis.ApiFactory
import com.shiyuan.fastclaw.core.network.apis.request.UserTokenBody
import com.nhsd.basic.ext.launchNoCrash

/**
 * 全部变量
 */
object Global {
    /**
     * 首次注册用户，需要弹出完善资料
     */
    var needCompleteProfile = false

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
}