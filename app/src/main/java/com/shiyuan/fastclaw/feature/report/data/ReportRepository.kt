package com.shiyuan.fastclaw.feature.report.data

import com.shiyuan.fastclaw.core.AppRepository
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
