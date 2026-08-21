package com.shiyuan.fastclaw.feature.message.data

import com.shiyuan.fastclaw.feature.message.MessageFormatter.customPayload
import com.tulele.im.custom.CustomAttachment
import com.tulele.im.entity.MessageWrapper
import org.json.JSONObject

// 职位卡片的 IM 自定义附件，按跨端协议输出和解析 attachment.raw。
class MessageJobAttachment : CustomAttachment(MSG_TYPE) {

    // 协议版本号。
    var protocolVersion: Int? = null

    // 协议消息类型。
    var protocolType: String? = null

    // 职位卡片业务幂等 ID。
    var cardId: String? = null

    // 职位业务 ID。
    var jobId: String? = null

    // 职位名称快照。
    var jobName: String? = null

    // 薪资展示快照。
    var salaryText: String? = null

    // 品牌业务 ID。
    var brandId: String? = null

    // 品牌名称。
    var brandName: String? = null

    // 公司业务 ID。
    var companyId: String? = null

    // 公司名称快照。
    var companyName: String? = null

    // 城市编码。
    var cityCode: String? = null

    // 城市名称。
    var cityName: String? = null

    // 经验枚举值。
    var experienceCode: String? = null

    // 经验展示文案。
    var experienceText: String? = null

    // 学历枚举值。
    var educationCode: String? = null

    // 学历展示文案。
    var educationText: String? = null

    // 是否为更换后的沟通职位。
    var isJobChanged: Boolean = false

    /**
     * 返回通知栏和会话摘要降级文案。
     */
    override fun getPushContent(): CharSequence = jobName.orEmpty()

    /**
     * 序列化成协议约定的 attachment.raw JSON。
     */
    override fun toJson(send: Boolean): String {
        val job = JSONObject()
            .put(KEY_JOB_ID, jobId)
            .put(KEY_JOB_NAME, jobName)
            .put(KEY_SALARY_TEXT, salaryText)
            .put(KEY_BRAND_ID, brandId)
            .put(KEY_BRAND_NAME, brandName)
            .put(KEY_COMPANY_ID, companyId)
            .put(KEY_COMPANY_NAME, companyName)
            .put(KEY_CITY_CODE, cityCode)
            .put(KEY_CITY_NAME, cityName)
            .put(KEY_EXPERIENCE_CODE, experienceCode)
            .put(KEY_EXPERIENCE_TEXT, experienceText)
            .put(KEY_EDUCATION_CODE, educationCode)
            .put(KEY_EDUCATION_TEXT, educationText)
            .put(KEY_IS_JOB_CHANGED, isJobChanged)
        return JSONObject()
            .put(KEY_VERSION, protocolVersion ?: PROTOCOL_VERSION)
            .put(KEY_TYPE, TYPE_JOB_CARD)
            .put(KEY_CARD_ID, cardId)
            .put(KEY_JOB, job)
            .toString()
    }

    companion object {
        // 职位卡片自定义消息 subtype。
        const val MSG_TYPE = 1003

        private const val PROTOCOL_VERSION = 1
        private const val TYPE_JOB_CARD = "jobCard"
        private const val KEY_VERSION = "v"
        private const val KEY_TYPE = "type"
        private const val KEY_CARD_ID = "cardId"
        private const val KEY_JOB = "job"
        private const val KEY_JOB_ID = "jobId"
        private const val KEY_JOB_NAME = "jobName"
        private const val KEY_SALARY_TEXT = "salaryText"
        private const val KEY_BRAND_ID = "brandId"
        private const val KEY_BRAND_NAME = "brandName"
        private const val KEY_COMPANY_ID = "companyId"
        private const val KEY_COMPANY_NAME = "companyName"
        private const val KEY_CITY_CODE = "cityCode"
        private const val KEY_CITY_NAME = "cityName"
        private const val KEY_EXPERIENCE_CODE = "experienceCode"
        private const val KEY_EXPERIENCE_TEXT = "experienceText"
        private const val KEY_EDUCATION_CODE = "educationCode"
        private const val KEY_EDUCATION_TEXT = "educationText"
        private const val KEY_IS_JOB_CHANGED = "isJobChanged"

        /**
         * 将职位卡片消息转换成聊天列表展示模型。
         */
        fun buildChat(message: MessageWrapper, timeText: String): JobChatItem {
            val payload = message.customPayload()
            val job = payload?.optJSONObject(KEY_JOB)
            return JobChatItem(
                id = message.messageClientId,
                protocolVersion = payload?.optInt(KEY_VERSION),
                protocolType = payload?.optString(KEY_TYPE),
                cardId = payload?.optString(KEY_CARD_ID),
                jobId = job?.optString(KEY_JOB_ID),
                jobName = job?.optString(KEY_JOB_NAME),
                salaryText = job?.optString(KEY_SALARY_TEXT),
                brandId = job?.optString(KEY_BRAND_ID),
                brandName = job?.optString(KEY_BRAND_NAME),
                companyId = job?.optString(KEY_COMPANY_ID),
                companyName = job?.optString(KEY_COMPANY_NAME),
                cityCode = job?.optString(KEY_CITY_CODE),
                cityName = job?.optString(KEY_CITY_NAME),
                experienceCode = job?.optString(KEY_EXPERIENCE_CODE),
                experienceText = job?.optString(KEY_EXPERIENCE_TEXT),
                educationCode = job?.optString(KEY_EDUCATION_CODE),
                educationText = job?.optString(KEY_EDUCATION_TEXT),
                isJobChanged = job?.optBoolean(KEY_IS_JOB_CHANGED) == true,
                time = message.time,
                timeText = timeText,
            )
        }
    }
}
