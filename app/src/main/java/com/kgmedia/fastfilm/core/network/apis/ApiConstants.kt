package com.kgmedia.fastfilm.core.network.apis

import com.kgmedia.fastfilm.BuildConfig

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

    /**
     * 获取用户主页信息接口
     */
    const val GET_USER_HOME_INFO = "auth/user/info-detail"

    /**
     * 修改用户信息
     */
    const val COMPLETE_USER_DATA_INFO = "user/modifyUserInfo"

    /**
     * 获取APP升级信息
     */
    const val GET_APP_UPGRADE_INFO = "sys/getAppUpgradeInfo"

    /**
     * 第三方登录
     */
    const val LOGIN_BY_THIRD = "user/loginByThirdParty"

    /**
     * 退出登录
     */
    const val LOGOUT_APP = "auth/sign-out"

    /**
     * 注销账号
     */
    const val LOG_OFF = "auth/logout"

    /**
     * 获取用户分享链接
     */
    const val GET_USER_SHARE_URL = "user/getUserShareUrl"


    /**
     * 用户分享操作上报
     */
    const val SHARE_REPORT = "user/action/share/report"

    /**
     * 提交用户的反馈数据
     */
    const val FEED_BACK = "user/feedback/submit"

    /**
     * 分页查询生成中状态的歌曲列表-轮询使用
     */
    const val GENERATING_BOOK_LIST = "userPictureBook/current/list"

    /**
     * 会员配置
     */
    const val VIP_CONFIG = "vip/getVipConfig"


    /**
     * 用户日活记录
     */
    const val DAILY = "user/login/daily"

    /**
     * 图片风控检测
     */
    const val IMAGE_CHECK = "sys/image/risk/check"

    /**
     * 歌曲生成来源上报
     */
    const val MUSIC_GENERATE_SOURCE_REPORT = "music/generate/source/report"


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
    const val LOGIN_BY_FLASH_MOBILE = "user/loginByFlashMobile"

    /**
     * 手机登录接口--验证码方式
     */
    const val LOGIN_BY_PHONE = "/auth/register"

    /**
     * 取消注销
     */
    const val CANCEL_LOG_OFF = "user/remove/cancel"

    /**
     * 用户注销状态
     */
    const val LOG_OFF_STATUS = "user/remove/state"

    /**
     * 添加/完善用户音色
     */
    const val TIMBRE_ADD_OR_OPTIMIZE = "timbre/addUserTimbre"

    /**
     * 我的声色
     */
    const val TIMBRE_MINE_LIST = "timbre/getPageUserTimbre"

    const val USER_TIMBRE_LIST = "timbre/getUserTimbreList"

    /**
     * 更新音色名称
     */
    const val UPDATE_TIMBRE_TITLE = "timbre/updateTimbreTitle"

    /**
     * 删除音色
     */
    const val DEL_TIMBRE = "timbre/delTimbre"

    /**
     * 获取音色状态
     */
    const val TIMBRE_STATUS_GET = "timbre/getTimbreStatus"

    /**
     * 微信下单会员
     */
    const val MEMBER_WECHAT_ORDERING = "vip/wxPurchaseVip"


    /**
     * 支付宝下单会员
     */
    const val MEMBER_ALIPAY_ORDERING = "vip/alipayPurchaseVip"

    /**
     * 绘本状态（轮询）
     */
    const val GET_PICTURE_BOOK_STATUS = "userPictureBook/getPictureBookStatus"

    /**
     * 获取积分消耗明细分页
     */
    const val GET_POINT_CONSUME_PAGE = "user/getPointConsumePage"

    /**
     * 获取充值明细分页
     */
    const val GET_RECHARGE_PAGE = "user/getRechargePage"

    /**
     * 获取积分配置
     */
    const val SCORE_CONFIG = "point/getPointConfig"


    /**
     * 微信下单积分
     */
    const val WECHAT_ORDERING_POINTS = "point/wxPurchase"


    /**
     * 支付宝下单积分
     */
    const val ALIPAY_ORDERING_POINTS = "point/alipayPurchase"

    /**
     * 预生成短剧剧本大纲
     */
    const val CREATION_GENERATE_PRE = "shortDramas/structure/script/generate/pre"

    /**
     * 灵感内容优化（AI优化灵感）
     */
    const val SSE_CONTENT_OPTIMIZE = "/ai/stream/inspiration/refine"

    /**
     * 随机灵感（无入参）
     */
    const val SSE_CONTENT_RANDOM = "/ai/stream/inspiration/random"

    /**
     * 灵感故事剧本（基于灵感写剧本/灵感续写/小说续写）
     */
    const val SSE_SCRIPT_FROM_IDEA = "/ai/stream/script/from-idea"

    /**
     * 小说直接生成剧集剧本
     */
    const val SSE_SCRIPT_FROM_STORY = "/ai/stream/script/from-novel"

    /**
     * 扩写剧集剧本（小说）
     */
    const val SSE_CONTENT_AI_EXPAND = "/ai/stream/script/expand"

    /**
     * 故事梗概生成（灵感）
     */
    const val SSE_CONTENT_SYNOPSIS = "/ai/stream/script/synopsis"

    /**
     *灵感保存故事梗概（下一步创建剧本）
     */
    const val SAVE_WORK_SUMMARY = "/work/production/new/saveStorySummary"

    /**
     *小说保存故事剧集剧本（首次创建）
     */
    const val SAVE_STORY_EPISODE_SCRIPT = "/work/production/new/novel/saveStoryContent"

    /**
     *保存故事剧集剧本（灵感首创/灵感续集/小说续集）
     */
    const val SAVE_EPISODE_SCRIPT = "/work/production/new/saveStoryContent"

    /**
     *轮询剧本分镜大纲
     */
    const val GET_STORY_BOARD_LIST = "shortDramas/structure/shot/info"

    /**
     *预生成剧本分镜大纲（重新生成分镜大纲）
     */
    const val GET_STORY_BOARD_LIST_RETRY = "shortDramas/structure/shot/generate/pre"

    /**
     * 编辑故事剧本内容
     */
    const val DRAMA_CONTENT_EDIT = "shortDramas/scriptContent/edit"

    /**
     * 编辑剧本分镜内容(画面)
     */
    const val SCENE_CONTENT_EDIT = "shortDramas/structure/shot/content/edit"

    /**
     * 计算生成剧本价格（积分）
     */
    const val SHORT_DRAMAS_CALCULATE_PRICE = "shortDramas/script/generate/price/calculate"

    /**
     * 正式生成剧本(扣积分)
     */
    const val SHOT_DRAMA_GENERATE = "shortDramas/script/generate"

    /**
     *短剧风格列表
     */
    const val CREATION_STYLE_LIST = "shortDramas/style/list"

    /**
     * 优化生成剧本的角色的画面描述
     */
    const val OPTIMIZE_ROLE_IMAGE_DESC = "shortDramas/script/role/imageDesc/optimize"

    /**
     * 重新生成剧本的角色
     */
    const val ROLE_IMAGE_REGENERATE = "shortDramas/script/role/generate"

    /**
     * 查询剧本的角色标签列表
     */
    const val ROLE_TAG_LIST = "shortDramas/script/role/tag/list"

    /**
     * 场景扣费
     */
    const val SCENE_COST = "sceneDeductionFee/info"

    /**
     * 获取APP浮动窗口
     */
    const val GET_APP_FLOAT_WINDOWS = "sys/getAppFloatWindows"

    /**
     * 邀请收益信息
     */
    const val INVITE_INCOME_PAGE = "inviteUser/getInviteIncomePage"

    /**
     * 邀请用户记录分页
     */
    const val INVITE_USER_PAGE = "inviteUser/getInviteUserPage"

    /**
     * 用户提现记录（余额）
     */
    const val WITHDRAWAL_RECORD = "withdrawal/getWithdrawalRecord"

    /**
     * 合成短剧
     */
    const val SHORT_DRAMAS_SYNTHESIS = "shortDramas/shot/synthesis"

    /**
     * 合成短剧(上传合成的视频)
     */
    const val SHORT_DRAMAS_SYNTHESIS_UPLOAD = "shortDramas/shot/synthesis/upload"

    /**
     * 查询剧本信息（作品草稿详情也用这个、未上架使用）
     */
    const val SHORT_DRAMAS_INFO = "shortDramas/script/info/unlaunch"

    /**
     * 查询剧本信息（已经上架使用，看他人的剧本）
     */
    const val SHORT_DRAMAS_RELEASED_INFO = "shortDramas/script/info/launched/other"

    /**
     * 查询剧本状态信息
     */
    const val SHORT_DRAMAS_STATUS = "shortDramas/script/status"

    /**
     * 提交字幕配音
     */
    const val SHORT_DRAMAS_DUBBING = "shortDramas/confirm/subtitle/voice"

    /**
     * 配音失败-重新配音
     */
    const val SHORT_DRAMAS_DUBBING_RETRY = "shortDramas/new/dubbing"

    /**
     * 使用配音
     */
    const val SHORT_DRAMAS_DUBBING_USE = "shortDramas/use/subtitle/voice"

    /**
     * 字幕配音生成状态
     */
    const val SHORT_DRAMAS_DUBBING_STATUS = "shortDramas/subtitle/voice/status"

    /**
     * 字幕配音音色列表
     */
    const val DUBBING_VOICE_LIST = "shortDramas/subtitle/voice/list"

    /**
     * 使用历史分镜（分镜替换）
     */
    const val SHORT_DRAMAS_SEGMENT_IMAGE_REPLACE = "shortDramas/script/shot/image/history/use"

    /**
     * 查询历史分镜
     */
    const val SHORT_DRAMAS_SEGMENT_IMAGE_HISTORY = "shortDramas/script/shot/image/history"

    /**
     * 重绘分镜（重新生成分镜的图片）
     */
    const val SHORT_DRAMAS_SEGMENT_IMAGE_REGENERATE = "shortDramas/script/shot/image/regenerate"

    /**
     * 优化重绘分镜的画面描述
     */
    const val SHORT_DRAMAS_SEGMENT_IMAGE_DESC_OPTIMIZE = "shortDramas/script/shot/imageDesc/optimize"

    /**
     * 生成剧本图片(失败重试)
     */
    const val SHORT_DRAMAS_SEGMENT_IMAGE_GENERATE_RETRY = "shortDramas/script/image/generate/retry"

    /**
     *生成剧本图片(生成画面)
     */
    const val GENERATING_SEGMENT_IMAGE = "shortDramas/script/image/generate"

    /**
     * 下载短剧视频（合成后回传给后端）
     */
    const val SHORT_DRAMAS_DOWNLOAD = "shortDramas/shot/video/download"

    /**
     * 修改视频信息
     */
    const val EDIT_SHORT_DRAMA_INFO = "shortDramas/script/info/edit"

    /**
     * 删除剧本
     */
    const val DELETE_DRAMA_INFO = "shortDramas/work/remove"

    /**
     * 查询剧本的角色列表
     */
    const val CHARACTER_LIST = "shortDramas/script/role/list"

    /**
     * 分页查询我的剧本草稿列表
     */
    const val USER_SHORT_DRAMS = "user/shortDramas/script/page"

    /**
     * 获取支付宝信息(并绑定)
     */
    const val BIND_ZFB = "withdrawal/getAilUserInfo"

    /**
     * 绑定账号
     */
    const val BIND_WITHDRAW_ACCOUNT = "withdrawal/bindingAccount"

    /**
     * 余额提现
     */
    const val WITHDRAW = "withdrawal/addWithdrawal"

    /**
     * 收益提现详情
     */
    const val WITHDRAW_INFO = "withdrawal/getWithdrawalInfo"

    /**
     * 余额提现弹窗详情-钻石
     */
    const val WITHDRAW_CONFIRM = "withdrawal/getWithdrawalOpen"

    /**
     * 查询所有类型消息列表
     */
    const val MESSAGES_SUMMARY = "messages/list"

    /**
     * 系统通知
     */
    const val SYSTEM_NOTICE = "messages/system"

    /**
     * 分页查询用户追剧消息列表
     */
    const val MESSAGES_WATCHING = "messages/watching"

    /**
     * 分页查询用户喜欢消息列表
     */
    const val MESSAGES_LIKES = "messages/likes"

    /**
     * 消息全部设置成已读
     */
    const val MESSAGES_MARK_READ = "messages/markAllAsRead"

    /**
     * 查询用户未读消息数量
     */
    const val MESSAGES_UNREAD_NUM = "messages/unread/num"

    /**
     * 爆款推荐列表
     */
    const val HIT_RECOMMEND_LIST = "popular/recommend/dramas/list"

    /**
     * 爆款推荐tab
     */
    const val HIT_RECOMMEND_TABS = "popular/recommend/dramas/tab/list"

    /**
     * 攻略咨询列表
     */
    const val STRATEGY_LIST = "strategy/list"

    /**
     * 短剧配置展示数据
     */
    const val SHORT_DRAMA_STATISTICS = "sys/shortdrama/statistics"

    /**
     * 获取弹幕列表
     */
    const val DANMAKU_LIST = "appUserTip/list"

    /**
     * 用户收益信息
     */
    const val USER_REVENUE_INFORMATION = "user/profit/info"

    /**
     * 收益明细列表
     */
    const val PROFIT_DETAIL_LIST = "user/profit/detailList"

    /**
     * 解锁首次生成的短剧(首次生成的短剧付费)
     */
    const val UNLOCK_FIRST_GENERATED_SHORT_DRAMA = "shortDramas/script/generate/first/payStatus/unlock"

    /**
     * 追剧或取消追剧
     */
    const val SERIES_FOLLOW = "shortDramasSeries/follow"

    /**
     * 点赞剧本
     */
    const val LIKE_SHORT_DRAMA = "shortDramas/script/like"

    /**
     * 修改剧集价格
     */
    const val SERIES_SET_PRICE = "shortDramasSeries/modifyPrice"

    /**
     * 获取用户可续写的剧集列表
     */
    const val USER_CONTINUABLE_DRAMAS = "shortDramasSeries/userCanRewriteDramasList"

    /**
     * 续创时查询上一集内容详情
     */
    const val LATEST_DRAMAS_WORK_DETAIL = "/work/production/sequel/sequelDetail"

    /**
     * 查询我的作品详情
     */
    const val WORK_INFO = "/work/detail"

    /**
     * 流式生成帮我续写(生成续写的用户输出内容)
     */
    const val AI_CONTINUE_GENERATE = "shortDramas/script/inputContent/extend/generate/stream"

    /**
     * 分页查询我的剧集列表
     */
    const val MY_TV_SERIES_LIST = "user/shortDramas/series/page"

    /**
     * 获取剧集价格配置
     */
    const val SERIES_PRICE_CONFIG = "shortDramasSeries/priceConfig"

    /**
     * 修改剧集信息
     */
    const val SERIES_MODIFY_INFO = "shortDramasSeries/modify"

    /**
     * 上架剧集
     */
    const val SERIES_PUBLISH = "shortDramasSeries/shelf"

    /**
     * 分页查看作品剧集列表
     */
    const val EPISODE_LIST = "/work/storyPage"

    /**
     * 新用户创作体验引导条
     */
    const val NEW_USER_CREATION_GUIDE_BAR = "user/newUserCreationGuideBar"

    /**
     * 分页查询我追的剧集列表
     */
    const val THE_TV_SERIES_I_FOLLOW = "user/shortDramas/series/watching/page"

    /**
     * 分页查询我喜欢的剧本列表
     */
    const val MY_FAVORITE_SCRIPT = "user/shortDramas/script/like/page"

    /**
     * 查询我的作品数量统计信息
     */
    const val QUANTITY_STATISTICS_OF_WORKS = "user/work/num/stat"

    /**
     * 流式生成短剧剧本内容（灵感、自定义、续写使用）
     */
    const val SSE_CONTENT_GENERATE = "shortDramas/scriptContent/generate/stream"

    /**
     * 流式生成短剧剧本内容（爆款制作同款使用）
     */
    const val SSE_MAKE_SAME_CONTENT_GENERATE = "shortDramas/scriptContent/generate/popular/stream"

    /**
     * 预生成短剧剧本(流式输出完之后调)
     */
    const val DRAMA_GENERATE = "shortDramas/script/generate/pre"

    /**
     * 生成短剧来源上报(调完生成短剧剧本接口后调用)
     */
    const val DRAMA_GENERATE_SOURCE_REPORT = "shortDramas/script/generate/source/report"

    /**
     * 攻略咨询点击上报
     */
    const val TUTORIAL_CLICK_REPORT = "strategy/click"

    /**
     * 查询剧本的角色列表和标题(轮询使用)
     */
    const val DRAMA_ROLES_POLLING = "shortDramas/script/roleListAndName/polling"

    /**
     * 解锁剧集
     */
    const val UNLOCK_SHORT_DRAMA = "shortDramasSeries/unlockDramas"

    /**
     * 自动解锁剧集开关上报
     */
    const val AUTO_UNLOCK_SWITCH = "shortDramasSeries/autoUnlockSwitch"

    /**
     * 首页短剧推荐列表
     */
    const val RECOMMEND_SHORT_LIST = "shortDramas/recommend/list"

    /**
     * 发布剧本
     */
    const val PUBLISH_SHORT_DRAMA = "shortDramas/work/publish"

    /**
     * 播放剧本上报
     */
    const val SHORT_DRAMA_PLAY_REPORT = "shortDramas/play"

    /**
     * 获取订单状态
     */
    const val GET_ORDER_STATUS = "order/getOrderStatus"

    /**
     * 爆款推荐点击上报
     */
    const val RECOMMEND_CLICK_REPORT = "popular/recommend/dramas/click"

    /**
     * 获取正在生成的短剧信息
     */
    const val GET_GENERATING_INFO = "shortDramas/script/generating/info"

    /**
     * 关闭正在生成的短剧
     */
    const val CLOSE_GENERATING_DRAMAS = "shortDramas/script/generating/close"

    /**
     * 新增剧本的角色
     */
    const val DRAMAS_ADD_ROLE = "/shortDramas/script/role/create"

    /**
     * 删除剧本的角色
     */
    const val DRAMAS_DEL_ROLE = "/shortDramas/script/role/remove"

    const val APP_BANNERS = "/app/home/banner/list"

    /**
     * 获取随机灵感示例
     */
    const val INSPIRATION_QUICK_LIST = "/work/config/getInspirationExamples"

    /**
     * 使用历史角色
     */
    const val HISTORY_ROLE_SWITCH = "/shortDramas/script/role/history/use"

    /**
     * 查询历史角色
     */
    const val GET_HISTORY_ROLES = "/shortDramas/script/role/history"

    /**
     * 首页作品列表
     */
    const val RECOMMEND_DRAMA_LIST = "/app/home/work/page"

    /**
     * 获取集数/视频比例/视频风格配置
     */
    const val VIDEO_SET_CONFIG = "/work/config/list"

    /**
     * 查询资产参考图列表
     */
    const val GET_HISTORY_REFERENCE_IMAGE = "/work/asset/getReferenceImage"

    /**
     * 编辑我的作品信息
     */
    const val UPDATE_WORK_INFO = "/work/update"

    /**
     * 作品播放量计数
     */
    const val PLAY_EVENT_REPORT = "/app/home/work/playCount"

    /**
     * 轮询查询全部资产
     */
    const val WORK_ASSET_ALL_STATUS = "/ai/task/query/asset/all"

    /**
     * 轮询查询单个资产
     */
    const val WORK_ASSET_SINGLE_STATUS = "/ai/task/query/asset/single"

    /**
     * 场景列表
     */
    const val WORK_ASSET_SCENE_LIST = "/work/asset/scene/list"

    /**
     * 场景保存
     */
    const val WORK_ASSET_SCENE_SAVE = "/work/asset/scene/save"

    /**
     * 场景单个删除
     */
    const val WORK_ASSET_SCENE_DELETE = "/work/asset/scene/delete"

    /**
     * 角色列表
     */
    const val WORK_ASSET_ROLE_LIST = "/work/asset/role/list"

    /**
     * 角色保存
     */
    const val WORK_ASSET_ROLE_SAVE = "/work/asset/role/save"

    /**
     * 角色单个删除
     */
    const val WORK_ASSET_ROLE_DELETE = "/work/asset/role/delete"

    /**
     * 道具列表
     */
    const val WORK_ASSET_PROP_LIST = "/work/asset/prop/list"

    /**
     * 道具保存
     */
    const val WORK_ASSET_PROP_SAVE = "/work/asset/prop/save"

    /**
     * 道具单个删除
     */
    const val WORK_ASSET_PROP_DELETE = "/work/asset/prop/delete"

    /**
     * 生成单个资产图片（重新生成/生成）
     */
    const val WORK_ASSET_SINGLE_GENERATE = "ai/asset/generation/single/image"

    /**
     * 轮询查询单个分镜图片
     */
    const val WORK_STORYBOARD_SINGLE_STATUS = "/ai/task/query/storyboard/single"

    /**
     * 分镜设计列表
     */
    const val WORK_STORYBOARD_LIST = "/work/storyboard/designList"

    /**
     * 分镜保存
     */
    const val WORK_STORYBOARD_SAVE = "/work/storyboard/save"

    /**
     * 分镜单个删除
     */
    const val WORK_STORYBOARD_DELETE = "/work/storyboard/delete"

    /**
     * 生成单个分镜图片（重新生成/点击生成）
     */
    const val WORK_STORYBOARD_SINGLE_GENERATE = "/ai/storyboard/generation/image"

    /**
     * 生成分镜视频
     */
    const val WORK_STORYBOARD_VIDEO_GENERATE = "/ai/storyboard/generation/video"

    /**
     * 举报提交
     */
    const val REPORT_SUBMISSION="app/report/save"

    /**
     * 分页查询我的作品列表
     */
    const val MY_WORK="work/page"
}