package com.shiyuan.fastclaw.core.network.apis.request

/**
 * 简历素材质量校验请求体
 *
 * @param resumeFileUrl 简历文件地址
 * @param resumeFileName 简历文件名
 * @param resumeFileType 简历文件类型
 * @param resumeFileSize 简历文件大小
 * @param checkId 校验 ID
 */
class ResumeMaterialQualityBody(
    /** 简历文件地址 */
    val resumeFileUrl:String? = null,
    /** 简历文件名 */
    val resumeFileName:String? = null,
    /** 简历文件类型 */
    val resumeFileType:String? = null,
    /** 简历文件大小 */
    val resumeFileSize:String? = null,
    /** 校验 ID */
    val checkId:String? = null,
)


/**
 * JD 素材质量校验请求体
 *
 * @param jdInputType JD 输入类型
 * @param jdText JD 文本
 * @param jdImageUrls JD 图片列表
 * @param checkId 校验 ID
 */
class JdMaterialQualityBody(
    /** JD 输入类型 */
    val jdInputType:String? = null,
    /** JD 文本 */
    val jdText:String? = null,
    /** JD 图片列表 */
    val jdImageUrls:List<String>? = null,
    /** 校验 ID */
    val checkId:String? = null,
)


/**
 * 面试报告数据
 *
 * @param resumeCheckId 简历校验 ID
 * @param jdCheckId JD 校验 ID
 * @param reportType 报告类型
 * @param reportId 报告 ID
 */
class InterviewReportData(
    /** 简历校验 ID */
    val resumeCheckId:String? = null,
    /** JD 校验 ID */
    val jdCheckId:String? = null,
    /** 报告类型 */
    val reportType:String? = null,
    /** 报告 ID */
    val reportId:String? = null,
)

/**
 * 从“我的报告”列表隐藏报告
 *
 * @param reportType 报告类型
 * @param reportId 我的报告列表项 ID
 */
class InterviewReportDeleteBody(
    /** 报告类型 */
    val reportType: String,
    /** 我的报告列表项 ID */
    val reportId: String,
)



