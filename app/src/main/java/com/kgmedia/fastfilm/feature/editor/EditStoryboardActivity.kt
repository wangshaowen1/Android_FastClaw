package com.kgmedia.fastfilm.feature.editor

import android.app.Activity
import android.content.Intent
import android.view.View
import com.kgmedia.fastfilm.common.Constants
import com.kgmedia.fastfilm.common.ext.topActivity
import com.kgmedia.fastfilm.core.network.apis.entity.SimpleStoryboardInfo
import com.kgmedia.fastfilm.databinding.ActivityEditStoryboardBinding
import com.nhsd.basic.BaseViewModelActivity

/**
 * 分镜重绘
 */
class EditStoryboardActivity : BaseViewModelActivity<ActivityEditStoryboardBinding, EditStoryboardViewModel>() {
    companion object {
        @JvmStatic
        fun launchIntent(activity: Activity, workId: String?, storyId: String?, info: SimpleStoryboardInfo): Intent {
            val intent = Intent(activity, EditStoryboardActivity::class.java)
            intent.putExtra(Constants.Extra.WORK_ID, workId)
            intent.putExtra(Constants.Extra.STORY_ID, storyId)
            intent.putExtra(Constants.Extra.DATA, info)
            return intent
        }

        @JvmStatic
        fun start(workId: String?, storyId: String?, info: SimpleStoryboardInfo) {
            topActivity?.let { it.startActivity(launchIntent(it, workId, storyId, info)) }
        }
    }

    override fun <Data> onClick(view: View?, data: Data?) {
        super.onClick(view, data)
        when (view?.id) {
        }
    }

}