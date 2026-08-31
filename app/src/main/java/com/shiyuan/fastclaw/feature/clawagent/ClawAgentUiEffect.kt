package com.shiyuan.fastclaw.feature.clawagent

import com.shiyuan.fastclaw.core.network.apis.response.CompanyBackgroundPreCheckResultData
import com.shiyuan.fastclaw.core.network.apis.response.InterviewPassPackageResumeStatusData
import com.shiyuan.fastclaw.core.network.apis.response.MockInterviewStartData
import com.shiyuan.fastclaw.core.network.apis.response.MockInterviewUnfinishedData
import com.shiyuan.fastclaw.feature.clawagent.bean.ClawAgentMessageInfo

/** 仅消费一次的页面效果，不承载可恢复页面状态。 */
sealed class ClawAgentUiEffect {

    data class ShowStreamError(val error: Throwable) : ClawAgentUiEffect()

    data class ShowMemoryConfirm(val message: ClawAgentMessageInfo) : ClawAgentUiEffect()

    object ShowNotificationSwitch : ClawAgentUiEffect()

    object ShowHelpYouFindJob : ClawAgentUiEffect()

    data class ShowInterviewRecovery(
        val data: MockInterviewUnfinishedData,
    ) : ClawAgentUiEffect()

    data class HandleInterviewStart(
        val data: MockInterviewStartData?,
    ) : ClawAgentUiEffect()

    data class HandleCompanyBackgroundPreCheck(
        val data: CompanyBackgroundPreCheckResultData?,
    ) : ClawAgentUiEffect()

    data class OpenInterviewPassPackage(
        val data: InterviewPassPackageResumeStatusData?,
    ) : ClawAgentUiEffect()
}
