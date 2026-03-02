package com.kgmedia.fastfilm.core.manager

import com.kgmedia.fastfilm.common.ext.ioScope
import com.kgmedia.fastfilm.core.network.apis.ApiFactory
import com.kgmedia.fastfilm.core.network.apis.request.MusicGenerateSourceBody
import com.kgmedia.fastfilm.core.network.apis.request.PlayEventBody
import com.kgmedia.fastfilm.core.network.apis.request.PopularRecommendDramasIdBody
import com.kgmedia.fastfilm.core.network.apis.request.ShareReportBody
import com.nhsd.basic.ext.launchNoCrash
import com.nhsd.basic.utils.Logger


/**
 * 自定义事件管理器（事件埋点）
 * @author xiaoowuu
 */
object EventManager {

    var playerInSquare = false
    private val apiService = ApiFactory.apiService


    /**
     * 用户分享操作上报
     * @param shareDataId 分享数据id(歌曲id/mvId)
     * @param shareTargetPlatformType 分享目标平台大类 1.好友(微信好友、朋友圈)  2.其他平台(抖音、小红书、快手)
     * @param shareTargetPlatformSubType 分享目标平台小类 10-微信好友 11-朋友圈 20-抖音 21-小红书 22-快手
     */
    fun onShare(
        shareDataId: String?,
        shareTargetPlatformType: Int,
        shareTargetPlatformSubType: Int
    ) {
        ioScope.launchNoCrash {
            apiService.shareReport(
                ShareReportBody(
                    shareDataId,
                    shareTargetPlatformType,
                    shareTargetPlatformSubType
                )
            )
        }
    }

    /**
     * 歌曲生成来源上报
     */
    fun musicGenerateSourceReport(body: MusicGenerateSourceBody) {
        ioScope.launchNoCrash {
            ApiFactory.apiService.musicGenerateSourceReport(body)
        }
    }

    /**
     *  爆款推荐点击上报
     */
    fun hitRecommendClickReport(popularRecommendDramasId: String?) {
        ioScope.launchNoCrash {
            ApiFactory.apiService.recommendClickReport(
                PopularRecommendDramasIdBody(
                    popularRecommendDramasId
                )
            )
        }
    }

    /**
     * 作品播放量计数
     */
    fun onPlayEvent(userId: String?, workId: String?, episodeId: String?) {
        ioScope.launchNoCrash {
            ApiFactory.apiService.playEventReport(PlayEventBody(userId, workId,episodeId))
        }
    }

