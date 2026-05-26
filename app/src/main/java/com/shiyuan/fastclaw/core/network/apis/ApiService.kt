package com.shiyuan.fastclaw.core.network.apis

import UnderstandingDetail
import UnderstandingScore
import com.shiyuan.fastclaw.core.model.H5PayInfo
import com.shiyuan.fastclaw.core.network.apis.entity.BannerInfo
import com.shiyuan.fastclaw.core.network.apis.entity.OrderStatusInfo
import com.shiyuan.fastclaw.core.network.apis.entity.PaymentData
import com.shiyuan.fastclaw.core.network.apis.entity.ReportData
import com.shiyuan.fastclaw.core.network.apis.entity.ResumeData
import com.shiyuan.fastclaw.core.network.apis.entity.SchemeData
import com.shiyuan.fastclaw.core.network.apis.entity.SensitiveWordCheckData
import com.shiyuan.fastclaw.core.network.apis.entity.WalletInfo
import com.shiyuan.fastclaw.core.network.apis.entity.WorkInfo
import com.shiyuan.fastclaw.core.network.apis.request.BannerBody
import com.shiyuan.fastclaw.core.network.apis.request.BindPhoneBody
import com.shiyuan.fastclaw.core.network.apis.request.FeedbackBody
import com.shiyuan.fastclaw.core.network.apis.request.ImgUrlBody
import com.shiyuan.fastclaw.core.network.apis.request.InterviewReportData
import com.shiyuan.fastclaw.core.network.apis.request.JdMaterialQualityBody
import com.shiyuan.fastclaw.core.network.apis.request.LoginByFlashMobileBody
import com.shiyuan.fastclaw.core.network.apis.request.LoginByPhoneBody
import com.shiyuan.fastclaw.core.network.apis.request.LoginByThirdBody
import com.shiyuan.fastclaw.core.network.apis.request.OrderStatusBody
import com.shiyuan.fastclaw.core.network.apis.request.PagedBody
import com.shiyuan.fastclaw.core.network.apis.request.PlayEventBody
import com.shiyuan.fastclaw.core.network.apis.request.ReportSubmissionBody
import com.shiyuan.fastclaw.core.network.apis.request.ResumeMaterialQualityBody
import com.shiyuan.fastclaw.core.network.apis.request.SendSmsValidateCodeBody
import com.shiyuan.fastclaw.core.network.apis.request.TipOffBody
import com.shiyuan.fastclaw.core.network.apis.request.UserTokenBody
import com.shiyuan.fastclaw.core.network.apis.response.AppConfigData
import com.shiyuan.fastclaw.core.network.apis.response.ReportListItem
import com.shiyuan.fastclaw.core.network.apis.response.ShelfStatusData
import com.shiyuan.fastclaw.core.network.apis.response.UpgradeData
import com.shiyuan.fastclaw.core.network.apis.response.UserHomeData
import com.shiyuan.fastclaw.core.network.apis.response.UserPointItemData
import com.shiyuan.fastclaw.core.network.apis.response.UserPointsData
import okhttp3.MultipartBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

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
     * 获取订单状态
     */
    @POST(ApiConstants.GET_ORDER_STATUS)
    suspend fun getOrderStatus(@Body body: OrderStatusBody): ApiResponse<OrderStatusInfo>

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
    @POST(ApiConstants.H5_PAY_WX)
    suspend fun h5PayWx(@Body body: H5PayInfo): ApiResponse<PaymentData?>

    /**
     * 购买会员-支付宝方式
     */
    @POST(ApiConstants.H5_PAY_ALIPAY)
    suspend fun h5PayAlipay(@Body body: H5PayInfo): ApiResponse<PaymentData?>

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
     * 上传简历
     */
    @Multipart
    @POST(ApiConstants.UPLOAD_RESUME)
    suspend fun uploadResume(
        @Part file: MultipartBody.Part,
        @Part("fileName") fileName: String
    ): ApiResponse<Any?>

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



    @POST(ApiConstants.COMMON_INIT)
    suspend fun commonInit(): ApiResponse<Any>


    @POST(ApiConstants.SIGN_OUT)
    suspend fun signOut(): ApiResponse<Any>

    @POST(ApiConstants.GET_SCHEME)
    suspend fun getScheme(): ApiResponse<SchemeData?>


    @POST(ApiConstants.UNDERSTANDING_SCORE)
    suspend fun understandingScore(): ApiResponse<UnderstandingScore?>


    @POST(ApiConstants.UNDERSTANDING_DETAIL)
    suspend fun understandingDetail(): ApiResponse<UnderstandingDetail?>


    @POST(ApiConstants.RESUME_QUERY)
    suspend fun resumeQuery(@Body body: PagedBody): ApiResponse<PagedList<ResumeData>?>




}