package com.shiyuan.fastclaw.core.network.apis.response

/**
 * 求职 Agent 各 cardType 的 payloadJson 端侧渲染模型。
 *
 * payloadJson 为字符串，由各卡 ViewHolder 用 GsonUtils 二次解析为对应 *Payload。
 * 字段对齐《APP消息对接文档》第 7 节（CARD 公共渲染协议 + 各 cardType 字段表）。
 * int64 ID 字段统一用 String? 接收。
 *
 * @author TooWhiteT
 */

/** UNDERSTANDING_SUMMARY 理解度摘要卡（7.5.5） */
data class UnderstandingSummaryPayloadBean(
    /** 卡片标题 */
    val title: String? = null,
    /** 摘要说明 */
    val description: String? = null,
    /** 当前理解度分数，0-100 */
    val understandingScore: Int? = null,
    /** 摘要区：7 个高权重字段补齐情况 */
    val summary: Summary? = null,
) {
    data class Summary(
        /** 摘要主文案 */
        val text: String? = null,
        /** 固定展示字段上限，本期 7 */
        val highlightLimit: Int? = null,
        /** 7 个字段中已确认且有展示值的数量 */
        val highlightAlignedCount: Int? = null,
        /** 固定 7 个高权重字段，按服务端返回顺序展示 */
        val fields: List<SummaryField>? = null,
    )

    data class SummaryField(
        /** 展示字段编码（UI key），不等同后台属性分组编码 */
        val attributeGroupCode: String? = null,
        /** 展示字段名 */
        val fieldName: String? = null,
        /** 已确认时返回展示值；未补齐/未确认/被拒绝为 null */
        val displayValue: String? = null,
        /** 是否已确认且有展示值 */
        val aligned: Boolean? = null,
    )
}

/** ATTRIBUTE_CONFIRM 属性候选值确认卡（7.5.4）；端侧走记忆确认弹窗，不进对话列表 */
data class AttributeConfirmPayloadBean(
    /** 卡片标题 */
    val title: String? = null,
    /** 控制整卡置灰 */
    val interactionState: CardInteractionState? = null,
    /** 问题队列 ID，确认/拒绝时回传 */
    val questionQueueId: String? = null,
    /** 属性分组编码，回传辅助定位 */
    val attributeGroupCode: String? = null,
    /** 元属性编码，回传辅助定位 */
    val metaCode: String? = null,
    /** 固定 ATTRIBUTE_CONFIRM */
    val questionType: String? = null,
    /** 确认问题文案 */
    val questionText: String? = null,
    /** 记忆确认场景，当前固定 MEMORY_CONFIRM */
    val confirmScene: String? = null,
    /** 记忆确认展示文案，如「我听到了：意向岗位 → AI 大模型工程师」 */
    val memoryText: String? = null,
    /** 属性名称，如「意向岗位」 */
    val fieldName: String? = null,
    /** 变更前的值 有毒 */
//    val oldValue: String? = null,
    /** 变更后的值 */
//    val newValue: String? = null,
    /** 待确认候选值 */
    val candidateValue: String? = null,
    /** 拒绝行为，当前为 REASK（拒绝后服务端重新下发问题卡） */
    val rejectBehavior: String? = null,
    /** 拒绝后重问文案 */
    val reaskQuestionText: String? = null,
    /** 展示确认、拒绝按钮 */
    val actions: List<CardAction>? = null
)

/** CARD 公共：整卡交互状态（7.4.1） */
data class CardInteractionState(
    /** ACTIVE / SUBMITTED / ANSWERED / SKIPPED */
    val status: String? = null,
    /** 整卡是否不可点击 */
    val disabled: Boolean? = null,
    /** 已选动作编码 */
    val selectedActionCode: String? = null,
    /** 已选值 */
    val selectedValue: String? = null,
    /** 已选展示文案 */
    val selectedText: String? = null,
    /** 端侧幂等提交 ID */
    val clientActionId: String? = null,
)

