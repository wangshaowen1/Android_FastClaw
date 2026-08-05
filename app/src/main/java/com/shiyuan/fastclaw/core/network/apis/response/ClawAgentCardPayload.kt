package com.shiyuan.fastclaw.core.network.apis.response

import com.shiyuan.fastclaw.feature.clawagent.bean.CandidateJobAgentClientActionEnum

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
    val confirmMode: String? = null,
    /** 候选值 */
    val confirmationItems: List<ConfirmActionItemBean>? = null,
    /** 属性名称，如「意向岗位」 */
    val fieldName: String? = null,
    val displayText: String? = null,
    /** 变更前的值 有毒 */
//    val oldValue: String? = null,
    /** 变更后的值 */
//    val newValue: String? = null,
    /** 待确认候选值 */
//    val candidateValue: String? = null,
    /** 拒绝行为，当前为 REASK（拒绝后服务端重新下发问题卡） */
    val rejectBehavior: String? = null,
    /** 拒绝后重问文案 */
    val reaskQuestionText: String? = null,
    /** 展示确认、拒绝按钮 */
    val actions: List<CardAction>? = null
)

/** 候选属性 */
data class ConfirmActionItemBean(

    val attributeGroupCode: String? = null,

    val metaCode: String? = null,

    val fieldName: String? = null,

    val displayText: String? = null,

    val collectMode: String? = null,

    val groupValueType: String? = null,
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
    //val selectedValue: String? = null,
    /** 已选展示文案 */
    val selectedText: String? = null,
    /** 端侧幂等提交 ID */
    val clientActionId: String? = null,
)

data class CardActionPayloadBean(
    val questionQueueId: String? = null,
    val attributeGroupCode: String? = null,
    val metaCode: String? = null,
    var selectedText: String? = null,
//    val value: String? = null,
//    val candidateValue: String? = null,
//    val newValue: String? = null,
    val rejectBehavior: String? = null,
    var selectedValue: String? = null,
    /** 后台推送给用户自动发的文本 */
    val skipBubbleText: String? = null,
)

/** CARD 公共：按钮（7.4.2） */
data class CardAction(
    var actionCode: String? = null,
    val text: String? = null,
    val primary: Boolean? = null,
    val disabled: Boolean? = null,
    val selected: Boolean? = null,
    /** 端侧本地行为，如 UPLOAD_FILE_THEN_SUBMIT */
    val clientBehavior: String? = null,
    /** 回传参数，原样转 actionPayloadJson */
    val actionPayload: CardActionPayloadBean? = null,
)

