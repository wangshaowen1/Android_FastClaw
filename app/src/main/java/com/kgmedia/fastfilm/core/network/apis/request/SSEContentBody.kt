package com.kgmedia.fastfilm.core.network.apis.request

data class SSEContentBody(
    val prompt: String?,
    /**
     * 业务类型枚举10=随机灵感 11=AI优化灵感 20=剧集梗概 30=梗概剧集剧本 31=小说剧集剧本 32=扩写剧集剧本 33=优化作品简介
     */
    val bizType: Int? = null//加一个这个参数
) : UserTokenBody() {
    companion object {
        /**
         * 随机灵感
         */
        const val TYPE_RANDOM_INSPIRATION = 10

        /**
         * AI优化灵感
         */
        const val TYPE_AI_OPTIMIZE_INSPIRATION = 11

        /**
         * 剧集梗概生成
         */
        const val TYPE_EPISODE_SYNOPSIS = 20

        /**
         * 剧本内容生成
         */
        const val TYPE_EPISODE_CONTENT = 30

        /**
         * 小说模式==》剧本内容生成
         */
        const val TYPE_FICTION_MODEL_EPISODE_CONTENT = 31

        /**
         * 扩写剧集剧本
         */
        const val TYPE_CONTINUE_WRITE = 32

        /**
         * 优化作品简介
         */
        const val TYPE_OPTIMIZE_DRAMA_DESC = 33
    }
}

data class OptimizeContentData(
    val content: String?,
)

/**
{
"inputContent": "输入内容",
"genWay": "[枚举]生成方式 [1=灵感剧本 2=自定义剧本]",
"styleId": "短剧风格表id(b_short_dramas_style)",
"videoRatio": "[枚举]视频比例 [1=9:16 2=16:9 3=3:4 4=4:3]",
"userToken": "用户token，接受Long和String类型"
}
 *
 */
data class CreationGeneratePreBody(
    val genWay: Int? = null,
    val inputContent: String? = null,
    val styleId: String? = null,
    val videoRatio: Int? = null
) : UserTokenBody() {

    companion object {
        const val GEN_WAY_INSPIRATION = 1
        const val GEN_WAY_CUSTOMIZED = 2

        const val RATIO_9_16 = 1
        const val RATIO_16_9 = 2
        const val RATIO_3_4 = 3
        const val RATIO_4_3 = 4
    }
}