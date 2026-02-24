package com.kgmedia.fastfilm.common.utils

import com.kgmedia.fastfilm.BuildConfig

object DebugUtils {
    val ipsName by lazy {
        arrayOf(
            "郭义明：192.168.10.44",
            "刘子帆：192.168.12.161",
            "测试外网：https://short-api.mynatapp.cc",
            "预发布：120.24.240.28:9010",
            "正式环境：kuaichupianjk.kuaigeai.cn"
        )
    }
    val ips by lazy {
        arrayOf(
            "http://192.168.10.44:8088",
            "http://192.168.12.161:8088",
            "https://short-api.mynatapp.cc",
            "http://120.24.240.28:7010",
            "https://kuaichupianjk.kuaigeai.cn"
        )
    }

    private val debugFlavor by lazy {
        arrayOf(
            "preview",
            "develop"
        )
    }

    @JvmStatic
    fun isDebug(): Boolean {
        if (BuildConfig.DEBUG) return true
        val flavorEnv = BuildConfig.FLAVOR_env
        for (i in debugFlavor.indices) {
            if (debugFlavor[i].contains(flavorEnv)) {
                return true
            }
        }
        return false
    }
}