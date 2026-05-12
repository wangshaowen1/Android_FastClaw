package com.shiyuan.fastclaw.feature.report.data

import com.fm.openinstall.OpenInstall
import com.shiyuan.fastclaw.core.AppRepository
import com.shiyuan.fastclaw.core.Config
import com.shiyuan.fastclaw.core.network.apis.request.BindPhoneBody
import com.shiyuan.fastclaw.core.network.apis.request.LoginByThirdBody
import com.shiyuan.fastclaw.core.network.apis.request.SendSmsValidateCodeBody
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 绑定手机号
 */
class ReportRepository : AppRepository() {

    suspend fun queryLatestResume() = withContext(Dispatchers.IO) {
        api.queryLatestResume().apply {

        }
    }

}