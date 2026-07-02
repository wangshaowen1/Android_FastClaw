package com.shiyuan.fastclaw.core.network.apis.response

/**
 * 在线简历信息
 *
 * @param sections 简历信息分组
 */
data class OnlineResumeInfoData(
    val sections: List<OnlineResumeInfoSection> = emptyList()
)

/**
 * 在线简历信息分组
 *
 * @param sectionCode 分组编码
 * @param sectionName 分组名称
 * @param fields 分组字段列表
 */
data class OnlineResumeInfoSection(
    val sectionCode: String? = null,
    val sectionName: String? = null,
    val fields: List<AttributeArrayQueryData> = emptyList()
)
