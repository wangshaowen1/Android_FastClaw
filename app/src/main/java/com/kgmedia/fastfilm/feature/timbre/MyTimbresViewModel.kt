package com.kgmedia.fastfilm.feature.timbre

import android.graphics.Color
import androidx.databinding.ObservableBoolean
import androidx.media3.common.Player
import com.chad.library.adapter.base.QuickAdapterHelper
import com.chad.library.adapter.base.loadState.LoadState
import com.chad.library.adapter.base.loadState.trailing.TrailingLoadStateAdapter
import com.kgmedia.fastfilm.R
import com.kgmedia.fastfilm.base.item.AppItemViewModel
import com.kgmedia.fastfilm.base.viewmodel.AppRepositoryViewModel
import com.kgmedia.fastfilm.base.viewmodel.ToolbarViewModel
import com.kgmedia.fastfilm.common.PagedProxy
import com.kgmedia.fastfilm.common.adapter.QuickAdapter
import com.kgmedia.fastfilm.core.ext.exception
import com.kgmedia.fastfilm.core.ext.launch
import com.kgmedia.fastfilm.core.network.apis.ApiResponse
import com.kgmedia.fastfilm.core.network.apis.PagedList
import com.kgmedia.fastfilm.core.network.apis.entity.TimbreInfo
import com.kgmedia.fastfilm.core.network.apis.request.PagedBody
import com.kgmedia.fastfilm.core.network.apis.request.UserTimbreIdBody
import com.kgmedia.fastfilm.core.response
import com.kgmedia.fastfilm.feature.timbre.data.TimbreRepository
import com.kgmedia.fastfilm.kit.PlayerFactory
import com.kgmedia.fastfilm.kit.play
import com.kgmedia.fastfilm.widget.DefaultTrailingLoadStateAdapter
import com.kgmedia.fastfilm.widget.MyTrackTrailingLoadStateAdapter
import com.kgmedia.fastfilm.widget.failState
import com.nhsd.basic.ext.dp
import com.nhsd.basic.utils.Logger
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay

/**
 * 我的音色
 */
class MyTimbresViewModel : AppRepositoryViewModel<TimbreRepository>() {

    var mToolbarVM = ToolbarViewModel(
        navigationAction = { back() },
        title = "我的音色",
        background = Color.TRANSPARENT
    )

