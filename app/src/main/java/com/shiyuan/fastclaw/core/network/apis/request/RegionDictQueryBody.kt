package com.shiyuan.fastclaw.core.network.apis.request

/**
 * 地区字典查询请求体（求职 Agent）
 *
 * @param sceneCode 场景编码，默认 JOB_AGENT
 * @param keyword 关键字搜索
 * @param selectedRegionCodes 已选地区编码列表
 * @param includeHotLocations 是否返回热门地点
 * @param includeHistoryLocations 是否返回历史地点
 * @param maxSelectCount 最大可选数量
 */
data class RegionDictQueryBody(
    /** 场景编码 */
    val sceneCode: String = SCENE_CODE_JOB_AGENT,
    /** 关键字搜索 */
    val keyword: String? = null,
    /** 已选地区编码列表 */
    val selectedRegionCodes: List<String>? = null,
    /** 是否返回热门地点 */
    val includeHotLocations: Boolean = true,
    /** 是否返回历史地点 */
    val includeHistoryLocations: Boolean = true,
    /** 最大可选数量 */
    val maxSelectCount: Int? = null,
) : UserTokenBody() {

    companion object {
        /** 求职 Agent 场景编码 */
        const val SCENE_CODE_JOB_AGENT = "JOB_AGENT"
    }
}


/**
 * 行业字典查询请求体
 *
 * @param keyword 关键字搜索
 * @param sessionId 会话 ID
 * @param conversationId 对话 ID
 */
data class DictIndustryQueryBody(
    /** 关键字搜索 */
    val keyword: String? = null,
    /** 会话 ID */
    val sessionId: String? = null,
    /** 对话 ID */
    val conversationId: String? = null,
)


/**
 * 职位字典查询请求体
 *
 * @param keyword 关键字搜索
 * @param sessionId 会话 ID
 * @param conversationId 对话 ID
 */
data class DictPositionQueryBody(
    /** 关键字搜索 */
    val keyword: String? = null,
    /** 会话 ID */
    val sessionId: String? = null,
    /** 对话 ID */
    val conversationId: String? = null,
)

/**
 * 福利字典查询请求体
 *
 * @param sessionId 会话 ID
 * @param conversationId 对话 ID
 * @param keyword 关键字搜索
 * @param limit 返回数量限制
 */
data class DictWelfareQueryBody(
    /** 会话 ID */
    val sessionId: String? = null,
    /** 对话 ID */
    val conversationId: String? = null,
    /** 关键字搜索 */
    val keyword: String? = null,
    /** 返回数量限制 */
    val limit: Int? = null,
)


