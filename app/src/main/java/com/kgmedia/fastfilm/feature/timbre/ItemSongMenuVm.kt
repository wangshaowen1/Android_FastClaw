package com.kgmedia.fastfilm.feature.timbre

import com.kgmedia.fastfilm.base.item.AppItemViewModel

/**
 * *****************************************
 * 项目名称: KidsTales
 * @Author: SZYMN
 * @Introduce:歌曲更多菜单
 * @Data: 2025/4/30  13:51
 * @Version: 1.0
 * *****************************************
 */
class ItemSongMenuVm(data: SongMenuData?) : AppItemViewModel<SongMenuData>(data)


data class SongMenuData(
    val title: String,
    val icon: Int,
    val type: SongMenuEnum,
)

enum class SongMenuEnum {
    SHARE,//分享歌曲
    MODIFY_NAME,//修改名称
    DELETE,//删除歌曲
}