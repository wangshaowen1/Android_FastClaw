package com.kgmedia.fastfilm.feature.editor

import android.app.Activity
import android.content.Intent
import com.kgmedia.fastfilm.common.Constants
import com.kgmedia.fastfilm.common.ext.showCommonDialog
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
        fun launchIntent(activity: Activity, info: SimpleStoryboardInfo): Intent {
            val intent = Intent(activity, EditStoryboardActivity::class.java)
            intent.putExtra(Constants.Extra.DATA, info)
            return intent
        }

        @JvmStatic
        fun start(info: SimpleStoryboardInfo) {
            topActivity?.let { it.startActivity(launchIntent(it, info)) }
        }
    }

    override fun onBackPressed() {
        if (mRealVM.storyboardDesc.value != mRealVM.storyboard.get()?.storyBoardDesc) {
            showCommonDialog(tip = "您对分镜信息有修改尚未保存，是否保存修改？", confirmText = "保存并退出") {
                mRealVM.save(true)
            }
        } else {
            super.onBackPressed()
        }
    }
}