package com.shiyuan.fastclaw.core

import android.text.TextUtils
import android.util.Log
import com.blankj.utilcode.util.NetworkUtils
import com.google.gson.Gson
import com.nhsd.basic.Session
import com.nhsd.basic.Session.NetworkChangedListener
import com.nhsd.basic.bus.FlowEventBus
import com.nhsd.basic.ext.launchNoCrash
import com.shiyuan.fastclaw.App
import com.shiyuan.fastclaw.BuildConfig
import com.shiyuan.fastclaw.common.Constants
import com.shiyuan.fastclaw.common.SP
import com.shiyuan.fastclaw.common.ext.ioScope
import com.shiyuan.fastclaw.common.utils.DateUtil
import com.shiyuan.fastclaw.common.utils.MmkvDelegate
import com.shiyuan.fastclaw.core.network.apis.ApiFactory.apiService
import com.shiyuan.fastclaw.core.network.apis.request.InviteAttribution
import com.shiyuan.fastclaw.core.network.apis.request.UserTokenBody
import com.shiyuan.fastclaw.core.network.apis.response.AppConfigData
import com.shiyuan.fastclaw.core.network.apis.response.TotalComposeConfigData
import com.tencent.mmkv.MMKV
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * App配置信息与角色是否登录或切换无关
 */
class Config : NetworkChangedListener {
    private val legacySp = SP(CONFIG_DATA_MMKV_ID)
    private val mmkv by lazy { MMKV.mmkvWithID(CONFIG_DATA_MMKV_ID) ?: MMKV.defaultMMKV() }

    /** 是否正在请求 App 配置，避免重复请求。 */
    private var isRequesting = false

    /** JSON 序列化工具。 */
    private val gson: Gson

    /** App 配置接口缓存数据。 */
    private var appConfigData: AppConfigData? = null

    /** 是否首次打开 App。 */
    var isFirstOpenApp: Boolean by MmkvDelegate(mmkvKey(FIRST_OPEN_APP), true) { mmkv }
        private set

    /** 是否首次设置书籍音色。 */
    var isFirstSetBookVoice: Boolean by MmkvDelegate(mmkvKey(FIRST_SET_BOOK_VOICE), true) { mmkv }
        private set

    /** 是否首次设置护眼模式。 */
    var isFirstEyeProtectionSetting: Boolean by MmkvDelegate(
        mmkvKey(FIRST_EYE_PROTECTION_SETTING),
        true
    ) { mmkv }
        private set

    /** 设备对象 id。 */
    var objectId: String? by MmkvDelegate(mmkvKey(OBJECT_ID), null as String?) { mmkv }
        private set

    /** 审核状态当前进程内存值。 */
    private var isInReviewMemory = false

    /** 当前是否处于审核状态。 */
    var isInReview: Boolean
        get() = isInReviewMemory
        private set(value) {
            isInReviewMemory = value
        }

    /** 接口环境当前进程内存值。 */
    private var environmentMemory = 0

    /** 当前接口环境。 */
    var environment: Int
        get() = environmentMemory
        private set(value) {
            environmentMemory = value
        }

    /** 是否开启开发模式。 */
    var isDevelopmentMode: Boolean by MmkvDelegate(mmkvKey(DEVELOPMENT_MODE), false) { mmkv }
        private set

    /** 是否允许通知。 */
    var isNotificationEnable: Boolean by MmkvDelegate(mmkvKey(NOTIFICATION_ENABLE), true) { mmkv }
        private set

    /** 是否展示跳转弹窗。 */
    var isShowJumpPop: Boolean by MmkvDelegate(mmkvKey(SHOW_JUMP_POP), true) { mmkv }
        private set

    /**
     * 测试环境下自己输入的 ip，供后台测试用
     */
    var baseUrlCustom: String? by MmkvDelegate(mmkvKey(Constants.BASE_URL_CUSTOM), null as String?) { mmkv }
        private set

    /** 设备推送 token。 */
    private var deviceToken: String? by MmkvDelegate(mmkvKey(DEVICE_TOKEN), null as String?) { mmkv }

    /** 邀请归因信息。 */
    var inviteAttribution: InviteAttribution? = null
        private set

    /** 外部渠道 open id。 */
    var opid: String? by MmkvDelegate(mmkvKey(OPID), null as String?) { mmkv }
        private set

    /** 护眼模式是否开启。 */
    var eyeProtectionEnabled: Boolean? by MmkvDelegate(mmkvKey(EYE_PROTECTION_ENABLE), false) { mmkv }
        private set

