package com.kgmedia.fastfilm.feature.creator.style

import com.kgmedia.fastfilm.R
import com.kgmedia.fastfilm.base.RecyclerViewVM
import com.kgmedia.fastfilm.base.viewmodel.AppRepositoryViewModel
import com.kgmedia.fastfilm.common.Constants
import com.kgmedia.fastfilm.core.AppRepository
import com.kgmedia.fastfilm.core.network.apis.entity.VideoStyleConfigInfo
import com.nhsd.basic.ext.getParcelableArrayListValue

/**
 * *****************************************
 * @Introduce:创作
 * *****************************************
 */
class CreationStyleChooseViewModel : AppRepositoryViewModel<AppRepository>() {
    val lastStyleId by lazy { mBundle.getString(Constants.Extra.ID) }
    val list by lazy { mBundle.getParcelableArrayListValue(Constants.Extra.DATA, VideoStyleConfigInfo::class.java) }

    var checkedStyle: ItemCreationStyleVm? = null
    val styleVms = arrayListOf<ItemCreationStyleVm>()
    val mStyleAdapterVm =
        RecyclerViewVM().Builder().setQuickAdapter(R.layout.item_creation_style_all, styleVms).build()

    override fun onCreate() {
        super.onCreate()
        list?.forEach {
            styleVms.add(initStyleVm(it))
        }
        mStyleAdapterVm.notifyDataSetChanged()
    }

    fun initStyleVm(info: VideoStyleConfigInfo): ItemCreationStyleVm {
        return ItemCreationStyleVm(info).apply {
            if (lastStyleId != null && info.styleCode == lastStyleId) {
                checked.set(true)
                checkedStyle = this
            } else if (lastStyleId == null && checkedStyle == null) {
                checked.set(true)
                checkedStyle = this
            }
            onItemClick.value = {
                if (checkedStyle != this) {
                    checkedStyle?.checked?.set(false)
                    this.checked.set(true)
                    checkedStyle = this
                }
            }
        }
    }
}