package com.kgmedia.fastfilm.feature.wallet.auth

interface AuthListener {
    fun onFailed(reason: String)
    fun onSuccess(authCode: String)
}