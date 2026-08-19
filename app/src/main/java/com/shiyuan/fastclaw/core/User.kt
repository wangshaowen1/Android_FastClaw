package com.shiyuan.fastclaw.core

import android.text.TextUtils
import com.google.gson.Gson
import com.growingio.android.sdk.autotrack.GrowingAutotracker
import com.nhsd.basic.Session
import com.nhsd.basic.bus.FlowEventBus
import com.shiyuan.fastclaw.App
import com.shiyuan.fastclaw.common.SP
import com.shiyuan.fastclaw.common.utils.MmkvDelegate
import com.shiyuan.fastclaw.core.network.apis.entity.CookieUserInfo
import com.shiyuan.fastclaw.core.network.apis.request.CompleteUserDataInfoBody
import com.shiyuan.fastclaw.core.network.apis.response.UserHomeData
import com.shiyuan.fastclaw.core.manager.DataNexusManager
import com.tencent.mmkv.MMKV
import com.tulele.im.ImUserAccount
import kotlin.text.get

/**
 * 当前登录用户信息。
 */
class User : IUser, ImUserAccount {
    private val legacySp = SP(USER_DATA_MMKV_ID)
    private val mmkv by lazy { MMKV.mmkvWithID(USER_DATA_MMKV_ID) ?: MMKV.defaultMMKV() }

    /** 后端用户信息接口返回的账户 id。 */
    var userId: String? by MmkvDelegate(mmkvKey(USER_ID), null as String?) { mmkv }
        private set

    /** 用户绑定的手机号。 */
    var phoneNum: String? by MmkvDelegate(mmkvKey(PHONE_NUM), null as String?) { mmkv }
        private set

    /** 候选人/业务侧 uid。 */
    var userUid: String? by MmkvDelegate(mmkvKey(U_ID), null as String?) { mmkv }
        private set

    /** 当前用户的邀请码。 */
    var inviteCode: String? by MmkvDelegate(mmkvKey(INVITE_CODE), null as String?) { mmkv }
        private set

    /** 应用内展示昵称。 */
    var nickName: String? by MmkvDelegate(mmkvKey(NICK_NAME), null as String?) { mmkv }
        private set

    /** 性别：0 未知，1 男，2 女。 */
    var gender: Int? by MmkvDelegate(mmkvKey(GENDER), 0) { mmkv }
        private set

    /** 用户头像地址。 */
    var avatarUrl: String? by MmkvDelegate(mmkvKey(AVATAR_URL), null as String?) { mmkv }
        private set

    /** 后端返回的用户来源类型。 */
    var userSource: Int? by MmkvDelegate(mmkvKey(USER_SOURCE), 0) { mmkv }
        private set

    /** 后端返回的注册方式类型。 */
    var registerWay: Int? by MmkvDelegate(mmkvKey(REGISTER_WAY), 0) { mmkv }
        private set

    /** 后端返回的最后登录时间。 */
    var lastLoginTime: String? by MmkvDelegate(mmkvKey(LAST_LOGIN_TIME), null as String?) { mmkv }
        private set

    /** 后端返回的注册状态。 */
    var registerStatus: Int? by MmkvDelegate(mmkvKey(REGISTER_STATUS), 0) { mmkv }
        private set

    /** 当前登录 token。 */
    var authToken: String? by MmkvDelegate(mmkvKey(TOKEN), null as String?) { mmkv }
        private set

    /** 当前用户算力余额。 */
    var computingPower: Int by MmkvDelegate(mmkvKey(COMPUTING_POWER), 0) { mmkv }
        private set

    /** 课程购买状态。 */
    var purchaseCourseStatus: Int? by MmkvDelegate(mmkvKey(PURCHASE_COURSE_STATUS), 0) { mmkv }
        private set

    /** 是否为付费用户标记。 */
    var isPaid: Int? by MmkvDelegate(mmkvKey(IS_PAID), 0) { mmkv }
        private set

    /** 已绑定的微信昵称。 */
    var wechatNickName: String? by MmkvDelegate(mmkvKey(WECHAT_NICK_NAME), null as String?) { mmkv }
        private set

