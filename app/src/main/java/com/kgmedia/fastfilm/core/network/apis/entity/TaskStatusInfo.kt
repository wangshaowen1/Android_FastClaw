package com.kgmedia.fastfilm.core.network.apis.entity

data class TaskStatusInfo(
    /**
     * 任务id
     */
    val taskId: String?,
    /**
     * [枚举]生成状态 [0=待生成 1=生成中 2=成功 3=失败]
     */
    val genStatus: Int?,
    /**
     * 查询单个是直接返回结果,比如资产、分镜图片url等
     */
    val result: String?
)