    init {
        migrateLegacyConfigIfNeeded()
        this.objectId = objectId ?: "1"
        this.isInReview =
            if (ifInReviewStatus()) {
                getBooleanValue(IN_REVIEW, ifInitialStatusInReview())
            } else {
                false
            }
        this.environment = getIntValue(ENVIRONMENT, initialState)
        this.baseUrlCustom = baseUrlCustom ?: BuildConfig.SERVER_URL
        this.inviteAttribution = InviteAttribution(
            inviteCode = getStringValue(INVITE_CODE, null),
            inviteScene = getStringValue(INVITE_SCENE, null)
        ).takeIf { !it.inviteCode.isNullOrBlank() || !it.inviteScene.isNullOrBlank() }
        gson = Gson()
    }

    private fun ifInitialStatusInReview(): Boolean {
        when (Session.getInstance().channel) {
            "huawei" -> return true
        }
        return false
    }

    private fun ifInReviewStatus(): Boolean {
        val buildTime = BuildConfig.BUILD_TIMESTAMP
        val timeDiff = System.currentTimeMillis() - buildTime
        return timeDiff < REVIEW_OUT_TIME
    }

    fun initAppConfig() {
        Session.getInstance().addNetworkChangedListener(this)
        updateConfig()
    }

    fun updateConfig() {
        if (NetworkUtils.isConnected() && !isRequesting) {
            isRequesting = true
            ioScope.launchNoCrash(block = {
                val response = apiService.appConfig(UserTokenBody())
                if (response.isSuccess) {
                    withContext(Dispatchers.Main) {
                        Session.getInstance().removeNetworkChangedListener(this@Config)
                    }
                    storeAppConfig(response.getData())
                }
                val shelfStatusData = apiService.getAppShelfStatus().data
                storeInReview(shelfStatusData?.shelfStatus == 2)
                isRequesting = false
            }, onError = {
                isRequesting = false
            })
        }
    }

    override fun onNetworkStateChanged(networkAvailable: Boolean) {
        updateConfig()
    }

    val initialState: Int
        get() = if (BuildConfig.DEBUG) UAT else ONLINE

    val appConfig: AppConfigData
        get() {
            if (appConfigData == null) {
                updateConfig()
                val json = getStringValue(APP_CONFIG, "")
                appConfigData = gson.fromJson(json, AppConfigData::class.java)
            }
            if (appConfigData == null) {
                appConfigData = DEFAULT_APP_CONFIG
            }
            return appConfigData!!
        }

    fun storeAppConfig(data: AppConfigData?) {
        this.appConfigData = data
        putStringValue(APP_CONFIG, gson.toJson(data))
    }

    fun storeComposeConfig(data: TotalComposeConfigData?) {
        putStringValue(COMPOSE_CONFIG, gson.toJson(data))
    }

    val totalComposeConfig: TotalComposeConfigData?
        get() {
            val json = getStringValue(COMPOSE_CONFIG, "")
            return gson.fromJson(json, TotalComposeConfigData::class.java)
        }

    val serviceUrl: String?
        get() {
            if (appConfigData != null && !TextUtils.isEmpty(appConfigData!!.userProtocolUrl)) {
                return appConfigData!!.userProtocolUrl
            }
            return Constants.SERVICE_URL
        }

    val privacyUrl: String?
        get() {
            if (appConfigData != null && !TextUtils.isEmpty(appConfigData!!.privacyProtocolUrl)) {
                return appConfigData!!.privacyProtocolUrl
            }
            return Constants.PRIVACY_URL
        }

    val userVipProtocolUrl
        get() = appConfigData?.userVipProtocolUrl ?: Constants.DEFAULT_USER_VIP_PROTOCOL_URL

    val algorithmUrl: String?
        get() = appConfigData?.algorithmUrl ?: Constants.DEFAULT_ALGORITHM_ANNOUNCEMENT

    fun notifyChange(vararg keys: String?) {
        FlowEventBus.postSync(Events.ConfigChangeEvent(keys))
    }

    fun storeObjectId(objectId: String?) {
        this.objectId = objectId
    }

    private fun storeInReviewWithNotify(inReview: Boolean) {
        if (this.isInReview != inReview) {
            storeInReview(inReview)
            notifyChange(IN_REVIEW)
        }
    }

    fun storeInReview(inReview: Boolean) {
        this.isInReview = inReview
        putBooleanValue(IN_REVIEW, inReview)
    }

    fun markAppOpenedWithNotify() {
        this.isFirstOpenApp = false
        notifyChange(FIRST_OPEN_APP)
    }

