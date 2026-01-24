package com.kgmedia.fastfilm.core.network.apis.request

data class OptimizeContentBody(
    val content: String?,
    /**
     * 优化类型 1: 灵感故事 2: 自己写故事
     */
    val optimizeType: Int? = null//加一个这个参数
) : UserTokenBody()

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