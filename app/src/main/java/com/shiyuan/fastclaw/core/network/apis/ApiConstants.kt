package com.shiyuan.fastclaw.core.network.apis

import com.shiyuan.fastclaw.BuildConfig

object ApiConstants {

    //改地址
    //"http://hk308942lu51.vicp.fun"
    val BASE_URL: String = BuildConfig.SERVER_URL

    /**
     * 获取软件配置信息接口
     */
    const val APP_CONFIG = "app/getAppConfigInfo"

    //用户举报
    const val TIP_OFF = "user/tipOff"

    //查询指定版本上架状态
    const val GET_APP_SHELF_STATUS = "app/getVersionShelfStatus"

    /**
     * 获取用户主页信息接口
     */
    const val GET_USER_HOME_INFO = "auth/user/info-detail"

    /**
     * 获取APP升级信息
     */
    const val GET_APP_UPGRADE_INFO = "app/getAppUpgradeInfo"

    /**
     * 第三方登录
     */
    const val LOGIN_BY_THIRD = "auth/loginByThirdParty"

    /**
     * 退出登录
     */
    const val LOGOUT_APP = "auth/sign-out"

    /**
     * 注销账号
     */
    const val LOG_OFF = "auth/logout"

    /**
     * 提交用户的反馈数据
     */
    const val FEED_BACK = "app/feedback/save"

    /**
     * 用户日活记录
     */
    const val DAILY = "user/login/daily"

    /**
     * 发送短信验证码
     */
    const val SEND_SMS_VALIDATE_CODE = "auth/sendSmsValidateCode"

    /**
     * 绑定手机号
     */
    const val BIND_PHONE = "user/phone/action/bind"


    /**
     * 手机登录接口--闪验方式
     */
    const val LOGIN_BY_FLASH_MOBILE = "auth/loginByFlashMobile"

    /**
     * 手机登录接口--验证码方式
     */
    const val LOGIN_BY_PHONE = "auth/register"

    /**
     * 取消注销
     */
    const val CANCEL_LOG_OFF = "user/remove/cancel"

    /**
     * 用户收益信息
     */
    const val USER_REVENUE_INFORMATION = "user/profit/info"

    /**
     * 获取订单状态
     */
    const val GET_ORDER_STATUS = "order/getOrderStatus"

    const val APP_BANNERS = "app/home/banner/list"

    /**
     * 首页作品列表
     */
    const val RECOMMEND_WORK_LIST = "app/home/work/page"

    /**
     * 作品播放量计数
     */
    const val PLAY_EVENT_REPORT = "app/home/work/playCount"

    /**
     * 举报提交
     */
    const val REPORT_SUBMISSION = "app/report/save"

    /**
     * 查询用户钱包信息
     */
    const val QUERY_WALLET_INFO = "trade/user/wallet/current"

    /**
     * App转发H5支付-微信下单
     */
    const val H5_PAY_WX = "app/forward/h5/createAppWechat"

    /**
     * App转发H5支付-支付宝下单
     */
    const val H5_PAY_ALIPAY = "app/forward/h5/createAppAliPay"

    /**
     * 敏感词校验
     */
    const val SENSITIVE_WORD_CHECK = "ai/content-security/text/check"

    /**
     * 内容安全-图片校验
     */
    const val SENSITIVE_IMAGE_CHECK = "ai/content-security/img/check"

    /**
     * 引导添加客服弹窗信息列表
     */
    const val GUIDE_LINK_CUSTOMER_POPUP_INFO = "app/window/floatWindow/wechatMini/list"
}