package com.kgmedia.fastfilm.core.network.apis.response

/**
{
"id": "剧集id",
"storyTitle": "剧集标题",
"storySummary": "剧集梗概",
"storyContent": "剧集剧本内容",
"sortNo": "序号",
"stepProgress": "[枚举]创作流程进度 [10=剧本输入 20=资产生成中 30=分镜生成中 40=已完成]",
"stepStatus": "[枚举]创作流程状态 [0=草稿 1=生成中 2=已完成 3=生成失败]",
"storyboardTotal": "分镜数"
}
 */
data class LastStoryData(
    val id: String? = null,
    val stepProgress: Int? = null,
    val stepStatus: Int? = null,
    val storyContent: String? = null,
    val storySummary: String? = null,
    val storyTitle: String? = null,
    val storyboardTotal: Int? = null,
    val sortNo: Int? = null,
)