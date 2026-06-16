package com.shiyuan.fastclaw.core.network

import com.shiyuan.fastclaw.core.User
import com.shiyuan.fastclaw.core.network.apis.ApiFactory
import com.shiyuan.fastclaw.core.network.apis.request.UserTokenBody
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * 登录 Token 刷新管理器
 */
object TokenRefreshManager {
    private val refreshMutex = Mutex()

    /**
     * 刷新登录 Token
     * @param expiredToken 触发 401 时使用的旧 Token
     */
    suspend fun refreshToken(expiredToken: String?): Boolean = refreshMutex.withLock {
        if (expiredToken.isNullOrBlank()) {
            return@withLock false
        }

        if (User.get().id != expiredToken) {
            return@withLock true
        }

        runCatching {
            ApiFactory.apiService.refreshToken(UserTokenBody(expiredToken))
        }.getOrNull()?.let { response ->
            val newToken = response.data?.token
            if (response.isSuccess && !newToken.isNullOrBlank()) {
                User.get().storeToken(newToken)
                User.get().notifyChange()
                true
            } else {
                false
            }
        } ?: false
    }
}
