package com.shiyuan.fastclaw.core.network.apis

import com.shiyuan.fastclaw.core.network.apis.entity.BannerInfo
import com.shiyuan.fastclaw.core.network.apis.entity.OrderStatusInfo
import com.shiyuan.fastclaw.core.network.apis.entity.PaymentData
import com.shiyuan.fastclaw.core.network.apis.entity.PointConfigListData
import com.shiyuan.fastclaw.core.network.apis.entity.ReportData
import com.shiyuan.fastclaw.core.network.apis.entity.ResumeData
import com.shiyuan.fastclaw.core.network.apis.entity.SchemeData
import com.shiyuan.fastclaw.core.network.apis.entity.SensitiveWordCheckData
import com.shiyuan.fastclaw.core.network.apis.entity.TradePayWayListData
import com.shiyuan.fastclaw.core.network.apis.entity.WalletInfo
import com.shiyuan.fastclaw.core.network.apis.entity.WorkInfo
import com.shiyuan.fastclaw.core.network.apis.request.ActionSubmitBody
import com.shiyuan.fastclaw.core.network.apis.request.AttributeArrayQueryBody
import com.shiyuan.fastclaw.core.network.apis.request.AttributeConfirmBody
import com.shiyuan.fastclaw.core.network.apis.request.AttributeRejectBody
import com.shiyuan.fastclaw.core.network.apis.request.BannerBody
import com.shiyuan.fastclaw.core.network.apis.request.BindMobileBody
import com.shiyuan.fastclaw.core.network.apis.request.BindPhoneBody
import com.shiyuan.fastclaw.core.network.apis.request.BindThirdPartyBody
import com.shiyuan.fastclaw.core.network.apis.request.ChangeMobileBody
import com.shiyuan.fastclaw.core.network.apis.request.CheckMobileBindStatusBody
import com.shiyuan.fastclaw.core.network.apis.request.CompanyBackgroundCompanySearchQueryBody
import com.shiyuan.fastclaw.core.network.apis.request.CompanyBackgroundGenerateResultQueryBody
import com.shiyuan.fastclaw.core.network.apis.request.CompanyBackgroundPreCheckResultQueryBody
import com.shiyuan.fastclaw.core.network.apis.request.CompanyBackgroundPreCheckSubmitBody
import com.shiyuan.fastclaw.core.network.apis.request.CompanyBackgroundRegenerateSubmitBody
import com.shiyuan.fastclaw.core.network.apis.request.JobCityQueryBody
import com.shiyuan.fastclaw.core.network.apis.request.FeedbackBody
import com.shiyuan.fastclaw.core.network.apis.request.CompleteFileUploadBody
import com.shiyuan.fastclaw.core.network.apis.request.ConversationQueryBody
import com.shiyuan.fastclaw.core.network.apis.request.CreateFileUploadCredentialBody
import com.shiyuan.fastclaw.core.network.apis.request.CurrentResumeQueryBody
import com.shiyuan.fastclaw.core.network.apis.request.FileSecurityStatusQueryBody
import com.shiyuan.fastclaw.core.network.apis.request.ImgUrlBody
import com.shiyuan.fastclaw.core.network.apis.request.InterviewPassPackageResumeStatusBody
import com.shiyuan.fastclaw.core.network.apis.request.PassPackageRegenerateBody
import com.shiyuan.fastclaw.core.network.apis.request.PassPackageStatusQueryBody
import com.shiyuan.fastclaw.core.network.apis.request.InterviewReportData
import com.shiyuan.fastclaw.core.network.apis.request.InterviewReportDeleteBody
import com.shiyuan.fastclaw.core.network.apis.request.InviteShareInfoBody
import com.shiyuan.fastclaw.core.network.apis.request.JdMaterialQualityBody
import com.shiyuan.fastclaw.core.network.apis.request.JobAgentCancelBody
import com.shiyuan.fastclaw.core.network.apis.request.JobStatusUpdateBody
import com.shiyuan.fastclaw.core.network.apis.request.JobAgentStreamBody
import com.shiyuan.fastclaw.core.network.apis.request.LoginByFlashMobileBody
import com.shiyuan.fastclaw.core.network.apis.request.LoginByPhoneBody
import com.shiyuan.fastclaw.core.network.apis.request.LoginByThirdBody
import com.shiyuan.fastclaw.core.network.apis.request.MessagePageBody
import com.shiyuan.fastclaw.core.network.apis.request.MessagePullBody
import com.shiyuan.fastclaw.core.network.apis.request.MockInterviewAudioTokenBody
import com.shiyuan.fastclaw.core.network.apis.request.MockInterviewForceCancelBody
import com.shiyuan.fastclaw.core.network.apis.request.MockInterviewReportProgressBody
import com.shiyuan.fastclaw.core.network.apis.request.MockInterviewReportRetryBody
import com.shiyuan.fastclaw.core.network.apis.request.MockInterviewStartBody
import com.shiyuan.fastclaw.core.network.apis.request.MockInterviewStreamBody
import com.shiyuan.fastclaw.core.network.apis.request.OnlineResumeAttributeQueryBody
import com.shiyuan.fastclaw.core.network.apis.request.OnlineResumeAttributeUpdateBody
import com.shiyuan.fastclaw.core.network.apis.request.OrderStatusBody
import com.shiyuan.fastclaw.core.network.apis.request.PagedBody
import com.shiyuan.fastclaw.core.network.apis.request.PlayEventBody
import com.shiyuan.fastclaw.core.network.apis.request.ReportSubmissionBody
import com.shiyuan.fastclaw.core.network.apis.request.RegionDictQueryBody
import com.shiyuan.fastclaw.core.network.apis.request.ResumeConfirmBody
import com.shiyuan.fastclaw.core.network.apis.request.ResumeListDeleteBody
import com.shiyuan.fastclaw.core.network.apis.request.ResumeMaterialQualityBody
import com.shiyuan.fastclaw.core.network.apis.request.ResumeUploadSubmitBody
import com.shiyuan.fastclaw.core.network.apis.request.SendSmsValidateCodeBody
import com.shiyuan.fastclaw.core.network.apis.request.SessionEnterBody
import com.shiyuan.fastclaw.core.network.apis.request.TipOffBody
import com.shiyuan.fastclaw.core.network.apis.request.TradePayCreateBody
import com.shiyuan.fastclaw.core.network.apis.request.UnbindThirdPartyBody
import com.shiyuan.fastclaw.core.network.apis.request.UpdateAvatarBody
import com.shiyuan.fastclaw.core.network.apis.request.UserTokenBody
import com.shiyuan.fastclaw.core.network.SSEvent
import com.shiyuan.fastclaw.core.network.apis.request.DictIndustryQueryBody
import com.shiyuan.fastclaw.core.network.apis.request.DictPositionQueryBody
import com.shiyuan.fastclaw.core.network.apis.request.DictWelfareQueryBody
import com.shiyuan.fastclaw.core.network.apis.request.ResumeUploadSubmitMyBody
import kotlinx.coroutines.flow.Flow
import com.shiyuan.fastclaw.core.network.apis.response.ActionSubmitData
import com.shiyuan.fastclaw.core.network.apis.response.ActivityStatusData
import com.shiyuan.fastclaw.core.network.apis.response.AppConfigData
import com.shiyuan.fastclaw.core.network.apis.response.AttributeArrayQueryData
import com.shiyuan.fastclaw.core.network.apis.response.AttributeSave
import com.shiyuan.fastclaw.core.network.apis.response.BindThirdPartyData
import com.shiyuan.fastclaw.core.network.apis.response.CheckMobileBindStatusData
import com.shiyuan.fastclaw.core.network.apis.response.JobCityData
import com.shiyuan.fastclaw.core.network.apis.response.ConversationQueryData
import com.shiyuan.fastclaw.core.network.apis.response.CompanyBackgroundCompanyInfo
import com.shiyuan.fastclaw.core.network.apis.response.CompanyBackgroundCompanySearchData
import com.shiyuan.fastclaw.core.network.apis.response.CompanyBackgroundGenerateResultData
import com.shiyuan.fastclaw.core.network.apis.response.CompanyBackgroundPreCheckResultData
import com.shiyuan.fastclaw.core.network.apis.response.CurrentResumeData
import com.shiyuan.fastclaw.core.network.apis.response.FileSecurityStatusData
import com.shiyuan.fastclaw.core.network.apis.response.FileUploadCredentialData
import com.shiyuan.fastclaw.core.network.apis.response.H5InterviewPassPackageRep
import com.shiyuan.fastclaw.core.network.apis.response.IndustryDict
import com.shiyuan.fastclaw.core.network.apis.response.InterviewPassPackageResumeStatusData
import com.shiyuan.fastclaw.core.network.apis.response.PassPackageRegenerateData
import com.shiyuan.fastclaw.core.network.apis.response.PassPackageStatusData
import com.shiyuan.fastclaw.core.network.apis.response.InterviewReportListItem
import com.shiyuan.fastclaw.core.network.apis.response.InviteRecordPageData
import com.shiyuan.fastclaw.core.network.apis.response.InviteStatsData
import com.shiyuan.fastclaw.core.network.apis.response.JobAgentCancelData
import com.shiyuan.fastclaw.core.network.apis.response.JobAgentModuleData
import com.shiyuan.fastclaw.core.network.apis.response.JobSearchStatus
import com.shiyuan.fastclaw.core.network.apis.response.MessagePageData
import com.shiyuan.fastclaw.core.network.apis.response.MessagePullData
import com.shiyuan.fastclaw.core.network.apis.response.MockInterviewAudioTokenData
import com.shiyuan.fastclaw.core.network.apis.response.MockInterviewForceCancelData
import com.shiyuan.fastclaw.core.network.apis.response.MockInterviewReportProgressData
import com.shiyuan.fastclaw.core.network.apis.response.MockInterviewStartData
import com.shiyuan.fastclaw.core.network.apis.response.MockInterviewUnfinishedData
import com.shiyuan.fastclaw.core.network.apis.response.OnlineResumeAttributeData
import com.shiyuan.fastclaw.core.network.apis.response.OnlineResumeInfoData
import com.shiyuan.fastclaw.core.network.apis.response.WelfareDict
import com.shiyuan.fastclaw.core.network.apis.response.PositionDict
import com.shiyuan.fastclaw.core.network.apis.response.PayOrderInfo
import com.shiyuan.fastclaw.core.network.apis.response.ReportListItem
import com.shiyuan.fastclaw.core.network.apis.response.RegionDictData
import com.shiyuan.fastclaw.core.network.apis.response.RefreshTokenData
import com.shiyuan.fastclaw.core.network.apis.response.ResumeConfirmData
import com.shiyuan.fastclaw.core.network.apis.response.ResumeListData
import com.shiyuan.fastclaw.core.network.apis.response.ResumeUploadSubmitData
import com.shiyuan.fastclaw.core.network.apis.response.ResumeUploadSubmitMyData
import com.shiyuan.fastclaw.core.network.apis.response.SessionEnterData
import com.shiyuan.fastclaw.core.network.apis.response.ShelfStatusData
import com.shiyuan.fastclaw.core.network.apis.response.UnderstandingDetail
import com.shiyuan.fastclaw.core.network.apis.response.UnderstandingScore
import com.shiyuan.fastclaw.core.network.apis.response.UpgradeData
import com.shiyuan.fastclaw.core.network.apis.response.UserHomeData
import com.shiyuan.fastclaw.core.network.apis.response.UserPointItemData
import com.shiyuan.fastclaw.core.network.apis.response.UserPointsData
import okhttp3.MultipartBody
import retrofit2.http.Body
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Streaming

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
     * 查询指定版本上架状态
     */
    @POST(ApiConstants.GET_APP_SHELF_STATUS)
    suspend fun getAppShelfStatus(): ApiResponse<ShelfStatusData?>

    /**
     * 第三方登录
     */
    @POST(ApiConstants.LOGIN_BY_THIRD)
    suspend fun loginByThird(@Body body: LoginByThirdBody): ApiResponse<UserHomeData?>

    /**
     * 绑定第三方账号
     */
    @POST(ApiConstants.BIND_THIRD_PARTY)
    suspend fun bindThirdParty(@Body body: BindThirdPartyBody): ApiResponse<BindThirdPartyData?>

    @POST(ApiConstants.REBIND_THIRD_PARTY)
    suspend fun rebindThirdParty(@Body body: BindThirdPartyBody): ApiResponse<BindThirdPartyData?>

    /**
     * 解除第三方账号绑定并与当前账号绑定
     */
    @POST(ApiConstants.UNBIND_THIRD_PARTY)
    suspend fun unbindThirdParty(@Body body: UnbindThirdPartyBody): ApiResponse<Any?>

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
     * 更新头像
     */
    @POST(ApiConstants.UPDATE_USER_AVATAR)
    suspend fun updateUserAvatar(@Body body: UpdateAvatarBody): ApiResponse<UserHomeData?>

    /**
     * APP版本检测，请求参数复用全局请求头 mobileType 和 appVersion
     */
    @POST(ApiConstants.CHECK_APP_VERSION)
    suspend fun checkAppVersion(): ApiResponse<UpgradeData>

    /**
     * 注销账号
     */
    @POST(ApiConstants.LOG_OFF)
    suspend fun logOff(@Body body: UserTokenBody): ApiResponse<Any?>

    /**
     * 提交用户的反馈数据
     */
    @POST(ApiConstants.FEED_BACK)
    suspend fun feedback(@Body body: FeedbackBody): ApiResponse<Any?>

    /**
     * 用户日活记录
     */
    @POST(ApiConstants.DAILY)
    suspend fun daily(@Body body: UserTokenBody): ApiResponse<Any>

    /**
     * 发送验证码
     */
    @POST(ApiConstants.SEND_SMS_VALIDATE_CODE)
    suspend fun sendSmsValidateCode(@Body body: SendSmsValidateCodeBody): ApiResponse<Any?>

    /**
     * 手机号绑定状态前置校验
     */
    @POST(ApiConstants.CHECK_MOBILE_BIND_STATUS)
    suspend fun checkMobileBindStatus(@Body body: CheckMobileBindStatusBody): ApiResponse<CheckMobileBindStatusData?>

    /**
     * 绑定手机号
     */
    @POST(ApiConstants.BIND_PHONE)
    suspend fun bindPhone(@Body body: BindPhoneBody): ApiResponse<Any?>

    /**
     * 三方登录后绑定手机号
     */
    @POST(ApiConstants.BIND_MOBILE)
    suspend fun bindMobile(@Body body: BindMobileBody): ApiResponse<UserHomeData?>

    /**
     * 换绑手机号
     */
    @POST(ApiConstants.CHANGE_MOBILE)
    suspend fun changeMobile(@Body body: ChangeMobileBody): ApiResponse<Any?>

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
     * 刷新登录 Token
     */
    @POST(ApiConstants.REFRESH_TOKEN)
    suspend fun refreshToken(@Body body: UserTokenBody): ApiResponse<RefreshTokenData?>

    /**
     * 取消注销
     */
    @POST(ApiConstants.CANCEL_LOG_OFF)
    suspend fun cancelLogOff(@Body body: UserTokenBody): ApiResponse<Any?>

    /**
     * 获取订单状态
     */
    @POST(ApiConstants.GET_ORDER_STATUS)
    suspend fun getOrderStatus(@Body body: OrderStatusBody): ApiResponse<OrderStatusInfo>

    /**
     * 分页查询候选人支付订单列表
     */
    @POST(ApiConstants.QUERY_PAY_ORDER_LIST)
    suspend fun queryPayOrderList(@Body body: PagedBody): ApiResponse<PagedList<PayOrderInfo>?>

    /**
     * 获取banner信息
     */
    @POST(ApiConstants.APP_BANNERS)
    suspend fun getAppBanner(@Body body: BannerBody): ApiResponse<List<BannerInfo>?>

    /**
     * 首页作品列表
     */
    @POST(ApiConstants.RECOMMEND_WORK_LIST)
    suspend fun recommendWorkList(@Body body: PagedBody): ApiResponse<PagedList<WorkInfo>?>

    /**
     * 编辑我的作品信息
     */
    @POST(ApiConstants.PLAY_EVENT_REPORT)
    suspend fun playEventReport(@Body body: PlayEventBody): ApiResponse<Any>

    /**
     * 举报提交
     */
    @POST(ApiConstants.REPORT_SUBMISSION)
    suspend fun reportSubmission(@Body body: ReportSubmissionBody): ApiResponse<Any>

    /**
     * 查询用户钱包信息
     */
    @POST(ApiConstants.QUERY_WALLET_INFO)
    suspend fun queryWalletInfo(@Body body: UserTokenBody): ApiResponse<WalletInfo?>

    /**
     * App转发H5支付-微信下单
     */
    @POST(ApiConstants.H5_PAY)
    suspend fun h5PayWx(@Body body: Any?): ApiResponse<H5InterviewPassPackageRep?>

    /**
     * 购买会员-支付宝方式
     */
    @POST(ApiConstants.H5_PAY)
    suspend fun h5PayAlipay(@Body body: Any?): ApiResponse<H5InterviewPassPackageRep?>

    /**
     * 创建app微信支付订单
     */
    @POST(ApiConstants.CREATE_APP_WECHAT_PAY_ORDER)
    suspend fun createAppWechatPayOrder(@Body body: TradePayCreateBody): ApiResponse<PaymentData?>

    /**
     * 创建app支付宝支付订单
     */
    @POST(ApiConstants.CREATE_APP_ALIPAY_ORDER)
    suspend fun createAppAlipayOrder(@Body body: TradePayCreateBody): ApiResponse<PaymentData?>

    /**
     * 查询安卓充值页支付方式展示列表
     */
    @POST(ApiConstants.QUERY_TRADE_PAY_WAY_LIST)
    suspend fun queryTradePayWayList(): ApiResponse<TradePayWayListData?>

    /**
     * 内容安全-图片校验
     */
    @POST(ApiConstants.SENSITIVE_IMAGE_CHECK)
    suspend fun sensitiveImageCheck(@Body body: ImgUrlBody): ApiResponse<SensitiveWordCheckData?>

    /**
     * 获取我的报告列表
     */
    @POST(ApiConstants.GET_MY_REPORT_LIST)
    suspend fun getMyReportList(@Body body: PagedBody): ApiResponse<PagedList<ReportListItem>?>

    /**
     * 获取面试报告列表
     */
    @POST(ApiConstants.INTERVIEW_REPORT_LIST_QUERY)
    suspend fun getInterviewReportList(@Body body: PagedBody): ApiResponse<PagedList<InterviewReportListItem>?>

    /**
     * 从“我的报告”列表隐藏报告
     */
    @POST(ApiConstants.INTERVIEW_REPORT_DELETE)
    suspend fun deleteInterviewReport(@Body body: InterviewReportDeleteBody): ApiResponse<Any?>

    /**
     * 上传简历
     */
    @Multipart
    @POST(ApiConstants.UPLOAD_RESUME)
    suspend fun uploadResume(
        @Part file: MultipartBody.Part,
        @Part("fileName") fileName: String
    ): ApiResponse<Any?>

    /**
     * 创建文件上传凭证
     */
    @POST(ApiConstants.CREATE_FILE_UPLOAD_CREDENTIAL)
    suspend fun createFileUploadCredential(
        @Body body: CreateFileUploadCredentialBody
    ): ApiResponse<List<FileUploadCredentialData>?>

    /**
     * 确认文件上传完成
     */
    @POST(ApiConstants.COMPLETE_FILE_UPLOAD)
    suspend fun completeFileUpload(
        @Body body: CompleteFileUploadBody
    ): ApiResponse<List<FileSecurityStatusData>?>

    /**
     * 查询文件安全状态
     */
    @POST(ApiConstants.QUERY_FILE_SECURITY_STATUS)
    suspend fun queryFileSecurityStatus(
        @Body body: FileSecurityStatusQueryBody
    ): ApiResponse<List<FileSecurityStatusData>?>

    @POST(ApiConstants.GET_RESUME_LATEST)
    suspend fun queryLatestResume(): ApiResponse<ResumeData?>


    @POST(ApiConstants.RESUME_MATERIAL_QUALITY)
    suspend fun resumeMaterialQuality(@Body body: ResumeMaterialQualityBody): ApiResponse<ResumeData?>

    @POST(ApiConstants.RESUME_MATERIAL_QUALITY_RESULT)
    suspend fun resumeMaterialQualityResult(@Body body: ResumeMaterialQualityBody): ApiResponse<ResumeData?>



    @POST(ApiConstants.JD_RESUME_MATERIAL_QUALITY)
    suspend fun jdMaterialQuality(@Body body: JdMaterialQualityBody): ApiResponse<ResumeData?>

    @POST(ApiConstants.JD_RESUME_MATERIAL_QUALITY_RESULT)
    suspend fun jdMaterialQualityResult(@Body body: JdMaterialQualityBody): ApiResponse<ResumeData>

    /**
     * 查询通关包简历状态
     */
    @POST(ApiConstants.INTERVIEW_PASS_PACKAGE_RESUME_STATUS)
    suspend fun queryInterviewPassPackageResumeStatus(
        @Body body: InterviewPassPackageResumeStatusBody
    ): ApiResponse<InterviewPassPackageResumeStatusData?>

    @POST(ApiConstants.INTERVIEW_REPORT)
    suspend fun interviewReport(@Body body: InterviewReportData): ApiResponse<ReportData>


    @POST(ApiConstants.INTERVIEW_REPORT_RESULT)
    suspend fun interviewReportResult(@Body body: InterviewReportData): ApiResponse<ReportData>

    /**
     * 查询虾条信息(积分)
     * @see <a href="https://space-nzj9og.w.eolink.com/home/api-studio/inside/Gq7fyxV22d65e957b3984991a671e481af3e9241a116bec/api/3376873/detail/56521903?spaceKey=space-nzj9og">
     */
    @POST(ApiConstants.GET_USER_POINTS)
    suspend fun getUserPoints(): ApiResponse<UserPointsData>

    /**
     * 分页查询虾条明细
     * @see <a href="https://space-nzj9og.w.eolink.com/home/api-studio/inside/Gq7fyxV22d65e957b3984991a671e481af3e9241a116bec/api/3376873/detail/56521904?spaceKey=space-nzj9og">
     */
    @POST(ApiConstants.QUERY_USER_POINTS_PAGE)
    suspend fun queryUserPointsPage(@Body body: PagedBody): ApiResponse<PagedList<UserPointItemData>?>

    /**
     * 分页查询邀请记录
     */
    @POST(ApiConstants.QUERY_INVITE_RECORD_PAGE)
    suspend fun queryInviteRecordPage(@Body body: PagedBody): ApiResponse<InviteRecordPageData?>

    @POST(ApiConstants.QUERY_INVITE_STATS)
    suspend fun queryInviteStats(): ApiResponse<InviteStatsData?>

    /**
     * 查询邀请分享信息
     */
    @POST(ApiConstants.QUERY_INVITE_SHARE_INFO)
    suspend fun queryInviteShareInfo(@Body body: InviteShareInfoBody): ApiResponse<UserHomeData?>

    /**
     * 查询虾条充值配置列表
     */
    @POST(ApiConstants.QUERY_POINT_CONFIG_LIST)
    suspend fun queryPointConfigList(): ApiResponse<PointConfigListData?>

    @POST(ApiConstants.COMMON_INIT)
    suspend fun commonInit(): ApiResponse<Any>


    @POST(ApiConstants.SIGN_OUT)
    suspend fun signOut(): ApiResponse<Any>

    @POST(ApiConstants.GET_SCHEME)
    suspend fun getScheme(@Body data: Any): ApiResponse<SchemeData?>

    /**
     * 进入或恢复会话
     * @see <a href="https://space-nzj9og.w.eolink.com/home/api-studio/inside/Gq7fyxV22d65e957b3984991a671e481af3e9241a116bec/api/3376873/detail/56544129?spaceKey=space-nzj9og">
     */
    @POST(ApiConstants.SESSION_ENTER)
    suspend fun enterSession(@Body body: SessionEnterBody): ApiResponse<SessionEnterData?>

    /**
     * 流式发送用户消息（求职 Agent SSE）
     * @see <a href="https://space-nzj9og.w.eolink.com/home/api-studio/inside/Gq7fyxV22d65e957b3984991a671e481af3e9241a116bec/api/3387364/detail/56544130?spaceKey=space-nzj9og">
     */
    @Streaming
    @POST(ApiConstants.JOB_AGENT_STREAM)
    fun sendMsgToAgent(@Body body: JobAgentStreamBody): Flow<SSEvent>

    @POST(ApiConstants.MOCK_INTERVIEW_SESSION_START)
    suspend fun startMockInterviewSession(@Body body: MockInterviewStartBody): ApiResponse<MockInterviewStartData?>

    @POST(ApiConstants.MOCK_INTERVIEW_UNFINISHED_QUERY)
    suspend fun queryMockInterviewUnfinished(): ApiResponse<MockInterviewUnfinishedData?>


    @POST(ApiConstants.MINIPROGRAM_CODERESUMEBINDING_QUERY)
    suspend fun queryCodeResumeBinding(@Body body: Map<String, String>): ApiResponse<ResumeData?>

    @Streaming
    @POST(ApiConstants.MOCK_INTERVIEW_STREAM)
    fun mockInterviewStream(@Body body: MockInterviewStreamBody): Flow<SSEvent>

    @POST(ApiConstants.MOCK_INTERVIEW_FORCE_CANCEL)
    suspend fun forceCancelMockInterview(@Body body: MockInterviewForceCancelBody): ApiResponse<MockInterviewForceCancelData?>

    @POST(ApiConstants.MOCK_INTERVIEW_AUDIO_TOKEN_QUERY)
    suspend fun queryMockInterviewAudioToken(@Body body: MockInterviewAudioTokenBody): ApiResponse<MockInterviewAudioTokenData?>

    @POST(ApiConstants.MOCK_INTERVIEW_REPORT_PROGRESS_QUERY)
    suspend fun queryMockInterviewReportProgress(@Body body: MockInterviewReportProgressBody): ApiResponse<MockInterviewReportProgressData?>

    @POST(ApiConstants.INTERVIEW_PASS_PACKAGE_STATUS_QUERY)
    suspend fun queryPassPackageProgress(@Body body: PassPackageStatusQueryBody): ApiResponse<PassPackageStatusData?>

    @POST(ApiConstants.INTERVIEW_PASS_PACKAGE_REGENERATE)
    suspend fun regeneratePassPackageReport(@Body body: PassPackageRegenerateBody): ApiResponse<PassPackageRegenerateData?>

    @POST(ApiConstants.MOCK_INTERVIEW_REPORT_RETRY)
    suspend fun retryMockInterviewReport(@Body body: MockInterviewReportRetryBody): ApiResponse<MockInterviewReportProgressData?>

    /**
     * 取消流式生成（求职 Agent）
     * @see <a href="https://space-nzj9og.w.eolink.com/home/api-studio/inside/Gq7fyxV22d65e957b3984991a671e481af3e9241a116bec/api/3387364/detail/56544131?spaceKey=space-nzj9og">
     */
    @POST(ApiConstants.JOB_AGENT_CANCEL)
    suspend fun cancelStreamGenerate(@Body body: JobAgentCancelBody): ApiResponse<JobAgentCancelData?>

    /**
     * 查询通信会话（求职 Agent，仅查状态不建 SSE）
     * @see <a href="https://space-nzj9og.w.eolink.com/home/api-studio/inside/Gq7fyxV22d65e957b3984991a671e481af3e9241a116bec/api/3387364/detail/56544132?spaceKey=space-nzj9og">
     */
    @POST(ApiConstants.JOB_AGENT_CONVERSATION_QUERY)
    suspend fun queryConversation(@Body body: ConversationQueryBody): ApiResponse<ConversationQueryData?>

    @POST(ApiConstants.JOB_AGENT_MODULE_LIST_QUERY)
    suspend fun queryJobAgentModuleList(): ApiResponse<List<JobAgentModuleData>?>

    @POST(ApiConstants.ACTIVITY_STATUS_QUERY)
    suspend fun queryActivityStatus(): ApiResponse<ActivityStatusData?>

    /**
     * 提交卡片或按钮动作（求职 Agent）
     * @see <a href="https://space-nzj9og.w.eolink.com/home/api-studio/inside/Gq7fyxV22d65e957b3984991a671e481af3e9241a116bec/api/3387364/detail/56544133?spaceKey=space-nzj9og">
     */
    @POST(ApiConstants.JOB_AGENT_ACTION_SUBMIT)
    suspend fun submitAction(@Body body: ActionSubmitBody): ApiResponse<ActionSubmitData?>

    /**
     * 更新顶部求职状态
     */
    @POST(ApiConstants.JOB_AGENT_JOB_STATUS_UPDATE)
    suspend fun updateJobStatus(@Body body: JobStatusUpdateBody): ApiResponse<JobSearchStatus?>

    /**
     * 查询历史消息分页（求职 Agent）
     * @see <a href="https://space-nzj9og.w.eolink.com/home/api-studio/inside/Gq7fyxV22d65e957b3984991a671e481af3e9241a116bec/api/3387364/detail/56544134?spaceKey=space-nzj9og">
     */
    @POST(ApiConstants.JOB_AGENT_MESSAGE_PAGE)
    suspend fun queryMessagePage(@Body body: MessagePageBody): ApiResponse<MessagePageData?>

    /**
     * 拉取增量消息（求职 Agent）
     * @see <a href="https://space-nzj9og.w.eolink.com/home/api-studio/inside/Gq7fyxV22d65e957b3984991a671e481af3e9241a116bec/api/3387364/detail/56544135?spaceKey=space-nzj9og">
     */
    @POST(ApiConstants.JOB_AGENT_MESSAGE_PULL)
    suspend fun pullMessages(@Body body: MessagePullBody): ApiResponse<MessagePullData?>

    /**
     * 查询公司背调公司搜索结果
     */
    @POST(ApiConstants.JOB_AGENT_COMPANY_BACKGROUND_COMPANY_SEARCH_QUERY)
    suspend fun queryCompanyBackgroundCompanySearch(@Body body: CompanyBackgroundCompanySearchQueryBody): ApiResponse<CompanyBackgroundCompanySearchData?>

    /**
     * 提交公司背调报告生成前置校验
     */
    @POST(ApiConstants.JOB_AGENT_COMPANY_BACKGROUND_PRE_CHECK_SUBMIT)
    suspend fun submitCompanyBackgroundPreCheck(@Body body: CompanyBackgroundPreCheckSubmitBody): ApiResponse<CompanyBackgroundPreCheckResultData?>

    /**
     * 查询公司背调报告生成前置校验结果
     */
    @POST(ApiConstants.JOB_AGENT_COMPANY_BACKGROUND_PRE_CHECK_RESULT_QUERY)
    suspend fun queryCompanyBackgroundPreCheckResult(@Body body: CompanyBackgroundPreCheckResultQueryBody): ApiResponse<CompanyBackgroundPreCheckResultData?>

    /**
     * 查询公司背调报告生成结果
     */
    @POST(ApiConstants.JOB_AGENT_COMPANY_BACKGROUND_GENERATE_RESULT_QUERY)
    suspend fun queryCompanyBackgroundGenerateResult(@Body body: CompanyBackgroundGenerateResultQueryBody): ApiResponse<CompanyBackgroundGenerateResultData?>

    /**
     * 重新生成公司背调失败报告
     */
    @POST(ApiConstants.JOB_AGENT_COMPANY_BACKGROUND_REGENERATE_SUBMIT)
    suspend fun regenerateCompanyBackgroundReport(@Body body: CompanyBackgroundRegenerateSubmitBody): ApiResponse<CompanyBackgroundPreCheckResultData?>

    /**
     * 查询当前可确认简历（求职 Agent）
     * @see <a href="https://space-nzj9og.w.eolink.com/home/api-studio/inside/Gq7fyxV22d65e957b3984991a671e481af3e9241a116bec/api/3387364/detail/56544136?spaceKey=space-nzj9og">
     */
    @POST(ApiConstants.JOB_AGENT_RESUME_CURRENT_QUERY)
    suspend fun queryCurrentResume(@Body body: CurrentResumeQueryBody): ApiResponse<CurrentResumeData?>

    /**
     * 确认或否认当前简历（求职 Agent）
     * @see <a href="https://space-nzj9og.w.eolink.com/home/api-studio/inside/Gq7fyxV22d65e957b3984991a671e481af3e9241a116bec/api/3387364/detail/56544137?spaceKey=space-nzj9og">
     */
    @POST(ApiConstants.JOB_AGENT_RESUME_CURRENT_CONFIRM)
    suspend fun confirmCurrentResume(@Body body: ResumeConfirmBody): ApiResponse<ResumeConfirmData?>

    /**
     * 提交简历上传结果（求职 Agent）
     * @see <a href="https://space-nzj9og.w.eolink.com/home/api-studio/inside/Gq7fyxV22d65e957b3984991a671e481af3e9241a116bec/api/3387364/detail/56544138?spaceKey=space-nzj9og">
     */
    @POST(ApiConstants.JOB_AGENT_RESUME_UPLOAD_SUBMIT)
    suspend fun submitResumeUpload(@Body body: ResumeUploadSubmitBody): ApiResponse<ResumeUploadSubmitData?>

    /**
     * 确认属性候选值（求职 Agent）；响应 envelope 复用 [ActionSubmitData]
     * @see <a href="https://space-nzj9og.w.eolink.com/home/api-studio/inside/Gq7fyxV22d65e957b3984991a671e481af3e9241a116bec/api/3387364/detail/56544139?spaceKey=space-nzj9og">
     */
    @POST(ApiConstants.JOB_AGENT_ATTRIBUTE_CONFIRM)
    suspend fun confirmAttribute(@Body body: AttributeConfirmBody): ApiResponse<ActionSubmitData?>

    /**
     * 拒绝属性候选值（求职 Agent）；响应 envelope 复用 [ActionSubmitData]
     * @see <a href="https://space-nzj9og.w.eolink.com/home/api-studio/inside/Gq7fyxV22d65e957b3984991a671e481af3e9241a116bec/api/3387364/detail/56544140?spaceKey=space-nzj9og">
     */
    @POST(ApiConstants.JOB_AGENT_ATTRIBUTE_REJECT)
    suspend fun rejectAttribute(@Body body: AttributeRejectBody): ApiResponse<ActionSubmitData?>

    @POST(ApiConstants.QUERY_REGION_DICT)
    suspend fun queryRegionDict(@Body body: RegionDictQueryBody): ApiResponse<RegionDictData?>


    @POST(ApiConstants.DICT_INDUSTRY_QUERY)
    suspend fun queryDictIndustryQuery(@Body body: DictIndustryQueryBody?): ApiResponse<IndustryDict?>


    @POST(ApiConstants.DICT_POSITION_QUERY)
    suspend fun queryDictPositionQuery(@Body body: DictPositionQueryBody?): ApiResponse<PositionDict?>

    @POST(ApiConstants.DICT_WELFARE_QUERY)
    suspend fun queryDictWelfareQuery(@Body body: DictWelfareQueryBody?): ApiResponse<WelfareDict?>


    @POST(ApiConstants.UNDERSTANDING_SCORE)
    suspend fun understandingScore(): ApiResponse<UnderstandingScore?>


    @POST(ApiConstants.UNDERSTANDING_DETAIL)
    suspend fun understandingDetail(): ApiResponse<UnderstandingDetail?>


    @POST(ApiConstants.RESUME_QUERY)
    suspend fun resumeQuery(@Body body: PagedBody): ApiResponse<PagedList<ResumeListData>?>

    /**
     * 删除我的简历列表项
     */
    @POST(ApiConstants.RESUME_LIST_DELETE)
    suspend fun deleteResume(@Body body: ResumeListDeleteBody): ApiResponse<Any?>

    @POST(ApiConstants.JOB_AGENT_RESUME_UPLOAD_MY_SUBMIT)
    suspend fun submitResumeMyUpload(@Body body: ResumeUploadSubmitMyBody): ApiResponse<ResumeUploadSubmitMyData?>

    /**
     * 查询在线简历信息
     */
    @POST(ApiConstants.JOB_AGENT_ONLINE_RESUME_INFO_QUERY)
    suspend fun queryOnlineResumeInfo(@Body body: Any): ApiResponse<OnlineResumeInfoData?>



    /**
     * 更新在线简历属性
     */
    @POST(ApiConstants.JOB_AGENT_ONLINE_RESUME_ATTRIBUTE_UPDATE)
    suspend fun updateOnlineResumeAttribute(@Body body: OnlineResumeAttributeUpdateBody): ApiResponse<Any?>

    /**
     * 查询线上简历单个普通属性
     */
    @POST(ApiConstants.JOB_AGENT_ONLINE_RESUME_ATTRIBUTE_QUERY)
    suspend fun queryOnlineResumeAttribute(@Body body: OnlineResumeAttributeQueryBody): ApiResponse<OnlineResumeAttributeData?>



    /**
     * 查询在线简历信息
     */
    @POST(ApiConstants.JOB_PREFERENCE_RESUME_INFO_QUERY)
    suspend fun queryJobPreferencesInfo(@Body body: Any): ApiResponse<OnlineResumeInfoData?>

    /**
     * 查询属性数组候选值（求职 Agent）
     */
    @POST(ApiConstants.JOB_AGENT_ATTRIBUTE_ARRAY_QUERY)
    suspend fun queryAttributeArray(@Body body: AttributeArrayQueryBody): ApiResponse<AttributeArrayQueryData?>

    @POST(ApiConstants.JOB_AGENT_DICT_REGION_QUERY)
    suspend fun queryCityList(@Body body: JobCityQueryBody): ApiResponse<JobCityData?>


    @POST(ApiConstants.JOBAGENT_ATTRIBUTE_ARRAY_SAVE)
    suspend fun attributeSave(@Body body: Any): ApiResponse<AttributeSave?>

    @POST(ApiConstants.ONLINERESUME_ATTRIBUTE_ARRAY_SAVE)
    suspend fun onlineResumeAttributeSave(@Body body: Any): ApiResponse<AttributeSave?>

    @POST(ApiConstants.ONLINERESUME_ATTRIBUTE_ARRAY_DELETE)
    suspend fun onlineResumeAttributeDelete(@Body body: Any): ApiResponse<AttributeSave?>

}
