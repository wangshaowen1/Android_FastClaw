package com.shiyuan.fastclaw.core.network.apis.response

/**
 * APP版本检测结果
 */
data class UpgradeData(
    /**
     * 升级动作：0-无更新，1-推荐更新，2-强制更新
     */
    val upgradeAction: Int?,
    /**
     * 当前最新版本号
     */
    val latestVersion: String?,
    /**
     * 最低支持版本号
     */
    val minSupportedVersion: String?,
    /**
     * 升级目标版本号
     */
    val targetVersion: String?,
    /**
     * 更新说明
     */
    val releaseNote: String?,
    /**
     * 升级地址
     */
    val upgradeUrl: String?,
    /**
     * APK MD5 校验码
     */
    val md5Hash: String?,
    /**
     * 服务端时间（接口文档声明为 object）
     */
    val serverTime: Any?
) {
    fun isNeedUpdate(): Boolean {
        return upgradeAction == 1 || upgradeAction == 2
    }

    fun isNeedForceUpdate(): Boolean {
        return upgradeAction == 2
    }
}
