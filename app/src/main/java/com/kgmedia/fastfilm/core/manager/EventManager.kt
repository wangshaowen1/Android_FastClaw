package com.kgmedia.fastfilm.core.manager

import com.kgmedia.fastfilm.App
import com.kgmedia.fastfilm.common.ext.ioScope
import com.kgmedia.fastfilm.core.User
import com.kgmedia.fastfilm.core.network.apis.ApiFactory
import com.kgmedia.fastfilm.core.network.apis.request.DramasIdBody
import com.kgmedia.fastfilm.core.network.apis.request.MusicGenerateSourceBody
import com.kgmedia.fastfilm.core.network.apis.request.PopularRecommendDramasIdBody
import com.kgmedia.fastfilm.core.network.apis.request.ShareReportBody
import com.growingio.android.sdk.autotrack.GrowingAutotracker
import com.nhsd.basic.ext.launchNoCrash
import com.nhsd.basic.utils.Logger
import com.umeng.analytics.MobclickAgent


/**
 * 自定义事件管理器（事件埋点）
 * @author xiaoowuu
 */
object EventManager {

    var playerInSquare = false
    private val apiService = ApiFactory.apiService


    /**
     * 后台接口埋点
     * @param event 事件名称
     * @param newUserOnly 仅24小时新用户上报
     */
    private fun backendApiEvent(event: String?, newUserOnly: Boolean = false) {
        if (event == null) return
        if (newUserOnly) {
            if (User.get().is24HourNewUser().not()) return
            onAppEvent(eventCode = event)
            return
        }
        onAppEvent(eventCode = event)
    }

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
     * App功能使用数据
     *
     * @param eventCode 事件编码
     * @param bizId 业务id
     * @param params 任何额外的自定义数据，可根据业务需求添加任意字段
     */
    @JvmStatic
    fun onAppEvent(
        eventCode: String? = null,
        params: Map<String, Any?>? = null
    ) {
        if (eventCode == null) return
        val map = params?.mapValues { it.value?.toString() ?: "" }
        Logger.d("EventManager onAppEvent eventCode=$eventCode params=$map")
        if (map == null) {
            MobclickAgent.onEvent(App.instance, eventCode)
        } else {
            MobclickAgent.onEvent(App.instance, eventCode, map)
        }
//        ioScope.launchNoCrash {
//            val page = ActivityUtils.getActivityList()?.take(5)?.reversed()
//                ?.joinToString("->") { it.javaClass.simpleName }
//            val body = EventTrackingBody(
//                eventCode = eventCode,
//                bizId = bizId,
//                customData = customData,
//                pageLinking = page
//            )
//            ApiFactory.eventService.eventTracking(body)
//        }
    }

