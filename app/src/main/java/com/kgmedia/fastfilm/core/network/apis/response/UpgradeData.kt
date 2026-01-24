package com.kgmedia.fastfilm.core.network.apis.response

data class UpgradeData(
    val upgradeDesc: String?,//升级的文字说明
    val upgradeUrl: String?,//升级跳转地址
    val versionDesc: String?,//待升级的版本号
    val versionStatus: Int?,//升级状态 0-不升级 1-强制升级 2-建议升级
    val md5Hash:String?
){
    fun isNeedUpdate(): Boolean {
        return versionStatus == 1 || versionStatus == 2
    }

    fun isNeedForceUpdate(): Boolean {
        return versionStatus == 1
    }
}