package com.shiyuan.fastclaw.core.network.apis.response

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue

/**
 * 查询属性数组候选值响应数据（求职 Agent）
 *
 * @param sessionId            候选人求职 Agent 会话 ID
 * @param conversationId       对话 ID
 * @param questionQueueId      问题队列 ID
 * @param attributeGroupCode   属性组编码
 * @param attributeGroupName   属性组名称
 * @param metaCode             元数据编码
 * @param inputMode            输入方式
 * @param dictType             字典类型
 * @param dictApi              字典接口
 * @param value                字段原始值
 * @param displayValue         字段展示值
 * @param options              字段选项
 * @param skippable           是否可跳过
 * @param complete            是否已完成
 * @param missingMetaCodes     缺失的元数据编码列表
 * @param fieldTree           字段树结构
 */
@Parcelize
data class AttributeArrayQueryData(
    val sessionId: String? = null,
    var clientRequestId: String? = null,
    val conversationId: String? = null,
    val questionQueueId: String? = null,
    val attributeGroupCode: String? = null,
    val attributeGroupName: String? = null,
    val metaCode: String? = null,
    val inputMode: String? = null,
    val dictType: String? = null,
    val dictApi: String? = null,
    val value: @RawValue Any? = null,
    val displayValue: String? = null,
    val options: List<AttributeOption>? = null,
    val skippable: Boolean? = null,
    val complete: Boolean? = null,
    var draft: Boolean? = false,
    var passable: Boolean? = false,
    val missingMetaCodes: MutableList<String>? = null,
    var fieldTree: MutableList<AttributeFieldTree>? = null,
) : Parcelable

/**
 * 属性字段树（工作经历/教育经历等实例容器）
 *
 * @param instanceIndex   实例索引
 * @param fieldCode       字段编码（顶层为实例类型编码）
 * @param fieldName       字段名称（顶层为实例类型名称）
 * @param fieldValue      字段值（顶层一般为空）
 * @param displayValue    显示值
 * @param inputMode       输入模式
 * @param dictType        字典类型
 * @param dictApi         字典接口
 * @param required        是否必填
 * @param missing          是否缺失
 * @param questionOptionsJson 问题选项 JSON
 * @param options         选项列表
 * @param validationRuleJson 验证规则 JSON
 * @param sortOrder       排序
 * @param complete        是否已完成
 * @param statusCode      状态码
 * @param statusText      状态文本
 * @param missingMetaCodes 缺失的元数据编码列表
 * @param fieldItems      子字段项列表（具体字段）
 */
@Parcelize
data class AttributeFieldTree(
    var instanceIndex: Long? = null,
    val fieldCode: String? = null,
    val fieldName: String? = null,
    val fieldValue:@RawValue Any? = null,
    val displayValue: String? = null,
    val inputMode: String? = null,
    val dictType: String? = null,
    val dictApi: String? = null,
    val required: Boolean? = null,
    val missing: Boolean? = null,
    var internship: Boolean? = null,
    var attributeGroupCode: String? = null,
    var operation: String? = null,
    val questionOptionsJson: String? = null,
    val options: List<AttributeOption>? = null,
    val validationRuleJson: String? = null,
    val sortOrder: Long? = null,
    val complete: Boolean? = null,
    val statusCode: String? = null,
    val statusText: String? = null,
    val missingMetaCodes: List<String>? = null,
    var fieldItems: List<AttributeFieldItem>? = null,
) : Parcelable

/**
 * 属性字段值
 *
 * @param value        字段值（JSON 字符串）
 * @param displayText  显示文本
 */
@Parcelize
data class AttributeFieldValue(
    val value: String? = null,
    val displayText: String? = null,
) : Parcelable

/**
 * 属性选项
 *
 * @param code      选项编码
 * @param name      选项名称
 * @param enabled   是否启用
 * @param sortOrder 排序
 */
@Parcelize
data class AttributeOption(
    val code: String? = null,
    val name: String? = null,
    val enabled: Boolean? = null,
    val sortOrder: Long? = null,
) : Parcelable