    /** 微信绑定状态。 */
    var wechatBindStatus: Int? by MmkvDelegate(mmkvKey(WECHAT_BIND_STATUS), WECHAT_BIND_STATUS_UNBOUND) { mmkv }
        private set

    /** 是否允许继续弹出通知权限提示。 */
    var requestNotificationsPermissionEnable: Boolean by MmkvDelegate(
        mmkvKey(REQUEST_NOTIFICATIONS_PERMISSION),
        true
    ) { mmkv }
        private set

    /** 同步给 H5 的用户信息字符串。 */
    val userInfoStr: String?
        get() {
            val userBasicInfo = CookieUserInfo(id, Session.getInstance().versionName)
            return Gson().toJson(userBasicInfo)
        }

    /** 当前是否为已登录状态。 */
    val isLogged: Boolean
        get() = !TextUtils.isEmpty(id)

    /**
     * 云信账号 ID，供 V2AccountManager 读取并作为 IM 登录账号。
     */
    var imAccessId: String? by MmkvDelegate<String?>(IM_ACCESS_ID, null) { mmkv }

    /**
     * 云信登录 token，供 V2AccountManager 读取并作为 IM 登录凭证。
     */
    var imToken: String? by MmkvDelegate<String?>(IM_TOKEN, null) { mmkv }

    init {
        migrateLegacyUserDataIfNeeded()
    }

    fun updateFrom(userData: UserHomeData?) {
        if (userData == null) return
        storeUid(userData.candidateId)
        storeInviteCode(userData.inviteCode)
        storePhoneNum(userData.phoneNum)
        storeUserId(userData.accountId)
        storeNickname(userData.nickName)
        storeNickname(userData.gender)
        storeAvatar(userData.avatarUrl)
        storeUserSource(userData.userSource)
        storeRegisterWay(userData.registerWay)
        storeLastLoginTime(userData.lastLoginTime)
        storePurchaseCourseStatus(userData.purchaseCourseStatus)
        storePaid(userData.userTag)
        storeWechatNickName(userData.wechatNickName)
        storeWechatBindStatus(userData.wechatBindStatus)
        userData.registerStatus?.apply {
            storeRegisterStatus(this)
        }
        userData.token?.apply {
            storeToken(this)
        }
        userData.imAccId?.let(::storeImAccessId)
        userData.imAccToken?.let(::storeImToken)
        notifyChange()

        if (id.isNullOrEmpty()) {
            return
        }
        GrowingAutotracker.get().setLoginUserId(userData.accountId)
        DataNexusManager.setUserUniqueId(userData.accountId)
    }

    fun updateFromCompleteUserBody(body: CompleteUserDataInfoBody) {
        body.avatarUrl?.let { storeAvatar(it) }
        body.nickName?.let { storeNickname(it) }
        body.gender?.let { storeNickname(it) }
        notifyChange()
    }

    private fun clear() {
        userId = null
        phoneNum = null
        userUid = null
        inviteCode = null
        nickName = null
        gender = null
        avatarUrl = null
        userSource = null
        registerWay = null
        lastLoginTime = null
        registerStatus = null
        authToken = null
        imAccessId = null
        imToken = null
        computingPower = 0
        purchaseCourseStatus = 0
        isPaid = null
        wechatNickName = null
        wechatBindStatus = WECHAT_BIND_STATUS_UNBOUND
        requestNotificationsPermissionEnable = true
        Config.get().clearBaseUrlCustom()
        clearLegacyUserData()
    }

    fun notifyChange(vararg keys: String?) {
        FlowEventBus.postSync(Events.UserDataEvent(*keys))
    }

    fun loginOut() {
        clear()
        notifyChange()
    }

    /** 当前是否为游客状态。 */
    val isVisitor: Boolean
        get() = TextUtils.isEmpty(id)

    override fun getId(): String? {
        return authToken
    }

    fun storeUserId(userId: String?) {
        this.userId = userId
    }

    fun storePhoneNum(phoneNum: String?) {
        this.phoneNum = phoneNum
    }

    fun storeUid(uid: String?) {
        this.userUid = uid
    }

    fun storeInviteCode(inviteCode: String?) {
        this.inviteCode = inviteCode
    }

