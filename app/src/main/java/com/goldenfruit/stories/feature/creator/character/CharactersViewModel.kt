package com.kgmedia.fastfilm.feature.creator.character

import androidx.databinding.ObservableField
import androidx.databinding.ObservableInt
import androidx.lifecycle.viewModelScope
import com.kgmedia.fastfilm.R
import com.kgmedia.fastfilm.base.RecyclerViewVM
import com.kgmedia.fastfilm.base.viewmodel.AppRepositoryViewModel
import com.kgmedia.fastfilm.common.Constants
import com.kgmedia.fastfilm.common.LoadState
import com.kgmedia.fastfilm.core.ext.launch
import com.kgmedia.fastfilm.core.ext.launchWithLoading
import com.kgmedia.fastfilm.core.network.apis.ApiException
import com.kgmedia.fastfilm.core.network.apis.entity.CharacterInfo
import com.kgmedia.fastfilm.core.network.apis.request.DramasIdBody
import com.kgmedia.fastfilm.core.network.apis.request.RoleIdBody
import com.kgmedia.fastfilm.core.response
import com.kgmedia.fastfilm.feature.creator.data.CreationRepository
import com.nhsd.basic.item.BaseItemEventsViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class CharactersViewModel : AppRepositoryViewModel<CreationRepository>() {

    val dramaId by lazy { mBundle.getString(Constants.Extra.ID) }

    //选中的形象
    val characterCount by lazy { ObservableInt(0) }

    val characterVms = arrayListOf<ItemCharacterVm>()
    val mAdapterVm =
        RecyclerViewVM().Builder().setQuickAdapter(R.layout.item_character, characterVms).build()
    val showHelp = ObservableField<LoadState>(LoadState.Loading)
    val eventVm by lazy {
        BaseItemEventsViewModel<CharacterEvent, String>()
    }
    val generationSuccess = ObservableField<Boolean>(false)

    // 轮询相关变量
    private var pollingJob: Job? = null
    private val pollingInterval = 3000L // 3秒

    fun getRoleList() {
        showHelp.set(LoadState.Loading)
        launch {
            val apiResponse = mRepo.api.characterList(DramasIdBody(dramaId))
            if (apiResponse.isSuccess) {
                characterVms.clear()
                if (apiResponse.data?.roleList?.isNotEmpty() == true) {
                    apiResponse.data?.roleList?.forEach {
                        characterVms.add(ItemCharacterVm(eventVm, it))
                    }
                    mAdapterVm.notifyDataSetChanged()
                    showHelp.set(LoadState.NotLoading(false))
                } else {
                    showHelp.set(LoadState.Empty)
                }
                characterCount.set(apiResponse.data?.roleList?.size ?: 0)
                val complete = apiResponse.data?.roleList?.all { it.genStatus == 2 } == true
                generationSuccess.set(complete)
                if (!complete) {
                    // 启动轮询
                    startPolling()
                }
            } else {
                showHelp.set(LoadState.Error(ApiException(apiResponse.code, apiResponse.msg)))
            }
        }
    }

    /**
     * 开始轮询角色状态
     */
    private fun startPolling() {
        // 先停止之前的轮询
        stopPolling()

        pollingJob = viewModelScope.launch {
            while (isActive) {
                delay(pollingInterval)
                fetchRoleListWithPolling()
            }
        }
    }

    /**
     * 轮询专用的获取角色列表方法
     */
    private suspend fun fetchRoleListWithPolling() {
        try {
            val apiResponse = mRepo.api.characterList(DramasIdBody(dramaId))
            if (apiResponse.isSuccess) {
                val newRoleList = apiResponse.data?.roleList ?: emptyList()

                // 更新现有角色数据
                updateCharacterVms(newRoleList)

                // 检查是否所有角色都已完成生成 (genStatus == 2)
                val allCompleted = newRoleList.all { it.genStatus == 2 }
                if (allCompleted) {
                    generationSuccess.set(true)
                    // 所有角色都完成，停止轮询
                    stopPolling()
                }
            }
        } catch (e: Exception) {
            // 轮询请求失败，继续下一次轮询
            e.printStackTrace()
        }
    }

    /**
     * 更新角色ViewModel列表
     */
    private fun updateCharacterVms(newRoleList: List<CharacterInfo>) {
        // 创建角色ID到ViewModel的映射
        val vmMap = characterVms.associateBy { it.data.get()?.roleId }

        // 更新或添加角色
        newRoleList.forEach { newCharacter ->
            val existingVm = vmMap[newCharacter.roleId]
            if (existingVm != null) {
                // 更新现有ViewModel的数据
                existingVm.updateData(newCharacter)
            } else {
                // 添加新的ViewModel（理论上不会发生，因为角色列表应该不变）
                characterVms.add(ItemCharacterVm(eventVm, newCharacter))
            }
        }

        // 通知数据更新
        mAdapterVm.notifyDataSetChanged()
    }

    /**
     * 停止轮询
     */
    private fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }

    fun generatingSegmentImage(callback: ((dramasId: String?) -> Unit)) {
        launchWithLoading {
            response(mRepo.api.generatingSegmentImage(DramasIdBody(dramaId))) {
                callback(data?.dramasId)
            }
        }
    }

    fun roleDel(roleId: String?, callback: (() -> Unit)) {
        launchWithLoading {
            response(mRepo.roleDel(RoleIdBody(roleId))) {
                callback()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        // ViewModel销毁时停止轮询
        stopPolling()
    }
}