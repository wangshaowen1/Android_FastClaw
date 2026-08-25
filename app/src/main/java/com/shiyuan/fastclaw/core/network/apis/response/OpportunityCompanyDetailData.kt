package com.shiyuan.fastclaw.core.network.apis.response

/**
 * 机会关联公司的公开信息响应。
 *
 * 用于公司主页展示企业基础资料、公司介绍、福利标签和所在地。
 */
data class OpportunityCompanyDetailData(
    /** 企业业务 ID。 */
    val enterpriseId: Long = 0,
    /** 公司品牌名称，企业名称为空时作为页面标题兜底。 */
    val brandName: String = "",
    /** 公司 Logo 地址。 */
    val logoUrl: String = "",
    /** 企业工商名称。 */
    val enterpriseName: String = "",
    /** 所属行业名称。 */
    val industryName: String = "",
    /** 公司规模名称。 */
    val scaleName: String = "",
    /** 融资阶段名称。 */
    val financingStageName: String = "",
    /** 公司介绍。 */
    val introduction: String = "",
    /** 公司文化描述。 */
    val companyCulture: String = "",
    /** 公司福利标签列表。 */
    val welfareTags: List<String> = emptyList(),
    /** 公司所在地展示文本。 */
    val location: String = "",
)