    suspend fun onDramaShortStart(dramasId: String) {
        ApiFactory.apiService.shortDramaPlayReport(DramasIdBody(dramasId))
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
     * 金果｜点击解锁作品弹窗充值解锁按钮
     * 点击解锁作品弹窗，充值解锁按钮，点击时上报；上报支付的金额和支付类型
     * 通用｜支付的金额	all_pay_money_yuan	小数
     * 通用｜支付方式	all_pay_type	字符串
     */
    const val JG_CLICK_POPUP_UNLOCK_WORK_RECHARGE_UNLOCK_BUTTON =
        "jg_click_popup_unlock_work_recharge_unlock_button"

    /**
     * 金果｜购买创作会员
     * 成功购买创作会员，客户端接收到购买成功的回调的时候上报，上报购买的 VIP 类型；支付的类型，支付的金额
     *
     * 通用｜支付的金额	all_pay_money_yuan	小数
     * 金果｜会员类型	jg_vip_type	整数
     * 通用｜支付方式	all_pay_type 字符串
     *
     */
    const val JG_BUY_VIP = "jg_buy_vip"

    /**
     * 金果｜充值积分
     * 成功充值积分，客户端接收到购买成功的回调的时候上报，上报充值的类型，上报充值的积分数，支付的金额
     *
     * 通用｜支付的金额	all_pay_money_yuan	小数
     * 通用｜数量INT	all_count_int	整数
     * 通用｜支付方式	all_pay_type	字符串
     */
    const val JG_BUY_INTEGRAL = "jg_buy_integral"

    /**
     * 金果｜漫剧播放页面有效浏览时长15s
     * 漫剧播放页面有效浏览时长达到15秒时候上报
     */
    const val JG_COMIC_PLAY_PAGE_EFFECTIVE_VIEW_DURATION =
        "jg_comic_play_page_effective_view_duration"

    /**
     * 金果｜点击自己写故事提高字数限制
     * 点击自己写故事提高字数限制，点击时上报
     */
    const val JG_CLICK_WRITE_OWN_STORY_INCREASE_WORD_LIMIT =
        "jg_click_write_own_story_increase_word_limit"

    /**
     * 金果｜显示会员中心的页面
     * 显示会员中的页面，显示时上报，上报当前用户端会员类型
     *
     * 金果｜会员类型	jg_vip_type	整数
     */
    const val JG_VIEW_VIP_PAGE = "jg_view_vip_page"

    /**
     * 金果｜点击会员页面的立即开通按钮
     * 点击会员页面的立即开通按钮，点击时上报
     *
     * 金果｜会员类型	jg_vip_type	整数
     */
    const val JG_CLICK_MY_PAGE_OPEN_NOW_BUTTON = "jg_click_my_page_open_now_button"

    /**
     * 金果｜点击解锁剧集弹窗点击立即解锁按钮
     * 点击解锁剧集弹窗点击立即解锁按钮，点击时上报积分数量；上报是否解锁成功
     *
     * 通用｜是否解锁成功	all_is_unlocked	字符串
     * 通用｜数量INT	all_count_int	整数
     */
    const val JG_CLICK_POPUP_UNLOCK_SERIES_UNLOCK_NOW_BUTTON =
        "jg_click_popup_unlock_series_unlock_now_button"

    /**
     * 金果｜显示剧集列表
     * 显示剧集列表，显示时上报
     */
    const val JG_VIEW_SERIES_PAGE = "jg_view_series_page"

    /**
     * 金果｜显示解锁作品弹窗
     * 显示解锁作品弹窗，显示时上报
     */
    const val JG_POPUP_UNLOCK_WORK_RECHARGE_UNLOCK = "jg_popup_unlock_work_recharge_unlock"

    /**
     * 金果｜点击合成视频按钮
     * 在分镜生成过程页面中点击合成视频按钮，点击时上报
     */
    const val JG_CLICK_SYNTHESIZE_VIDEO_BUTTON = "jg_click_synthesize_video_button"

    /**
     * 金果｜显示创作分镜页面
     * 显示分镜页面，显示/点击时上报，上报该集/章的第几个分镜数
     *
     * 通用｜数量INT	all_count_int	整数
     */
    const val JG_VIEW_WRITE_STORYBOARD_PAGE = "jg_view_write_storyboard_page"

    /**
     *金果｜显示生成分镜过程页面
     * 显示生成分镜过程页面，显示时上报
     */
    const val JG_VIEW_GENERATE_STORYBOARD_PROCESS_PAGE = "jg_view_generate_storyboard_process_page"

    /**
     * 金果｜点击确认生成按钮
     * 用户点击分镜列表的确认生成按钮，点击时上报，上报金豆数/是否扣费成功
     *
     * 通用｜是否扣费成功	all_is_consume	字符串
     * 通用｜数量INT	all_count_int	整数
     */
    const val JG_CLICK_GENERATE_IMAGE_POPUP_CONFIRM_BUTTON =
        "jg_click_generate_image_popup_confirm_button"

    /**
     * 金果｜点击生成分镜页面的确认生成按钮
     * 用户点击生成分镜页面的确认生成按钮，点击时上报；上报分镜数
     *
     * 通用｜数量INT	all_count_int	整数
     */
    const val JG_CLICK_GENERATE_STORYBOARD_PAGE_CONFIRM_BUTTON =
        "jg_click_generate_storyboard_page_confirm_button"

    /**
     * 金果｜显示生成分镜页面
     * 显示生成分镜页面，显示时上报
     */
    const val JG_VIEW_GENERATE_STORYBOARD_PAGE = "jg_view_generate_storyboard_page"

    /**
     * 金果｜点击续写故事的去续写按钮
     * 用户点击续写故事的去续写的按钮，点击时上报，上报小说Id
     *
     * 通用｜通用ID	all_general_id	字符串
     */
    const val JG_CLICK_CONTINUE_STORY_ACTION_BUTTON = "jg_click_continue_story_action_button"

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
     * 金果｜点击爆款页面的咨询攻略分类按钮
     * 用户点击爆款页面的咨询攻略的cell，点击时上报，上报攻略Id
     * 通用｜通用ID	all_general_id	字符串
     */
    const val JG_CLICK_FADDISH_PAGE_CONSULT_STRATEGY_CATEGORY_BUTTON =
        "jg_click_faddish_page_consult_strategy_category_button"

    /**
     * 金果｜用户登录账号
     * 用户成功登录手机号/微信号/AppleID后上报，是否首次注册登录用户
     *
     * 通用｜是否为新用户	all_is_new_user	字符串
     * 通用｜用户登录类型	all_login_type	字符串
     */
    const val JG_ACTION_USER_LOGIN = "jg_action_user_login"

    /**
     * 金果｜显示我的余额页面
     * 显示我的余额页面，显示时上报
     */
    const val JG_VIEW_MY_BALANCE_PAGE = "jg_view_my_balance_page"

    /**
     * 金果｜点击我的页面的会员入口
     * 点击我的页面的会员入口，点击时上报
     */
    const val JG_CLICK_MY_PAGE_VIP_ENTRY = "jg_click_my_page_vip_entry"

    /**
     * 金果｜点击角色确认页面的生成画面按钮
     * 用户点击角色确认页面的生成画面按钮，点击时上报
     */
    const val JG_CLICK_CHARACTER_CONFIRM_PAGE_GENERATE_IMAGE_BUTTON =
        "jg_click_character_confirm_page_generate_image_button"

    /**
     * 金果｜显示角色确认页面
     * 显示角色确认页面，显示时上报
     */
    const val JG_VIEW_CHARACTER_CONFIRM_PAGE = "jg_view_character_confirm_page"

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
     * 金果｜点击自己写故事的按钮
     * 用户点击自己写故事的按钮，点击时上报
     */
    const val JG_CLICK_WRITE_OWN_STORY_BUTTON = "jg_click_write_own_story_button"

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
     * 金果｜访问消息Tab页面
     * 用户访问消息TAB页面，显示的时候上报
     */
    const val JG_VIEW_MESSAGE_TAB_PAGE = "jg_view_message_tab_page"

    /**
     * 金果｜点击爆款页面的咨询攻略的cell
     * 用户点击爆款页面的咨询攻略的cell，点击时上报；上报咨询攻略Id
     * 通用｜通用ID	all_general_id	字符串
     */
    const val JG_CLICK_FADDISH_PAGE_CONSULT_STRATEGY_CELL =
        "jg_click_faddish_page_consult_strategy_cell"

    /**
     * 金果｜点击爆款页面的cell
     * 用户点击爆款页面的cell，做同款按钮，点击时上报，上报爆款Id
     *  通用｜通用ID	all_general_id	字符串
     */
    const val JG_CLICK_FADDISH_PAGE_CELL = "jg_click_faddish_page_cell"

    /**
     * 金果｜访问爆款Tab页面
     * 用户访问爆款TAB页面，显示的时候上报
     */
    const val JG_VIEW_FADDISH_TAB_PAGE = "jg_view_faddish_tab_page"

    /**
     * 金果｜浏览首页推荐视频
     * 用户浏览首页推荐视频时，记录上报；上报推荐视频Id
     * 通用｜通用ID	all_general_id	字符串
     */
    const val JG_VIEW_HOME_RECOMMEND_VIDEO = "jg_view_home_recommend_video"

    /**
     * 金果｜访问首页Tab页面
     * 用户访问首页TAB页面，显示的时候上报
     */
    const val JG_VIEW_HOME_TAB_PAGE = "jg_view_home_tab_page"

    /**
     * 金果｜点击用户完善资料弹窗的确认按钮
     * 用户点击用户完善资料弹窗的确认按钮，点击时上报
     */
    const val JG_CLICK_COMPLETE_PROFILE_POPUP_CONFIRM_BUTTON =
        "jg_click_complete_profile_popup_confirm_button"

    /**
     * 金果｜访问登录页面
     * 用户访问登录页面的时候上报，不区分登录类型
     */
    const val JG_VIEW_LOGIN_PAGE = "jg_view_login_page"

    /**
     * 金果｜显示创作收益页面
     * 用户访问创作收益的页面，上报用户会员的类型
     * 金果｜会员类型	jg_vip_type	整数
     */
    const val JG_VIEW_WRITE_EARNINGS_PAGE = "jg_view_write_earnings_page"

    /**
     * 金果｜点击新增角色页面的新增角色按钮
     * 用户点击新增角色页面的新增角色按钮
     */
    const val JG_CLICK_ADD_ROLE_PAGE_ADD_BUTTON = "jg_click_add_role_page_add_button"

    /**
     * 金果｜点击创作中确认角色新增角色按钮
     * 用户在创作视频中点击新增角色按钮
     */
    const val JG_CLICK_WRITE_ADD_ROLE_BUTTON = "jg_click_write_add_role_button"

    /**
     * 金果｜点击视频播放页面中展开章节的按钮
     * 用户点击播放页面中展开章节的按钮，上报视频Id
     * 通用｜通用ID	all_general_id	字符串
     */
    const val JG_CLICK_PLAY_VIEW_SPREAD_CHAPTER_BUTTON = "jg_click_play_view_spread_chapter_button"

    /**
     * 金果｜点击视频播放页面中分享按钮
     * 用户点击播放视频页面中分享按钮，上报视频Id
     *  通用｜通用ID	all_general_id	字符串
     */
    const val JG_CLICK_PLAY_VIEW_SHARE_BUTTON = "jg_click_play_view_share_button"

    /**
     * 金果｜点击视频播放页面中喜欢按钮
     * 用户点击播放视频页面中喜欢按钮，上报视频Id
     *  通用｜通用ID	all_general_id	字符串
     */
    const val JG_CLICK_PLAY_VIEW_LIKE_BUTTON = "jg_click_play_view_like_button"

    /**
     * 金果｜点击视频播放页面中追更按钮
     * 用户点击播放视频页面中追更按钮，上报视频Id
     *  通用｜通用ID	all_general_id	字符串
     */
    const val JG_CLICK_PLAY_VIEW_SUBSCRIBE_BUTTON = "jg_click_play_view_subscribe_button"

    /**
     * 金果｜点击重绘分镜的按钮
     * 用户点击重绘分镜的按钮，上报扣除积分的数量
     * 通用｜数量INT	all_count_int	整数
     */
    const val JG_CLICK_REDRAW_THE_STORYBOARDS_BUTTON = "jg_click_redraw_the_storyboards_button"

    /**
     * 金果｜显示作品设置收费的半屏弹窗
     * 显示作品设置收费的半屏弹窗；上报剧集Id；上报价格；上报会员类型
     * 通用｜通用ID	all_general_id	上报剧集Id
     */
    const val JG_VIEW_SET_UP_CHARGES_SHEET = "jg_view_set_up_charges_sheet"

    /**
     * 金果｜点击合成视频后的发布作品按钮
     * 点击合成视频后的发布作品按钮，上报视频Id
     *  通用｜通用ID	all_general_id	字符串
     */
    const val JG_CLICK_SYNTHETIC_VIDEO_PUBLISH_BUTTON = "jg_click_synthetic_video_publish_button"

    /**
     * 金果｜显示我的的余额购买积分的页面
     * 用户访问我的余额购买积分的页面
     */
    const val JG_VIEW_BUY_INTEGRAL_PAGE = "jg_view_buy_integral_page"

    /**
     * 金果｜点击作品设置收费的开通会员按钮
     * 点击作品设置收费的半屏弹窗的开通会员按钮；上报剧集Id；
     * 通用｜通用ID	all_general_id	字符串
     */
    const val JG_CLICK_SET_UP_CHARGES_SUBSCRIPTION_VIP_BUTTON = "jg_click_set_up_charges_subscription_vip_button"

    /**
     * 金果｜点击作品设置收费的立即上架按钮
     * 点击作品设置收费的立即上架按钮；上报剧集Id；上报设置价格；上报会员类型；这两个按钮都是同一个事件
     * 通用｜通用ID	all_general_id	字符串
     * 金果｜会员类型	jg_vip_type	整数
     * 通用｜支付的金额	all_pay_money_yuan	小数
     */
    const val JG_CLICK_SET_UP_CHARGES_IMMEDIATE_BUTTON = "jg_click_set_up_charges_immediate_button"
}


object GrowingIO {

