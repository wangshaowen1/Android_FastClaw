package com.kgmedia.fastfilm.feature.works.assetmanager.roleimage

import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.blankj.utilcode.util.StringUtils.getString
import com.kgmedia.fastfilm.R
import com.kgmedia.fastfilm.base.viewmodel.AppRepositoryViewModel
import com.kgmedia.fastfilm.base.viewmodel.ToolbarViewModel
import com.kgmedia.fastfilm.common.Constant
import com.kgmedia.fastfilm.common.Constants
import com.kgmedia.fastfilm.core.AppRepository
import com.kgmedia.fastfilm.core.network.apis.entity.RoleImageInfo
import com.kgmedia.fastfilm.core.network.apis.entity.SimpleRoleInfo
import com.kgmedia.fastfilm.feature.works.data.RoleImagePagingSource
import com.nhsd.basic.ext.getParcelableValue
import kotlinx.coroutines.flow.Flow

class RoleImageSelectViewModel : AppRepositoryViewModel<AppRepository>() {

    var mToolbarVM = ToolbarViewModel(
        navigationAction = { back() },
        title = ""
    )
    val selectModel by lazy { mBundle.getBoolean(Constants.Extra.SELECT_MODE, false) }
    val roleInfo by lazy { mBundle.getParcelableValue(Constants.Extra.DATA, SimpleRoleInfo::class.java) }
    val workId by lazy { mBundle.getString(Constants.Extra.WORK_ID) }


    override fun onCreate() {
        super.onCreate()
        mToolbarVM.mTitle.set(if (selectModel) getString(R.string.select_role_image) else getString(R.string.character_appearance))
    }

    fun getRoleImages(): Flow<PagingData<RoleImageInfo>> {
        return Pager(
            config = PagingConfig(
                initialLoadSize = Constant.Paged.PAGE_SIZE,
                pageSize = Constant.Paged.PAGE_SIZE,
                prefetchDistance = Constant.Paged.PREFETCH_DISTANCE,
            ),
            pagingSourceFactory = {
                RoleImagePagingSource(roleInfo?.roleId)
            }).flow.cachedIn(viewModelScope)
    }
}