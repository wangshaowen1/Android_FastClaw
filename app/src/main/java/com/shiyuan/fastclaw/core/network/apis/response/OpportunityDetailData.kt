package com.shiyuan.fastclaw.core.network.apis.response

/**
 * 机会详情响应。
 *
 * 包含岗位、公司、匹配推荐信息，以及服务端按当前状态下发的可用操作。
 */
data class OpportunityDetailData(
    /** 岗位 ID。 */
    val jobId: String = "",
    /** 岗位名称。 */
    val jobTitle: String = "",
    /** 岗位与候选人的匹配度，取值范围为 0～100。 */
    val matchPercent: Int = 0,
    /** 机会状态，当前使用 NEW、COMMUNICATING 或 UNSUITABLE。 */
    val status: String = "",
    /** 岗位是否已关闭；关闭时详情页操作不可用。 */
    val jobClosed: Boolean = false,
    /** 机会关联的企业业务 ID，用于进入公司主页。 */
    val enterpriseId: String = "",
    /** 公司展示名称。 */
    val companyName: String = "",
    /** 服务端组装完成的薪资文案。 */
    val salaryText: String = "",
    /** 工作城市展示文本。 */
    val workCity: String = "",
    /** 工作经验要求展示文本。 */
    val experienceText: String = "",
    /** 学历要求展示文本。 */
    val degreeText: String = "",
    /** 推荐该机会的原因，详情页按 Markdown 渲染。 */
    val recommendationReason: String = "",
    /** 岗位描述，详情页按 Markdown 渲染。 */
    val jobDescription: String = "",
    /** 公司所属行业。 */
    val companyIndustry: String = "",
    /** 公司规模。 */
    val companyScale: String = "",
    /** 公司融资阶段。 */
    val financingStage: String = "",
    /** 工作地点详细地址。 */
    val workAddress: String = "",
    /** 当前状态允许执行的操作，详情页按接口返回顺序展示。 */
    val actions: List<OpportunityDetailActionData> = emptyList(),
)

/** 机会详情底部操作，按接口返回顺序展示。 */
data class OpportunityDetailActionData(
    /** 按钮展示文案。 */
    val buttonText: String = "",
    /** 提交给状态更新接口的操作编码；为空时按钮不可操作。 */
    val actionCode: String? = null,
)