    fun markFirstSetBookVoiceWithNotify() {
        this.isFirstSetBookVoice = false
        notifyChange(FIRST_SET_BOOK_VOICE)
    }

    fun markFirstProtectionSettingWithNotify() {
        this.isFirstEyeProtectionSetting = false
        notifyChange(FIRST_EYE_PROTECTION_SETTING)
    }

    fun storeDeviceToken(deviceToken: String?) {
        this.deviceToken = deviceToken
    }

    fun storeInviteAttribution(inviteCode: String?, inviteScene: String?) {
        val validInviteCode = inviteCode?.takeIf { it.isNotBlank() }
        val validInviteScene = inviteScene?.takeIf { it.isNotBlank() }
        if (validInviteCode == null && validInviteScene == null) {
            return
        }
        inviteAttribution = InviteAttribution(
            inviteCode = validInviteCode,
            inviteScene = validInviteScene
        )
        putStringValue(INVITE_CODE, validInviteCode)
        putStringValue(INVITE_SCENE, validInviteScene)
    }

    fun clear() {
        this.isFirstOpenApp = true
        removeValue(FIRST_OPEN_APP)

        this.isInReview = true
        removeValue(IN_REVIEW)

        this.environment = this.initialState
        removeValue(ENVIRONMENT)
        this.isDevelopmentMode = false
        removeValue(DEVELOPMENT_MODE)
        this.inviteAttribution = null
        removeValue(INVITE_CODE)
        removeValue(INVITE_SCENE)
        removeValue(OPID)
        this.isShowJumpPop = true
        removeValue(SHOW_JUMP_POP)
        this.eyeProtectionEnabled = false
        removeValue(EYE_PROTECTION_ENABLE)
    }

    fun storeOpid(opid: String?) {
        this.opid = opid
    }

    fun storeDevelopmentMode(developMode: Boolean) {
        this.isDevelopmentMode = developMode
    }

    val isDebug: Boolean
        get() = if (this.isDevelopmentMode) true else BuildConfig.DEBUG_MODEL

    fun storeNotificationEnable(notificationEnable: Boolean) {
        this.isNotificationEnable = notificationEnable
    }

    fun markJumpPopShown() {
        this.isShowJumpPop = false
    }

    fun storeBaseUrlCustom(baseUrlCustom: String?) {
        this.baseUrlCustom = baseUrlCustom
    }

    fun storeEyeProtectionEnabled(eyeProtectionEnabled: Boolean) {
        if (this.eyeProtectionEnabled != eyeProtectionEnabled) {
            this.eyeProtectionEnabled = eyeProtectionEnabled
        }
    }

    fun activityPopupShowCount(id: String?): Int {
        if (id.isNullOrEmpty()) return 0
        val lastShowTime = getLongValue("activity_popup_show_time_$id", 0L)
        return if (DateUtil.isToday(lastShowTime)) {
            getIntValue("activity_popup_show_count_$id", 0)
        } else {
            0
        }
    }

    fun activityPopupShowMark(id: String?) {
        if (id.isNullOrEmpty()) return
        val lastShowTime = getLongValue("activity_popup_show_time_$id", 0L)
        val currentCount = if (DateUtil.isToday(lastShowTime)) {
            getIntValue("activity_popup_show_count_$id", 0)
        } else {
            0
        }
        putIntValue("activity_popup_show_count_$id", currentCount + 1)
        putLongValue("activity_popup_show_time_$id", System.currentTimeMillis())
    }

    fun activityPopupShowTime(id: String?): Long {
        return getLongValue("activity_popup_show_time_$id", 0L)
    }

    fun activityFloatShowCount(id: String?): Int {
        if (id.isNullOrEmpty()) return 0
        val lastShowTime = getLongValue("activity_float_show_time_$id", 0L)
        return if (DateUtil.isToday(lastShowTime)) {
            getIntValue("activity_float_show_count_$id", 0)
        } else {
            0
        }
    }

    fun activityFloatShowMark(id: String?) {
        if (id.isNullOrEmpty()) return
        val lastShowTime = getLongValue("activity_float_show_time_$id", 0L)
        val currentCount = if (DateUtil.isToday(lastShowTime)) {
            getIntValue("activity_float_show_count_$id", 0)
        } else {
            0
        }
        putIntValue("activity_float_show_count_$id", currentCount + 1)
        putLongValue("activity_float_show_time_$id", System.currentTimeMillis())
    }

    fun activityFloatShowTime(id: String?): Long {
        return getLongValue("activity_float_show_time_$id", 0L)
    }