    fun storeNickname(nickName: String?) {
        this.nickName = nickName
    }

    fun storeNickname(gender: Int?) {
        this.gender = gender ?: 0
    }

    fun storeAvatar(avatar: String?) {
        this.avatarUrl = avatar
    }

    fun storeUserSource(userSource: Int?) {
        this.userSource = userSource ?: 0
    }

    fun storeRegisterWay(registerWay: Int?) {
        this.registerWay = registerWay ?: 0
    }

    fun storeLastLoginTime(lastLoginTime: String?) {
        this.lastLoginTime = lastLoginTime
    }

    fun storeRegisterStatus(registerStatus: Int?) {
        this.registerStatus = registerStatus ?: 0
    }

    fun storeToken(token: String?) {
        this.authToken = token
    }

    fun storeComputingPower(computingPower: Int) {
        this.computingPower = computingPower
        notifyChange(COMPUTING_POWER)
    }

    fun storePurchaseCourseStatus(purchaseCourseStatus: Int?) {
        this.purchaseCourseStatus = purchaseCourseStatus
    }

    fun hasPaid(): Boolean {
        return isPaid == 2
    }

    fun storePaid(isPaid: Int?) {
        this.isPaid = isPaid
    }

    fun storeWechatNickName(wechatNickName: String?) {
        this.wechatNickName = wechatNickName
    }

    fun storeWechatBindStatus(wechatBindStatus: Int?) {
        this.wechatBindStatus = wechatBindStatus ?: WECHAT_BIND_STATUS_UNBOUND
    }

    fun isFemale(): Boolean {
        return gender == FEMALE
    }

    fun isSameId(userId: String?): Boolean {
        if (isVisitor) {
            return false
        }
        return TextUtils.equals(userId, this.userId)
    }

    fun isPurchasedCourse(): Boolean {
        return purchaseCourseStatus == 1
    }

    fun setRequestNotificationsPermissionDisable() {
        requestNotificationsPermissionEnable = false
    }

    fun storeImAccessId(accId: String?) {
        imAccessId = accId
    }

    fun storeImToken(token: String?) {
        imToken = token
    }

    private fun migrateLegacyUserDataIfNeeded() {
        if (mmkv.decodeBool(mmkvKey(USER_DATA_MIGRATED), false)) {
            return
        }
        migrateLegacyString(USER_ID)
        migrateLegacyString(PHONE_NUM)
        migrateLegacyString(U_ID)
        migrateLegacyString(INVITE_CODE)
        migrateLegacyString(NICK_NAME)
        migrateLegacyInt(GENDER, 0)
        migrateLegacyString(AVATAR_URL)
        migrateLegacyInt(USER_SOURCE, 0)
        migrateLegacyInt(REGISTER_WAY, 0)
        migrateLegacyString(LAST_LOGIN_TIME)
        migrateLegacyInt(REGISTER_STATUS, 0)
        migrateLegacyString(TOKEN)
        migrateLegacyInt(COMPUTING_POWER, 0)
        migrateLegacyInt(PURCHASE_COURSE_STATUS, 0)
        migrateLegacyInt(IS_PAID, 0)
        migrateLegacyString(WECHAT_NICK_NAME)
        migrateLegacyInt(WECHAT_BIND_STATUS, WECHAT_BIND_STATUS_UNBOUND)
        migrateLegacyBoolean(REQUEST_NOTIFICATIONS_PERMISSION, true)
        mmkv.encode(mmkvKey(USER_DATA_MIGRATED), true)
    }

    private fun migrateLegacyString(key: String) {
        val targetKey = mmkvKey(key)
        if (mmkv.containsKey(targetKey)) return
        legacySp.getString(key, null)?.let { mmkv.encode(targetKey, it) }
    }

    private fun migrateLegacyInt(key: String, default: Int) {
        val targetKey = mmkvKey(key)
        if (mmkv.containsKey(targetKey)) return
        mmkv.encode(targetKey, legacySp.getInt(key, default))
    }

    private fun migrateLegacyBoolean(key: String, default: Boolean) {
        val targetKey = mmkvKey(key)
        if (mmkv.containsKey(targetKey)) return
        mmkv.encode(targetKey, legacySp.getBoolean(key, default))
    }

