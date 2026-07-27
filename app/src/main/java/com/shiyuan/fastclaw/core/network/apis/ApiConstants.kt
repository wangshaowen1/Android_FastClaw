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
    const val GET_USER_HOME_INFO = "api/candidate/auth/user/info-detail"

    /**
     * 更新头像
     */
    const val UPDATE_USER_AVATAR = "api/candidate/user/avatar/update"

    /**
     * APP版本检测
     */
    const val CHECK_APP_VERSION = "api/candidate/app/version/check"

    /**
     * 第三方登录
     */
    const val LOGIN_BY_THIRD = "api/candidate/auth/loginByThirdParty"

    /**
     * 绑定第三方账号
     */
    const val BIND_THIRD_PARTY = "/api/candidate/auth/bindThirdParty"

    const val REBIND_THIRD_PARTY = "/api/candidate/auth/rebindThirdParty"

    /**
     * 解除第三方账号绑定
     */
    const val UNBIND_THIRD_PARTY = "/api/candidate/auth/unbindThirdParty"

    /**
     * 退出登录
     */
    const val LOGOUT_APP = "auth/sign-out"

    /**
     * 注销账号
     */
    const val LOG_OFF = "/api/candidate/auth/applyCancelAccount"

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
    const val SEND_SMS_VALIDATE_CODE = "/api/candidate/auth/sendSmsValidateCode"

    /**
     * 手机号绑定状态前置校验
     */
    const val CHECK_MOBILE_BIND_STATUS = "/api/candidate/auth/checkMobileBindStatus"

    /**
     * 绑定手机号
     */
    const val BIND_PHONE = "user/phone/action/bind"

    /**
     * 三方登录后绑定手机号
     */
    const val BIND_MOBILE = "/api/candidate/auth/bindMobile"

    /**
     * 换绑手机号
     */
    const val CHANGE_MOBILE = "/api/candidate/auth/changeMobile"


    /**
     * 手机登录接口--闪验方式
     */
    const val LOGIN_BY_FLASH_MOBILE = "/api/candidate/auth/loginByFlashMobile"

    /**
     * 手机登录接口--验证码方式
     */
    const val LOGIN_BY_PHONE = "/api/candidate/auth/loginByMobile"
    const val REFRESH_TOKEN = "/api/candidate/auth/refreshToken"

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
    const val GET_ORDER_STATUS = "/api/candidate/app/trade/pay/order/status/query"

    /**
     * 分页查询候选人支付订单列表
     */
    const val QUERY_PAY_ORDER_LIST = "/api/candidate/app/trade/pay/order/list/query"

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
    const val GET_MY_REPORT_LIST = "/api/candidate/app/interviewReport/report/list"
    const val GET_RESUME_LATEST = "/api/candidate/app/interviewReport/resume/latest/query"
    const val RESUME_MATERIAL_QUALITY = "/api/candidate/app/interviewReport/resumeMaterialQuality/check"
    const val RESUME_MATERIAL_QUALITY_RESULT = "/api/candidate/app/interviewReport/resumeMaterialQuality/result/query"

    const val JD_RESUME_MATERIAL_QUALITY = "/api/candidate/app/interviewReport/jdMaterialQuality/check"
    const val JD_RESUME_MATERIAL_QUALITY_RESULT = "/api/candidate/app/interviewReport/jdMaterialQuality/result/query"
    const val INTERVIEW_PASS_PACKAGE_RESUME_STATUS = "/api/candidate/app/interviewPassPackage/resume/status"
    const val INTERVIEW_REPORT_LIST_QUERY = "/api/candidate/app/myReport/report/list/query"
    const val INTERVIEW_REPORT_DELETE = "/api/candidate/app/myReport/report/delete"
    const val INTERVIEW_REPORT = "/api/candidate/app/interviewReport/report/submit"
    const val INTERVIEW_REPORT_RESULT = "/api/candidate/app/interviewReport/report/status/query"
    const val GET_SCHEME = "/api/candidate/miniProgram/scheme"

    const val UNDERSTANDING_SCORE = "/api/candidate/app/jobAgent/understanding/score/query"
    const val UNDERSTANDING_DETAIL = "/api/candidate/app/jobAgent/understanding/detail/query"

    const val RESUME_QUERY = "/api/candidate/app/jobAgent/resume/list/query"
    const val QUERY_REGION_DICT = "/api/candidate/app/jobAgent/dict/region/query"

    /**
     * 查询用户钱包信息
     */
    const val QUERY_WALLET_INFO = "trade/user/wallet/current"

/*
    */