    /**
     * 通用｜数量  表达事件附带的数量属性，如：领取金豆的数量/K币的数量等
     */
    const val KEY_ALL_COUNT_INT = "all_count_int"

    /**
     * 通用｜是否解锁成功
     *     是否解锁成功： "yes" = 成功； "no" = 不成功；
     */
    const val ALL_IS_UNLOCKED = "all_is_unlocked"

    /**
     * 通用｜是否扣费成功
     * 是否扣费/消耗成功，"yes"=成功；"no"=不成功
     */
    const val KEY_ALL_IS_CONSUME = "all_is_consume"

    /**
     * 通用｜通用ID
     * 此事件属性为全局属性，默认对所有事件生效。
     */
    const val KEY_ALL_GENERAL_ID = "all_general_id"

    /**
     * 通用｜是否为新用户
     * 用户登录时候是否为首次注册账号，如果是="yes"，如果是老用户登录非首次注册="no"
     */
    const val KEY_ALL_IS_NEW_USER = "all_is_new_user"

    /**
     * 通用｜访问来源页面
     * 用户访问到目标页面的上一级页面（来源页面），如用户通过页面A的按钮点击进入到页面B，则记录页面B的来源页面为页面A
     */
    const val KEY_ALL_FROM_PAGE = "all_from_page"