    fun shouldShowRecommendUpgrade(intervalMillis: Long): Boolean {
        val lastDismissTime = getLongValue(RECOMMEND_UPGRADE_DISMISS_TIME, 0L)
        val currentTime = System.currentTimeMillis()
        return lastDismissTime == 0L ||
            currentTime < lastDismissTime ||
            currentTime - lastDismissTime >= intervalMillis
    }

    fun markRecommendUpgradeDismissed() {
        Log.d("onRemindLater", System.currentTimeMillis().toString())
        putLongValue(RECOMMEND_UPGRADE_DISMISS_TIME, System.currentTimeMillis())
    }

    fun notifyPowerConsume(scene: Int): Boolean {
        return getBooleanValue("notifyPowerConsume${User.get().userId}_$scene", true)
    }

    fun notifyPowerConsumeClose(scene: Int) {
        putBooleanValue("notifyPowerConsume${User.get().userId}_$scene", false)
    }

    fun isFirstRechargeShown(scene: Int): Boolean {
        return getBooleanValue("firstRechargeShown${User.get().userId}_$scene", false)
    }

    fun markFirstRechargeShown(scene: Int) {
        putBooleanValue("firstRechargeShown${User.get().userId}_$scene", true)
    }

    /**
     * 用户id作为key存储，记录用户是否第一次登录
     */
    fun recordFirstLogin(key: String) {
        putBooleanValue(key, false)
    }

    fun hasEnableNotification(): Boolean {
        return getBooleanValue(NOTIFICATION_SWITCH_KEY, false)
    }

    fun switchNotification(flag: Boolean) {
        putBooleanValue(NOTIFICATION_SWITCH_KEY, flag)
    }

    /**
     * 用户是否第一次登录
     */
    fun isFirstLogin(key: String): Boolean {
        return getBooleanValue(key, true)
    }

    private fun getStringValue(key: String, defaultValue: String?): String? {
        val realKey = mmkvKey(key)
        return if (mmkv.containsKey(realKey)) {
            mmkv.decodeString(realKey, defaultValue)
        } else {
            legacySp.getString(key, defaultValue)
        }
    }

    private fun putStringValue(key: String, value: String?) {
        val realKey = mmkvKey(key)
        if (value == null) {
            mmkv.removeValueForKey(realKey)
            legacySp.remove(key)
        } else {
            mmkv.encode(realKey, value)
        }
    }

    private fun getBooleanValue(key: String, defaultValue: Boolean): Boolean {
        val realKey = mmkvKey(key)
        return if (mmkv.containsKey(realKey)) {
            mmkv.decodeBool(realKey, defaultValue)
        } else {
            legacySp.getBoolean(key, defaultValue)
        }
    }

    private fun putBooleanValue(key: String, value: Boolean) {
        mmkv.encode(mmkvKey(key), value)
    }

    private fun getIntValue(key: String, defaultValue: Int): Int {
        val realKey = mmkvKey(key)
        return if (mmkv.containsKey(realKey)) {
            mmkv.decodeInt(realKey, defaultValue)
        } else {
            legacySp.getInt(key, defaultValue)
        }
    }

    private fun putIntValue(key: String, value: Int) {
        mmkv.encode(mmkvKey(key), value)
    }

    private fun getLongValue(key: String, defaultValue: Long): Long {
        val realKey = mmkvKey(key)
        return if (mmkv.containsKey(realKey)) {
            mmkv.decodeLong(realKey, defaultValue)
        } else {
            legacySp.getLong(key, defaultValue)
        }
    }

    private fun putLongValue(key: String, value: Long) {
        mmkv.encode(mmkvKey(key), value)
    }

    private fun removeValue(key: String) {
        mmkv.removeValueForKey(mmkvKey(key))
        legacySp.remove(key)
    }

    /** 首次初始化时迁移旧 SharedPreferences 配置数据。 */
    private fun migrateLegacyConfigIfNeeded() {
        if (mmkv.decodeBool(mmkvKey(CONFIG_DATA_MIGRATED), false)) {
            return
        }
        migrateLegacyBoolean(FIRST_OPEN_APP, true)
        migrateLegacyBoolean(FIRST_SET_BOOK_VOICE, true)
        migrateLegacyBoolean(FIRST_EYE_PROTECTION_SETTING, true)
        migrateLegacyString(OBJECT_ID)
        migrateLegacyBoolean(IN_REVIEW, ifInitialStatusInReview())
        migrateLegacyInt(ENVIRONMENT, initialState)
        migrateLegacyBoolean(DEVELOPMENT_MODE, false)
        migrateLegacyBoolean(NOTIFICATION_ENABLE, true)
        migrateLegacyBoolean(SHOW_JUMP_POP, true)
        migrateLegacyString(Constants.BASE_URL_CUSTOM, BuildConfig.SERVER_URL)
        migrateLegacyString(DEVICE_TOKEN)
        migrateLegacyString(INVITE_CODE)
        migrateLegacyString(INVITE_SCENE)
        migrateLegacyString(OPID)
        migrateLegacyBoolean(EYE_PROTECTION_ENABLE, false)
        mmkv.encode(mmkvKey(CONFIG_DATA_MIGRATED), true)
    }

