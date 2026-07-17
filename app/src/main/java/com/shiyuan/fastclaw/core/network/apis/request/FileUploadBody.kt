package com.shiyuan.fastclaw.core.network.apis.request

/**
 * 创建上传凭证请求体。
 *
 * @param files 待创建上传凭证的文件列表
 */
data class CreateFileUploadCredentialBody(
    val files: List<FileUploadCredentialItemBody>,
)

/**
 * 待创建上传凭证的文件。
 *
 * @param bizType 文件上传业务类型
 * @param fileName 文件名称
 * @param fileType 文件扩展名，例如 pdf、docx、jpg
 * @param fileSize 文件大小，单位字节
 */
data class FileUploadCredentialItemBody(
    val bizType: String,
    val fileName: String,
    val fileType: String,
    val fileSize: Long,
) {
    companion object {
        /** 首页小虾对话上传简历 */
        const val BIZ_TYPE_XIAOXIA_RESUME_UPLOAD = "XIAOXIA_RESUME_UPLOAD"

        /** 在线简历上传简历 */
        const val BIZ_TYPE_CANDIDATE_ONLINE_RESUME_A = "CANDIDATE_ONLINE_RESUME_A"

        /** 在线简历上传头像 */
        const val BIZ_TYPE_CANDIDATE_AVATAR_UPLOAD = "CANDIDATE_AVATAR_UPLOAD"

        /** 模拟面试上传 JD */
        const val BIZ_TYPE_MOCK_INTERVIEW_JD_UPLOAD = "MOCK_INTERVIEW_JD_UPLOAD"
    }
}

/**
 * 确认上传完成请求体。
 *
 * @param files 已上传文件列表
 */
data class CompleteFileUploadBody(
    val files: List<CompleteFileUploadItemBody>,
)

/**
 * 已上传文件。
 *
 * @param fileRecordId 文件记录业务 ID
 * @param etag OSS ETag，可选
 */
data class CompleteFileUploadItemBody(
    val fileRecordId: String,
    val etag: String? = null,
)

/**
 * 查询文件安全状态请求体。
 *
 * @param fileRecordIds 文件记录业务 ID 列表
 */
data class FileSecurityStatusQueryBody(
    val fileRecordIds: List<String>,
)
