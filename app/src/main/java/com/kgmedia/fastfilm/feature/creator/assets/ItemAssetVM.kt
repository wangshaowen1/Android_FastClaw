package com.kgmedia.fastfilm.feature.creator.assets

import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.databinding.ObservableInt
import com.kgmedia.fastfilm.base.item.AppItemViewModel
import com.kgmedia.fastfilm.core.network.apis.ApiResponse
import com.kgmedia.fastfilm.core.network.apis.entity.ReferenceImageInfo
import com.kgmedia.fastfilm.core.network.apis.entity.SimpleAsset
import com.kgmedia.fastfilm.widget.RecyclerEditText
import kotlinx.coroutines.Job

abstract class ItemAssetVM<Item : SimpleAsset>(
    val index: Int,
    /**
     * 作品 ID
     */
    val workId: String?,
    /**
     * 剧集 id
     */
    val storyId: String?,
    protected val onItemChanged: () -> Unit,
    data: Item?
) : AppItemViewModel<Item>(data) {

    /**
     * -1=新创建 0=待生成 1=生成中 2=成功 3=失败
     */
    val status by lazy { ObservableInt(data?.status ?: -1) }

    /**
     * 当前应用的资源描述
     */
    val assetDesc by lazy { ObservableField(data?.assetDesc) }

    /**
     * 当前应用的资源图片URL
     */
    val assetImage by lazy { ObservableField(data?.assetImage) }

    /**
     * 编辑中的资源描述
     */
    var editingAssetDescText: String? = data?.assetDesc
        private set
    val editingAssetDesc by lazy { ObservableBoolean(false) }
    val assetDescInputListener by lazy { ObservableField<RecyclerEditText.OnTextInputListener>() }

    /**
     * 当前应用的资源名称
     */
    val assetName by lazy { ObservableField(data?.assetName) }

    /**
     * 编辑中的资源名称
     */
    var editingAssetNameText: String? = data?.assetName
        private set
    val editingAssetName by lazy { ObservableBoolean(false) }
    val assetNameInputListener by lazy { ObservableField<RecyclerEditText.OnTextInputListener>() }
    protected var loopGenStatusJob: Job? = null

    init {
        if (status.get() == 1) {
            loopGenStatus()
        }
    }

    fun clear() {
        loopGenStatusJob?.cancel()
    }

    override fun onCleared() {
        loopGenStatusJob?.cancel()
        super.onCleared()
    }

    fun editAssetDesc() {
        assetDescInputListener.set(object : RecyclerEditText.OnTextInputListener {
            override fun onTextInput(text: String) {
                editingAssetDescText = text
            }
        })
        editingAssetDesc.set(true)
        editingAssetDescText = assetDesc.get()
    }

    open fun exitEditAssetDesc() {
        assetDescInputListener.set(null)
        editingAssetDesc.set(false)
        assetDesc.set(editingAssetDescText)
    }

    fun editAssetName() {
        assetNameInputListener.set(object : RecyclerEditText.OnTextInputListener {
            override fun onTextInput(text: String) {
                editingAssetNameText = text
            }
        })
        editingAssetName.set(true)
        editingAssetNameText = assetName.get()
    }

    open fun exitEditAssetName() {
        assetNameInputListener.set(null)
        editingAssetName.set(false)
        assetName.set(editingAssetNameText)
    }

    /**
     * 资产类型
     */
    abstract val assetType: String
    abstract val assetDescHint: String
    abstract val assetNameHint: String
    abstract val generateButtonText: String

    /**
     * 点击单个资产 生成/重新生成
     */
    abstract fun clickGenerate()

    /**
     * 调用生成资产图片接口
     * @param reference 参考图信息
     */
    abstract fun generate(assetDesc: String, assetName: String, reference: ReferenceImageInfo? = null)

    /**
     * 调用删除资产接口
     */
    abstract suspend fun delete(): ApiResponse<Any>

    abstract fun loopGenStatus()
}