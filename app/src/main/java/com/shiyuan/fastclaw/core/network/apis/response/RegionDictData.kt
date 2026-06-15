package com.shiyuan.fastclaw.core.network.apis.response

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * 地区字典响应数据（求职 Agent 城市/区县选择）
 *
 * @param maxSelectCount       最大可选地点数
 * @param selectedLocations    服务端按场景反显的当前已选地点列表
 * @param historyLocations     当前用户历史选择地点列表
 * @param hotLocations         热门地点列表
 * @param groupedLocations     按拼音首字母分组的地点列表
 * @param matchedLocations      关键字搜索命中的地点列表
 * @param alphabetIndex        右侧字母索引列表
 */
@Parcelize
data class RegionLocation(
    /** 行政区划编码 */
    val regionCode: String? = null,
    /** 行政区划名称 */
    val regionName: String? = null,
    /** 层级，工作地点选择器通常返回地级或县级 */
    val regionLevel: Int? = null,
    /** 地点类型 */
    val locationType: String? = null,
    /** 完整名称路径 */
    val fullNamePath: String? = null,
    /** 拼音首字母 */
    val firstLetter: String? = null,
) : Parcelable

/**
 * 地区字典响应数据（求职 Agent）
 *
 * @param maxSelectCount       最大可选地点数
 * @param selectedLocations    服务端按场景反显的当前已选地点列表
 * @param historyLocations     当前用户历史选择地点列表
 * @param hotLocations         热门地点列表
 * @param groupedLocations      按拼音首字母分组的地点列表
 * @param matchedLocations     关键字搜索命中的地点列表
 * @param alphabetIndex        右侧字母索引列表
 */
data class RegionDictData(
    /** 最大可选地点数 */
    val maxSelectCount: Int? = null,
    /** 服务端按场景反显的当前已选地点列表 */
    val selectedLocations: List<RegionLocation>? = null,
    /** 当前用户历史选择地点列表 */
    val historyLocations: List<RegionLocation>? = null,
    /** 热门地点列表 */
    val hotLocations: List<RegionLocation>? = null,
    /** 按拼音首字母分组的地点列表 */
    val groupedLocations: List<RegionGroupedLocation>? = null,
    /** 关键字搜索命中的地点列表 */
    val matchedLocations: List<RegionLocation>? = null,
    /** 右侧字母索引列表 */
    val alphabetIndex: List<String>? = null,
)

/**
 * 地区字母分组
 *
 * @param letter    分组字母（A-Z）
 * @param locations 当前字母分组下的地点列表
 */
data class RegionGroupedLocation(
    /** 分组字母 */
    val letter: String? = null,
    /** 当前字母分组下的地点列表 */
    val locations: List<RegionLocation>? = null,
)
