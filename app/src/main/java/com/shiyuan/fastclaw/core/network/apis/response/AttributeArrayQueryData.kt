package com.shiyuan.fastclaw.core.network.apis.response

/**
 * 查询属性数组候选值响应数据（求职 Agent）
 *
 * @param sessionId          候选人求职 Agent 会话 ID
 * @param conversationId     对话 ID
 * @param questionQueueId   问题队列 ID
 * @param attributeGroupCode 属性组编码
 * @param attributeGroupName 属性组名称
 * @param skippable          是否可跳过
 * @param complete           是否已完成
 * @param missingMetaCodes   缺失的元数据编码列表
 * @param fieldTree          字段树结构
 */
data class AttributeArrayQueryData(
    val sessionId: String? = null,
    val conversationId: String? = null,
    val questionQueueId: String? = null,
    val attributeGroupCode: String? = null,
    val attributeGroupName: String? = null,
    val skippable: Boolean? = null,
    val complete: Boolean? = null,
    val missingMetaCodes: List<String>? = null,
    val fieldTree: List<AttributeFieldTree>? = null,
)

/**
 * 属性字段树
 *
 * @param fieldCode           字段编码
 * @param fieldName           字段名称
 * @param fieldValue          字段值
 * @param displayValue        显示值
 * @param inputMode           输入模式
 * @param dictType            字典类型
 * @param dictApi             字典接口
 * @param required            是否必填
 * @param missing             是否缺失
 * @param questionOptionsJson 问题选项 JSON
 * @param options             选项列表
 * @param validationRuleJson  验证规则 JSON
 * @param sortOrder           排序
 * @param instanceIndex       实例索引
 * @param complete            是否已完成
 * @param statusCode          状态码
 * @param statusText          状态文本
 * @param missingMetaCodes    缺失的元数据编码列表
 * @param fieldItems          子字段项列表
 */
data class AttributeFieldTree(
    val fieldCode: String? = null,
    val fieldName: String? = null,
    val fieldValue: AttributeFieldValue? = null,
    val displayValue: String? = null,
    val inputMode: String? = null,
    val dictType: String? = null,
    val dictApi: String? = null,
    val required: Boolean? = null,
    val missing: Boolean? = null,
    val questionOptionsJson: String? = null,
    val options: List<AttributeOption>? = null,
    val validationRuleJson: String? = null,
    val sortOrder: Long? = null,
    val instanceIndex: Long? = null,
    val complete: Boolean? = null,
    val statusCode: String? = null,
    val statusText: String? = null,
    val missingMetaCodes: List<String>? = null,
    val fieldItems: List<AttributeFieldItem>? = null,
)

/**
 * 属性字段值
 *
 * @param value       字段值（对象类型）
 * @param displayText 显示文本
 */
data class AttributeFieldValue(
    val value: Map<String, Any>? = null,
    val displayText: String? = null,
)

/**
 * 属性选项
 *
 * @param code      选项编码
 * @param name      选项名称
 * @param enabled   是否启用
 * @param sortOrder 排序
 */
data class AttributeOption(
    val code: String? = null,
    val name: String? = null,
    val enabled: Boolean? = null,
    val sortOrder: Long? = null,
)

/**
 * 子字段项
 *
 * @param fieldCode           字段编码
 * @param fieldName           字段名称
 * @param fieldValue          字段值
 * @param displayValue        显示值
 * @param inputMode           输入模式
 * @param dictType            字典类型
 * @param dictApi             字典接口
 * @param required            是否必填
 * @param missing             是否缺失
 * @param questionOptionsJson 问题选项 JSON
 * @param options             选项列表
 * @param validationRuleJson  验证规则 JSON
 * @param sortOrder           排序
 * @param fieldItems          子字段项列表
 */
data class AttributeFieldItem(
    val fieldCode: String? = null,
    val fieldName: String? = null,
    val fieldValue: AttributeFieldValue? = null,
    val displayValue: String? = null,
    val inputMode: String? = null,
    val dictType: String? = null,
    val dictApi: String? = null,
    val required: Boolean? = null,
    val missing: Boolean? = null,
    val questionOptionsJson: String? = null,
    val options: List<AttributeOption>? = null,
    val validationRuleJson: String? = null,
    val sortOrder: Long? = null,
    val fieldItems: List<AttributeFieldItem>? = null,
)
