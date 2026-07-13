package com.shiyuan.fastclaw.core.network.apis.response

/**
 * 线上简历单个普通属性
 *
 * @param attributeGroupCode 属性分组编码
 * @param attributeGroupName 属性分组名称
 * @param metaCode 元属性编码
 * @param inputMode 输入模式
 * @param dictType 字典类型
 * @param dictApi 字典查询接口
 * @param value 字段业务值
 * @param displayValue 字段展示值
 * @param options 字段选项
 * @param complete 字段当前是否满足必填完整性
 * @param skippable 字段是否允许跳过整个属性分组
 * @param missingMetaCodes 缺失元属性编码列表
 * @param passable 字段当前是否允许继续
 */
data class OnlineResumeAttributeData(
    val attributeGroupCode: String? = null,
    val attributeGroupName: String? = null,
    val metaCode: String? = null,
    val inputMode: String? = null,
    val dictType: String? = null,
    val dictApi: String? = null,
    val value: Any? = null,
    val displayValue: String? = null,
    val options: List<OnlineResumeAttributeOption>? = null,
    val complete: Boolean? = null,
    val skippable: Boolean? = null,
    val missingMetaCodes: List<String>? = null,
    val passable: Boolean? = null,
)

/**
 * 线上简历普通属性选项
 *
 * @param code 选项编码
 * @param name 选项展示名称
 * @param enabled 是否启用
 * @param sortOrder 展示排序
 */
data class OnlineResumeAttributeOption(
    val code: String? = null,
    val name: String? = null,
    val enabled: Boolean? = null,
    val sortOrder: Long? = null,
)
