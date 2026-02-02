package com.kgmedia.fastfilm.core.network.apis.response

/**
"id": "作品ID",
"workTitle": "作品名称",
"workDesc": "作品简介",
"workStyleCode": "作品风格code",
"workRatio": "视频比例：16:9、4:3、1:1、3:4、9:16、21:9、adaptive(自动)",
"workInspiration": "作品灵感",
"workCoverUrl": "作品封面图URL（AI生成或默认图）",
"workType": "作品类型：1-灵感创作 2-小说改编",
"workStatus": "作品状态：草稿（30=资产生成中） 生成中（40=分镜生成中  42=分镜视频生成中 50=合成中） 60=已完成 70=失败",
"totalEpisodeCnt": "总集数（规划集数）",
"finishedEpisodeCnt": "已完成集数",
"lastEpisodeNo": "最近生成的集数编号",
"lastGenerateTime": "最近一次生成时间",
"playCount": "播放量(初始化1000-10000)"
}
}
 */
data class LastEpisodeData(
    val finishedEpisodeCnt: Int? = null,
    val id: String? = null,
    val lastEpisodeNo: Int? = null,
    val lastGenerateTime: Long? = null,
    val playCount: Int? = null,
    val totalEpisodeCnt: Int? = null,
    val workCoverUrl: String? = null,
    val workDesc: String? = null,
    val workInspiration: String? = null,
    val workRatio: String? = null,
    val workStatus: Int? = null,
    val workStyleCode: String? = null,
    val workTitle: String? = null,
    val workType: Int? = null
)