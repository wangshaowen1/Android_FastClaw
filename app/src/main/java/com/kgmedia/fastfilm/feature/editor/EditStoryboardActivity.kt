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
        fun launchIntent(activity: Activity, info: SimpleStoryboardInfo, workRatio: String?, storyStatus: Int?): Intent {
            val intent = Intent(activity, EditStoryboardActivity::class.java)
            intent.putExtra(Constants.Extra.DATA, info)
            intent.putExtra(Constants.Extra.VIDEO_RATIO, workRatio)
            intent.putExtra(Constants.Extra.STATUS, storyStatus)
            return intent
        }

        @JvmStatic
        fun start(info: SimpleStoryboardInfo, workRatio: String?, storyStatus: Int?) {
            topActivity?.let { it.startActivity(launchIntent(it, info, workRatio, storyStatus)) }
        }
    }

    override fun onBackPressed() {
        if (mRealVM.storyBoardDesc.value != mRealVM.storyboard.get()?.storyBoardDesc) {
            showCommonDialog(
                tip = "您对分镜信息有修改尚未保存，是否保存修改？",
                cancelText = "不保存",
                confirmText = "保存并退出",
                confirm = { mRealVM.save(true) })
        } else {
            super.onBackPressed()
        }
    }
}