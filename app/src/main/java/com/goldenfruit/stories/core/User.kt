package com.kgmedia.fastfilm.core

import android.text.TextUtils
import android.text.format.DateUtils
import com.blankj.utilcode.util.GsonUtils
import com.kgmedia.fastfilm.App
import com.kgmedia.fastfilm.common.SP
import com.kgmedia.fastfilm.common.utils.DateUtil
import com.kgmedia.fastfilm.core.network.apis.entity.CookieUserInfo
import com.kgmedia.fastfilm.core.network.apis.entity.UserInfo
import com.kgmedia.fastfilm.core.network.apis.entity.WalletInfo
import com.kgmedia.fastfilm.core.network.apis.request.CompleteUserDataInfoBody
import com.kgmedia.fastfilm.core.network.apis.response.UserHomeData
import com.google.gson.Gson
import com.growingio.android.sdk.autotrack.GrowingAutotracker
import com.nhsd.basic.Session
import com.nhsd.basic.bus.FlowEventBus

/**
 * 当前登录角色信息
 */
class User : IUser {

    val sp: SP


    var signature: String?
    var lastComposeConfig: String?
        private set

    //用户ID
    private var id: String?

    /**
     * 获取昵称
     *
     * @return 昵称
     */
    var nickname: String?
        private set

    /**
     * 获取头像
     *
     * @return 头像地址
     */
    var avatar: String?
        private set

    /**
     * 用户AppId
     *
     * @return 用户AppId
     */
    var appId: String?
        private set

    /**
     * 账号创建时间
     *
     * @return 账号创建时间
     */
    var creationTime: Long
        private set

    /**
     * 会员状态 0=非会员 1=会员 2-会员过期
     *
     * @return 会员状态 0=非会员 1=会员 2-会员过期
     */
    var vipStatus: Int
        private set

    /**
     * 会员到期时间
     *
     * @return 会员到期时间
     */
    var vipExpiredTime: Long?
        private set

    /**
     * vip类型：1-初级会员 2-中级会员 3-高级会员
     */
    var vipType: Int
        private set

    /**
     * vip类型对应的分成比例
     */
    var profitSharingRatio: String?
        private set

    var requestNotificationsPermissionEnable = true


    /**
     * 积分余额
     *
     * @return 积分余额
     */
    var scoreBalance: Int
        private set

    val isVip: Boolean
        /**
         * 是否是会员
         * @return true:会员 false:普通
         */
        get() = vipStatus == 1

    val isVipOrExpired: Boolean
        /**
         * 是否是会员(包含过期)
         * @return true:会员(包含过期) false:普通
         */
        get() = (vipStatus == 1 || vipStatus == 2)

    /**
     * 是否是过期会员
     */
    val isExpiredVip: Boolean
        get() = vipStatus == 2

    val userInfoStr: String?
        get() {
            val userBasicInfo = CookieUserInfo(id, Session.getInstance().versionName)
            return Gson().toJson(userBasicInfo)
        }


    val isVisitor: Boolean
        /**
         * 检查登录状态
         *
         * @return true:未登录 false:已登录
         */
        get() = TextUtils.isEmpty(id)

    val isLogged: Boolean
        /**
         * 检查登录状态
         *
         * @return true:已登录 false:未登录
         */
        get() = !TextUtils.isEmpty(id)

    /**
     * 钱包信息
     */
    var walletInfo: WalletInfo? = null

    var gender = 0
    private var hasPaid = false


    //手机号
    var phoneBind: String?
        private set

    var birthday: String?
        private set

    var city: String? = null
        private set

    var province: String? = null
        private set