    /**
     * 通用｜是否首次购买
     * "no" = 非首次; "yes" = 首次购买;
     */
    const val KEY_ALL_IS_FIRST_BUY = "all_is_first_buy"

    /**
     * 通用｜用户登录类型
     * 用户登录类型： unknown = 其他或未知 phone = 手机号 wechat = 微信 AppleID = AppleID
     */
    const val KEY_ALL_LOGIN_TYPE = "all_login_type"

    /**
     * 金果｜购买积分支付的金额
     * 金果｜购买积分支付的金额，单位元，支持2位小数
     */
    const val JG_BUY_INTEGRAL_PAY_YUAN = "jg_buy_integral_pay_yuan"

    /**
     * 金果｜购买会员支付的钱
     * 购买金果会员支付的钱，单位是元，支持2位小数
     */
    const val JG_BUY_VIP_PAY_YUAN = "jg_buy_vip_pay_yuan"

    /**
     * 金果｜有效时长
     * 0 = 浏览时长小于15秒 1 = 浏览时长15-60秒 2 = 浏览时长大于60秒
     */
    const val JG_EFFECTIVE_DURATION = "jg_effective_duration"

    /**
     * 金果｜是否点击
     * "no" = 未点击 "yes" = 点击;
     */
    const val JG_IS_CLICKED = "jg_is_clicked"

