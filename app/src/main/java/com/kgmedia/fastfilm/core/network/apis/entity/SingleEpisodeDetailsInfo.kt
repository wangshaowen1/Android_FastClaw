package com.kgmedia.fastfilm.core.network.apis.entity

/**
 * *****************************************
 * 项目名称: FastFilm
 * @Author: SZYMN
 * @Introduce:查看作品单个剧集详情
 * @Data: 2026/2/7/周六  16:33
 * @Version: 1.0
 * *****************************************
 */
data class SingleEpisodeDetailsInfo(
    //剧集生成任务结果
    val taskGenResultStoryVO: TaskGenResultStoryVO,
    //查询单个是直接返回结果,比如资产、分镜图片url等
    //剧集id
    val storyId: String? = null,
    //剧集标题
    val storyTitle: String? = null,
    //封面图URL
    val workCoverUrl: String? = null,
    //剧集梗概
    val storySummary: String? = null,
    //剧集剧本内容
    val storyContent: String? = null,
    //序号
    val sortNo: Int? = null,
    //[枚举]创作流程进度 [10=剧本输入 15=剧本生成中 20=资产生成中 30=分镜生成中 32=分镜视频生成中 35=待合成 40=已完成]
    val stepProgress: Int? = null,
    //[枚举]创作流程状态 [0=草稿 1=生成中 2=已完成 3=生成失败]
    val stepStatus: Int? = null,
    //分镜数
    val storyboardTotal: Int? = null,
    //当前视频url
    val videoUrl: String? = null,
    //视频时长(毫秒)
    val duration: Int? = null,
    //视频比例 16:9、4:3、1:1、3:4、9:16、21:9、adaptive(自动)
    val workRatio: String? = null,
    //播放量(初始化1000-10000)
    val playCount: Int? = null
) {
    data class TaskGenResultStoryVO(
        //业务类型 40=生成所有资产描述 70=生成所有分镜描述
        val bizType: Int? = null,
        //任务id
        val taskId: String? = null,
        //[枚举]生成状态 [0=待生成 1=生成中 2=成功 3=失败]
        val genStatus: Int? = null,
        //查询单个是直接返回结果,比如资产、分镜图片url等
        val result: String? = null
    )
}