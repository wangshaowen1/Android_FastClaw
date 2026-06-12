package com.shiyuan.fastclaw.core.network.apis.response

import android.os.Parcelable
import com.shiyuan.fastclaw.core.network.apis.entity.SimpleMsgAgent
import com.shiyuan.fastclaw.core.network.apis.request.JobCityQueryBody
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlin.collections.plus

@Serializable
data class CommonEnumItem(
    val code: String? = null,
    val name: String? = null,
    val fullNamePath: String? = null,
    val parentCode: String? = null,
    val level: Int? = null,
    val enabled: Boolean? = null,
    val sortOrder: Int? = null,
    val firstCategoryCode: String? = null,
    val firstCategoryName: String? = null,
    val secondCategoryCode: String? = null,
    val secondCategoryName: String? = null,
    val positionName: String? = null,
    var fieldCode: String? = null,
    var isSelected: Boolean = false,
    var children:  List<CommonEnumItem>? = null,
    var secondCategories:  List<CommonEnumItem>? = null,
    var positions:  List<CommonEnumItem>? = null,
    var positionCode: String? = null,
    var categoryPath: String? = null
)


@Parcelize
data class CommonItems(
    var fieldCode: String? = null,
    var fieldName: String? = null,
    var fieldType: String? = null,
    var fieldValue: String? = null,
    var fieldItems: List<CommonItems> = emptyList(),
): Parcelable {}


data class IndustryDict(
    val dictType: String? = null,
    val items: List<CommonEnumItem> = emptyList(),
    val selectedItems: List<CommonEnumItem> = emptyList(),
)

data class PositionDict(
    val dictType: String? = null,
    val selectedItems: List<CommonEnumItem> = emptyList(),
    val matchedPositions: List<CommonEnumItem> = emptyList(),
    val firstCategories: List<CommonEnumItem> = emptyList(),
)

data class WelfareDict(
    val dictType: String? = null,
    val items: List<CommonEnumItem> = emptyList(),
    val selectedItems: List<CommonEnumItem> = emptyList(),
)

object CommonEnumTreeUtil {

    fun toJson(data: List<CommonEnumItem>): String? {
        return Json.encodeToString(
            data.map { item ->
                SimpleMsgAgent(code = item.code, name = item.name)
            }
        )
    }

    fun toJson(data: LinkedHashMap<String, CommonEnumItem>): String? {
            return Json.encodeToString(
                data.values.map { item ->
                    SimpleMsgAgent(code = item.code, name = item.name)
                }
            )
}

    /**
     * 根据 level 筛选
     * @param list 原始平铺列表
     * @param level 目标层级（如 1,2,3）
     * @return 筛选后的列表
     */
    fun filterByLevel(list: List<CommonEnumItem>, level: Int): List<CommonEnumItem> {
        return list.filter { it.level == level }
    }

    /**
     * 根据 parentCode 获取直接子项
     * @param list 原始平铺列表
     * @param parentCode 父节点的 code
     * @return 子项列表
     */
    fun getChildrenByParentCode(list: List<CommonEnumItem>, parentCode: String): List<CommonEnumItem> {
        return list.filter { it.parentCode == parentCode }
    }

    /**
     * 获取指定 code 的所有后代（递归）
     * @param list 原始平铺列表
     * @param code 起始节点的 code
     * @return 所有后代节点（不包含起始节点自身）
     */
    fun getAllDescendants(list: List<CommonEnumItem>, code: String): List<CommonEnumItem> {
        val result = mutableListOf<CommonEnumItem>()
        val directChildren = getChildrenByParentCode(list, code)
        result.addAll(directChildren)
        directChildren.forEach { child ->
            result.addAll(getAllDescendants(list, child.code ?: return@forEach))
        }
        return result
    }

    /**
     * 将平铺列表构建成树形结构（多级）
     * @param list 原始平铺列表（通常按 level 升序排列或任意顺序）
     * @param rootParentCode 根节点的 parentCode（通常为 null 或 "0"）
     * @return 树形节点列表（每项包含自身及其 children）
     */
    fun buildTree(list: List<CommonEnumItem>, rootParentCode: String? = null): List<TreeNode> {
        // 先转成 Map<code, item>
        val itemMap = list.associateBy { it.code ?: return emptyList() }
        // 找出所有根节点
        val roots = list.filter { it.parentCode == rootParentCode }
        // 递归构建子树
        fun buildChildren(parentCode: String): List<TreeNode> {
            val children = list.filter { it.parentCode == parentCode }
            return children.map { child ->
                TreeNode(
                    item = child,
                    children = buildChildren(child.code ?: "")
                )
            }
        }
        return roots.map { root ->
            TreeNode(
                item = root,
                children = buildChildren(root.code ?: "")
            )
        }
    }

    /**
     * 树形节点（用于展示）
     */
    data class TreeNode(
        val item: CommonEnumItem,
        val children: List<TreeNode>
    )

    /**
     * 将树形节点展平（深度优先）
     */
    fun flattenTree(nodes: List<TreeNode>): List<CommonEnumItem> {
        val result = mutableListOf<CommonEnumItem>()
        fun traverse(node: TreeNode) {
            result.add(node.item)
            node.children.forEach { traverse(it) }
        }
        nodes.forEach { traverse(it) }
        return result
    }


    fun convertCodeBooleanToString(jsonStr: String): String {
        // 1. 解析 JSON 字符串为 JsonElement
        val jsonArray = Json.parseToJsonElement(jsonStr).jsonArray

        val newArray = jsonArray.map { element ->
            val obj = element.jsonObject
            // 获取原始 code 值
            val codeValue = obj["code"]

            // 转换逻辑：如果是布尔型 JsonPrimitive，则转为字符串；否则保持不变
            val newCode = when (codeValue) {
                is JsonPrimitive -> {
                    if (codeValue.booleanOrNull != null) {
                        JsonPrimitive(codeValue.boolean.toString())
                    } else {
                        codeValue
                    }
                }
                else -> JsonPrimitive("")
            }

            // 3. 构建新的 JsonObject
            JsonObject(obj.toMap() + ("code" to newCode))
        }

        // 4. 将修改后的 JsonArray 重新编码为 JSON 字符串
        return Json.encodeToString(newArray)
    }
}
