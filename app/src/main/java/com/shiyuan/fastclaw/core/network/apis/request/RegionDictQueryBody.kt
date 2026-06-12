package com.shiyuan.fastclaw.core.network.apis.request

data class RegionDictQueryBody(
    val sceneCode: String = SCENE_CODE_JOB_AGENT,
    val keyword: String? = null,
    val selectedRegionCodes: List<String>? = null,
    val includeHotLocations: Boolean = true,
    val includeHistoryLocations: Boolean = true,
    val maxSelectCount: Int? = null,
) : UserTokenBody() {

    companion object {
        const val SCENE_CODE_JOB_AGENT = "JOB_AGENT"
    }
}


data class DictIndustryQueryBody(
    val keyword: String? = null,
    val sessionId: String? = null,
    val conversationId: String? = null,
)  {
}


data class DictPositionQueryBody(
    val keyword: String? = null,
    val sessionId: String? = null,
    val conversationId: String? = null,
)  {
}

data class DictWelfareQueryBody(
    val sessionId: String? = null,
    val conversationId: String? = null,
    val keyword: String? = null,
    val limit: Int? = null,
)