@Parcelize
data class AttributeSave(
    val sessionId: String? = null,
    val conversationId: String? = null,
    val attributeStatus: String? = null,
    val draft: String? = null,
    val nextStepType: String? = null,
    val requestId: String? = null,
) : Parcelable


/**
 * 子字段项（具体字段）
 *
 * @param fieldCode           字段编码
 * @param fieldName           字段名称
 * @param fieldValue          字段值
 * @param displayValue        显示值
 * @param inputMode           输入模式（TEXT / DICT_SINGLE / DATE_MONTH / SINGLE_CHOICE 等）
 * @param dictType            字典类型
 * @param dictApi             字典接口
 * @param required            是否必填
 * @param missing             是否缺失
 * @param questionOptionsJson 问题选项 JSON
 * @param options             选项列表
 * @param validationRuleJson  验证规则 JSON
 * @param sortOrder           排序
 * @param complete            是否已完成
 * @param statusCode          状态码
 * @param statusText          状态文本
 * @param missingMetaCodes    缺失的元数据编码列表
 * @param fieldItems          子字段项列表
 */
@Parcelize
data class AttributeFieldItem(
    val fieldCode: String? = null,
    var fieldName: String? = null,
    var fieldValue: @RawValue Any? = null,
    var displayValue: String? = null,
    val inputMode: String? = null,
    val dictType: String? = null,
    val dictApi: String? = null,
    val required: Boolean? = null,
    val missing: Boolean? = null,
    val questionOptionsJson: String? = null,
    val options: List<AttributeOption>? = null,
    val validationRuleJson: String? = null,
    val sortOrder: Long? = null,
    val complete: Boolean? = null,
    val statusCode: String? = null,
    val statusText: String? = null,
    var missingMetaCodes: List<String>? = null,
    var fieldItems: List<AttributeFieldItem>? = null,
) : Parcelable


object AttributeFieldItemFinder {

    /**
     * 在 [AttributeFieldTree] 中查找第一个匹配 fieldCode 的 [AttributeFieldItem]
     *
     * @param root      根节点（属性字段树）
     * @param fieldCode 要查找的字段编码（区分大小写，非空）
     * @return 匹配的第一个 [AttributeFieldItem]，未找到返回 null
     */
    fun findFirst(root: AttributeFieldTree?, fieldCode: String): AttributeFieldItem? {
        return root?.fieldItems?.let { findFirst(it, fieldCode) }
    }

    /**
     * 在 [AttributeFieldItem] 列表中查找第一个匹配 fieldCode 的 [AttributeFieldItem]（递归搜索）
     *
     * @param items     待搜索的 [AttributeFieldItem] 列表
     * @param fieldCode 要查找的字段编码（区分大小写，非空）
     * @return 匹配的第一个 [AttributeFieldItem]，未找到返回 null
     */
    fun findFirst(items: List<AttributeFieldItem>?, fieldCode: String): AttributeFieldItem? {
        if (items.isNullOrEmpty() || fieldCode.isEmpty()) return null

        for (item in items) {
            // 当前节点匹配
            if (item.fieldCode == fieldCode) {
                return item
            }
            // 递归搜索子节点
            val foundInChildren = findFirst(item.fieldItems, fieldCode)
            if (foundInChildren != null) {
                return foundInChildren
            }
        }
        return null
    }

    /**
     * 在 [AttributeFieldTree] 中查找所有匹配 fieldCode 的 [AttributeFieldItem]（支持重复 fieldCode）
     *
     * @param root      根节点（属性字段树）
     * @param fieldCode 要查找的字段编码（区分大小写，非空）
     * @return 匹配的 [AttributeFieldItem] 列表（顺序为深度优先遍历顺序）
     */
    fun findAll(root: AttributeFieldTree?, fieldCode: String): List<AttributeFieldItem> {
        return root?.fieldItems?.let { findAll(it, fieldCode) } ?: emptyList()
    }

