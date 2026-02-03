package com.kgmedia.fastfilm.feature.series.editinfo

import androidx.databinding.ObservableField
import androidx.databinding.ObservableInt
import androidx.lifecycle.MutableLiveData
import com.kgmedia.fastfilm.App
import com.kgmedia.fastfilm.base.viewmodel.AppRepositoryViewModel
import com.kgmedia.fastfilm.base.viewmodel.ToolbarViewModel
import com.kgmedia.fastfilm.common.Constants
import com.kgmedia.fastfilm.core.Events
import com.kgmedia.fastfilm.core.ext.launch
import com.kgmedia.fastfilm.core.network.apis.entity.DramasSeriesInfo
import com.kgmedia.fastfilm.core.network.apis.entity.WorkInfo
import com.kgmedia.fastfilm.core.response
import com.kgmedia.fastfilm.feature.series.SeriesRepository
import com.kgmedia.fastfilm.widget.LoadStateViewHolder
import com.kuaige.oss.OnUploadListener
import com.kuaige.oss.OssClient
import com.nhsd.basic.bus.FlowEventBus
import com.nhsd.basic.ext.showToast

/**
 * 修改剧集信息
 */
class EditInfoViewModel : AppRepositoryViewModel<SeriesRepository>() {
    val toolbar by lazy {
        ToolbarViewModel(
            title = "修改信息",
            menuText = "保存",
            menuAction = { save() },
            navigationAction = { back() }
        )
    }
    private val seriesId by lazy { mBundle.getString(Constants.Extra.ID) }
    val loadState by lazy { ObservableInt(LoadStateViewHolder.CONTENT) }
    val series by lazy { ObservableField<WorkInfo>() }
    val cover by lazy { ObservableField<MediaItem>() }
    val title by lazy { MutableLiveData<String>() }
    val description by lazy { MutableLiveData<String>() }
    val keyboardHeight by lazy { ObservableInt() }

    override fun onCreate() {
        super.onCreate()
        getSeriesInfo()
    }

    fun getSeriesInfo() {
        loadState.set(LoadStateViewHolder.LOADING)
        if (series.get() == null) loadState.set(LoadStateViewHolder.LOADING)
        launch(
            block = {
                mRepo.getWorkInfo(seriesId).data?.let {
                    series.set(it)
                    cover.set(it.workCoverUrl?.let { MediaItem(remoteUrl = it) })
                    title.value = it.workTitle
                    description.value = it.workDesc
                }
                updateLoadState()
            },
            onError = {
                updateLoadState()
            }
        )
    }

    private fun updateLoadState() {
        loadState.set(LoadStateViewHolder.CONTENT)
        if (series.get() == null) {
            loadState.set(LoadStateViewHolder.RETRY)
        } else {
            loadState.set(LoadStateViewHolder.CONTENT)
        }
    }

    private fun save() {
        val cover = cover.get()?.uploadPath
        val title = title.value
        val description = description.value
        if (cover.isNullOrEmpty() || title.isNullOrEmpty() || description.isNullOrEmpty()) {
            "请完善信息".showToast()
            return
        }
        setLoading(true)
        val ossClient = OssClient.newInstance(App.instance)
        ossClient.uploadFile(cover, OssClient.TYPE_ASSET, object : OnUploadListener() {
            override fun onUploadSuccess(uploadSuccess: List<String?>) {
                val url = uploadSuccess.firstOrNull()
                if (url.isNullOrEmpty()) {
                    onUploadFail(null)
                } else {
                    commit(url, title, description)
                }
            }

            override fun onUploadFail(uploadFail: MutableList<String>?) {
                super.onUploadFail(uploadFail)
                setLoading(false)
                "封面上传失败".showToast()
            }
        })
    }

    private fun commit(cover: String, title: String, description: String) {
        launch(
            block = {
                response(
                    bean = mRepo.seriesModifyInfo(seriesId, cover, title, description),
                    result = {
                        "保存成功".showToast()
                        setLoading(false)
                        finish()
                        FlowEventBus.postSync(Events.SeriesUpdatedEvent(seriesId))
                    },
                    error = {
                        (this?.msg ?: "保存失败").showToast()
                        setLoading(false)
                    }
                )
            },
            onError = {
                "保存失败".showToast()
                setLoading(false)
            }
        )
    }

    fun changeCover(path: String) {
        cover.set(MediaItem(localPath = path))
    }

    fun onSoftInputChanged(it: Int) {
        keyboardHeight.set(it)
    }

}