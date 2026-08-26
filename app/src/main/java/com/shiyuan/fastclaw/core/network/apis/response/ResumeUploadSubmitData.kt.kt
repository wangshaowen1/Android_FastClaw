package com.shiyuan.fastclaw.core.network.apis.response

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * 提交简历上传结果响应数据
 *
 * @createtime 2026年05月25日
 * @author TooWhiteT
 */
data class ResumeUploadSubmitData(
    /** 服务端是否接收本次简历动作：true 已接收并进入处理 / false 未接收或被业务校验拒绝 */
    val accepted: Boolean? = null,

    /** 简历 ID */
    val resumeId: String? = null,

    /** 简历解析任务 ID */
    val resumeTaskId: String? = null,

    /** 处理后的最新消息游标 */
    val latestCursor: String? = null,
)



/**
 * 提交简历上传我的
 *
 */
data class ResumeUploadSubmitMyData(
    /** 服务端是否接收本次简历动作：true 已接收并进入处理 / false 未接收或被业务校验拒绝 */
    val accepted: Boolean? = null,
)



/**
 * 简历列表
 *
 */
@Parcelize
data class ResumeListData(
    /** 简历业务 ID，端侧后续确认动作优先回传该字段。 */
    val resumeId: String? = null,
    /** 简历来源类型，我的简历列表固定为 MY_RESUME。 */
    val resumeSourceType: String? = null,
    /** 来源业务 ID，我的简历列表中等同 resumeTaskId。 */
    val sourceBizId: String? = null,
    /** 文件安全模块文件记录业务 ID。 */
    val fileRecordId: String? = null,
    /** 简历业务 ID 兼容字段，等同 resumeId。 */
    val resumeBizId: String? = null,
    /** 简历结构化内部记录 ID，解析成功后才会存在。 */
    val resumeStructId: String? = null,
    /** 简历解析任务业务 ID，解析状态查询和问题定位优先使用。 */
    val resumeTaskId: String? = null,
    /** 简历解析任务业务 ID 兼容字段，等同 resumeTaskId。 */
    val resumeTaskBizId: String? = null,
    /** 简历展示名称。 */
    val resumeName: String? = null,
    /** 简历轻量摘要，不包含全文、联系方式和文件地址。 */
    val resumeSummary: String? = null,
    /** 上传文件名。 */
    val fileName: String? = null,
    /** 简历临时下载或预览地址。 */
    val resumeUrl: String? = null,
    /** 上传文件类型，支持 pdf、doc、docx、txt、jpg、jpeg、png、webp、bmp。 */
    val fileType: String? = null,
    /** 上传文件大小，单位 byte，历史数据可能为空。 */
    val fileSize: Long? = null,
    /** 解析状态。 */
    val parseStatus: Int? = null,
    /** 解析状态文案。 */
    val parseStatusText: String? = null,
    /** 是否可作为历史简历确认进入 Agent 状态机。 */
    val canConfirm: Boolean? = null,
    /** 是否可重新上传或重试。 */
    val canRetry: Boolean? = null,
    /** 是否可从我的简历页重新上传文件。 */
    val canReupload: Boolean? = null,
    /** 上传时间。 */
    val uploadTime: String? = null,
    /** 创建时间。 */
    val createTime: String? = null,
    /** 更新时间。 */
    val updateTime: String? = null,
) : Parcelable
