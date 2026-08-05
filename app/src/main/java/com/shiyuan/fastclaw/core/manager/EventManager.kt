package com.shiyuan.fastclaw.core.manager

import com.growingio.android.sdk.autotrack.GrowingAutotracker
import com.shiyuan.fastclaw.common.ext.ioScope
import com.shiyuan.fastclaw.core.network.apis.ApiFactory
import com.shiyuan.fastclaw.core.network.apis.request.PlayEventBody
import com.nhsd.basic.ext.launchNoCrash
import com.nhsd.basic.utils.Logger


/**
 * 自定义事件管理器（事件埋点）
 * @author xiaoowuu
 */
object EventManager {

    /**
     * 作品播放量计数
     */
    fun onPlayEvent(userId: String?, workId: String?, episodeId: String?) {
        ioScope.launchNoCrash {
            ApiFactory.apiService.playEventReport(PlayEventBody(userId, workId, episodeId))
        }
    }

    /**
     * @param code GrowingIO上配置的Code
     * @param paramsMap code附带的子属性 传参：mapOf("属性key" to "属性值")
     */
    @JvmStatic
    fun onAppTrack(code: String, paramsMap: Map<String, String?>? = null) {
        Logger.d("[onAppTrack] Tracking Event -> Code: $code, Params: $paramsMap")
        GrowingAutotracker.get().trackCustomEvent(code, paramsMap)
    }

    /**
     * @param code GrowingIO上配置的Code
     * @param params code附带的子属性 传参："属性key" to "属性值"
     */
    @JvmStatic
    fun onAppTrack(code: String, vararg params: Pair<String, String?>) {
        if (params.isEmpty()) {
            onAppTrack(code, null)
        } else {
            onAppTrack(code, params.toMap())
        }
    }

}

/**
 * App功能使用数据
 */
object AppEvent {


    /**
     * 快龙虾｜C端APP-小虾页面访问
     * 用户访问APP小虾页面时上报
     */
    const val KLX_CAPP_XIAOXIA_PAGE = "klx_capp_xiaoxia_page"

    /**
     * 快龙虾｜C端APP-我的页面访问
     * 用户访问APP我的页面时上报
     */
    const val KLX_CAPP_MY_PAGE = "klx_capp_my_page"

    /**
     * 快龙虾｜C端APP-登录页面访问
     * 用户访问APP登录页时上报
     */
    const val KLX_CAPP_LOGIN_PAGE = "klx_capp_login_page"

    /**
     * 快龙虾｜C端APP-小虾对你的理解-我的简历页面访问
     * 用户访问APP小虾对你的理解页-我的简历tab时上报
     */
    const val KLX_CAPP_UNDERSTANDING_MYRESUME_PAGE = "klx_capp_understanding_myresume_page"

    /**
     * 快龙虾｜C端APP-小虾对你的理解-求职偏好页面访问
     * 用户访问APP小虾对你的理解页-求职偏好tab时上报
     */
    const val KLX_CAPP_UNDERSTANDING_PREFERENCE_PAGE = "klx_capp_understanding_preference_page"

    /**
     * 快龙虾｜C端APP-我的简历页面访问
     * 用户访问APP我的-我的简历页时上报
     */
    const val KLX_CAPP_MY_RESUME_PAGE = "klx_capp_my_resume_page"

    /**
     * 快龙虾｜C端APP-我的虾条页面访问
     * 用户访问APP我的-我的虾条页时上报
     */
    const val KLX_CAPP_MY_SHRIMPSTICK_PAGE = "klx_capp_my_shrimpstick_page"

    /**
     * 快龙虾｜C端APP-面试宝典落地页访问
     * 用户访问APP面试宝典落地页时上报
     */
    const val KLX_CAPP_INTERVIEWREPORT_LANDING_PAGE = "klx_capp_interviewreport_landing_page"

    /**
     * 快龙虾｜C端APP-面试宝典加载报告页访问
     * 用户在APP面试宝典上传简历后，报告加载页时上报
     */
    const val KLX_CAPP_INTERVIEWREPORT_LOADINGREPORT_PAGE = "klx_capp_interviewreport_loadingreport_page"

    /**
     * 快龙虾｜C端APP-面试宝典报告页访问
     * 用户在APP面试宝典上传简历后，访问报告页时上报
     */
    const val KLX_CAPP_INTERVIEWREPORT_REPORT_PAGE = "klx_capp_interviewreport_report_page"

    /**
     * 快龙虾｜C端APP-登陆成功（归到同一动作中）
     * 用户在APP登录页登陆成功时上报
     */
    const val KLX_C_LOGIN_SUCCESS = "klx_c_login_success"

