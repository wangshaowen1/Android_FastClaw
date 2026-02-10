package com.kgmedia.fastfilm.core.network.apis.response

/**
 * *****************************************
 * 项目名称: KidsTales
 * @Author: SZYMN
 * @Introduce:获取积分消耗明细分页
 * @Data: 2025/9/10/周三  17:56
 * @Version: 1.0
 * *****************************************
 */
data class ComputingPowerDetailsInfo(
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