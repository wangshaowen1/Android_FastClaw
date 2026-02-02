package com.kgmedia.fastfilm.feature.creator.assets

import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.databinding.ObservableInt
import com.kgmedia.fastfilm.base.item.AppItemViewModel
import com.kgmedia.fastfilm.core.network.apis.entity.SimpleAsset
import com.kgmedia.fastfilm.widget.RecyclerEditText

abstract class ItemAssetVM<Item : SimpleAsset>(data: Item?, create: Boolean = false) : AppItemViewModel<Item>(data) {
    /**
     * 0 新创建 1 生成中 2 上传失败 3 已生成
     */
    val status by lazy { ObservableInt(if (create) 0 else 3) }

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

    fun exitEditAssetDesc() {
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

    fun exitEditAssetName() {
        assetNameInputListener.set(null)
        editingAssetName.set(false)
        assetName.set(editingAssetNameText)
    }

    abstract fun generate()
}