package com.kgmedia.fastfilm.feature.play

import android.graphics.Color
import com.kgmedia.fastfilm.R
import com.kgmedia.fastfilm.base.viewmodel.ToolbarViewModel
import com.kgmedia.fastfilm.common.Constants
import com.kgmedia.fastfilm.core.ext.launchWithLoading
import com.kgmedia.fastfilm.core.response
import com.kgmedia.fastfilm.feature.play.data.ShortPlayRepository
import com.kgmedia.fastfilm.feature.play.more.ShareShortDramaViewModel
import com.kgmedia.fastfilm.kit.PlayerFactory
import com.kgmedia.fastfilm.kit.play
import com.nhsd.basic.ext.getDrawable

/**
 * 短剧播放
 */
class ShortDramaPlayerHostViewModel : ShareShortDramaViewModel<ShortPlayRepository>(){

    var mToolbarVM = ToolbarViewModel(
        navigationDrawable = R.drawable.ic_nav_back.getDrawable(),
        navigationAction = { back() },title = "", background = Color.TRANSPARENT)

    val player by lazy { PlayerFactory.Companion.createShortPlayer(withCache = true, autoAudioFocus = true)}
    val dramaId by lazy{ mBundle.getString(Constants.Extra.DRAMA_ID)}
    override fun onCreate() {
        super.onCreate()
        launchWithLoading {
            response(mRepo.getShortDramaDetail(dramaId)) {
                this.data?.let {player.play(it) }
            }
        }
    }
}