    /**
     * 金果｜会员类型
     * 0 = 非会员 3 = 高级会员 2 = 中级会员 1 = 初级会员
     */
    const val JG_VIP_TYPE = "jg_vip_type"

    /**
     * 金果｜支付方式
     * 支付宝 = zhifubao 微信=weixin
     */
    const val JG_PAYMENT_METHOD = "jg_payment_method"

    /**
     * 金果｜购买积分数量
     * 用户购买积分的数量
     */
    const val JG_BUY_POINTS_AMOUNT = "jg_buy_points_amount"

    /**
     * 金果｜显示分镜页面
     * 显示分镜页面，显示时上报，记录分镜页面生成状态，成功或失败，0 - 成功 ， 1 = 失败
     */
    const val VIEW_STORYBOARD_1 = "view_storyboard_1"

    /**
     * 金果｜是否生成成功
     * "no" = 未生成 "yes" = 生成成功;
     */
    const val JG_IS_GENERATE_SUCCESS = "jg_is_generate_success"

    /**
     * 金果｜用户实际支付的金额，单位是元，支持2位小数，请注意单位数值转换！
     * 此事件属性为全局属性，默认对所有事件生效
     */
    const val ALL_PAY_MONEY_YUAN = "all_pay_money_yuan"

    /**
     * 金果｜支付方式：
     * 微信支付 = "weixinpay"； 支付宝 = "alipay"； 苹果支付/内购 = "applepay"
     */
    const val ALL_PAY_TYPE = "all_pay_type"

}