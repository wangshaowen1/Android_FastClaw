package com.kgmedia.fastfilm.core.network.apis

import com.kgmedia.fastfilm.core.network.SSEvent
import com.kgmedia.fastfilm.core.network.apis.entity.BannerInfo
import com.kgmedia.fastfilm.core.network.apis.entity.CharacterInfo
import com.kgmedia.fastfilm.core.network.apis.entity.ContentInfo
import com.kgmedia.fastfilm.core.network.apis.entity.CreationStyleInfo
import com.kgmedia.fastfilm.core.network.apis.entity.DanmakuItem
import com.kgmedia.fastfilm.core.network.apis.entity.WorkInfo
import com.kgmedia.fastfilm.core.network.apis.entity.DramasSeriesInfo
import com.kgmedia.fastfilm.core.network.apis.entity.DramasTimbreInfo
import com.kgmedia.fastfilm.core.network.apis.entity.DubbingGenStatusInfo
import com.kgmedia.fastfilm.core.network.apis.entity.EpisodeInfo
import com.kgmedia.fastfilm.core.network.apis.entity.GuideBarStatusInfo
import com.kgmedia.fastfilm.core.network.apis.entity.HitRecommendInfo
import com.kgmedia.fastfilm.core.network.apis.entity.HitTabInfo
import com.kgmedia.fastfilm.core.network.apis.entity.InspirationInfo
import com.kgmedia.fastfilm.core.network.apis.entity.LogOffStatusInfo
import com.kgmedia.fastfilm.core.network.apis.entity.MyFavoriteScriptInfo
import com.kgmedia.fastfilm.core.network.apis.entity.MyTvSeriesListInfo
import com.kgmedia.fastfilm.core.network.apis.entity.OrderStatusInfo
import com.kgmedia.fastfilm.core.network.apis.entity.PointConsumePageInfo
import com.kgmedia.fastfilm.core.network.apis.entity.ProfitDetailInfo
import com.kgmedia.fastfilm.core.network.apis.entity.QuantityStatisticsOfWorksInfo
import com.kgmedia.fastfilm.core.network.apis.entity.ReferenceImageInfo
import com.kgmedia.fastfilm.core.network.apis.entity.RoleTagInfo
import com.kgmedia.fastfilm.core.network.apis.entity.SegmentImageHistoryInfo
import com.kgmedia.fastfilm.core.network.apis.entity.SeriesPriceConfigInfo
import com.kgmedia.fastfilm.core.network.apis.entity.ShortDramaInfo
import com.kgmedia.fastfilm.core.network.apis.entity.ShortDramaStatusInfo
import com.kgmedia.fastfilm.core.network.apis.entity.ShortDramasStatisticsData
import com.kgmedia.fastfilm.core.network.apis.entity.SimplePropInfo
import com.kgmedia.fastfilm.core.network.apis.entity.SimpleRoleInfo
import com.kgmedia.fastfilm.core.network.apis.entity.SimpleSceneInfo
import com.kgmedia.fastfilm.core.network.apis.entity.StrategyInfo
import com.kgmedia.fastfilm.core.network.apis.entity.SystemMessageLikeInfo
import com.kgmedia.fastfilm.core.network.apis.entity.SystemMessageWatchingInfo
import com.kgmedia.fastfilm.core.network.apis.entity.SystemNoticeInfo
import com.kgmedia.fastfilm.core.network.apis.entity.TaskStatusInfo
import com.kgmedia.fastfilm.core.network.apis.entity.TheTvSeriesIFollowInfo
import com.kgmedia.fastfilm.core.network.apis.entity.TimbreInfo
import com.kgmedia.fastfilm.core.network.apis.entity.UserRevenueInformationInfo
import com.kgmedia.fastfilm.core.network.apis.entity.UserShareUrlInfo
import com.kgmedia.fastfilm.core.network.apis.entity.VideoConfigData
import com.kgmedia.fastfilm.core.network.apis.entity.VipConfigInfo
import com.kgmedia.fastfilm.core.network.apis.entity.WithdrawConfirmInfo
import com.kgmedia.fastfilm.core.network.apis.entity.WithdrawInfo
import com.kgmedia.fastfilm.core.network.apis.entity.WithdrawSuccessInfo
import com.kgmedia.fastfilm.core.network.apis.request.AIExpandBody
import com.kgmedia.fastfilm.core.network.apis.request.AISynopsisBody
import com.kgmedia.fastfilm.core.network.apis.request.AppFloatWindowsBody
import com.kgmedia.fastfilm.core.network.apis.request.BannerBody
import com.kgmedia.fastfilm.core.network.apis.request.BindPhoneBody
import com.kgmedia.fastfilm.core.network.apis.request.BindWithdrawAccountBody
import com.kgmedia.fastfilm.core.network.apis.request.BindZfbBody
import com.kgmedia.fastfilm.core.network.apis.request.CompleteUserDataInfoBody
import com.kgmedia.fastfilm.core.network.apis.request.ContinueGenerateBody
import com.kgmedia.fastfilm.core.network.apis.request.CreateStoryWorkBody
import com.kgmedia.fastfilm.core.network.apis.request.CreateWorkBody
import com.kgmedia.fastfilm.core.network.apis.request.CreationGeneratePreBody
import com.kgmedia.fastfilm.core.network.apis.request.DanmakuBody
import com.kgmedia.fastfilm.core.network.apis.request.DownloadReportBody
import com.kgmedia.fastfilm.core.network.apis.request.DramaContentBody
import com.kgmedia.fastfilm.core.network.apis.request.DramaGenerateBody
import com.kgmedia.fastfilm.core.network.apis.request.DramaGenerateSourceReportBody
import com.kgmedia.fastfilm.core.network.apis.request.DramasIdBody
import com.kgmedia.fastfilm.core.network.apis.request.DramasSeriesId
import com.kgmedia.fastfilm.core.network.apis.request.WorkIdPagedBody
import com.kgmedia.fastfilm.core.network.apis.request.DramasTimbreBody
import com.kgmedia.fastfilm.core.network.apis.request.DramasTimbrePagedBody
import com.kgmedia.fastfilm.core.network.apis.request.EditDramaBody
import com.kgmedia.fastfilm.core.network.apis.request.FeedbackBody
import com.kgmedia.fastfilm.core.network.apis.request.GenerateAssetBody
import com.kgmedia.fastfilm.core.network.apis.request.HitRecommendPagedBody
import com.kgmedia.fastfilm.core.network.apis.request.IdsBody
import com.kgmedia.fastfilm.core.network.apis.request.ImageUrlBody
import com.kgmedia.fastfilm.core.network.apis.request.InputContentBody
import com.kgmedia.fastfilm.core.network.apis.request.LoginByFlashMobileBody
import com.kgmedia.fastfilm.core.network.apis.request.LoginByPhoneBody
import com.kgmedia.fastfilm.core.network.apis.request.LoginByThirdBody
import com.kgmedia.fastfilm.core.network.apis.request.MemberPaymentBody
import com.kgmedia.fastfilm.core.network.apis.request.MessagesTypeBody
import com.kgmedia.fastfilm.core.network.apis.request.MusicGenerateSourceBody
import com.kgmedia.fastfilm.core.network.apis.request.MyTvSeriesBody
import com.kgmedia.fastfilm.core.network.apis.request.OptimizeContentData
import com.kgmedia.fastfilm.core.network.apis.request.OptimizeRoleImageDescBody
import com.kgmedia.fastfilm.core.network.apis.request.OrderStatusBody
import com.kgmedia.fastfilm.core.network.apis.request.OrderingPointsBody
import com.kgmedia.fastfilm.core.network.apis.request.PagedBody
import com.kgmedia.fastfilm.core.network.apis.request.PictureBookId
import com.kgmedia.fastfilm.core.network.apis.request.PlayEventBody
import com.kgmedia.fastfilm.core.network.apis.request.PointConfigBody
import com.kgmedia.fastfilm.core.network.apis.request.PopularRecommendDramasIdBody
import com.kgmedia.fastfilm.core.network.apis.request.QueryAssetBody
import com.kgmedia.fastfilm.core.network.apis.request.RecommendShortsBody
import com.kgmedia.fastfilm.core.network.apis.request.ReferenceImageBody
import com.kgmedia.fastfilm.core.network.apis.request.ReportSubmissionBody
import com.kgmedia.fastfilm.core.network.apis.request.RoleIdBody
import com.kgmedia.fastfilm.core.network.apis.request.RoleImageRegenerateBody
import com.kgmedia.fastfilm.core.network.apis.request.RoleSwitchBody
import com.kgmedia.fastfilm.core.network.apis.request.SSEContentBody
import com.kgmedia.fastfilm.core.network.apis.request.SaveEpisodeScriptBody
import com.kgmedia.fastfilm.core.network.apis.request.SavePropBody
import com.kgmedia.fastfilm.core.network.apis.request.SaveRoleBody
import com.kgmedia.fastfilm.core.network.apis.request.SaveSceneBody
import com.kgmedia.fastfilm.core.network.apis.request.SceneContentEditBody
import com.kgmedia.fastfilm.core.network.apis.request.SceneCostBody
import com.kgmedia.fastfilm.core.network.apis.request.SegmentImageDescOptimizeBody
import com.kgmedia.fastfilm.core.network.apis.request.SegmentImageRegenerateBody
import com.kgmedia.fastfilm.core.network.apis.request.SegmentImageReplaceBody
import com.kgmedia.fastfilm.core.network.apis.request.SegmentImageRetryBody
import com.kgmedia.fastfilm.core.network.apis.request.SendSmsValidateCodeBody
import com.kgmedia.fastfilm.core.network.apis.request.SeriesFollowBody
import com.kgmedia.fastfilm.core.network.apis.request.SeriesModifyInfoBody
import com.kgmedia.fastfilm.core.network.apis.request.SeriesSetPriceBody
import com.kgmedia.fastfilm.core.network.apis.request.ShareReportBody
import com.kgmedia.fastfilm.core.network.apis.request.ShortLikeBody
import com.kgmedia.fastfilm.core.network.apis.request.ShotSegmentIdBody
import com.kgmedia.fastfilm.core.network.apis.request.StrategyInfoIdBody
import com.kgmedia.fastfilm.core.network.apis.request.SynthesisUploadBody
import com.kgmedia.fastfilm.core.network.apis.request.TaskIdBody
import com.kgmedia.fastfilm.core.network.apis.request.TimbreAddOrOptimizeBody
import com.kgmedia.fastfilm.core.network.apis.request.TipOffBody
import com.kgmedia.fastfilm.core.network.apis.request.UpdateTimbreTitleBody
import com.kgmedia.fastfilm.core.network.apis.request.UserShareUrlBody
import com.kgmedia.fastfilm.core.network.apis.request.UserTimbreIdBody
import com.kgmedia.fastfilm.core.network.apis.request.UserTokenBody
import com.kgmedia.fastfilm.core.network.apis.request.WithdrawBody
import com.kgmedia.fastfilm.core.network.apis.request.WorkIdBody
import com.kgmedia.fastfilm.core.network.apis.request.WorkInfoUpdateBody
import com.kgmedia.fastfilm.core.network.apis.response.AlipayData
import com.kgmedia.fastfilm.core.network.apis.response.AppConfigData
import com.kgmedia.fastfilm.core.network.apis.response.AppFloatWindowsInfo
import com.kgmedia.fastfilm.core.network.apis.response.BalanceData
import com.kgmedia.fastfilm.core.network.apis.response.CharacterListData
import com.kgmedia.fastfilm.core.network.apis.response.DramasIdData
import com.kgmedia.fastfilm.core.network.apis.response.DramasInfoData
import com.kgmedia.fastfilm.core.network.apis.response.DramasSeriesIdData
import com.kgmedia.fastfilm.core.network.apis.response.FollowingData
import com.kgmedia.fastfilm.core.network.apis.response.InviteIncomePageInfo
import com.kgmedia.fastfilm.core.network.apis.response.InviteUserPageInfo
import com.kgmedia.fastfilm.core.network.apis.response.MessageSummaryData
import com.kgmedia.fastfilm.core.network.apis.response.MessagesUnreadNumData
import com.kgmedia.fastfilm.core.network.apis.response.PictureBook
import com.kgmedia.fastfilm.core.network.apis.response.PictureBookGenerateStatusData
import com.kgmedia.fastfilm.core.network.apis.response.PointAmountData
import com.kgmedia.fastfilm.core.network.apis.response.RechargePageInfo
import com.kgmedia.fastfilm.core.network.apis.response.SceneCostData
import com.kgmedia.fastfilm.core.network.apis.response.ScoreConfigData
import com.kgmedia.fastfilm.core.network.apis.response.ShortLikeData
import com.kgmedia.fastfilm.core.network.apis.response.StoryboardListData
import com.kgmedia.fastfilm.core.network.apis.response.TimbreIdData
import com.kgmedia.fastfilm.core.network.apis.response.UpgradeData
import com.kgmedia.fastfilm.core.network.apis.response.UserHomeData
import com.kgmedia.fastfilm.core.network.apis.response.WithdrawalRecordInfo
import com.kgmedia.fastfilm.core.network.apis.response.WorkIdData
import com.kgmedia.fastfilm.core.network.apis.response.WxPayData
import kotlinx.coroutines.flow.Flow
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface ApiService {

    /**
     * 获取APP配置接口
     */
    @POST(ApiConstants.APP_CONFIG)
    suspend fun appConfig(@Body body: UserTokenBody): ApiResponse<AppConfigData>

    /**
     * 举报
     */
    @POST(ApiConstants.TIP_OFF)
    suspend fun tipOff(@Body body: TipOffBody): ApiResponse<Any>

    /**
     * 第三方登录
     */
    @POST(ApiConstants.LOGIN_BY_THIRD)
    suspend fun loginByThird(@Body body: LoginByThirdBody): ApiResponse<UserHomeData?>

    /**
     * 退出登录
     */
    @POST(ApiConstants.LOGOUT_APP)
    suspend fun logoutApp(@Body body: UserTokenBody): ApiResponse<Any>

    /**
     * 获取用户主页信息接口
     */
    @POST(ApiConstants.GET_USER_HOME_INFO)
    suspend fun getUserHomeInfo(@Body body: UserTokenBody): ApiResponse<UserHomeData?>

    /**
     * 完善用户资料接口
     */
    @POST(ApiConstants.COMPLETE_USER_DATA_INFO)
    suspend fun modifyUserInfo(@Body body: CompleteUserDataInfoBody): ApiResponse<Any?>

    /**
     * 获取APP升级信息
     */
    @POST(ApiConstants.GET_APP_UPGRADE_INFO)
    suspend fun upgrade(@Body userTokenBody: UserTokenBody): ApiResponse<UpgradeData>

    /**
     * 注销账号
     */
    @POST(ApiConstants.LOG_OFF)
    suspend fun logOff(@Body body: UserTokenBody): ApiResponse<Any?>

    /**
     * 获取用户分享链接
     */
    @POST(ApiConstants.GET_USER_SHARE_URL)
    suspend fun getUserShareUrl(@Body body: UserShareUrlBody): ApiResponse<UserShareUrlInfo?>

    /**
     * 用户分享操作上报
     */
    @POST(ApiConstants.SHARE_REPORT)
    suspend fun shareReport(@Body body: ShareReportBody): ApiResponse<Any?>

    /**
     * 提交用户的反馈数据
     */
    @POST(ApiConstants.FEED_BACK)
    suspend fun feedback(@Body body: FeedbackBody): ApiResponse<Any?>

    /**
     * 会员配置
     */
    @POST(ApiConstants.VIP_CONFIG)
    suspend fun vipConfig(@Body body: UserTokenBody): ApiResponse<VipConfigInfo?>

    /**
     * 用户日活记录
     */
    @POST(ApiConstants.DAILY)
    suspend fun daily(@Body body: UserTokenBody): ApiResponse<Any>

    /**
     * 图片风控检测
     */
    @POST(ApiConstants.IMAGE_CHECK)
    suspend fun imageCheck(@Body body: ImageUrlBody): ApiResponse<Any>

    /**
     * 歌曲生成来源上报
     */
    @POST(ApiConstants.MUSIC_GENERATE_SOURCE_REPORT)
    suspend fun musicGenerateSourceReport(@Body body: MusicGenerateSourceBody): ApiResponse<Any?>

    /**
     * 发送验证码
     */
    @POST(ApiConstants.SEND_SMS_VALIDATE_CODE)
    suspend fun sendSmsValidateCode(@Body body: SendSmsValidateCodeBody): ApiResponse<Any?>


    /**
     * 绑定手机号
     */
    @POST(ApiConstants.BIND_PHONE)
    suspend fun bindPhone(@Body body: BindPhoneBody): ApiResponse<Any?>

    /**
     * 闪验登录
     */
    @POST(ApiConstants.LOGIN_BY_FLASH_MOBILE)
    suspend fun loginByFlashMobile(@Body body: LoginByFlashMobileBody): ApiResponse<UserHomeData?>

    /**
     * 手机登录接口
     */
    @POST(ApiConstants.LOGIN_BY_PHONE)
    suspend fun loginByPhone(@Body body: LoginByPhoneBody): ApiResponse<UserHomeData?>

    /**
     * 取消注销
     */
    @POST(ApiConstants.CANCEL_LOG_OFF)
    suspend fun cancelLogOff(@Body body: UserTokenBody): ApiResponse<Any?>

    /**
     * 用户注销状态
     */
    @POST(ApiConstants.LOG_OFF_STATUS)
    suspend fun logOffStatus(@Body body: UserTokenBody): ApiResponse<LogOffStatusInfo?>

    /**
     * 添加/完善用户音色
     */
    @POST(ApiConstants.TIMBRE_ADD_OR_OPTIMIZE)
    suspend fun timbreAddOrOptimize(@Body body: TimbreAddOrOptimizeBody): ApiResponse<TimbreIdData?>

    /**
     * 我的声色
     */
    @POST(ApiConstants.TIMBRE_MINE_LIST)
    suspend fun timbreMineList(@Body body: PagedBody): ApiResponse<PagedList<TimbreInfo>?>

    /**
     * 添加/完善用户音色
     */
    @POST(ApiConstants.UPDATE_TIMBRE_TITLE)
    suspend fun updateTimbreTitle(@Body body: UpdateTimbreTitleBody): ApiResponse<Any?>

    /**
     * 删除音色
     */
    @POST(ApiConstants.DEL_TIMBRE)
    suspend fun delTimbre(@Body body: UserTimbreIdBody): ApiResponse<Any?>

    /**
     * 获取音色状态
     */
    @POST(ApiConstants.TIMBRE_STATUS_GET)
    suspend fun timbreStatusGet(@Body body: UserTimbreIdBody): ApiResponse<TimbreInfo?>

    /**
     * 微信下单会员
     */
    @POST(ApiConstants.MEMBER_WECHAT_ORDERING)
    suspend fun memberWechatOrdering(@Body body: MemberPaymentBody): ApiResponse<WxPayData?>

    /**
     * 支付宝下单会员
     */
    @POST(ApiConstants.MEMBER_ALIPAY_ORDERING)
    suspend fun memberAlipayOrdering(@Body body: MemberPaymentBody): ApiResponse<AlipayData?>

    /**
     * 绘本状态（轮询）
     */
    @POST(ApiConstants.GET_PICTURE_BOOK_STATUS)
    suspend fun getPictureBookStatus(@Body body: PictureBookId?): ApiResponse<PictureBookGenerateStatusData?>

    /**
     * 预生成短剧剧本大纲
     */
    @POST(ApiConstants.CREATION_GENERATE_PRE)
    suspend fun creationGeneratePre(@Body body: CreationGeneratePreBody): ApiResponse<DramasIdData?>

    /**
     * SSE流式输出：业务类型枚举10=随机灵感 11=AI优化灵感 20=剧集梗概 30=梗概剧集剧本 31=小说剧集剧本 32=扩写剧集剧本 33=优化作品简介
     */
    @Headers(
        "Accept: text/event-stream",  // 声明 SSE 媒体类型
        "Cache-Control: no-cache",     // 禁用缓存
        "Connection: keep-alive"       // 保持长连接
    )
    @POST(ApiConstants.SSE_CONTENT_OPTIMIZE)
    fun sseContentOptimize(@Body body: SSEContentBody): Flow<SSEvent>

    /**
     * SSE流式输出：业务类型枚举10=随机灵感 11=AI优化灵感 20=剧集梗概 30=梗概剧集剧本 31=小说剧集剧本 32=扩写剧集剧本 33=优化作品简介
     */
    @Headers(
        "Accept: text/event-stream",  // 声明 SSE 媒体类型
        "Cache-Control: no-cache",     // 禁用缓存
        "Connection: keep-alive"       // 保持长连接
    )
    @POST(ApiConstants.SSE_CONTENT_RANDOM)
    fun sseContentRandom(@Body body: UserTokenBody): Flow<SSEvent>

    /**
     *灵感故事剧本（基于灵感写剧本/灵感续写/小说续写）
     */
    @Headers(
        "Accept: text/event-stream",  // 声明 SSE 媒体类型
        "Cache-Control: no-cache",     // 禁用缓存
        "Connection: keep-alive"       // 保持长连接
    )
    @POST(ApiConstants.SSE_SCRIPT_FROM_IDEA)
    fun sseScriptFromIdea(@Body body: AIExpandBody): Flow<SSEvent>

    /**
     *小说直接生成剧集剧本
     */
    @Headers(
        "Accept: text/event-stream",  // 声明 SSE 媒体类型
        "Cache-Control: no-cache",     // 禁用缓存
        "Connection: keep-alive"       // 保持长连接
    )
    @POST(ApiConstants.SSE_SCRIPT_FROM_STORY)
    fun sseScriptFromStory(@Body body: AISynopsisBody): Flow<SSEvent>

    /**
     *扩写剧集剧本（小说）
     */
    @Headers(
        "Accept: text/event-stream",  // 声明 SSE 媒体类型
        "Cache-Control: no-cache",     // 禁用缓存
        "Connection: keep-alive"       // 保持长连接
    )
    @POST(ApiConstants.SSE_CONTENT_AI_EXPAND)
    fun sseContentAIExpand(@Body body: AIExpandBody): Flow<SSEvent>

    /**
     *故事梗概生成（灵感）
     */
    @Headers(
        "Accept: text/event-stream",  // 声明 SSE 媒体类型
        "Cache-Control: no-cache",     // 禁用缓存
        "Connection: keep-alive"       // 保持长连接
    )
    @POST(ApiConstants.SSE_CONTENT_SYNOPSIS)
    fun sseContentSynopsis(@Body body: AISynopsisBody): Flow<SSEvent>

    /**
     * 灵感保存故事梗概（下一步创建剧本）
     */
    @POST(ApiConstants.SAVE_WORK_SUMMARY)
    suspend fun saveWorkSummary(@Body body: CreateWorkBody): ApiResponse<WorkIdData?>

    /**
     * 小说保存故事剧集剧本（首次创建）
     */
    @POST(ApiConstants.SAVE_STORY_EPISODE_SCRIPT)
    suspend fun saveStoryEpisodeScript(@Body body: CreateStoryWorkBody): ApiResponse<WorkIdData?>

    /**
     *保存故事剧集剧本（灵感首创/灵感续集/小说续集）
     */
    @POST(ApiConstants.SAVE_EPISODE_SCRIPT)
    suspend fun saveEpisodeScript(@Body body: SaveEpisodeScriptBody): ApiResponse<WorkIdData?>

    /**
     * 轮询剧本分镜大纲
     */
    @POST(ApiConstants.GET_STORY_BOARD_LIST)
    suspend fun getStoryBoardList(@Body body: DramasIdBody): ApiResponse<StoryboardListData?>

    /**
     * 预生成剧本分镜大纲（重新生成分镜大纲）
     */
    @POST(ApiConstants.GET_STORY_BOARD_LIST_RETRY)
    suspend fun getStoryBoardListRetry(@Body body: DramasIdBody): ApiResponse<DramasIdData?>

    /**
     * 编辑故事剧本内容
     */
    @POST(ApiConstants.DRAMA_CONTENT_EDIT)
    suspend fun dramaContentEdit(@Body body: DramaContentBody): ApiResponse<Any>

    /**
     * 编辑剧本分镜内容(画面、旁白、动作)
     */
    @POST(ApiConstants.SCENE_CONTENT_EDIT)
    suspend fun sceneContentEdit(@Body body: SceneContentEditBody): ApiResponse<Any>

    /**
     * 计算生成剧本价格（积分）
     */
    @POST(ApiConstants.SHORT_DRAMAS_CALCULATE_PRICE)
    suspend fun shotDramasCalculatePrice(@Body body: DramasIdBody): ApiResponse<PointAmountData?>


    /**
     * 正式生成剧本(扣积分)
     */
    @POST(ApiConstants.SHOT_DRAMA_GENERATE)
    suspend fun shotDramaGenerate(@Body body: DramasIdBody): ApiResponse<DramasIdData?>

    /**
     * 短剧风格列表
     */
    @POST(ApiConstants.CREATION_STYLE_LIST)
    suspend fun creationStyleList(@Body body: PagedBody): ApiResponse<PagedList<CreationStyleInfo>?>

    /**
     * 优化生成剧本的角色的画面描述
     */
    @POST(ApiConstants.OPTIMIZE_ROLE_IMAGE_DESC)
    suspend fun optimizeRoleImageDesc(@Body body: OptimizeRoleImageDescBody): ApiResponse<OptimizeContentData?>

    /**
     * 重新生成剧本的角色
     */
    @POST(ApiConstants.ROLE_IMAGE_REGENERATE)
    suspend fun roleImageRegenerate(@Body body: RoleImageRegenerateBody): ApiResponse<Any>

    /**
     * 查询剧本的角色标签列表
     */
    @POST(ApiConstants.ROLE_TAG_LIST)
    suspend fun roleTagList(@Body body: UserTokenBody): ApiResponse<ListData<RoleTagInfo>?>

    /**
     * 角色形象列表
     */
    @POST(ApiConstants.CHARACTER_LIST)
    suspend fun characterList(@Body body: DramasIdBody): ApiResponse<CharacterListData?>


    /**
     * 获取积分消耗明细分页
     */
    @POST(ApiConstants.GET_POINT_CONSUME_PAGE)
    suspend fun getPointConsumePage(@Body body: PagedBody): ApiResponse<PagedList<PointConsumePageInfo>?>

    /**
     * 获取充值明细分页
     */
    @POST(ApiConstants.GET_RECHARGE_PAGE)
    suspend fun getRechargePage(@Body body: PagedBody): ApiResponse<PagedList<RechargePageInfo>?>

    /**
     * 获取积分配置
     */
    @POST(ApiConstants.SCORE_CONFIG)
    suspend fun scoreConfig(@Body body: PointConfigBody): ApiResponse<ScoreConfigData?>

    /**
     * 微信下单积分
     */
    @POST(ApiConstants.WECHAT_ORDERING_POINTS)
    suspend fun wechatOrderingPoints(@Body body: OrderingPointsBody): ApiResponse<WxPayData?>

    /**
     * 支付宝下单积分
     */
    @POST(ApiConstants.ALIPAY_ORDERING_POINTS)
    suspend fun alipayOrderingPoints(@Body body: OrderingPointsBody): ApiResponse<AlipayData?>


    /**
     * 场景扣费
     */
    @POST(ApiConstants.SCENE_COST)
    suspend fun sceneCost(@Body body: SceneCostBody): ApiResponse<SceneCostData>

    /**
     * 获取APP浮动窗口
     */
    @POST(ApiConstants.GET_APP_FLOAT_WINDOWS)
    suspend fun getAppFloatWindows(@Body body: AppFloatWindowsBody): ApiResponse<ListData<AppFloatWindowsInfo>?>

    /**
     * 邀请收益信息
     */
    @POST(ApiConstants.INVITE_INCOME_PAGE)
    suspend fun getInviteIncomePage(@Body body: PagedBody): ApiResponse<PagedList<InviteIncomePageInfo>?>

    /**
     * 邀请用户记录分页
     */
    @POST(ApiConstants.INVITE_USER_PAGE)
    suspend fun getInviteUserPage(@Body body: PagedBody): ApiResponse<PagedList<InviteUserPageInfo>?>

    /**
     * 用户提现记录（余额）
     */
    @POST(ApiConstants.WITHDRAWAL_RECORD)
    suspend fun getWithdrawalRecord(@Body body: PagedBody): ApiResponse<PagedList<WithdrawalRecordInfo>?>

    /**
     * 生成中的绘本列表
     */
    @POST(ApiConstants.GENERATING_BOOK_LIST)
    suspend fun generatingBookList(@Body body: UserTokenBody): ApiResponse<PagedList<PictureBook>?>

    /**
     * 合成短剧
     */
    @POST(ApiConstants.SHORT_DRAMAS_SYNTHESIS)
    suspend fun shortDramasSynthesis(@Body body: DramasIdBody): ApiResponse<Any?>

    /**
     * 合成短剧(上传合成的视频)
     */
    @POST(ApiConstants.SHORT_DRAMAS_SYNTHESIS_UPLOAD)
    suspend fun shortDramasSynthesisUpload(@Body body: SynthesisUploadBody): ApiResponse<Any?>

    /**
     * 查询剧本信息（作品详情也用这个）
     */
    @POST(ApiConstants.SHORT_DRAMAS_INFO)
    suspend fun shortDramasInfo(@Body body: DramasIdBody): ApiResponse<ShortDramaInfo?>

    /**
     * 查询剧本信息（作品详情也用这个）
     */
    @POST(ApiConstants.SHORT_DRAMAS_RELEASED_INFO)
    suspend fun releasedShortDramasInfo(@Body body: DramasIdBody): ApiResponse<ShortDramaInfo?>

    /**
     * 查询剧本信息（作品详情也用这个）
     */
    @POST(ApiConstants.SHORT_DRAMAS_STATUS)
    suspend fun shortDramasStatus(@Body body: DramasIdBody): ApiResponse<ShortDramaStatusInfo?>

    /**
     * 提交字幕配音
     */
    @POST(ApiConstants.SHORT_DRAMAS_DUBBING)
    suspend fun shortDramasDubbing(@Body body: DramasTimbreBody): ApiResponse<Any?>

    /**
     * 配音失败-重新配音
     */
    @POST(ApiConstants.SHORT_DRAMAS_DUBBING_RETRY)
    suspend fun shortDramasDubbingRetry(@Body body: DramasTimbreBody): ApiResponse<Any?>

    /**
     * 解锁首次生成的短剧(首次生成的短剧付费)
     */
    @POST(ApiConstants.UNLOCK_FIRST_GENERATED_SHORT_DRAMA)
    suspend fun unlockFirstGeneratedShortDrama(@Body body: DramasIdBody): ApiResponse<Any>

    /**
     * 使用配音
     */
    @POST(ApiConstants.SHORT_DRAMAS_DUBBING_USE)
    suspend fun shortDramasDubbingUse(@Body body: DramasTimbreBody): ApiResponse<Any?>

    /**
     * 字幕配音生成状态
     */
    @POST(ApiConstants.SHORT_DRAMAS_DUBBING_STATUS)
    suspend fun shortDramasDubbingStatus(@Body body: DramasTimbreBody): ApiResponse<DubbingGenStatusInfo?>

    /**
     * 字幕配音音色列表
     */
    @POST(ApiConstants.DUBBING_VOICE_LIST)
    suspend fun dubbingVoiceList(@Body body: DramasTimbrePagedBody): ApiResponse<PagedList<DramasTimbreInfo>?>

    /**
     * 使用历史分镜（分镜替换）
     */
    @POST(ApiConstants.SHORT_DRAMAS_SEGMENT_IMAGE_REPLACE)
    suspend fun shortDramasSegmentImageReplace(@Body body: SegmentImageReplaceBody): ApiResponse<Any?>

    /**
     * 查询历史分镜
     */
    @POST(ApiConstants.SHORT_DRAMAS_SEGMENT_IMAGE_HISTORY)
    suspend fun shortDramasSegmentImageHistory(@Body body: ShotSegmentIdBody): ApiResponse<ListData<SegmentImageHistoryInfo>?>

    /**
     * 重绘分镜（重新生成分镜的图片）
     */
    @POST(ApiConstants.SHORT_DRAMAS_SEGMENT_IMAGE_REGENERATE)
    suspend fun shortDramasSegmentImageRegenerate(@Body body: SegmentImageRegenerateBody): ApiResponse<Any?>

    /**
     * 优化重绘分镜的画面描述
     */
    @POST(ApiConstants.SHORT_DRAMAS_SEGMENT_IMAGE_DESC_OPTIMIZE)
    suspend fun shortDramasSegmentImageDescOptimize(@Body body: SegmentImageDescOptimizeBody): ApiResponse<ContentInfo?>

    /**
     * 生成剧本图片(失败重试)
     */
    @POST(ApiConstants.SHORT_DRAMAS_SEGMENT_IMAGE_GENERATE_RETRY)
    suspend fun shortDramasSegmentImageGenerateRetry(@Body body: SegmentImageRetryBody): ApiResponse<Any?>

    /**
     * 生成剧本图片
     */
    @POST(ApiConstants.GENERATING_SEGMENT_IMAGE)
    suspend fun generatingSegmentImage(@Body body: DramasIdBody): ApiResponse<DramasIdData?>

    /**
     * 下载短剧视频（合成后回传给后端）
     */
    @POST(ApiConstants.SHORT_DRAMAS_DOWNLOAD)
    suspend fun shortDramasDownload(@Body body: DownloadReportBody): ApiResponse<Any?>

    /**
     * 修改剧本信息
     */
    @POST(ApiConstants.EDIT_SHORT_DRAMA_INFO)
    suspend fun editShotDramaInfo(@Body body: EditDramaBody): ApiResponse<Any?>

    /**
     * 删除剧本
     */
    @POST(ApiConstants.DELETE_DRAMA_INFO)
    suspend fun deleteDramaInfo(@Body body: DramasIdBody): ApiResponse<Any?>

    /**
     * 分页查询我的剧本草稿列表
     */
    @POST(ApiConstants.USER_SHORT_DRAMS)
    suspend fun userShortDramaDrafts(@Body body: PagedBody): ApiResponse<PagedList<ShortDramaInfo>?>

    /**
     * 获取支付宝信息(并绑定)
     */
    @POST(ApiConstants.BIND_ZFB)
    suspend fun bindZfb(@Body body: BindZfbBody): ApiResponse<Any?>

    /**
     * 绑定账号
     */
    @POST(ApiConstants.BIND_WITHDRAW_ACCOUNT)
    suspend fun bindWithdrawAccount(@Body body: BindWithdrawAccountBody): ApiResponse<Any?>

    /**
     * 钻石提现
     */
    @POST(ApiConstants.WITHDRAW)
    suspend fun withdraw(@Body body: WithdrawBody): ApiResponse<WithdrawSuccessInfo?>

    /**
     * 收益提现详情-钻石
     */
    @POST(ApiConstants.WITHDRAW_INFO)
    suspend fun withdrawInfo(@Body body: UserTokenBody): ApiResponse<WithdrawInfo?>

    /**
     * 余额提现弹窗详情-钻石
     */
    @POST(ApiConstants.WITHDRAW_CONFIRM)
    suspend fun withdrawConfirm(@Body body: WithdrawBody): ApiResponse<WithdrawConfirmInfo?>

    /**
     * 查询所有类型消息列表
     */
    @POST(ApiConstants.MESSAGES_SUMMARY)
    suspend fun messagesSummary(@Body body: UserTokenBody): ApiResponse<MessageSummaryData?>

    /**
     * 系统通知
     */
    @POST(ApiConstants.SYSTEM_NOTICE)
    suspend fun systemNotice(@Body body: PagedBody): ApiResponse<PagedList<SystemNoticeInfo>?>

    /**
     * 分页查询用户追剧消息列表
     */
    @POST(ApiConstants.MESSAGES_WATCHING)
    suspend fun messagesWatching(@Body body: PagedBody): ApiResponse<PagedList<SystemMessageWatchingInfo>?>

    /**
     * 分页查询用户喜欢消息列表
     */
    @POST(ApiConstants.MESSAGES_LIKES)
    suspend fun messagesLikes(@Body body: PagedBody): ApiResponse<PagedList<SystemMessageLikeInfo>?>

    /**
     * 消息全部设置成已读
     */
    @POST(ApiConstants.MESSAGES_MARK_READ)
    suspend fun messagesMarkRead(@Body body: MessagesTypeBody): ApiResponse<Any>

    /**
     * 查询用户未读消息数量
     */
    @POST(ApiConstants.MESSAGES_UNREAD_NUM)
    suspend fun messagesUnreadNum(@Body body: UserTokenBody): ApiResponse<MessagesUnreadNumData?>

    /** 爆款推荐列表
     */
    @POST(ApiConstants.HIT_RECOMMEND_LIST)
    suspend fun hitRecommendList(@Body body: HitRecommendPagedBody): ApiResponse<PagedList<HitRecommendInfo>?>

    /**爆款推荐tab
     */
    @POST(ApiConstants.HIT_RECOMMEND_TABS)
    suspend fun hitRecommendTabs(@Body body: UserTokenBody): ApiResponse<ListData<HitTabInfo>?>

    /** 攻略咨询列表
     */
    @POST(ApiConstants.STRATEGY_LIST)
    suspend fun strategyList(@Body body: PagedBody): ApiResponse<PagedList<StrategyInfo>?>

    /** 短剧配置展示数据
     */
    @POST(ApiConstants.SHORT_DRAMA_STATISTICS)
    suspend fun shortDramaStatistics(@Body body: UserTokenBody): ApiResponse<ShortDramasStatisticsData?>

    /** 获取弹幕列表
     */
    @POST(ApiConstants.DANMAKU_LIST)
    suspend fun danmakuList(@Body body: DanmakuBody): ApiResponse<ListData<DanmakuItem>?>

    /**
     * 用户收益信息
     */
    @POST(ApiConstants.USER_REVENUE_INFORMATION)
    suspend fun userRevenueInformation(@Body body: UserTokenBody): ApiResponse<UserRevenueInformationInfo?>

    /**
     * 收益明细列表
     */
    @POST(ApiConstants.PROFIT_DETAIL_LIST)
    suspend fun profitDetailList(@Body body: PagedBody): ApiResponse<PagedList<ProfitDetailInfo>?>

    /**
     * 获取用户可续写的剧集列表
     */
    @POST(ApiConstants.USER_CONTINUABLE_DRAMAS)
    suspend fun userContinuableSeries(@Body body: PagedBody): ApiResponse<PagedList<MyTvSeriesListInfo>?>

    /**
     * 获取剧集最新一集剧本详情
     */
    @POST(ApiConstants.LATEST_DRAMAS_WORK_DETAIL)
    suspend fun latestEpisode(@Body body: WorkIdBody): ApiResponse<EpisodeInfo?>

    /**
     * 查询我的作品详情
     */
    @POST(ApiConstants.WORK_INFO)
    suspend fun getWorkInfo(@Body body: WorkIdBody): ApiResponse<WorkInfo?>

    /**
     *流式生成帮我续写(生成续写的用户输出内容)
     */
    @Headers(
        "Accept: text/event-stream",  // 声明 SSE 媒体类型
        "Cache-Control: no-cache",     // 禁用缓存
        "Connection: keep-alive"       // 保持长连接
    )
    @POST(ApiConstants.AI_CONTINUE_GENERATE)
    fun aiContinueGenerate(@Body body: ContinueGenerateBody): Flow<SSEvent>

    /**
     * 分页查询我的剧集列表
     */
    @POST(ApiConstants.MY_TV_SERIES_LIST)
    suspend fun myTvSeriesList(@Body body: MyTvSeriesBody): ApiResponse<PagedList<MyTvSeriesListInfo>?>

    /**
     * 追剧或取消追剧
     */
    @POST(ApiConstants.SERIES_FOLLOW)
    suspend fun seriesFollow(@Body body: SeriesFollowBody): ApiResponse<FollowingData>

    /**
     * 点赞短剧
     */
    @POST(ApiConstants.LIKE_SHORT_DRAMA)
    suspend fun likeShortDrama(@Body body: ShortLikeBody): ApiResponse<ShortLikeData>

    /**
     * 修改剧集价格
     */
    @POST(ApiConstants.SERIES_SET_PRICE)
    suspend fun seriesSetPrice(@Body body: SeriesSetPriceBody): ApiResponse<Any>

    /**
     * 获取剧集价格配置
     */
    @POST(ApiConstants.SERIES_PRICE_CONFIG)
    suspend fun seriesPriceConfig(@Body body: WorkIdBody): ApiResponse<SeriesPriceConfigInfo?>

    /**
     * 修改剧集信息
     */
    @POST(ApiConstants.SERIES_MODIFY_INFO)
    suspend fun seriesModifyInfo(@Body body: SeriesModifyInfoBody): ApiResponse<Any>

    /**
     * 上架剧集
     */
    @POST(ApiConstants.SERIES_PUBLISH)
    suspend fun seriesPublish(@Body body: SeriesSetPriceBody): ApiResponse<Any>

    /**
     *分页查看作品剧集列表
     */
    @POST(ApiConstants.EPISODE_LIST)
    suspend fun episodeList(@Body body: WorkIdPagedBody): ApiResponse<PagedList<EpisodeInfo>?>

    /**
     * 新用户创作体验引导条
     */
    @POST(ApiConstants.NEW_USER_CREATION_GUIDE_BAR)
    suspend fun newUserCreationGuideBar(@Body body: UserTokenBody): ApiResponse<GuideBarStatusInfo?>

    /**
     * 分页查询我追的剧集列表
     */
    @POST(ApiConstants.THE_TV_SERIES_I_FOLLOW)
    suspend fun theTvSeriesIFollow(@Body body: PagedBody): ApiResponse<PagedList<TheTvSeriesIFollowInfo>?>

    /**
     * 分页查询我喜欢的剧本列表
     */
    @POST(ApiConstants.MY_FAVORITE_SCRIPT)
    suspend fun myFavoriteScript(@Body body: PagedBody): ApiResponse<PagedList<MyFavoriteScriptInfo>?>

    /**
     * 查询我的作品数量统计信息
     */
    @POST(ApiConstants.QUANTITY_STATISTICS_OF_WORKS)
    suspend fun quantityStatisticsOfWorks(@Body body: UserTokenBody): ApiResponse<QuantityStatisticsOfWorksInfo?>


    /**
     * 流式生成短剧剧本内容（灵感、自定义、续写使用）
     */
    @POST(ApiConstants.SSE_CONTENT_GENERATE)
    @Headers(
        "Accept: text/event-stream",  // 声明 SSE 媒体类型
        "Cache-Control: no-cache",     // 禁用缓存
        "Connection: keep-alive"       // 保持长连接
    )
    fun sseContentGenerate(@Body body: InputContentBody): Flow<SSEvent>

    /**
     * 流式生成短剧剧本内容（爆款制作同款使用）
     */
    @POST(ApiConstants.SSE_MAKE_SAME_CONTENT_GENERATE)
    @Headers(
        "Accept: text/event-stream",  // 声明 SSE 媒体类型
        "Cache-Control: no-cache",     // 禁用缓存
        "Connection: keep-alive"       // 保持长连接
    )
    fun sseMakeSameContentGenerate(@Body body: PopularRecommendDramasIdBody): Flow<SSEvent>

    /**
     * 预生成短剧剧本(流式输出完之后调)
     */
    @POST(ApiConstants.DRAMA_GENERATE)
    suspend fun dramaGenerate(@Body body: DramaGenerateBody): ApiResponse<DramasIdData?>

    /**
     * 生成短剧来源上报(调完生成短剧剧本接口后调用)
     */
    @POST(ApiConstants.DRAMA_GENERATE_SOURCE_REPORT)
    suspend fun dramaGenerateSourceReport(@Body body: DramaGenerateSourceReportBody): ApiResponse<Any>

    /**
     * 生成短剧来源上报(调完生成短剧剧本接口后调用)
     */
    @POST(ApiConstants.TUTORIAL_CLICK_REPORT)
    suspend fun tutorialClickReport(@Body body: StrategyInfoIdBody): ApiResponse<Any>

    /**
     * 查询剧本的角色列表(轮询使用)
     */
    @POST(ApiConstants.DRAMA_ROLES_POLLING)
    suspend fun dramaRolesPolling(@Body body: DramasIdBody): ApiResponse<CharacterListData?>

    /**
     * 解锁剧集
     */
    @POST(ApiConstants.UNLOCK_SHORT_DRAMA)
    suspend fun unlockShortDrama(@Body body: DramasSeriesId): ApiResponse<BalanceData>

    /**
     * 自动解锁剧集开关上报
     */
    @POST(ApiConstants.AUTO_UNLOCK_SWITCH)
    suspend fun autoUnlockSwitch(@Body body: DramasSeriesId): ApiResponse<Any>


    /**
     * 首页短剧推荐列表
     */
    @POST(ApiConstants.RECOMMEND_SHORT_LIST)
    suspend fun recommendShortList(@Body body: RecommendShortsBody): ApiResponse<PagedList<ShortDramaInfo>?>

    /**
     * 发布剧本
     */
    @POST(ApiConstants.PUBLISH_SHORT_DRAMA)
    suspend fun publishShortDrama(@Body body: DramasIdBody): ApiResponse<DramasSeriesIdData?>

    /**
     * 剧本播放上报
     */
    @POST(ApiConstants.SHORT_DRAMA_PLAY_REPORT)
    suspend fun shortDramaPlayReport(@Body body: DramasIdBody): ApiResponse<Any>

    /**
     * 获取订单状态
     */
    @POST(ApiConstants.GET_ORDER_STATUS)
    suspend fun getOrderStatus(@Body body: OrderStatusBody): ApiResponse<OrderStatusInfo>

    @POST(ApiConstants.RECOMMEND_CLICK_REPORT)
    suspend fun recommendClickReport(@Body body: PopularRecommendDramasIdBody): ApiResponse<Any>

    /**
     * 获取正在生成的短剧信息
     */
    @POST(ApiConstants.GET_GENERATING_INFO)
    suspend fun getGeneratingInfo(@Body body: UserTokenBody): ApiResponse<DramasInfoData>

    /**
     * 关闭正在生成的短剧
     */
    @POST(ApiConstants.CLOSE_GENERATING_DRAMAS)
    suspend fun closeGeneratingDramas(@Body body: DramasIdBody): ApiResponse<Any>

    /**
     * 新增剧本的角色
     */
    @POST(ApiConstants.DRAMAS_ADD_ROLE)
    suspend fun dramasAddRole(@Body body: RoleImageRegenerateBody): ApiResponse<Any>

    /**
     * 删除剧本的角色
     */
    @POST(ApiConstants.DRAMAS_DEL_ROLE)
    suspend fun dramasDelRole(@Body body: RoleIdBody): ApiResponse<Any>

    /**
     * 获取banner信息
     */
    @POST(ApiConstants.APP_BANNERS)
    suspend fun getAppBanner(@Body body: BannerBody): ApiResponse<ListData<BannerInfo>?>

    /**
     * 获取快捷灵感(2条)
     */
    @POST(ApiConstants.INSPIRATION_QUICK_LIST)
    suspend fun getInspirationQuickList(@Body body: UserTokenBody): ApiResponse<ListData<InspirationInfo>?>

    /**
     * 查询历史角色
     */
    @POST(ApiConstants.GET_HISTORY_ROLES)
    suspend fun getHistoryRoles(@Body body: RoleIdBody): ApiResponse<ListData<CharacterInfo>?>

    /**
     * 历史角色使用
     */
    @POST(ApiConstants.HISTORY_ROLE_SWITCH)
    suspend fun historyRoleSwitch(@Body body: RoleSwitchBody): ApiResponse<Any>

    /**
     * 首页作品列表
     */
    @POST(ApiConstants.RECOMMEND_DRAMA_LIST)
    suspend fun recommendDramaList(@Body body: PagedBody): ApiResponse<PagedList<WorkInfo>?>

    /**
     * 获取集数/视频比例/视频风格配置
     */
    @POST(ApiConstants.VIDEO_SET_CONFIG)
    suspend fun videoSetConfig(@Body body: UserTokenBody): ApiResponse<VideoConfigData?>

    /**
     * 查询资产参考图列表
     */
    @POST(ApiConstants.GET_HISTORY_REFERENCE_IMAGE)
    suspend fun getHistoryReferenceImage(@Body body: ReferenceImageBody): ApiResponse<List<ReferenceImageInfo>?>

    /**
     * 编辑我的作品信息
     */
    @POST(ApiConstants.UPDATE_WORK_INFO)
    suspend fun updateWorkInfo(@Body body: WorkInfoUpdateBody): ApiResponse<Any>

    /**
     * 编辑我的作品信息
     */
    @POST(ApiConstants.PLAY_EVENT_REPORT)
    suspend fun playEventReport(@Body body: PlayEventBody): ApiResponse<Any>

    /**
     * 轮询查询全部资产
     */
    @POST(ApiConstants.WORK_ASSET_ALL_STATUS)
    suspend fun workAssetAllStatus(@Body body: TaskIdBody): ApiResponse<TaskStatusInfo?>

    /**
     * 轮询查询单个资产
     */
    @POST(ApiConstants.WORK_ASSET_SINGLE_STATUS)
    suspend fun workAssetSingleStatus(@Body body: TaskIdBody): ApiResponse<TaskStatusInfo?>

    /**
     * 场景列表
     */
    @POST(ApiConstants.WORK_ASSET_SCENE_LIST)
    suspend fun workAssetSceneList(@Body body: QueryAssetBody): ApiResponse<List<SimpleSceneInfo>?>

    /**
     * 场景保存
     */
    @POST(ApiConstants.WORK_ASSET_SCENE_SAVE)
    suspend fun workAssetSceneSave(@Body body: SaveSceneBody): ApiResponse<SimpleSceneInfo?>

    /**
     * 场景单个删除
     */
    @POST(ApiConstants.WORK_ASSET_SCENE_DELETE)
    suspend fun workAssetSceneDelete(@Body body: IdsBody): ApiResponse<Any>

    /**
     * 角色列表
     */
    @POST(ApiConstants.WORK_ASSET_ROLE_LIST)
    suspend fun workAssetRoleList(@Body body: QueryAssetBody): ApiResponse<List<SimpleRoleInfo>?>

    /**
     * 角色保存
     */
    @POST(ApiConstants.WORK_ASSET_ROLE_SAVE)
    suspend fun workAssetRoleSave(@Body body: SaveRoleBody): ApiResponse<SimpleRoleInfo?>

    /**
     * 角色单个删除
     */
    @POST(ApiConstants.WORK_ASSET_ROLE_DELETE)
    suspend fun workAssetRoleDelete(@Body body: IdsBody): ApiResponse<Any>

    /**
     * 道具列表
     */
    @POST(ApiConstants.WORK_ASSET_PROP_LIST)
    suspend fun workAssetPropList(@Body body: QueryAssetBody): ApiResponse<List<SimplePropInfo>?>

    /**
     * 道具保存
     */
    @POST(ApiConstants.WORK_ASSET_PROP_SAVE)
    suspend fun workAssetPropSave(@Body body: SavePropBody): ApiResponse<SimplePropInfo?>

    /**
     * 道具单个删除
     */
    @POST(ApiConstants.WORK_ASSET_PROP_DELETE)
    suspend fun workAssetPropDelete(@Body body: IdsBody): ApiResponse<Any>

    /**
     * 生成单个资产图片（重新生成/生成）
     */
    @POST(ApiConstants.WORK_ASSET_SINGLE_GENERATE)
    suspend fun workAssetSingleGenerate(@Body body: GenerateAssetBody): ApiResponse<TaskStatusInfo?>

    /**
     * 举报提交
     */
    @POST(ApiConstants.REPORT_SUBMISSION)
    suspend fun reportSubmission(@Body body: ReportSubmissionBody): ApiResponse<Any>
}