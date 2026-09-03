package com.shiyuan.fastclaw.feature.clawagent.message

import com.shiyuan.fastclaw.core.network.apis.response.CardAction
import com.shiyuan.fastclaw.core.network.apis.response.CardActionPayloadBean
import com.shiyuan.fastclaw.core.network.apis.response.CoreQuestionPayloadBean

/** 构造求职 Agent 消息动作，避免网络响应模型依赖消息 feature。 */
fun CoreQuestionPayloadBean.buildAction(
    action: CandidateJobAgentClientActionEnum,
): CardAction {
    return CardAction(
        actionCode = action.name,
        actionPayload = CardActionPayloadBean(
            questionQueueId = questionQueueId,
            attributeGroupCode = attributeGroupCode,
            metaCode = metaCode,
        ),
    )
}
