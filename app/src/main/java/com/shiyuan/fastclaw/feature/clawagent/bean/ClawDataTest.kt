package com.shiyuan.fastclaw.feature.clawagent.bean

import com.blankj.utilcode.util.GsonUtils
import com.shiyuan.fastclaw.core.network.apis.response.CardAction
import com.shiyuan.fastclaw.core.network.apis.response.CardActionPayloadBean
import com.shiyuan.fastclaw.core.network.apis.response.CardInteractionState
import com.shiyuan.fastclaw.core.network.apis.response.CardOption
import com.shiyuan.fastclaw.core.network.apis.response.CoreQuestionPayloadBean
import com.shiyuan.fastclaw.feature.clawagent.ClawAgentMsgAdapter
import com.shiyuan.fastclaw.feature.clawagent.ClawAgentViewModel

/**
 * @desc：测试数据
 * @author：CaoWei
 * @date：2026/6/9 16:52
 */
object ClawDataTest {

    fun getDataList(newList: List<ClawAgentMessageInfo>): List<ClawAgentMessageInfo> {
        var dataList = newList.toMutableList()
        /*dataList.add(2, ClawAgentMessageInfo(
            id = "-1",
            messageType = ClawAgentMsgAdapter.Companion.TYPE_QUESTION,
            sendObj = ClawAgentViewModel.Companion.SENDER_AGENT,
            receiveObj = ClawAgentViewModel.Companion.SENDER_USER,
            metaData = GsonUtils.toJson(
                CoreQuestionPayloadBean(
                    questionQueueId = "123",
                    attributeGroupCode = "expectedCity",
                    metaCode = "expectedCity",
                    questionType = "CORE_PROFILE",
                    questionText = "你的意向城市是？",
                    inputMode = "DICT_MULTI",
                    answeredText = null,
                    interactionState = CardInteractionState(
                        status = "ACTIVE",
                        disabled = false
                    ),
                    options = emptyList(),
                    actions = listOf(
                        CardAction(
                            actionCode = "OPEN_DICT_SELECTOR",
                            text = "请选择",
                            primary = true,
                            disabled = false,
                            selected = false,
                            clientBehavior = null,
                            actionPayload = CardActionPayloadBean(
                                questionQueueId = "123",
                                attributeGroupCode = "expectedCity",
                                metaCode = "expectedCity"
                            )
                        )
                    )
                )
            )
        ))*/
       /* dataList.add(2, ClawAgentMessageInfo(
            id = "-1",
            messageType = ClawAgentMsgAdapter.Companion.TYPE_QUESTION,
            sendObj = ClawAgentViewModel.Companion.SENDER_AGENT,
            receiveObj = ClawAgentViewModel.Companion.SENDER_USER,
            metaData = GsonUtils.toJson(
                CoreQuestionPayloadBean(
                    questionQueueId = "456",
                    attributeGroupCode = "workExperience",
                    metaCode = "workExperience",
                    questionType = "EXTEND_QUESTION",
                    questionText = "你来自哪里啊啊啊啊啊啊啊啊啊啊？",
                    inputMode = "SINGLE_CHOICE",
                    answeredText = null,
                    interactionState = CardInteractionState(
                        status = "ANSWERED",
                        disabled = false
                    ),
                    options = listOf(
                        CardOption(
                            actionCode = "OPEN_ARRAY_FORM",
                            text = "湖南",
                            disabled = true,
                            selected = false,
                            actionPayload = mapOf(
                                "questionQueueId" to "456",
                                "attributeGroupCode" to "workExperience"
                            )
                        ),
                        CardOption(
                            actionCode = "OPEN_ARRAY_FORM",
                            text = "深圳",
                            disabled = true,
                            selected = true,
                            actionPayload = mapOf(
                                "questionQueueId" to "456",
                                "attributeGroupCode" to "workExperience"
                            )
                        ),
                        CardOption(
                            actionCode = "OPEN_ARRAY_FORM",
                            text = "上海",
                            disabled = true,
                            selected = false,
                            actionPayload = mapOf(
                                "questionQueueId" to "456",
                                "attributeGroupCode" to "workExperience"
                            )
                        ),
                    ),
                    actions = emptyList()
                )
            )
        ))*/
        dataList.add(2, ClawAgentMessageInfo(
            id = "-1",
            messageType = ClawAgentMsgAdapter.Companion.TYPE_COMPANY_BACKGROUND_ENTRY,
            sendObj = ClawAgentViewModel.Companion.SENDER_AGENT,
            receiveObj = ClawAgentViewModel.Companion.SENDER_USER,
            metaData = GsonUtils.toJson(
                CoreQuestionPayloadBean(
                    questionQueueId = "123",
                    attributeGroupCode = "expectedCity",
                    metaCode = "expectedCity",
                    questionType = "CORE_PROFILE",
                    questionText = "背调卡片",
                    inputMode = "SALARY_RANGE",
                    answeredText = null,
                    interactionState = CardInteractionState(
                        status = "ACTIVE",
                        disabled = false
                    ),
                    options = emptyList(),
                    actions = listOf(
                        CardAction(
                            actionCode = "OPEN_DICT_SELECTOR",
                            text = "请选择",
                            primary = true,
                            disabled = false,
                            selected = false,
                            clientBehavior = null,
                            actionPayload = CardActionPayloadBean(
                                questionQueueId = "123",
                                attributeGroupCode = "expectedCity",
                                metaCode = "expectedCity"
                            )
                        )
                    )
                )
            )
        ))
        dataList.add(2, ClawAgentMessageInfo(
            id = "-1",
            messageType = ClawAgentMsgAdapter.Companion.TYPE_COMPANY_BACKGROUND_REPORT_GENERATING,
            sendObj = ClawAgentViewModel.Companion.SENDER_AGENT,
            receiveObj = ClawAgentViewModel.Companion.SENDER_USER,
            metaData = GsonUtils.toJson(
                CoreQuestionPayloadBean(
                    questionQueueId = "123",
                    attributeGroupCode = "expectedCity",
                    metaCode = "expectedCity",
                    questionType = "CORE_PROFILE",
                    questionText = "背调生成中",
                    inputMode = "SALARY_RANGE",
                    answeredText = null,
                    interactionState = CardInteractionState(
                        status = "ACTIVE",
                        disabled = false
                    ),
                    options = emptyList(),
                    actions = listOf(
                        CardAction(
                            actionCode = "OPEN_DICT_SELECTOR",
                            text = "请选择",
                            primary = true,
                            disabled = false,
                            selected = false,
                            clientBehavior = null,
                            actionPayload = CardActionPayloadBean(
                                questionQueueId = "123",
                                attributeGroupCode = "expectedCity",
                                metaCode = "expectedCity"
                            )
                        )
                    )
                )
            )
        ))
        dataList.add(2, ClawAgentMessageInfo(
            id = "-1",
            messageType = ClawAgentMsgAdapter.Companion.TYPE_COMPANY_BACKGROUND_REPORT_FAILED,
            sendObj = ClawAgentViewModel.Companion.SENDER_AGENT,
            receiveObj = ClawAgentViewModel.Companion.SENDER_USER,
            metaData = GsonUtils.toJson(
                CoreQuestionPayloadBean(
                    questionQueueId = "123",
                    attributeGroupCode = "expectedCity",
                    metaCode = "expectedCity",
                    questionType = "CORE_PROFILE",
                    questionText = "背调生成失败",
                    inputMode = "SALARY_RANGE",
                    answeredText = null,
                    interactionState = CardInteractionState(
                        status = "ACTIVE",
                        disabled = false
                    ),
                    options = emptyList(),
                    actions = listOf(
                        CardAction(
                            actionCode = "OPEN_DICT_SELECTOR",
                            text = "请选择",
                            primary = true,
                            disabled = false,
                            selected = false,
                            clientBehavior = null,
                            actionPayload = CardActionPayloadBean(
                                questionQueueId = "123",
                                attributeGroupCode = "expectedCity",
                                metaCode = "expectedCity"
                            )
                        )
                    )
                )
            )
        ))
        dataList.add(2, ClawAgentMessageInfo(
            id = "-1",
            messageType = ClawAgentMsgAdapter.Companion.TYPE_COMPANY_BACKGROUND_REPORT_DONE,
            sendObj = ClawAgentViewModel.Companion.SENDER_AGENT,
            receiveObj = ClawAgentViewModel.Companion.SENDER_USER,
            metaData = GsonUtils.toJson(
                CoreQuestionPayloadBean(
                    questionQueueId = "123",
                    attributeGroupCode = "expectedCity",
                    metaCode = "expectedCity",
                    questionType = "CORE_PROFILE",
                    questionText = "背调生成成功",
                    inputMode = "SALARY_RANGE",
                    answeredText = null,
                    interactionState = CardInteractionState(
                        status = "ACTIVE",
                        disabled = false
                    ),
                    options = emptyList(),
                    actions = listOf(
                        CardAction(
                            actionCode = "OPEN_DICT_SELECTOR",
                            text = "请选择",
                            primary = true,
                            disabled = false,
                            selected = false,
                            clientBehavior = null,
                            actionPayload = CardActionPayloadBean(
                                questionQueueId = "123",
                                attributeGroupCode = "expectedCity",
                                metaCode = "expectedCity"
                            )
                        )
                    )
                )
            )
        ))
        return dataList
    }
}