    init {
        sp = SP("USER_DATA")
        id = sp.getString(ID, null)
        avatar = sp.getString(AVATAR, null)
        nickname = sp.getString(NICKNAME, null)
        appId = sp.getString(APP_ID, null)
        creationTime = sp.getLong(CREATION_TIME, 0)
        vipStatus = sp.getInt(VIP_STATUS, 0)
        vipExpiredTime = sp.getLong(VIP_EXPIRED_TIME, 0L)
        vipType = sp.getInt(VIP_TYPE, 0)
        profitSharingRatio= sp.getString(PROFIT_SHARING_RATIO, null)

        scoreBalance = sp.getInt(SCORE_BALANCE, 0)

        birthday = sp.getString(BIRTHDAY, null)
        creationTime = sp.getLong(CREATION_TIME, 0)
        signature = sp.getString(SIGNATURE, null)
        lastComposeConfig = sp.getString(LAST_COMPOSE_CONFIG, null)
        requestNotificationsPermissionEnable = sp.getBoolean(REQUEST_NOTIFICATIONS_PERMISSION, true)
        gender = sp.getInt(GENDER, GENDER_NULL)
        hasPaid = sp.getBoolean(HAS_PAID, false)

        phoneBind = sp.getString(PHONE_BIND, "")

        initWalletInfo()
    }

    fun updateFrom(userData: UserHomeData?) {
        if (userData == null) return
        val userInfo = userData.userInfo
        if (userInfo != null) {
            updateUserInfoFrom(userInfo)
        }
        notifyChange()

        if (id.isNullOrEmpty()) {
            return
        }
        GrowingAutotracker.get().setLoginUserId(id)
        val mutableMap = mutableMapOf(
            "jg_user_jingguo_registratio_time" to get().creationTime.toString(),//用户在金果上注册账户的时间，用户成功注册时更新，或用户登录到新版本后上初始化上传数据时候更新
            "jg_user_jinguo_id" to get().appId,//用户金果唯一 Id，用户成功注册时更新，或用户登录到新版本后上初始化上传数据时候更新
            "jg_vip_type" to get().vipType.toString(),//0=非会员，未购买会员服务 1=初级会员 2=中级会员 3=高级会员/高级会员 当前会员的状态，当用户购买会员或会员过期的时候需要更新
//            "xy_user_age_bracket" to ageMsg,//用户选择的年龄段；用户更新该数据的时候更新

            "\$basic_createdAt" to creationTime.toString(),//注册时间
            "\$basic_birthday" to birthday,//出生年月日
//            "\$basic_mobile" to phoneNumber,//手机号
            "\$basic_gender" to if (gender == User.FEMALE) "FEMALE" else "MALE",//性别
            "\$basic_address" to if (!province.isNullOrBlank() || !city.isNullOrBlank()) "${province},${city}" else null,//地址
            "\$basic_name" to nickname,//姓名
        ).filterValues { !it.isNullOrBlank() }
        GrowingAutotracker.get().setLoginUserAttributes(mutableMap)
    }

    fun updateUserInfoFrom(userInfo: UserInfo) {
        storeId(userInfo.userToken)
        storeAvatar(userInfo.avatarUrl)
        storeNickname(userInfo.nickName)
        storeAppId(userInfo.userAppId)
        storeCreationTime(userInfo.createTime)
        updateWalletInfo(userInfo.walletInfo)
        val userGrant = userInfo.vip
        storeVipStatus(userGrant?.vipStatus ?: 0)
        storeVipExpiredTime(userGrant?.vipExpiredTime)
        storeVipType(userGrant?.vipType)
        storeVipProfitSharingRatio(userGrant?.profitSharingRatio)


        storePhoneBind(userInfo.phoneBind)
        userInfo.gender?.let { storeGender(it) }
        storeCity(userInfo.city)
        storeProvince(userInfo.province)
        storeSignature(userInfo.signature)
        storeBirthday(userInfo.birthday)
    }

    fun updateFromCompleteUserBody(body: CompleteUserDataInfoBody) {
        body.avatarUrl?.let { storeAvatar(it) }
        body.nickName?.let {
            storeNickname(it)
        }
        body.gender?.let { storeGender(it) }
        body.birthday?.let { storeBirthday(it) }
        body.province?.let { storeProvince(it) }
        body.city?.let { storeCity(it) }
        body.signature?.let { storeSignature(it) }
        notifyChange()
    }