    /**
     * 快龙虾｜C端APP-上传附件简历（归到同一动作中）
     * 用户在APP点击上传附件简历时上报
     */
    const val KLX_C_UPLOAD_RESUME = "klx_c_upload_resume"

    /**
     * 快龙虾｜C端APP-确认绑定简历
     * 用户在APP小虾对话页被小虾询问确认是否绑定简历时，点击是我的简历时上报
     */
    const val KLX_CAPP_CONFIRM_RESUME = "klx_capp_confrim_resume"

    /**
     * 快龙虾｜C端APP-拒绝绑定简历
     * 用户在APP小虾对话页被小虾询问确认是否绑定简历时，点击不是，重新上传时上报
     */
    const val KLX_CAPP_DENY_RESUME = "klx_capp_deny_resume"

    /**
     * 快龙虾｜C端APP-点击我没有简历
     * 用户在APP小虾对话页，在上传简历卡片信息点击我没有简历时上报
     */
    const val KLX_CAPP_NO_RESUME = "klx_capp_no_resume"

    /**
     * 快龙虾｜C端APP-简历上传失败点击重新上传
     * 用户在APP小虾对话页，上传简历失败后点击重新上传时上报
     */
    const val KLX_CAPP_UPLOAD_FAILED_RE_RESUME = "klx_capp_upload_failed_re_resume"

    /**
     * 快龙虾｜填写意向岗位（归到同一动作中）
     * 用户在APP小虾对话过程中。完成意向岗位填写时上报
     */
    const val KLX_C_EXPECTED_JOB_COMPLETED = "klx_c_expected_job_completed"

    /**
     * 快龙虾｜填写意向城市（归到同一动作中）
     * 用户在APP小虾对话过程中。完成意向城市填写时上报
     */
    const val KLX_C_EXPECTED_CITY_COMPLETED = "klx_c_expected_city_completed"

    /**
     * 快龙虾｜填写现居向城市（归到同一动作中）
     * 用户在APP小虾对话过程中。完成意向城市填写时上报
     */
    const val KLX_C_EXPECTED_CURRENTLY_CITY_COMPLETED = "klx_c_expected_currently_city_completed"

    /**
     * 快龙虾｜填写意向薪资（归到同一动作中）
     * 用户在APP小虾对话过程中。完成意向薪资填写时上报
     */
    const val KLX_C_EXPECTED_SALARY_COMPLETED = "klx_c_expected_salary_completed"

    /**
     * 快龙虾｜填写求职状态（归到同一动作中）
     * 用户在APP小虾对话过程中。完成求职状态填写时上报
     */
    const val KLX_C_JOBSEEKING_STATUS_COMPLETED = "klx_c_jobseeking_status_completed"

    /**
     * 进入登录页展示
     *
     */
    const val KLX_VIEW_LOGIN = "klx_view_login"

    /**
     * 点击登录按钮触发登录请求 比如微信或者手机号
     *
     */
    const val KLX_CLICK_LOGIN_BTN = "klx_click_login_btn"

    /**
     * 点击退出登录
     *
     */
    const val KLX_CLICK_LOGOUT = "klx_click_logout"

    /**
     * 登录后进入公司背调
     *
     */
    const val KLX_CLICK_COMPANY_CHECK = "klx_click_company_check"

    /**
     * 登录后进入测简历
     *
     */
    const val KLX_CLICK_RESUME_TEST = "klx_click_resume_test"


    /**
     * 登录后进入测薪资
     *
     */
    const val KLX_CLICK_SALARY_TEST = "klx_click_salary_test"

    /**
     * 登录后进入模拟面试
     *
     */
    const val KLX_CLICK_MOCK_INTERVIEW = "klx_click_mock_interview"


    /**
     * 登录后进入面试通关包
     *
     */
    const val KLX_CLICK_INTERVIEW_PASS = "klx_click_interview_pass"

}

/**
 * App功能使用数据参数
 */
object AppEventParam {

    /**
     * 埋点来源渠道属性
     */
    const val KEY_PIPE_SOURCE = "klx_pipe_source"

    /**
     * App渠道
     */
    const val VALUE_PIPE_SOURCE_APP = "APP"

    /**
     * 用户登录状态属性
     */
    const val KEY_IS_LOGIN = "klx_is_login"

    /**
     * 已登录
     */
    const val VALUE_IS_LOGIN_YES = "是"

    /**
     * 未登录
     */
    const val VALUE_IS_LOGIN_NO = "否"

    /**
     * 登录页触发来源属性
     */
    const val KEY_LOGIN_TRIGGER_SOURCE = "klx_login_trigger_source"

    /**
     * 小虾Tab-聊天输入
     */
    const val XIAOXIA_CHAT = "xiaoxia_chat"

    /**
     * 小虾Tab-公司背调
     */
    const val XIAOXIA_TAB_COMPANY_CHECK = "xiaoxia_tab_company_check"