    private var page = 0
    val isRefreshing by lazy { ObservableBoolean(false) }
    val adapter by lazy { QuickAdapter<ItemMyTimbreVM>(R.layout.item_my_timbre, emptyList()) }
    private val player by lazy {
        PlayerFactory.createPlayer(withCache = true, autoAudioFocus = true).apply {
            repeatMode = Player.REPEAT_MODE_ONE
            addListener(object : Player.Listener {

                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    super.onIsPlayingChanged(isPlaying)
                    currentTimbre?.playing?.set(isPlaying)
                }
            })
        }
    }
    val adapterHelper by lazy {
        QuickAdapterHelper.Builder(adapter)
            .setTrailingLoadStateAdapter(
                MyTrackTrailingLoadStateAdapter(
                    emptyDrawableRes = 0,//R.drawable.empty_common,
                    emptyText = "还没有克隆音色哦\n快去克隆吧",
                    emptyIconTopMargin = 115.dp
                ).apply {
                    loadEndMsg = ""
                    setOnLoadMoreListener(object : TrailingLoadStateAdapter.OnTrailingListener {
                        override fun onFailRetry() {
                            loadMore()
                        }

                        override fun onLoad() {
                            loadMore()
                        }
                    })
                })
            .build()
    }

    override fun onCreate() {
        super.onCreate()
        refresh()
    }

    /**
     * 刷新
     */
    fun refresh() {
        if (isRefreshing.get()) return
        isRefreshing.set(true)
        page = 0
        stopPlay()
        loadMore()
    }

    var onTimbreLongClick: ((ItemMyTimbreVM) -> Unit)? = null
    var onTimbreGenerateClick: ((ItemMyTimbreVM) -> Unit)? = null

    /**
     * 加载更多
     */
    private fun loadMore() {
        page++
        if (page == 1) {
            adapterHelper.trailingLoadState = LoadState.None
            isRefreshing.set(true)
        }
        launch(block = {
            loadData(mRepo.api.timbreMineList(PagedBody(page, PagedProxy.DEFAULT_PAGE_SIZE)), adapter, {
                val list = adapter.items.filter { it.data.get()?.isGenerating() == true }.map { it.data.get()?.timbreId }
                Logger.d(TAG,"返回需要轮询的音色数据:$list")
                getTimbreStatus(list.toMutableList())
            }, { timbre ->
                initItemMyTimbreVM(timbre)
            })
        }, onError = {
            page--
            adapterHelper.trailingLoadState = failState(adapter, it)
            isRefreshing.set(false)
        })
    }

    var currentTimbre: ItemMyTimbreVM? = null

    fun initItemMyTimbreVM(timbre: TimbreInfo): ItemMyTimbreVM {
        return ItemMyTimbreVM(timbre).apply {
            if (timbre.timbreId == currentTimbre?.data?.get()?.timbreId && currentTimbre?.playing?.get() == true) {
                playing.set(true)
                currentTimbre = this@apply
            }

            onGenerateClick = {
                onTimbreGenerateClick?.invoke(this@apply)
            }

            onLongClick = {
                if (data.get()?.isNameModifyValid() == true) {
                    onTimbreLongClick?.invoke(this@apply)
                }
                true
            }
            onPlayClick = {
                data.get()?.let { onPlayClick(it) }
            }
        }
    }
    private fun ItemMyTimbreVM.onPlayClick(it1: TimbreInfo) {
        if (it1.available()) {
            if (currentTimbre != this) {
                currentTimbre?.playing?.set(false)
                it1.timbreSoundUrl?.let { player.play(it) }
                playing.set(true)
                currentTimbre = this
            } else {
                if (playing.get()) {
                    player.stop()
                    playing.set(false)
                } else {
                    it1.timbreSoundUrl?.let { player.play(it) }
                    playing.set(true)
                }
            }
        }
    }

    fun stopPlay() {
        if (player.isPlaying) {
            currentTimbre?.data?.get()?.let { currentTimbre?.onPlayClick(it) }
        }
    }

    var job: Job? = null
    var timbreIds: MutableList<String?>? = null

    companion object{
        const val TAG = "MyTimbresViewModel"
    }

    fun getTimbreStatus(timbreId: String?) {
        if (timbreIds.isNullOrEmpty()) {
            timbreIds = mutableListOf()
        }
        timbreIds?.add(timbreId)
        getTimbreStatus(timbreIds)
    }

    fun getTimbreStatus(list: MutableList<String?>? = null) {
        job?.cancel()
        if (list.isNullOrEmpty()) return
        timbreIds = list
        job = launch {
            while (true) {
                delay(1000)
                timbreIds?.toList()?.forEach { timbreId ->
                    response(mRepo.api.timbreStatusGet(UserTimbreIdBody(timbreId))) {
                        val vM = adapter.items.find { it.data.get()?.timbreId == timbreId }
                        if (vM == null && data != null) {
                            adapter.add(if (adapter.itemCount > 2) 2 else adapter.itemCount, initItemMyTimbreVM(data!!))
                        }
                        if (data?.isGenerated() == true) {
                            vM?.data?.set(data)
                            vM?.name?.set(data?.getGenerateTitle())
                            timbreIds?.remove(timbreId)
                            if (timbreIds?.isEmpty() == true) {
                                job?.cancel()
                            }
                        } else if (data?.isGenerating() == true) {
                            vM?.data?.set(data)
                        }
                    }
                }
                if (timbreIds.isNullOrEmpty()) {
                    job?.cancel()
                }
            }
        }
    }

    fun removeTimbreId(timbreId: String?) {
        timbreIds?.remove(timbreId)
        if (timbreIds?.isEmpty() == true) {
            job?.cancel()
        }
    }

    /**
     * 加载数据
     */
    private fun <Item, ItemVM : AppItemViewModel<Item>> loadData(
        resp: ApiResponse<PagedList<Item>?>,
        adapter: QuickAdapter<ItemVM>,
        totalSize: (Int) -> Unit,
        itemVMConverter: (Item) -> ItemVM
    ) {
        response(
            resp,
            {
                data?.items?.map { itemVMConverter.invoke(it) }.let {
                    if (page == 1) {
                        adapter.submitList(it)
                    } else {
                        it?.let { adapter.addAll(it) }
                    }
                }
                adapterHelper.trailingLoadState = when {
                    adapter.items.isEmpty() -> if (adapterHelper.beforeAdapterList.isNotEmpty()) LoadState.NotLoading.Complete
                    else LoadState.Error(DefaultTrailingLoadStateAdapter.NoDataException())

                    page >= (data?.pages ?: 0) -> LoadState.NotLoading.Complete
                    else -> LoadState.NotLoading.Incomplete
                }
                isRefreshing.set(false)
                data?.totalSize?.let(totalSize)
            },
            {
                page--
                adapterHelper.trailingLoadState = failState(adapter, this?.exception() ?: Exception())
                isRefreshing.set(false)
            })
    }

    override fun onCleared() {
        if (currentTimbre?.playing?.get() == true) {
            player.stop()
        }
        job?.cancel()
        super.onCleared()
    }
}