/**
     * App转发H5支付-微信下单
     *//*

    const val H5_PAY_WX = "app/forward/h5/createAppWechat"
*/

    /**
     * App转发H5支付-支付宝 微信下单
     */
    const val H5_PAY = "/api/candidate/app/interviewPassPackage/pay/create"

    /**
     * 创建app微信支付订单
     */
    const val CREATE_APP_WECHAT_PAY_ORDER = "/api/candidate/app/trade/pay/wechat/create"

    /**
     * 创建app支付宝支付订单
     */
    const val CREATE_APP_ALIPAY_ORDER = "/api/candidate/app/trade/pay/alipay/create"

    /**
     * 查询安卓充值页支付方式展示列表
     */
    const val QUERY_TRADE_PAY_WAY_LIST = "/api/candidate/app/trade/pay/way/list"

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

    /**
     * 上传简历
     */
    const val UPLOAD_RESUME = "app/resume/upload"

    /**
     * 创建文件上传凭证
     */
    const val CREATE_FILE_UPLOAD_CREDENTIAL = "/api/file/upload/credential/create"

    /**
     * 确认文件上传完成
     */
    const val COMPLETE_FILE_UPLOAD = "/api/file/upload/complete"

    /**
     * 查询文件安全状态
     */
    const val QUERY_FILE_SECURITY_STATUS = "/api/file/security/status/query"

    /**
     * 查询积分信息
     */
    const val GET_USER_POINTS = "api/candidate/points/info"

    /**
     * 分页查询积分交易明细
     */
    const val QUERY_USER_POINTS_PAGE = "api/candidate/points/transaction/page"

    /**
     * 分页查询邀请记录
     */
    const val QUERY_INVITE_RECORD_PAGE = "api/candidate/h5/invite/record/page"
    const val QUERY_INVITE_STATS = "/api/candidate/app/invite/stats/query"

    /**
     * 查询邀请分享信息
     */
    const val QUERY_INVITE_SHARE_INFO = "/api/candidate/app/invite/share/info/query"

    /**
     * 查询积分充值配置列表
     */
    const val QUERY_POINT_CONFIG_LIST = "/api/candidate/app/trade/point/config/list"

    const val COMMON_INIT = "/api/app/common/init"
    const val SIGN_OUT = "/candidate/auth/sign-out"

    /**
     * 进入或恢复会话
     */
    const val SESSION_ENTER = "/api/candidate/app/jobAgent/session/enter"

    /**
     * 流式发送用户消息（求职 Agent SSE，text/event-stream）
     */
    const val JOB_AGENT_STREAM = "/api/candidate/app/jobAgent/stream"
    const val MOCK_INTERVIEW_UNFINISHED_QUERY = "/api/candidate/app/interviewReport/mockInterview/unfinished/query"
    const val MOCK_INTERVIEW_SESSION_START = "/api/candidate/app/interviewReport/mockInterview/session/start"
    const val MOCK_INTERVIEW_STREAM = "/api/candidate/app/interviewReport/mockInterview/stream"
    const val MOCK_INTERVIEW_FORCE_CANCEL = "/api/candidate/app/interviewReport/mockInterview/forceCancel"
    const val MOCK_INTERVIEW_AUDIO_TOKEN_QUERY = "/api/candidate/app/interviewReport/mockInterview/audioToken/query"
    const val MOCK_INTERVIEW_REPORT_PROGRESS_QUERY = "/api/candidate/app/interviewReport/mockInterview/report/progress/query"
    const val MOCK_INTERVIEW_REPORT_RETRY = "/api/candidate/app/interviewReport/mockInterview/report/retry"
    const val INTERVIEW_PASS_PACKAGE_STATUS_QUERY = "/api/candidate/app/interviewPassPackage/status/query"
    const val INTERVIEW_PASS_PACKAGE_REGENERATE = "/api/candidate/app/interviewPassPackage/regenerate"
    const val MINIPROGRAM_CODERESUMEBINDING_QUERY = "/api/candidate/miniProgram/codeResumeBinding/query"

    /**
     * 取消流式生成（求职 Agent）
     */
    const val JOB_AGENT_CANCEL = "/api/candidate/app/jobAgent/cancel"

    /**
     * 查询通信会话（求职 Agent，仅查状态不建 SSE）
     */
    const val JOB_AGENT_CONVERSATION_QUERY = "/api/candidate/app/jobAgent/conversation/query"
    const val JOB_AGENT_MODULE_LIST_QUERY = "/api/candidate/app/jobAgent/module/list/query"

    /**
     * 提交卡片或按钮动作（求职 Agent）
     */
    const val JOB_AGENT_ACTION_SUBMIT = "/api/candidate/app/jobAgent/action/submit"

    /**
     * 更新顶部求职状态
     */
    const val JOB_AGENT_JOB_STATUS_UPDATE = "/api/candidate/app/jobAgent/jobStatus/update"

    /**
     * 查询历史消息分页（求职 Agent）
     */
    const val JOB_AGENT_MESSAGE_PAGE = "/api/candidate/app/jobAgent/message/page"

    /**
     * 拉取增量消息（求职 Agent）
     */
    const val JOB_AGENT_MESSAGE_PULL = "/api/candidate/app/jobAgent/message/pull"

    /**
     * 查询公司背调公司搜索结果
     */
    const val JOB_AGENT_COMPANY_BACKGROUND_COMPANY_SEARCH_QUERY = "/api/candidate/app/jobAgent/companyBackground/company/search/query"

    /**
     * 提交公司背调报告生成前置校验
     */
    const val JOB_AGENT_COMPANY_BACKGROUND_PRE_CHECK_SUBMIT = "/api/candidate/app/jobAgent/companyBackground/report/preCheck/submit"

    /**
     * 查询公司背调报告生成前置校验结果
     */
    const val JOB_AGENT_COMPANY_BACKGROUND_PRE_CHECK_RESULT_QUERY = "/api/candidate/app/jobAgent/companyBackground/report/preCheck/result/query"

    /**
     * 查询公司背调报告生成结果
     */
    const val JOB_AGENT_COMPANY_BACKGROUND_GENERATE_RESULT_QUERY = "/api/candidate/app/jobAgent/companyBackground/report/generate/result/query"

    /**
     * 重新生成公司背调失败报告
     */
    const val JOB_AGENT_COMPANY_BACKGROUND_REGENERATE_SUBMIT = "/api/candidate/app/jobAgent/companyBackground/report/regenerate/submit"

    /**
     * 查询当前可确认简历（求职 Agent）
     */
    const val JOB_AGENT_RESUME_CURRENT_QUERY = "/api/candidate/app/jobAgent/resume/current/query"

    /**
     * 确认或否认当前简历（求职 Agent）
     */
    const val JOB_AGENT_RESUME_CURRENT_CONFIRM = "/api/candidate/app/jobAgent/resume/current/confirm"

    /**
     * 提交简历上传结果（求职 Agent）
     */
    const val JOB_AGENT_RESUME_UPLOAD_SUBMIT = "/api/candidate/app/jobAgent/resume/upload/submit"


    /**
     * 提交简历上传我的
     */
    const val JOB_AGENT_RESUME_UPLOAD_MY_SUBMIT = "/api/candidate/app/jobAgent/resume/my/upload/submit"

    /**
     * 查询在线简历信息
     */
    const val JOB_AGENT_ONLINE_RESUME_INFO_QUERY = "/api/candidate/app/jobAgent/onlineResume/info/query"



    /**
     * 查询在
     */
    const val JOB_PREFERENCE_RESUME_INFO_QUERY = "/api/candidate/app/jobAgent/onlineResume/preference/query"

    /**
     * 更新在线简历属性
     */
    const val JOB_AGENT_ONLINE_RESUME_ATTRIBUTE_UPDATE = "/api/candidate/app/jobAgent/onlineResume/attribute/update"

    /**
     * 查询线上简历单个普通属性
     */
    const val JOB_AGENT_ONLINE_RESUME_ATTRIBUTE_QUERY = "/api/candidate/app/jobAgent/onlineResume/attribute/query"


    /**
     * 查询属性数组候选值（求职 Agent）
     */
    const val JOB_AGENT_ATTRIBUTE_ARRAY_QUERY = "/api/candidate/app/jobAgent/attribute/array/query"

    /**
     * 确认属性候选值（求职 Agent）
     */
    const val JOB_AGENT_ATTRIBUTE_CONFIRM = "/api/candidate/app/jobAgent/attribute/confirm"

    /**
     * 拒绝属性候选值（求职 Agent）
     */
    const val JOB_AGENT_ATTRIBUTE_REJECT = "/api/candidate/app/jobAgent/attribute/reject"

    /**
     * 查询城市字典组件提供已选、历史、热门、A-Z 分组和搜索数据。
     */
    const val JOB_AGENT_DICT_REGION_QUERY = "/api/candidate/app/jobAgent/dict/region/query"


    /**
     *
     */
    const val JOBAGENT_ATTRIBUTE_ARRAY_SAVE = "/api/candidate/app/jobAgent/attribute/array/save"

    /**
     *
     */
    const val ONLINERESUME_ATTRIBUTE_ARRAY_SAVE = "/api/candidate/app/jobAgent/onlineResume/attribute/array/item/save"

    const val ONLINERESUME_ATTRIBUTE_ARRAY_DELETE = "/api/candidate/app/jobAgent/onlineResume/attribute/array/item/delete"


    /**
     * 查询小虾行业字典
     */
    const val DICT_INDUSTRY_QUERY = "/api/candidate/app/jobAgent/dict/industry/query"

    /**
     * 查询小虾岗位字典
     */
    const val DICT_POSITION_QUERY = "/api/candidate/app/jobAgent/dict/position/query"

    /**
     * 查询福利偏好字典
     */
    const val DICT_WELFARE_QUERY = "/api/candidate/app/jobAgent/dict/welfare/query"
}