/** CARD 公共：问题卡选项（7.4.3） */
data class CardOption(
    val code: String? = null,
    val text: String? = null,
    val label: String? = null,
//    val value: String? = null,
//    val selectedValue: String? = null,
    /** 问题卡选项当前固定 QUESTION_OPTION_SUBMIT */
    var actionCode: String? = null,
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
) {

    companion object {
        /** 跳过按钮 */
        const val ACTION_SKIP_CURRENT_QUESTION = "SKIP_CURRENT_QUESTION"
        /** 重新回答 */
        const val ACTION_QUESTION_REANSWER_SUBMIT = "QUESTION_REANSWER_SUBMIT"
        /** 没有简历 */
        const val ACTION_NO_RESUME_START_QUESTIONING = "NO_RESUME_START_QUESTIONING"
        /** 暂时不需要面试 */
        const val ACTION_MOCK_INTERVIEW_NOT_NEEDED = "MOCK_INTERVIEW_NOT_NEEDED"

        /** 当前卡可操作 */
        const val STATUS_ACTIVE = "ACTIVE"
        /** 当前题已有效提交或已填写 */
        const val STATUS_ANSWERED = "ANSWERED"
        /** 其他系统关闭或兼容动作已提交 */
        const val STATUS_SUBMITTED = "SUBMITTED"
        /** 用户明确点击跳过或拒绝 */
        const val STATUS_SKIPPED = "SKIPPED"
        /** 旧题未完成，已被重发题卡替代 */
        const val STATUS_REASKED = "REASKED"

        /** 文本输入 */
        const val MODE_TEXT = "TEXT"
        /** 枚举单选 */
        const val MODE_SINGLE_CHOICE = "SINGLE_CHOICE"
        /** 枚举多选 */
        const val MODE_MULTI_CHOICE = "MULTI_CHOICE"
        /** 字典单选 */
        const val MODE_DICT_SINGLE = "DICT_SINGLE"
        /** 字典多选 */
        const val MODE_DICT_MULTI = "DICT_MULTI"
        /** 年月选择 */
        const val MODE_DATE_MONTH = "DATE_MONTH"
        /** 薪资范围 */
        const val MODE_SALARY_RANGE = "SALARY_RANGE"
        /** 数组表单 */
        const val MODE_ARRAY_FORM = "ARRAY_FORM"
    }

    fun buildAction(enum: CandidateJobAgentClientActionEnum): CardAction {
        return CardAction(
            actionCode = enum.name,
            actionPayload = CardActionPayloadBean(
                questionQueueId = questionQueueId,
                attributeGroupCode = attributeGroupCode,
                metaCode = metaCode
            )
        )
    }

    /** 整卡是否已回答：状态机终态或整卡禁用 */
    fun isAnswered(): Boolean {
        //用户点了跳过按钮，也算回答了这个问题，所以问题的状态是“跳过状态”
        val state = interactionState ?: return false
        return state.status == STATUS_ANSWERED ||
                state.status == STATUS_SUBMITTED ||
                state.status == STATUS_SKIPPED ||
                state.status == STATUS_REASKED
    }

    fun isDisabled(): Boolean {
        val state = interactionState ?: return true
        return state.disabled == true
    }

    fun isJobStatus(): Boolean {// 求职状态
        return inputMode == MODE_SINGLE_CHOICE && attributeGroupCode == "jobStatus"
    }

    //`现居住城市-location`、`民族-ethnicity`
    fun isLocationCity(): Boolean {// 现居住城市
        return inputMode == MODE_DICT_SINGLE && attributeGroupCode == "location"
    }

    //`意向城市-expectedCity`、`意向岗位-jobIntention`、`期望行业-expectedIndustry`、`福利偏好-welfarePreference
    fun isExpectedCity(): Boolean {// 意向城市
        return inputMode == MODE_DICT_MULTI && attributeGroupCode == "expectedCity"
    }

    fun isJobIntention(): Boolean {// 意向岗位
        return inputMode == MODE_DICT_MULTI && attributeGroupCode == "jobIntention"
    }

    fun isExpectedIndustry(): Boolean {// 期望行业
        return inputMode == MODE_DICT_MULTI && attributeGroupCode == "expectedIndustry"
    }

    fun isWelfarePreference(): Boolean {// 福利偏好
        return inputMode == MODE_DICT_MULTI && attributeGroupCode == "welfarePreference"
    }

    //``出生年月-birthday`
    fun isBirthday(): Boolean {// 出生年月
        return inputMode == MODE_DATE_MONTH && attributeGroupCode == "birthday"
    }

    //``期望薪资-expectedSalary`
    fun isExpectedSalary(): Boolean {// 薪资范围
        return inputMode == MODE_SALARY_RANGE &&  attributeGroupCode == "expectedSalary"
    }

   // `工作经历-workExperience`、`教育经历-educationInfo`、`技能-skills`、`项目经历-projectExperience`、`证书-certificates`、`奖项-awards`
    fun isWorkExperience(): Boolean {// 工作经历
       return inputMode == MODE_ARRAY_FORM && attributeGroupCode == "workExperience"
    }

    fun isEducationInfo(): Boolean {// 教育经历
        return inputMode == MODE_ARRAY_FORM && attributeGroupCode == "educationInfo"
    }

    fun isSkills(): Boolean {// 技能
        return inputMode == MODE_ARRAY_FORM && attributeGroupCode == "skills"
    }

    fun isProjectExperience(): Boolean {// 项目经历
        return inputMode == MODE_ARRAY_FORM && attributeGroupCode == "projectExperience"
    }

    fun isCertificates(): Boolean {// 证书
        return inputMode == MODE_ARRAY_FORM && attributeGroupCode == "certificates"
    }

    fun isAwards(): Boolean {//奖项
        return inputMode == MODE_ARRAY_FORM && attributeGroupCode == "awards"
    }
}

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
) {
    fun isDisabled(): Boolean {
        val state = interactionState ?: return true
        return state.disabled == true
    }
}

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

/** FILE_CARD 用户侧文件卡片。 */
data class UserFilePayloadBean(
    /** 文件名 */
    val fileName: String? = null,
    /** 文件大小，单位 byte */
    val fileSize: Long? = null,
    /** 文件类型，如 pdf/doc/docx */
    val fileType: String? = null,
    /** 简历业务 ID */
    val resumeId: String? = null,
    /** 简历名称 */
    val resumeName: String? = null,
    /** 简历上传时间 */
    val resumeTime: String? = null,
    val interactionState: CardInteractionState? = null,
)

data class CompanyBackgroundEntryPayloadBean(
    /** 卡片交互状态，用于控制入口卡是否禁用 */
    val interactionState: CardInteractionState? = null,
    /** 入口卡可执行动作列表，点击开始背调时取首个动作回传 */
    val actions: List<CardAction>? = null,
)