    private fun clearLegacyUserData() {
        legacySp.remove(USER_ID)
        legacySp.remove(PHONE_NUM)
        legacySp.remove(U_ID)
        legacySp.remove(INVITE_CODE)
        legacySp.remove(NICK_NAME)
        legacySp.remove(GENDER)
        legacySp.remove(AVATAR_URL)
        legacySp.remove(USER_SOURCE)
        legacySp.remove(REGISTER_WAY)
        legacySp.remove(LAST_LOGIN_TIME)
        legacySp.remove(REGISTER_STATUS)
        legacySp.remove(TOKEN)
        legacySp.remove(COMPUTING_POWER)
        legacySp.remove(PURCHASE_COURSE_STATUS)
        legacySp.remove(IS_PAID)
        legacySp.remove(WECHAT_NICK_NAME)
        legacySp.remove(WECHAT_BIND_STATUS)
        legacySp.remove(REQUEST_NOTIFICATIONS_PERMISSION)
    }

    /**
     * 实现了IM的ImUserAccount
     */

    override fun getAccessId(): String? {
        return imAccessId
    }

    override fun getToken(): String? {
        return imToken
    }

    override fun isRingOn(): Boolean {
        return true
    }

    override fun isVibrateOn(): Boolean {
        return true
    }

    override fun isValid(): Boolean {
        return !TextUtils.isEmpty(imAccessId) && !TextUtils.isEmpty(imToken)
    }

    override fun getAvatar(): String? {
        return avatarUrl
    }


    companion object {
        private const val USER_DATA_MMKV_ID = "USER_DATA"
        private const val USER_DATA_MIGRATED = "USER_DATA_MIGRATED"

        private const val IM_ACCESS_ID = "imAccessId"
        private const val IM_TOKEN = "imToken"

        /** 后端账户 id 存储 key。 */
        val USER_ID = "userId"

        /** 绑定手机号存储 key。 */
        const val PHONE_NUM = "phoneNum"

        /** 候选人/业务 uid 存储 key。 */
        const val U_ID = "uid"

        /** 邀请码存储 key。 */
        const val INVITE_CODE = "inviteCode"

        /** 昵称存储 key。 */
        const val NICK_NAME = "nickName"

        /** 性别存储 key。 */
        const val GENDER = "gender"

        /** 头像地址存储 key。 */
        const val AVATAR_URL = "avatarUrl"

        /** 用户来源存储 key。 */
        const val USER_SOURCE = "userSource"

        /** 注册方式存储 key。 */
        const val REGISTER_WAY = "registerWay"

        /** 最后登录时间存储 key。 */
        const val LAST_LOGIN_TIME = "lastLoginTime"

        /** 注册状态存储 key。 */
        const val REGISTER_STATUS = "registerStatus"

        /** 登录 token 存储 key。 */
        const val TOKEN = "token"

        /** 算力余额存储 key。 */
        const val COMPUTING_POWER = "computingPower"

        /** 课程购买状态存储 key。 */
        const val PURCHASE_COURSE_STATUS = "purchaseCourseStatus"

        /** 付费用户标记存储 key。 */
        const val IS_PAID = "IS_PAID"

        /** 微信昵称存储 key。 */
        const val WECHAT_NICK_NAME = "wechatNickName"

        /** 微信绑定状态存储 key。 */
        const val WECHAT_BIND_STATUS = "wechatBindStatus"

        /** 微信未绑定。 */
        const val WECHAT_BIND_STATUS_UNBOUND = 0

        /** 微信已绑定。 */
        const val WECHAT_BIND_STATUS_BOUND = 1

        /** 男性性别值。 */
        const val MALE = 1

        /** 女性性别值。 */
        const val FEMALE = 2

        /** 通知权限弹窗标记存储 key。 */
        private const val REQUEST_NOTIFICATIONS_PERMISSION = "REQUEST_NOTIFICATIONS_PERMISSION"

        @JvmStatic
        fun get(): User {
            return App.instance.user
        }

        private fun mmkvKey(key: String): String = "${USER_DATA_MMKV_ID}_$key"
    }


}
