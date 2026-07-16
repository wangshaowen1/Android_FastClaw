package com.shiyuan.fastclaw.core.network.apis.response

/**
 * 文件上传凭证。
 *
 * @param fileRecordId 文件记录业务 ID
 * @param bucket OSS Bucket 名称
 * @param objectKey OSS 对象 Key
 * @param fileName 原请求文件名称
 * @param fileType 原请求文件类型
 * @param fileSize 原请求文件大小，单位字节
 * @param uploadUrl OSS 预签名 PUT 上传地址
 * @param headers 上传时必须携带的请求头
 */
data class FileUploadCredentialData(
    val fileRecordId: String? = null,
    val bucket: String? = null,
    val objectKey: String? = null,
    val fileName: String? = null,
    val fileType: String? = null,
    val fileSize: Long? = null,
    val uploadUrl: String? = null,
    val headers: Map<String, String>? = null,
)

/**
 * 文件上传及安全审核状态。
 *
 * @param fileRecordId 文件记录业务 ID
 * @param uploadStatus 上传状态，对应 FileUploadStatusEnum
 * @param securityStatus 安全状态，30 为通过，40/50 为失败终态
 * @param failReason 可安全展示给用户的审核失败原因
 */
data class FileSecurityStatusData(
    val fileRecordId: String? = null,
    val uploadStatus: Int? = null,
    val securityStatus: Int? = null,
    val failReason: String? = null,
)