/** CARD 公共：按钮（7.4.2） */
data class CardAction(
    val actionCode: String? = null,
    val text: String? = null,
    val primary: Boolean? = null,
    val disabled: Boolean? = null,
    val selected: Boolean? = null,
    /** 端侧本地行为，如 UPLOAD_FILE_THEN_SUBMIT */
    val clientBehavior: String? = null,
    /** 回传参数，原样转 actionPayloadJson */
    val actionPayload: CardActionPayloadBean? = null,
)

data class CardActionPayloadBean(
    val questionQueueId: String? = null,
    val attributeGroupCode: String? = null,
    val metaCode: String? = null,
    val selectedText: String? = null,
    val value: String? = null,
    val candidateValue: String? = null,
    val newValue: String? = null,
    val rejectBehavior: String? = null,
)

/** CARD 公共：问题卡选项（7.4.3） */
data class CardOption(
    val text: String? = null,
    val label: String? = null,
    val value: String? = null,
    val selectedValue: String? = null,
    /** 问题卡选项当前固定 QUESTION_OPTION_SUBMIT */
    val actionCode: String? = null,
    val disabled: Boolean? = null,
    val selected: Boolean? = null,
    val actionPayload: Map<String, Any?>? = null,
)

/** CORE_QUESTION 核心 / 扩展补问卡（7.5.3） */
data class CoreQuestionPayloadBean(
    /** 问题队列 ID，回答/跳过/置灰定位 */
    val questionQueueId: String? = null,
    val attributeGroupCode: String? = null,
    val metaCode: String? = null,
    /** 区分核心补问 / 扩展补问 / 槽位追问 */
    val questionType: String? = null,
    /** 问题文案 */
    val questionText: String? = null,
    /** 问题文案兼容字段 */
    val question: String? = null,
    /** TEXT 文本 / SINGLE_CHOICE 单选 / MULTI_CHOICE 多选 */
    val inputMode: String? = null,
    /** 已回答后的回显摘要 */
    val answeredText: String? = null,
    val interactionState: CardInteractionState? = null,
    val options: List<CardOption>? = null,
    val actions: List<CardAction>? = null,
)

/** UPLOAD_RESUME 上传简历卡（7.5.1）；端侧按 resumeUploadStatus 路由到 引导 / 成功 / 失败 卡 */
data class UploadResumePayloadBean(
    /** 卡片标题 */
    val title: String? = null,
    /** 卡片说明 */
    val description: String? = null,
    /** 理解 */
    val understandingScore: Int? = null,
    /** 对齐度 */
    val alignmentScore: Int? = null,
    /** 对齐度文案 */
    val scoreChangeText: String? = null,
    /** 描述 */
    val nextStepText: String? = null,
    /** 上传/解析状态：WAITING_UPLOAD / PARSING / PARSE_SUCCESS / PARSE_FAILED */
    val resumeUploadStatus: String? = null,
    /** 状态短文案，如「简历解析中」「已上传」「解析失败」 */
    val statusText: String? = null,
    /** 状态说明文案 */
    val statusDesc: String? = null,
    /** 已上传文件名 */
    val fileName: String? = null,
    /** 解析任务 ID */
    val resumeTaskId: String? = null,
    val interactionState: CardInteractionState? = null,
    val actions: List<CardAction>? = null,
)

/** HISTORY_RESUME_CONFIRM 历史简历确认卡（7.5.2） */
data class HistoryResumeConfirmPayloadBean(
    /** 卡片标题 */
    val title: String? = null,
    /** 卡片说明 */
    val description: String? = null,
    /** 简历业务 ID，确认/否认时回传 */
    val resumeId: String? = null,
    /** 结构化简历 ID，确认/否认时回传 */
    val resumeStructId: String? = null,
    /** 解析任务 ID，确认/否认时回传 */
    val resumeTaskId: String? = null,
    /** 简历名称 */
    val resumeName: String? = null,
    /** 简历摘要 */
    val resumeSummary: String? = null,
    /** 简历上传时间 */
    val resumeTime: String? = null,
    val interactionState: CardInteractionState? = null,
    val actions: List<CardAction>? = null,
)