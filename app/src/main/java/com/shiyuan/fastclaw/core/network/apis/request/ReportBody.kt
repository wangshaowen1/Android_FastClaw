package com.shiyuan.fastclaw.core.network.apis.request

class ResumeMaterialQualityBody(
    val resumeFileUrl:String? = null,
    val resumeFileName:String? = null,
    val resumeFileType:String? = null,
    val resumeFileSize:String? = null,
    val checkId:String? = null,
)


class JdMaterialQualityBody(
    val jdInputType:String? = null,
    val jdText:String? = null,
    val jdImageUrls:List<String>? = null,
    val checkId:String? = null,
)


class InterviewReportData(
    val resumeCheckId:String? = null,
    val jdCheckId:String? = null,
    val reportType:String? = null,
    val reportId:String? = null,
)




