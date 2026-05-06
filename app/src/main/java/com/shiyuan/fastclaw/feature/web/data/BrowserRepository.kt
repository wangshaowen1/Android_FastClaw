package com.shiyuan.fastclaw.feature.web.data


import com.shiyuan.fastclaw.core.AppRepository
import com.shiyuan.fastclaw.core.model.H5PayInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BrowserRepository : AppRepository() {

    suspend fun h5PayAlipay(info: H5PayInfo) = withContext(Dispatchers.IO) {
        api.h5PayAlipay(info)
    }

    suspend fun h5PayWx(info: H5PayInfo) = withContext(Dispatchers.IO) {
        api.h5PayWx(info)
    }

}