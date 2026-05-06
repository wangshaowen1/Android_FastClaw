package com.shiyuan.fastclaw.core

import com.nhsd.basic.BaseRepository
import com.shiyuan.fastclaw.core.db.AppDatabase
import com.shiyuan.fastclaw.core.network.apis.ApiFactory
import com.shiyuan.fastclaw.core.network.apis.ApiService

open class AppRepository() : BaseRepository<ApiService, AppDatabase>() {
    override val api = ApiFactory.apiService
    override val appDatabase = AppDatabase.Companion.getInstance()
}