data class CompanyBackgroundReportPayloadBean(
    /** 报告生成状态，具体枚举值以服务端约定为准 */
    val reportStatus: String? = null,
    /** 公司背调报告 ID，用于后续查看报告详情 */
    val reportId: String? = null,
    /** 报告名称，优先用于已生成卡片主标题展示 */
    val reportName: String? = null,
    /** 被背调的公司名称，报告名称为空时作为展示兜底 */
    val companyName: String? = null,
    /** 报告生成时间，优先用于已生成卡片时间展示 */
    val reportGenerateTime: String? = null,
    /** 报告生成完成时间兼容字段 */
    val generatedAt: String? = null,
    /** 报告生成时间兼容字段 */
    val generateTime: String? = null,
    /** 报告创建时间兼容字段 */
    val createTime: String? = null,
    /** 生成失败原因或错误提示 */
    val errorMessage: String? = null,
    /** 报告卡动作列表，如重新生成、查看报告 */
    val actions: List<CardAction>? = null,
)

data class MockInterviewEntryPayloadBean(
    /** 报告生成状态，具体枚举值以服务端约定为准 */
    val reportStatus: String? = null,
    /** 卡片业务类型，用于区分模拟面试入口等业务卡 */
    val cardBizType: String? = null,
    /** 入口消息 ID，开始模拟面试时回传给服务端 */
    val entryMessageId: String? = null,
    /** 入口来源，开始模拟面试时回传给服务端 */
    val entrySource: String? = null,
    /** 当前使用的简历 ID */
    val resumeId: String? = null,
    /** 简历状态，用于服务端或端侧判断是否可发起面试 */
    val resumeStatus: String? = null,
    /** 按钮状态，DISABLED 时端侧置灰并禁止点击 */
    val buttonStatus: String? = null,
    /** 本次模拟面试消耗数量 */
    val chargeAmount: Int? = null,
    /** 消耗单位，如虾条、积分等 */
    val chargeUnit: String? = null,
    /** 入口卡标题 */
    val title: String? = null,
    /** 入口卡副标题 */
    val subtitle: String? = null,
    /** 主按钮动作，优先于 actions 首项使用 */
    val primaryAction: CardAction? = null,
    /** 卡片交互状态，用于控制入口卡是否禁用 */
    val interactionState: CardInteractionState? = null,
    /** 入口卡备用动作列表 */
    val actions: List<CardAction>? = null,
)

data class MockInterviewReportPayloadBean(
    /** 卡片业务类型，用于区分模拟面试报告等业务卡 */
    val cardBizType: String? = null,
    /** 模拟面试会话 ID */
    val interviewId: String? = null,
    /** 模拟面试报告 ID，用于查看报告详情或查询进度 */
    val mockReportId: String? = null,
    /** 报告生成状态，具体枚举值以服务端约定为准 */
    val reportStatus: String? = null,
    /** 报告生成进度百分比 */
    val progressPercent: Int? = null,
    /** 报告生成进度文案 */
    val progressText: String? = null,
    /** 面试评分，优先用于已生成报告卡展示 */
    val totalScore: Int? = null,
    /** 面试评分兼容字段 */
    val interviewScore: Int? = null,
    /** 报告评分兼容字段 */
    val reportScore: Int? = null,
    /** 面试时间，优先用于报告卡时间展示 */
    val interviewTime: String? = null,
    /** 报告生成时间 */
    val reportGenerateTime: String? = null,
    /** 报告生成完成时间兼容字段 */
    val generatedAt: String? = null,
    /** 报告生成时间兼容字段 */
    val generateTime: String? = null,
    /** 生成失败原因或错误提示 */
    val errorMessage: String? = null,
    /** 报告卡动作列表，如重新生成、查看报告 */
    val actions: List<CardAction>? = null,
)

data class PassPackageReportPayloadBean(
    /** 面试通关包 ID，用于后续查看报告或重新生成动作回传。 */
    val packageId: String? = null,
    /** 报告生成状态：GENERATING / SUCCESS / FAILED 等。 */
    val reportStatus: String? = null,
    /** 报告生成进度百分比。 */
    val progress: Int? = null,
    /** 是否允许重新生成。 */
    val retryable: Boolean? = null,
    /** 生成批次。 */
    val generateBatchNo: Int? = null,
    /** 创建时间。 */
    val createTime: String? = null,
    /** 生成时间。 */
    val generateTime: String? = null,
    /** 报告时间。 */
    val reportTime: String? = null,
    /** 面试通过率。 */
    val passRate: Int? = null,
    /** 生成失败原因或错误提示。 */
    val errorMessage: String? = null,
    /** 卡片交互状态。 */
    val interactionState: CardInteractionState? = null,
    /** 报告卡动作列表，如重新生成、查看报告。 */
    val actions: List<CardAction>? = null,
)
