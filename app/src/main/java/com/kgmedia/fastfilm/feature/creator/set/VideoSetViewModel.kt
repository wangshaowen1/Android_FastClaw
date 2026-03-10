package com.kgmedia.fastfilm.feature.creator.set

import android.view.KeyEvent
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import androidx.appcompat.widget.AppCompatEditText
import com.blankj.utilcode.util.KeyboardUtils
import com.kgmedia.fastfilm.R
import com.kgmedia.fastfilm.base.RecyclerViewVM
import com.kgmedia.fastfilm.base.viewmodel.AppRepositoryViewModel
import com.kgmedia.fastfilm.common.Constants
import com.kgmedia.fastfilm.common.ext.getChildByType
import com.kgmedia.fastfilm.core.AppRepository
import com.kgmedia.fastfilm.core.network.apis.entity.VideoConfigData
import com.kgmedia.fastfilm.core.network.apis.entity.VideoConfigInfo
import com.kgmedia.fastfilm.core.network.apis.entity.VideoStyleConfigInfo
import com.kgmedia.fastfilm.feature.creator.set.allstyle.ItemCreationStyleVm
import com.kgmedia.fastfilm.widget.decoration.GridSpacingItemDecoration
import com.nhsd.basic.ext.dp
import com.nhsd.basic.ext.getParcelableValue
import com.nhsd.basic.ext.showToast

/**
 * *****************************************
 * @Introduce:创作
 * *****************************************
 */
class VideoSetViewModel : AppRepositoryViewModel<AppRepository>() {
    val configsData by lazy { mBundle.getParcelableValue(Constants.Extra.DATA, VideoConfigData::class.java) }
    val workTitle by lazy { mBundle.getString(Constants.Extra.TITLE) }

    var checkedRatio: ItemVideoConfigVm? = null
    var checkedEpisodeSet: ItemVideoConfigVm? = null
    var checkedStyle: ItemCreationStyleVm? = null

    val ratioVms = arrayListOf<ItemVideoConfigVm>()
    val styleVms = arrayListOf<ItemCreationStyleVm>()
    val episodeVms = arrayListOf<ItemVideoConfigVm>()

    override fun onCreate() {
        super.onCreate()
        episodeVms.clear()
        ratioVms.clear()
        styleVms.clear()
        val charCount = mBundle.getInt(Constants.Extra.COUNT)
        if (charCount > 0) {
            configsData?.novelRecommendEpisodeGroupList?.find {
                charCount > (it.minCount ?: 0) && charCount < (it.maxCount ?: 0)
            }?.itemList?.forEach {
                episodeVms.add(initEpisodeVm(it))
            }
        } else {
            configsData?.totalEpisodeCntItemVoList?.forEach {
                episodeVms.add(initEpisodeVm(it))
            }
        }

        configsData?.ratioItemVoList?.forEach {
            ratioVms.add(initRatioVm(it))
        }
        configsData?.styleItemVOList?.forEach {
            styleVms.add(initStyleVm(it))
        }
    }

    fun initEpisodeVm(ratio: VideoConfigInfo): ItemVideoConfigVm {
        return ItemVideoConfigVm(ratio).apply {
            if (checkedEpisodeSet == null) {
                checked.set(true)
                checkedEpisodeSet = this
            }
            onItemClick.value = { view ->
                if (checkedEpisodeSet != this) {
                    checkedEpisodeSet?.checked?.set(false)
                    this.checked.set(true)
                    checkedEpisodeSet = this
                }

                if (ratio.isCustomInput()) {
                    (view as? ViewGroup)?.getChildByType(
                        EditText::class.java,
                        AppCompatEditText::class.java
                    )?.let {
                        (it as? EditText)?.setOnEditorActionListener { v, actionId, event ->
                            // 只要是点击了右下角的确认、完成、搜索等键，都会触发
                            if (actionId == EditorInfo.IME_ACTION_DONE ||
                                actionId == EditorInfo.IME_ACTION_NEXT ||
                                (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)) {
                                val targetCount = input.get()?.toIntOrNull() ?: 0
                                if (targetCount > 60) {
                                    "剧集数不可超过60集".showToast()
                                    input.set("60")
                                }
                                KeyboardUtils.hideSoftInput(v)
                                return@setOnEditorActionListener true
                            }
                            false
                        }
                        KeyboardUtils.showSoftInput(it)
                        it.requestFocus()
                        (it as? EditText)?.setSelection(it.length())
                    }

                } else {
                    view?.let { KeyboardUtils.hideSoftInput(it) }
                }
            }
        }
    }

    fun initRatioVm(ratio: VideoConfigInfo): ItemVideoConfigVm {
        return ItemVideoConfigVm(ratio).apply {
            if (checkedRatio == null) {
                checked.set(true)
                checkedRatio = this
            }
            onItemClick.value = {
                if (checkedRatio != this) {
                    checkedRatio?.checked?.set(false)
                    this.checked.set(true)
                    checkedRatio = this
                }
            }
        }
    }

    fun initStyleVm(info: VideoStyleConfigInfo): ItemCreationStyleVm {
        return ItemCreationStyleVm(info).apply {
            if (checkedStyle == null) {
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

    val mRatioAdapterVm =
        RecyclerViewVM().Builder().setQuickAdapter(R.layout.item_video_ratio, ratioVms)
            .setItemDecoration(
                GridSpacingItemDecoration(
                    2,
                    12.dp,
                    0
                )
            ).build()

    val mStyleAdapterVm =
        RecyclerViewVM().Builder().setQuickAdapter(R.layout.item_creation_style, styleVms).build()

    val mEpisodeAdapterVm =
        RecyclerViewVM().Builder().setQuickAdapter(R.layout.item_series_count, episodeVms).build()
}