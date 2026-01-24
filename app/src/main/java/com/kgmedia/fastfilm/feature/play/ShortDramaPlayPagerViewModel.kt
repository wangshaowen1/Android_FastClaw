package com.kgmedia.fastfilm.feature.play

import androidx.databinding.ObservableField
import com.kgmedia.fastfilm.common.Constants
import com.kgmedia.fastfilm.common.PagedProxy
import com.kgmedia.fastfilm.common.livedata.UnPeekLiveData
import com.kgmedia.fastfilm.core.ext.launch
import com.kgmedia.fastfilm.core.manager.EventManager
import com.kgmedia.fastfilm.core.network.apis.entity.ShortDramaInfo
import com.kgmedia.fastfilm.feature.play.data.ShortPlayRepository
import com.kgmedia.fastfilm.feature.play.more.ShareShortDramaViewModel
import com.kgmedia.fastfilm.kit.DefaultPlaylistPlayer
import com.kgmedia.fastfilm.kit.DetailLoadingPlaylistPlayer
import com.kgmedia.fastfilm.kit.PlaylistListener
import com.kgmedia.fastfilm.kit.getMediaSourceManager

/**
 * 短剧播放
 */
class ShortDramaPlayPagerViewModel : ShareShortDramaViewModel<ShortPlayRepository>(),
    PlaylistListener {
    val seriesId by lazy {
        mBundle.getString(Constants.Extra.SERIES_ID)
    }
    val initPosition by lazy { mBundle.getInt(Constants.Extra.POSITION) }

    val shortsPlayer by lazy {
        DetailLoadingPlaylistPlayer(delegate = DefaultPlaylistPlayer())
    }
    val playlist: MutableList<ShortDramaInfo>?
        get() = shortsPlayer.playlist as? MutableList<ShortDramaInfo>?

    val checkedShort = UnPeekLiveData<ShortDramaInfo?>()
    val workNoTitle = ObservableField<String>()

    override fun onCreate() {
        super.onCreate()
        shortsPlayer.addListener(this)
        setNavigationTextByPosition(initPosition)
    }


    fun handleOnDramaChanged(shortInfo: ShortDramaInfo?) {
        shortInfo?.let {
            launch {
                EventManager.onDramaShortStart(
                    it.dramasId)
            }

        }
    }
    fun setNavigationTextByPosition(position:Int) {
        workNoTitle.set(makeTitleByPosition(position))
    }

    private fun makeTitleByPosition(position: Int): String {
        return "第${position + 1}章"
    }

    override fun onShortChanged(short: ShortDramaInfo?) {
        super.onShortChanged(short)
        checkedShort.value = short
        handleOnDramaChanged(short)
        if(short?.isLocked() == true){//临时处理自动切换到下一集被锁住的短剧
            shortsPlayer.pause()
        }
    }

    var isPlayingBeforeSkip = false
    override fun onResume() {
        super.onResume()
        if(isPlayingBeforeSkip){
            if(!shortsPlayer.isPlaying){
                shortsPlayer.play()
            }
            isPlayingBeforeSkip=false
        }
    }



    override fun onDestroy() {
        super.onDestroy()
        shortsPlayer.removeListener(this)
        shortsPlayer.getMediaSourceManager()?.release()
        shortsPlayer.release()
    }

    fun minPageSize(): Int {
        if ((shortsPlayer.playlist?.size ?: 0) < initPosition) {
            val pos = initPosition.coerceAtLeast(0)
            return ((pos / 10) + 1) * 10
        }
        return PagedProxy.DEFAULT_PAGE_SIZE
    }

    fun updateAutoUnlock(seriesId: String?, auto: Boolean) {
        playlist?.forEach {
            if (it.dramasSeries?.dramasSeriesId == seriesId) {
                it.dramasSeries?.autoUnlockStatus = if (auto) 1 else 0
            }
        }
    }

    fun pausePlayerBeforeSkip() {
        if(shortsPlayer.isPlaying) {
            shortsPlayer.pause()
            isPlayingBeforeSkip = true
        }
    }

}