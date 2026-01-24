package com.kgmedia.fastfilm.feature.play

import android.content.Intent
import android.os.Bundle
import android.view.View
import com.kgmedia.fastfilm.R
import com.kgmedia.fastfilm.common.Constants.Extra
import com.kgmedia.fastfilm.common.ext.topActivity
import com.kgmedia.fastfilm.databinding.ActivityShortDramaPlayerBinding
import com.kgmedia.fastfilm.feature.play.more.ShareShortDramaActivity
import com.kgmedia.fastfilm.kit.globalShortsPlayer
import com.kgmedia.fastfilm.kit.setupMediaSourceManager
import com.kgmedia.fastfilm.kit.source.impl.ReleasedShortDetailSource
import com.nhsd.basic.ext.setupTranslucentStatus
import com.nhsd.basic.ext.switchFragment

/**
 * 短剧播放
 */
class ShortDramaPlayerActivity :
    ShareShortDramaActivity<ActivityShortDramaPlayerBinding, ShortDramaPlayerHostViewModel>() {
    companion object {
        fun start(dramaShortId: String?) {
            val activity = topActivity ?: return
            val intent = Intent(activity, ShortDramaPlayerActivity::class.java)
            intent.putExtra(Extra.DRAMA_ID, dramaShortId)
            activity.startActivity(intent)
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val dramaId = intent.getStringExtra(Extra.DRAMA_ID)
        supportFragmentManager.switchFragment(
            R.id.fragment,
            ShortDramaPlayerFragment::class.java,
            ShortDramaPlayerFragment::class.java.simpleName,
            addToBackStack = false,
            Bundle().apply {
                putString(Extra.DRAMA_ID, dramaId)
                //true 是作品播放
                putBoolean(Extra.MODE, true)
            })
    }

    override fun setTranslucentStatus() {
        setupTranslucentStatus(false)
    }

    override val exportVideoOverlay: View?
        get() = bindingNullable?.overlay?.root

}