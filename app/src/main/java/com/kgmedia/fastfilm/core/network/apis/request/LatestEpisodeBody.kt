package com.kgmedia.fastfilm.core.network.apis.request

/**
 * *****************************************
 * 项目名称: FastFilm
 * @Author: SZYMN
 * @Introduce:续创时查询上一集内容详情（获取最近一集的内容）
 * @Data: 2026/2/6/周五  10:44
 * @Version: 1.0
 * *****************************************
 */
data class LatestEpisodeBody(
    //作品类型：1-灵感创作 2-小说改编
    val workType: Int? = null,
    //剧集 id 默认找后台确认了，如果我不传，后端会自动去找最新的一集的id填充 2023.2.6 10:51:27
    val storyId: String? = null,
    //作品 ID
    val workId: String? = null
) : UserTokenBody() {
    companion object {
        //1-灵感创作
        const val WORK_TYPE_INSPIRATION = 1

        //2-小说改编
        const val WORK_TYPE_NOVEL = 2
    }
}