    fun initWalletInfo() {
        val json = sp.getString(WALLET_INFO, null)
        if (TextUtils.isEmpty(json)) return
        try {
            walletInfo = GsonUtils.fromJson(json, WalletInfo::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun updateWalletInfo(walletInfo: WalletInfo?) {
        if (walletInfo == null) {
            storeScoreBalance(0)
        } else {
            storeScoreBalance(walletInfo.pointBalance)
        }
        storeWalletInfo(walletInfo)
    }

    fun storeGender(gender: Int) {
        this.gender = gender
        sp.putInt(GENDER, gender)
    }

    fun storeBirthday(birthday: Long?) {
        this.birthday = if (birthday == null) "" else DateUtil.format(birthday, DateUtil.FORMAT_DATE)
        sp.putString(BIRTHDAY, this.birthday)
    }


    fun storeCity(city: String?) {
        if (!TextUtils.equals(this.city, city)) {
            this.city = city
            sp.putString(CITY, city)
        }
    }

    fun storeProvince(province: String?) {
        if (!TextUtils.equals(this.province, province)) {
            this.province = province
            sp.putString(PROVINCE, province)
        }
    }


    private fun clear() {
        id = null
        sp.remove(ID)
        nickname = null
        sp.remove(NICKNAME)
        avatar = null
        sp.remove(AVATAR)
        appId = null
        sp.remove(APP_ID)
        creationTime = 0
        sp.remove(CREATION_TIME)
        vipStatus = 0
        sp.remove(VIP_STATUS)
        vipExpiredTime = null
        sp.remove(VIP_EXPIRED_TIME)
        vipType = 0
        sp.remove(VIP_TYPE)
        profitSharingRatio = null
        sp.remove(PROFIT_SHARING_RATIO)

        scoreBalance = 0
        sp.remove(SCORE_BALANCE)
        walletInfo = null
        sp.remove(WALLET_INFO)
        creationTime = 0
        sp.remove(CREATION_TIME)
        sp.remove(USER_INFO)
        signature = null
        sp.remove(SIGNATURE)
        lastComposeConfig = null
        sp.remove(LAST_COMPOSE_CONFIG)
        gender = GENDER_NULL
        sp.remove(GENDER)
        hasPaid = false
        sp.remove(HAS_PAID)
        phoneBind = null
        sp.remove(PHONE_BIND)
        birthday = null
        sp.remove(BIRTHDAY)
    }

    fun notifyChange(vararg keys: String?) {
        FlowEventBus.postSync(Events.UserDataEvent(*keys))
    }


    fun loginOut() {
        clear()
        notifyChange()
    }


    fun storeSignature(signature: String?) {
        this.signature = signature
        sp.putString(SIGNATURE, signature)
    }


    fun storeLastComposeConfig(lastComposeConfig: String?) {
        this.lastComposeConfig = lastComposeConfig
        sp.putString(LAST_COMPOSE_CONFIG, lastComposeConfig)
    }


    fun storeId(id: String?) {
        sp.putString(ID, id)
        this.id = id
    }


    override fun getId(): String? {
        return id
    }

    fun isSameId(userToken: String?): Boolean {
        if (this.isVisitor) {
            return false
        }
        return TextUtils.equals(userToken, id)
    }


    /**
     * 存储昵称
     *
     * @param nickname 昵称
     */
    fun storeNickname(nickname: String?) {
        this.nickname = nickname
        sp.putString(NICKNAME, nickname)
    }

    /**
     * 存储头像
     *
     * @param avatar 头像地址
     */
    fun storeAvatar(avatar: String?) {
        this.avatar = avatar
        sp.putString(AVATAR, avatar)
    }

    /**
     * 用户AppId
     *
     * @param appId 用户AppId
     */
    fun storeAppId(appId: String?) {
        this.appId = appId
        sp.putString(APP_ID, appId)
    }


    /**
     * 账号创建时间
     *
     * @param creationTime 账号创建时间
     */
    fun storeCreationTime(creationTime: Long?) {
        this.creationTime = creationTime ?: 0L
        sp.putLong(CREATION_TIME, this.creationTime)
    }

    /**
     * @return 注册24小时内
     */
    fun is24HourNewUser(): Boolean {
        val pastTime = System.currentTimeMillis() - creationTime
        return this.isLogged && pastTime > 0 && pastTime < 24 * DateUtils.HOUR_IN_MILLIS
    }

    /**
     * 会员状态 0=非会员 1=会员 2-会员过期
     *
     * @param vipStatus 会员状态 0=非会员 1=会员 2-会员过期
     */
    fun storeVipStatus(vipStatus: Int?) {
        this.vipStatus = vipStatus ?: 0
        sp.putInt(VIP_STATUS, this.vipStatus)
    }


    /**
     * 会员到期时间
     *
     * @param vipExpiredTime 会员到期时间
     */
    fun storeVipExpiredTime(vipExpiredTime: Long?) {
        this.vipExpiredTime = vipExpiredTime
        sp.putLong(VIP_EXPIRED_TIME, this.vipExpiredTime ?: 0L)
    }

    /**
     * vip类型：1-普通会员，2-标准会员，3-高级会员
     */
    fun storeVipType(vipType: Int?) {
        this.vipType = vipType ?: 0
        sp.putInt(VIP_TYPE, this.vipType)
    }

    /**
     * vip类型对应的分成比例
     */
    fun storeVipProfitSharingRatio(vipProfitSharingRatio: String?) {
        this.profitSharingRatio = vipProfitSharingRatio
        sp.putString(PROFIT_SHARING_RATIO, vipProfitSharingRatio)
    }

    fun setRequestNotificationsPermissionDisable() {
        requestNotificationsPermissionEnable = false
        sp.putBoolean(REQUEST_NOTIFICATIONS_PERMISSION, false)
    }

    /**
     * 积分余额
     *
     * @param scoreBalance 积分余额
     */
    fun storeScoreBalance(scoreBalance: Int?) {
        this.scoreBalance = scoreBalance ?: 0
        sp.putInt(SCORE_BALANCE, this.scoreBalance)
    }

    fun storeWalletInfo(walletInfo: WalletInfo?) {
        if (walletInfo == null) {
            this.walletInfo = null
            sp.remove(WALLET_INFO)
            return
        }
        try {
            this.walletInfo = walletInfo
            sp.putString(WALLET_INFO, GsonUtils.toJson(walletInfo))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun isFemale(): Boolean {
        return gender == FEMALE
    }

    fun hasPaid(): Boolean {
        return hasPaid
    }

    fun checkBalance(cost: Int?, withToast: Boolean = false): Boolean {
        return scoreBalance >= (cost ?: 0)
    }

    /**
     *手机号
     */
    fun storePhoneBind(phoneBind: String?) {
        this.phoneBind = phoneBind
        sp.putString(PHONE_BIND, this.phoneBind)
    }

    companion object {
        //1-初级会员
        const val VIP_TYPE_PRIMARY = 1
        //2-中级会员
        const val VIP_TYPE_MIDDLE = 2
        //3-高级会员
        const val VIP_TYPE_LIFE = 3

        //replace userId
        private const val USER_INFO = "userInfo"
        private const val SIGNATURE = "signature"

        //用户自己选择的模型
        private const val LAST_COMPOSE_CONFIG = "lastComposeConfig"
        const val GENDER_NULL: Int = 0
        const val MALE: Int = 1
        const val FEMALE: Int = 2

        //用户id，其实就是UserToken
        const val ID: String = "id"

        //昵称
        const val NICKNAME: String = "nickname"

        //用户头像
        const val AVATAR: String = "avatar"

        //用户appId
        private const val APP_ID = "appId"

        //账号创建时间
        private const val CREATION_TIME = "creation_time"

        //会员状态 0=非会员 1=会员 2-会员过期
        const val VIP_STATUS = "grant_status"

        //权益名称
        private const val VIP_EXPIRED_TIME = "grant_name"

        //是否购买年度会员
        private const val VIP_TYPE = "vipType"

        //积分余额
        private const val SCORE_BALANCE = "creditsBalance"

        const val BIRTHDAY = "birthday"

        private const val REQUEST_NOTIFICATIONS_PERMISSION: String =
            "REQUEST_NOTIFICATIONS_PERMISSION"

        private const val WALLET_INFO: String = "WALLET_INFO"

        private const val HAS_PAID: String = "hasPaid"
        private const val GENDER: String = "gender"

        private const val PHONE_BIND = "phoneBind"

        const val CITY: String = "city"
        const val PROVINCE: String = "province"

        //当前会员对应的分成比例
        const val PROFIT_SHARING_RATIO="profitSharingRatio"

        @JvmStatic
        fun get(): User {
            return App.instance.user
        }
    }
}