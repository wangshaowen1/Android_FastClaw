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
