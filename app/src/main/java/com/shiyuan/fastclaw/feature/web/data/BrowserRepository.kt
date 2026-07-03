package com.shiyuan.fastclaw.feature.web.data


import com.shiyuan.fastclaw.core.AppRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BrowserRepository : AppRepository() {

    suspend fun h5PayAlipay(info: Any?) = withContext(Dispatchers.IO) {
        api.h5PayAlipay(info)
    }

    suspend fun h5PayWx(info: Any?) = withContext(Dispatchers.IO) {
        api.h5PayWx(info)
    }

}