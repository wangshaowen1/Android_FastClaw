package com.shiyuan.fastclaw.core.network.apis.response

/**
 * 查询上架状态响应数据
 *
 * @param shelfStatus 上架状态：0-下架 / 1-上架 / 2-审核中 / null-记录不存在
 */
data class ShelfStatusData(
    /** 上架状态：0-下架 1-上架 2-审核中 null-记录不存在 */
    val shelfStatus: Int? = null,
)