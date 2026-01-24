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
data class RechargePageInfo(
    //充值标题
    val detailTitle: String?,
    //充值时间
    val detailTime: Long?,
    //充值金额
    val detailAmount: Long?,
    //交易类型：1-收入，2-支出
    val tradeType: Int?,
    //积分明细ID
    val userScoreDetailsId: String?,
    //类型：1-会员充值，2-积分充值
    val detailType: String?,
    //备注
    val remark: String?,
){
    companion object{
        //积分获取
        const val SCORE_TYPE_GET = 1
        //积分消耗
        const val SCORE_TYPE_CONSUME = 2
    }
}