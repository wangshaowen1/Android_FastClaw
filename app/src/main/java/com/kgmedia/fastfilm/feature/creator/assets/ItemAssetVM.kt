package com.kgmedia.fastfilm.feature.creator.assets

import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.databinding.ObservableInt
import com.kgmedia.fastfilm.base.item.AppItemViewModel
import com.kgmedia.fastfilm.core.network.apis.entity.SimpleAsset
import com.kgmedia.fastfilm.widget.RecyclerEditText

abstract class ItemAssetVM<Item : SimpleAsset>(
    /**
     * 作品 ID
     */
    val workId: String?,
    /**
     * 剧集 id
     */
    val storyId: String?,
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
     * 编辑中的资源描述
     */
    var editingAssetDescText: String? = data?.assetDesc
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
    val editingAssetName by lazy { ObservableBoolean(false) }
    val assetNameInputListener by lazy { ObservableField<RecyclerEditText.OnTextInputListener>() }

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

    abstract fun generate()

    abstract fun deleteAsset()
}