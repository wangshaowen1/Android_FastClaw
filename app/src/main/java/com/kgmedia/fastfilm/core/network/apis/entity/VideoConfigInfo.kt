package com.kgmedia.fastfilm.core.network.apis.entity

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 *  {
 *                     "configCode": "配置 code剧集：ep_15=15集以内 ep_30=15~30集 ep_custom=自定义视频比例：ratio_16_9=16:9 ratio_9_16=9:16视频风格：style_cn_anime=国风动漫 style_kr_manhwa=韩漫风 style_realistic_anime=写实动漫 style_jp_anime=日漫风style_hand_drawn=手绘风 style_semi_real=二次元写实风 style_gu_feng=古风 style_anime=动漫风 style_us_modern=现代美式风",
 *                     "configValue": "配置值",
 *                     "extraConfig": "扩展配置（视频风格图片url）",
 *                     "sortNo": "排序号"
 *                 }
 */
@Parcelize
data class VideoConfigInfo(
    val configCode: String? = null,
    //配置值
    val configValue: String? = null,
    //扩展配置（视频风格图片url）
    val extraConfig: String? = null,
    //排序号
    val sortNo: Int? = null
) : Parcelable {
    fun isCustomInput(): Boolean {
        return configCode == "ep_custom"
    }
}

/**
 * "styleCode": "视频风格 Code",
 *                     "styleName": "风格名称",
 *                     "styleCoverUrl": "风格封面",
 *                     "stylePrompt": "风格提示词",
 *                     "sortNo": "排序号"
 */
@Parcelize
data class VideoStyleConfigInfo(
    val sortNo: Int? = null,
    val styleCode: String? = null,
    val styleCoverUrl: String? = null,
    val styleName: String? = null,
    val stylePrompt: String? = null
) : Parcelable

//配置类型(2-故事集数,3-视频比例,4-视频风格)
@Parcelize
data class VideoConfigData(
    val totalEpisodeCntItemVoList: List<VideoConfigInfo>?,
    val ratioItemVoList: List<VideoConfigInfo>?,
    val styleItemVOList: List<VideoStyleConfigInfo>?
) : Parcelable