    /**
     * 小虾Tab-测简历
     */
    const val XIAOXIA_TAB_RESUME_TEST = "xiaoxia_tab_resume_test"

    /**
     * 小虾Tab-测薪资
     */
    const val XIAOXIA_TAB_SALARY_TEST = "xiaoxia_tab_salary_test"

    /**
     * 小虾Tab-模拟面试
     */
    const val XIAOXIA_TAB_MOCK_INTERVIEW = "xiaoxia_tab_mock_interview"

    /**
     * 小虾Tab-面试通关包
     */
    const val XIAOXIA_TAB_INTERVIEW_PASS = "xiaoxia_tab_interview_pass"

    /**
     * 小虾Tab-上传简历
     */
    const val XIAOXIA_TAB_UPLOAD_RESUME = "xiaoxia_tab_upload_resume"

    /**
     * 小虾Tab-暂无简历
     */
    const val XIAOXIA_TAB_NO_RESUME = "xiaoxia_tab_no_resume"

    /**
     * 小虾Tab-选择求职状态
     */
    const val XIAOXIA_TAB_SELECT_JOB_STATUS = "xiaoxia_tab_select_job_status"

    /**
     * 小虾Tab-理解度升级编辑
     */
    const val XIAOXIA_TAB_SHRIMP_UNDERSTANDS = "xiaoxia_tab_shrimp_understands"

    /**
     * 宝典Tab-公司背调
     */
    const val BAODIAN_TAB_COMPANY_CHECK = "baodian_tab_company_check"

    /**
     * 宝典Tab-测简历
     */
    const val BAODIAN_TAB_RESUME_TEST = "baodian_tab_resume_test"

    /**
     * 宝典Tab-测薪资
     */
    const val BAODIAN_TAB_SALARY_TEST = "baodian_tab_salary_test"

    /**
     * 宝典Tab-模拟面试
     */
    const val BAODIAN_TAB_MOCK_INTERVIEW = "baodian_tab_mock_interview"

    /**
     * 宝典Tab-面试通关包
     */
    const val BAODIAN_TAB_INTERVIEW_PASS = "baodian_tab_interview_pass"

    /**
     * 宝典Tab-理解度
     */
    const val BAODIAN_TAB_SHRIMP_UNDERSTANDS = "baodian_tab_shrimp_understands"

    /**
     * 我的Tab-点击登录
     */
    const val ME_TAB_LOGIN_BTN = "me_tab_login_btn"

    /**
     * 我的Tab-设置
     */
    const val ME_TAB_SETTINGS = "me_tab_settings"

    /**
     * 我的Tab-邀请好友
     */
    const val ME_TAB_INVITE_FRIEND = "me_tab_invite_friend"

    /**
     * 我的Tab-头像
     */
    const val ME_TAB_AVATAR = "me_tab_avatar"

    /**
     * 我的Tab-理解度球
     */
    const val ME_TAB_UNDERSTANDING_BALL = "me_tab_understanding_ball"

    /**
     * 我的Tab-简历信息
     */
    const val ME_TAB_RESUME_INFO = "me_tab_resume_info"

    /**
     * 我的Tab-求职偏好
     */
    const val ME_TAB_JOB_PREFERENCE = "me_tab_job_preference"

    /**
     * 我的Tab-我的报告
     */
    const val ME_TAB_MY_REPORTS = "me_tab_my_reports"

    /**
     * 我的Tab-我的订单
     */
    const val ME_TAB_MY_ORDERS = "me_tab_my_orders"

    /**
     * 登录按钮对应的登录方式属性
     */
    const val KEY_LOGIN_TYPE = "klx_click_login_btn"

    /**
     * 手机号登录
     */
    const val VALUE_LOGIN_TYPE_PHONE = "phone"

    /**
     * 微信登录
     */
    const val VALUE_LOGIN_TYPE_WECHAT = "wechat"

    /**
     * 功能入口来源  公司背调点击.....
     */
    const val KLX_ENTRY_SOURCE = "klx_entry_source"


    /**
     * 小虾Tab
     */
    const val XIAOXIA_TAB = "xiaoxia_tab"

    /**
     * 宝典Tab
     */
    const val BAODIAN_TAB = "baodian_tab"

}


object GrowingIO {

    /**
     * 通用｜是否为新用户
     * 用户登录时候是否为首次注册账号，如果是="yes"，如果是老用户登录非首次注册="no"
     */
    const val KEY_ALL_IS_NEW_USER = "all_is_new_user"


    /**
     * 通用｜用户登录类型
     * 用户登录类型： unknown = 其他或未知 phone = 手机号 wechat = 微信 AppleID = AppleID
     */
    const val KEY_ALL_LOGIN_TYPE = "all_login_type"
}
