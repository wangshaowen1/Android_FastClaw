package com.shiyuan.fastclaw.core.manager

import android.content.Context
import android.util.Log
import com.qq.gdt.action.GDTAction
import com.shiyuan.fastclaw.BuildConfig
import com.nhsd.basic.utils.Logger
import org.json.JSONObject

/**
 * 广点通转化 SDK 统一入口。
 */
object DataNexusManager {
    private const val TAG = "DataNexusManager"

    @Volatile
    private var initialized = false

    fun initAfterPrivacyAgreed(context: Context) {
        if (initialized) return

        val userActionSetId = BuildConfig.DATANEXUS_USER_ACTION_SET_ID
        val appSecretKey = BuildConfig.DATANEXUS_APP_SECRET_KEY
        if (userActionSetId.isBlank() || appSecretKey.isBlank()) {
            if (BuildConfig.DEBUG) {
                Logger.i(TAG, "DataNexus config is empty, skip init")
            }
            return
        }

        runCatching {
            GDTAction.setAnidEnable(BuildConfig.DATANEXUS_ANID_ENABLE)
            GDTAction.init(context.applicationContext, userActionSetId, appSecretKey)
            GDTAction.start()
            initialized = true
        }.onFailure { error ->
            if (BuildConfig.DEBUG) {
                Logger.e(TAG, "DataNexus init failed: ${error.message}")
            }
        }
    }

    fun setUserUniqueId(userUniqueId: String?) {
        if (!initialized || userUniqueId.isNullOrBlank()) return
        runCatching {
            Log.d("setUserUniqueId",userUniqueId+"")
            GDTAction.setUserUniqueId(userUniqueId)
        }
    }

    fun logAction(actionType: String, actionParam: JSONObject? = null) {
        if (!initialized || actionType.isBlank()) return
        runCatching {
            if (actionParam == null) {
                GDTAction.logAction(actionType)
            } else {
                GDTAction.logAction(actionType, actionParam)
            }
        }
    }
}
