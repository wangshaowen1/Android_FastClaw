package com.shiyuan.fastclaw.feature.clawagent.bean

/**
 * 动作枚举, 核心问题卡片有部分ActionCode使用是从payloadjson的option中取
 * @createtime 2026年05月27日 11:43
 * @author TooWhiteT
 */

enum class CandidateJobAgentClientActionEnum {
    /**
     * 简历已上传到 OSS，提交文件地址并触发解析
     */
    RESUME_UPLOAD_SUBMIT,

    /**
     * 点击上传简历入口
     */
    RESUME_UPLOAD_CLICK,

    /**
     * 确认历史简历
     */
    RESUME_CONFIRM_YES,

    /**
     * 否认历史简历
     */
    RESUME_CONFIRM_NO,

    /**
     * 上传失败后重新上传
     */
    RESUME_REUPLOAD_CLICK,

    /**
     * 记忆卡片 确认
     */
    ATTRIBUTE_CONFIRM,

    /**
     * 记忆卡片 拒绝
     */
    ATTRIBUTE_REJECT
}