    /**
     * @param code GrowingIO上配置的Code
     * @param paramsMap code附带的子属性 传参：mapOf("属性key" to "属性值")
     */
    @JvmStatic
    fun onAppTrack(code: String, paramsMap: Map<String, String?>? = null) {
        Logger.d("[onAppTrack] Tracking Event -> Code: $code, Params: $paramsMap")
        //GrowingAutotracker.get().trackCustomEvent(code, paramsMap)
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
     * 金果｜显示生成分镜页面
     * 显示生成分镜页面，显示时上报
     */
    const val JG_VIEW_GENERATE_STORYBOARD_PAGE = "jg_view_generate_storyboard_page"


    /**
     * 金果｜点击自己写故事的下一步按钮
     * 用户点击自己写故事页面的下一步的按钮，点击时上报
     */
    const val JG_CLICK_WRITE_OWN_STORY_NEXT_STEP_BUTTON =
        "jg_click_write_own_story_next_step_button"

    /**
     * 金果｜点击灵感故事的下一步按钮
     * 用户点击灵感故事页面的下一步的按钮，点击时上报
     */
    const val JG_CLICK_INSPIRATION_STORY_NEXT_STEP_BUTTON =
        "jg_click_inspiration_story_next_step_button"


    /**
     * 金果｜用户登录账号
     * 用户成功登录手机号/微信号/AppleID后上报，是否首次注册登录用户
     *
     * 通用｜是否为新用户	all_is_new_user	字符串
     * 通用｜用户登录类型	all_login_type	字符串
     */
    const val JG_ACTION_USER_LOGIN = "jg_action_user_login"


    /**
     * 金果｜点击续写故事的按钮
     */
    const val JG_CLICK_CONTINUE_STORY_BUTTON = "jg_click_continue_story_button"

    /**
     * 金果｜点击自己写故事的帮我优化按钮
     * 用户点击自己写故事页面的帮我优化的按钮，点击时上报
     */
    const val JG_CLICK_WRITE_OWN_STORY_OPTIMIZE_BUTTON = "jg_click_write_own_story_optimize_button"


    /**
     * 金果｜点击灵感故事的随机灵感按钮
     * 用户点击灵感故事页面的随机灵感的按钮，点击时上报
     */
    const val JG_CLICK_INSPIRATION_STORY_RANDOM_INSPIRATION_BUTTON =
        "jg_click_inspiration_story_random_inspiration_button"

    /**
     * 金果｜点击灵感故事的帮我优化按钮
     * 用户点击灵感故事页面的帮我优化的按钮，点击时上报
     */
    const val JG_CLICK_INSPIRATION_STORY_OPTIMIZE_BUTTON =
        "jg_click_inspiration_story_optimize_button"

    /**
     * 金果｜显示创作的页面
     * 用户访问了创作页面，不区分灵感故事/自己写故事/续写故事的页面
     */
    const val JG_VIEW_WRITE_STORY_PAGE = "jg_view_write_story_page"

    /**
     * 金果｜点击爆款页面的立即创作按钮
     * 用户点击爆款页面的立即创作的按钮，点击时上报
     */
    const val JG_CLICK_FADDISH_PAGE_CREATE_NOW_BUTTON = "jg_click_faddish_page_create_now_button"

    /**
     * 金果｜访问我的Tab页面
     * 用户访问我的TAB页面，显示的时候上报
     */
    const val JG_VIEW_MY_TAB_PAGE = "jg_view_my_tab_page"


    /**
     * 金果｜访问爆款Tab页面
     * 用户访问爆款TAB页面，显示的时候上报
     */
    const val JG_VIEW_FADDISH_TAB_PAGE = "jg_view_faddish_tab_page"



    /**
     * 访问首页｜用户进入App访问首页页面
     */
    const val SF_VIEW_HOME_PAGE = "sf_view_home_page"

    /**
     * 浏览首页推荐视频点击观看｜用户在首页浏览推荐视频内容并进行观看
     */
    const val SF_CLICK_HOME_RECOMMEND_VIDEO = "sf_click_home_recommend_video"

    /**
     * 访问作品tab页｜用户点击底部导航栏作品Tab，进入个人作品页
     */
    const val SF_VIEW_WORKS_TAB_PAGE = "sf_view_works_tab_page"

    /**
     * 访问我的tab页｜用户点击底部导航栏我的Tab，进入个人页面
     */
    const val SF_VIEW_MINE_TAB_PAGE = "sf_view_mine_tab_page"

    /**
     * 算力充值页面｜用户进入算力充值页面
     */
    const val SF_VIEW_RECHARGE_PAGE = "sf_view_recharge_page"

    /**
     * 用户进入故事灵感页面｜用户进入故事灵感页面
     */
    const val SF_VIEW_STORY_INSPIRATION_PAGE = "sf_view_story_inspiration_page"

    /**
     * 故事灵感tab｜用户进入故事灵感页面
     */
    const val SF_VIEW_STORY_INSPIRATION_TAB = "sf_view_story_inspiration_tab"

    /**
     * 故事灵感页面的下一步按钮｜用户在故事灵感tab页 输入文字后点击下一步按钮
     */
    const val SF_CLICK_STORY_INSPIRATION_NEXT_BUTTON = "sf_click_story_inspiration_next_button"

    /**
     * 小说改编tab页面｜用户进入小说改编
     */
    const val SF_VIEW_NOVEL_ADAPTATION_TAB = "sf_view_novel_adaptation_tab"

    /**
     * 小说改编页面的下一步按钮｜用户在小说改编tab页 输入文字后点击下一步按钮
     */
    const val SF_CLICK_NOVEL_ADAPTATION_NEXT_BUTTON = "sf_click_novel_adaptation_next_button"

    /**
     * 故事梗概环节｜用户进入创作流程中的故事梗概环节页面
     */
    const val SF_VIEW_STORY_OUTLINE_PAGE = "sf_view_story_outline_page"

    /**
     * 故事剧本环节｜用户进入创作流程中的故事剧本环节页面
     */
    const val SF_VIEW_SCRIPT_PAGE = "sf_view_script_page"

    /**
     * 资产管理环节｜用户进入资产管理页面（角色/场景/道具管理）
     */
    const val SF_VIEW_ASSET_MANAGEMENT_PAGE = "sf_view_asset_management_page"

    /**
     * 资产管理页的一键生成｜用户在资产管理页面点击“一键生成”按钮进行资产生成
     */
    const val SF_CLICK_ASSET_GENERATE_BUTTON = "sf_click_asset_generate_button"

    /**
     * 分镜设计环节｜用户进入创作流程中的分镜设计环节页面
     */
    const val SF_VIEW_STORYBOARD_DESIGN_PAGE = "sf_view_storyboard_design_page"

    /**
     * 分镜视频生成环节｜用户进入创作流程中的分镜视频生成环节页面
     */
    const val SF_VIEW_VIDEO_GENERATION_PAGE = "sf_view_video_generation_page"

    /**
     * 重绘分镜按钮｜在重绘已合成视频页面 或新集创作的分镜视频合成页面，用户点击左下角“重绘分镜”按钮
     */
    const val SF_CLICK_REDRAW_STORYBOARD_BUTTON = "sf_click_redraw_storyboard_button"

    /**
     * 合成按钮｜在分镜视频生成页面 和重绘视频页面的 右上角的用户点击“合成”按钮开始视频生成
     */
    const val SF_CLICK_COMBINE_VIDEO_BUTTON = "sf_click_combine_video_button"

    /**
     * 用户作品播放页重绘视频按钮｜在用户作品播放页中，用户点击“重绘视频”按钮进行视频重绘
     */
    const val SF_CLICK_WORK_REPLAY_REDRAW_BUTTON = "sf_click_work_replay_redraw_button"

    /**
     * 算力充值支付弹窗｜在创作各流程（如生成资产、分镜或合成视频）算力不足或需付费时，系统弹出支付弹窗
     */
    const val SF_SHOW_COMPUTE_RECHARGE_PAYMENT_POPUP = "sf_show_compute_recharge_payment_popup"

    /**
     * 算力充值支付弹窗购买按钮｜用户在购买弹窗点击了购买按钮
     */
    const val SF_CLICK_COMPUTE_RECHARGE_PAYMENT_BUTTON = "sf_click_compute_recharge_payment_button"

    /**
     * 首页课程报名弹窗｜首页的课程弹窗
     */
    const val SF_SHOW_COURSE_ENROLL_POPUP = "sf_show_course_enroll_popup"

    /**
     * 悬浮窗课程报名｜用户点击页面悬浮窗中的“课程报名”按钮
     */
    const val SF_CLICK_FLOATING_COURSE_ENROLL = "sf_click_floating_course_enroll"

    /**
     * H5的课程页面｜用户通过课程入口进入访问H5课程页面
     */
    const val SF_VIEW_H5_COURSE_PAGE = "sf_view_h5_course_page"

    /**
     * 课程报名支付弹窗｜用户在课程报名流程中触发并看到课程支付弹窗
     */
    const val SF_SHOW_COURSE_PAYMENT_POPUP = "sf_show_course_payment_popup"

    /**
     * 课程报名支付弹窗购买按钮｜用户点击课程购买按钮
     */
    const val SF_CLICK_COURSE_PAYMENT_BUTTON = "sf_click_course_payment_button"

    /**
     * 去发行按钮｜在作品完成页或签约入口页，用户点击“去发行”按钮
     */
    const val SF_CLICK_GO_PUBLISH_BUTTON = "sf_click_go_publish_button"

    /**
     * 提交申请按钮｜在签约/发行申请填写完成后，用户点击“提交申请”按钮
     */
    const val SF_CLICK_SUBMIT_APPLY_BUTTON = "sf_click_submit_apply_button"

    /**
     * 申请签约按钮｜在签约/发行页面中，用户点击“申请签约”按钮
     */
    const val SF_CLICK_SIGN_CONTRACT_BUTTON = "sf_click_sign_contract_button"

    /**
     * 签约/发行主页面｜用户进入签约/发行主页面
     */
    const val SF_VIEW_SIGN_PUBLISH_MAIN_PAGE = "sf_view_sign_publish_main_page"

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