    /**
     * 在 [AttributeFieldItem] 列表中查找所有匹配 fieldCode 的 [AttributeFieldItem]（递归搜索）
     *
     * @param items     待搜索的 [AttributeFieldItem] 列表
     * @param fieldCode 要查找的字段编码（区分大小写，非空）
     * @return 匹配的 [AttributeFieldItem] 列表（顺序为深度优先遍历顺序）
     */
    fun findAll(items: List<AttributeFieldItem>?, fieldCode: String): List<AttributeFieldItem> {
        if (items.isNullOrEmpty() || fieldCode.isEmpty()) return emptyList()

        val result = mutableListOf<AttributeFieldItem>()
        for (item in items) {
            if (item.fieldCode == fieldCode) {
                result.add(item)
            }
            result.addAll(findAll(item.fieldItems, fieldCode))
        }
        return result
    }
}

object AttributeDataHelper {

    /**
     * 更新或添加指定实例中的某个字段项。
     * @param attributeData 原始数据（可为 null）
     * @param instanceIndex 目标实例的索引
     * @param fieldCode 要更新的字段编码
     * @param newItemProvider 根据原有字段项（可能为 null）生成新字段项的函数
     * @return 更新后的 AttributeArrayQueryData，如果输入为 null 或未找到实例则返回原值
     */
    fun updateOrAddFieldItem(
        attributeData: AttributeArrayQueryData?,
        instanceIndex: Long,
        fieldCode: String,
        newItemProvider: (oldItem: AttributeFieldItem?) -> AttributeFieldItem
    ): AttributeArrayQueryData? {
        val tree = attributeData?.fieldTree ?: return attributeData
        val targetInstance = tree.find { it.instanceIndex == instanceIndex } ?: return attributeData

        val currentItems = targetInstance.fieldItems?.toMutableList() ?: mutableListOf()
        val index = currentItems.indexOfFirst { it.fieldCode == fieldCode }
        val oldItem = if (index >= 0) currentItems[index] else null
        val newItem = newItemProvider(oldItem)

        if (index >= 0) {
            currentItems[index] = newItem
        } else {
            currentItems.add(newItem)
        }
        targetInstance.fieldItems = currentItems
        return attributeData
    }

    /**
     * 简化版：直接替换或添加字段项。
     * @param attributeData 原始数据
     * @param instanceIndex 目标实例索引
     * @param fieldCode 字段编码
     * @param fieldName 字段名称
     * @param fieldValue 字段值（Map 或任意类型，需匹配 AttributeFieldItem 构造函数）
     * @param displayValue 显示值
     */
    @JvmOverloads
    fun updateFieldItemSimple(
        attributeData: AttributeArrayQueryData?,
        instanceIndex: Long,
        fieldCode: String,
        fieldName: String,
        fieldValue: Map<String, String?>,
        displayValue: String
    ): AttributeArrayQueryData? {
        return updateOrAddFieldItem(attributeData, instanceIndex, fieldCode) { oldItem ->
            AttributeFieldItem(
                fieldCode = fieldCode,
                fieldName = fieldName,
                fieldValue = fieldValue,
                displayValue = displayValue
            )
        }
    }

    /**
     * 直接替换或添加一个字段项到指定的实例中。
     * @param attributeData 原始数据
     * @param instanceIndex 目标实例的索引
     * @param newItem 新的字段项（fieldCode 将作为替换或添加的依据）
     * @return 更新后的 AttributeArrayQueryData，如果未找到实例或输入为 null 则返回原值
     */
    fun updateFieldItemDirect(
        attributeData: AttributeArrayQueryData?,
        instanceIndex: Long,
        newItem: AttributeFieldItem
    ): AttributeArrayQueryData? {
        val targetInstance = attributeData?.fieldTree?.find { it.instanceIndex == instanceIndex }
            ?: return attributeData

        val currentItems = targetInstance.fieldItems?.toMutableList() ?: mutableListOf()
        val index = currentItems.indexOfFirst { it.fieldCode == newItem.fieldCode }
        if (index >= 0) {
            currentItems[index] = newItem
        } else {
            currentItems.add(newItem)
        }
        targetInstance.fieldItems = currentItems
        return attributeData
    }

}


