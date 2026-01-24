package com.kgmedia.fastfilm.core

import com.nhsd.basic.BaseRepository
import com.kgmedia.fastfilm.core.db.AppDatabase
import com.kgmedia.fastfilm.core.network.apis.ApiFactory
import com.kgmedia.fastfilm.core.network.apis.ApiService

open class AppRepository() : BaseRepository<ApiService, AppDatabase>() {
    override val api = ApiFactory.apiService
    override val appDatabase = AppDatabase.Companion.getInstance()
}