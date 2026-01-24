package com.kgmedia.fastfilm.core.network.apis.entity

/**
 * *****************************************
 * 项目名称: GoldenFruit
 * @Author: SZYMN
 * @Introduce:获取充值明细分页
 * @Data: 2025/10/28/周二  19:51
 * @Version: 1.0
 * *****************************************
 */
data class PointConsumePageInfo(
    //标题
    val detailTitle: String? = null,
    // 时间
    val detailTime: Long? = null,
    //明细类型：1-生成画面，2-字幕配音，3-活动赠送
    val detailType: String? = null,
    //交易类型：1-收入，2-支出
    val tradeType: Int? = null,
    //数量
    val detailAmount: Long? = null,
){
    companion object{
        //积分获取
        const val SCORE_TYPE_GET = 1
        //积分消耗
        const val SCORE_TYPE_CONSUME = 2
    }
}