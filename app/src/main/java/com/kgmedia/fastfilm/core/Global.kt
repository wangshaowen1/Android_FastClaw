package com.kgmedia.fastfilm.core

import com.kgmedia.fastfilm.common.ext.ioScope
import com.kgmedia.fastfilm.core.network.apis.ApiFactory
import com.kgmedia.fastfilm.core.network.apis.request.UserTokenBody
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