    /** 迁移旧字符串配置项。 */
    private fun migrateLegacyString(key: String, default: String? = null) {
        val targetKey = mmkvKey(key)
        if (mmkv.containsKey(targetKey)) return
        legacySp.getString(key, default)?.let { mmkv.encode(targetKey, it) }
    }

    /** 迁移旧整型配置项。 */
    private fun migrateLegacyInt(key: String, default: Int) {
        val targetKey = mmkvKey(key)
        if (mmkv.containsKey(targetKey)) return
        mmkv.encode(targetKey, legacySp.getInt(key, default))
    }

    /** 迁移旧布尔配置项。 */
    private fun migrateLegacyBoolean(key: String, default: Boolean) {
        val targetKey = mmkvKey(key)
        if (mmkv.containsKey(targetKey)) return
        mmkv.encode(targetKey, legacySp.getBoolean(key, default))
    }

    companion object {
        /** Config MMKV 实例 id。 */
        private const val CONFIG_DATA_MMKV_ID = "CONFIG_DATA"

        /** Config 旧数据迁移完成标记。 */
        private const val CONFIG_DATA_MIGRATED = "CONFIG_DATA_MIGRATED"

        /** 首次打开 App 存储 key。 */
        const val FIRST_OPEN_APP: String = "firstOpenApp"

        /** 首次设置书籍音色存储 key。 */
        const val FIRST_SET_BOOK_VOICE: String = "firstSetBookVoice"

        /** 通知总开关存储 key。 */
        private const val NOTIFICATION_SWITCH_KEY = "notification_switch_key"

        /** 首次设置护眼模式存储 key。 */
        const val FIRST_EYE_PROTECTION_SETTING: String = "firstEyeProtectionSetting"

        /** 设备对象 id 存储 key。 */
        private const val OBJECT_ID = "objectId"

        /** 审核状态存储 key。 */
        const val IN_REVIEW: String = "inReview"

        /** 开发模式存储 key。 */
        private const val DEVELOPMENT_MODE = "developmentMode"

        /** 接口环境存储 key。 */
        private const val ENVIRONMENT: String = "environment"

        /** App 配置接口缓存 key。 */
        private const val APP_CONFIG = "app_config"

        /** 合成配置缓存 key。 */
        private const val COMPOSE_CONFIG: String = "COMPOSE_CONFIG"

        /** 邀请码归因存储 key。 */
        private const val INVITE_CODE = "invite_code"

        /** 邀请场景归因存储 key。 */
        private const val INVITE_SCENE = "invite_scene"

        /** 外部渠道 open id 存储 key。 */
        private const val OPID = "opid"

        /** 默认 App 配置。 */
        private val DEFAULT_APP_CONFIG: AppConfigData = AppConfigData()

        /** 通知权限状态存储 key。 */
        private const val NOTIFICATION_ENABLE = "NOTIFICATION_ENABLE"

        /** 跳转弹窗展示状态存储 key。 */
        private const val SHOW_JUMP_POP = "showJumpPop"

        /** 推荐升级弹窗稍后提醒时间存储 key。 */
        private const val RECOMMEND_UPGRADE_DISMISS_TIME = "recommend_upgrade_dismiss_time"

        /** 护眼模式开关存储 key。 */
        private const val EYE_PROTECTION_ENABLE: String = "eyeProtectionEnable"

        /** 日常环境值。 */
        const val DAILY: Int = 1

        /** UAT 环境值。 */
        const val UAT: Int = 2

        /** 线上环境值。 */
        const val ONLINE: Int = 3

        /** 设备推送 token 存储 key。 */
        const val DEVICE_TOKEN = "DEVICE_TOKEN"

        /** 审核态启动保护时长。 */
        private const val REVIEW_OUT_TIME = (5 * 24 * 60 * 60 * 1000).toLong()

        @JvmStatic
        fun get(): Config {
            return App.instance.config
        }

        /** 生成 Config 下统一前缀的 MMKV key。 */
        private fun mmkvKey(key: String): String = "${CONFIG_DATA_MMKV_ID}_$key"
    }
}
