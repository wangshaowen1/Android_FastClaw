package com.kgmedia.fastfilm.feature.editor

import android.app.Activity
import android.content.Intent
import android.view.View
import com.kgmedia.fastfilm.R
import com.kgmedia.fastfilm.common.Constants
import com.kgmedia.fastfilm.common.ext.showCommonDialog
import com.kgmedia.fastfilm.common.ext.topActivity
import com.kgmedia.fastfilm.core.network.apis.entity.SimpleStoryboardInfo
import com.kgmedia.fastfilm.databinding.ActivityEditStoryboardBinding
import com.kgmedia.fastfilm.feature.PhotoPagerActivity
import com.nhsd.basic.BaseViewModelActivity

/**
 * 分镜重绘
 */
class EditStoryboardActivity : BaseViewModelActivity<ActivityEditStoryboardBinding, EditStoryboardViewModel>() {
    companion object {
        @JvmStatic
        fun launchIntent(activity: Activity, info: SimpleStoryboardInfo, workRatio: String?): Intent {
            val intent = Intent(activity, EditStoryboardActivity::class.java)
            intent.putExtra(Constants.Extra.DATA, info)
            intent.putExtra(Constants.Extra.VIDEO_RATIO, workRatio)
            return intent
        }

        @JvmStatic
        fun start(info: SimpleStoryboardInfo, workRatio: String?) {
            topActivity?.let { it.startActivity(launchIntent(it, info, workRatio)) }
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

    override fun <Data> onClick(view: View?, data: Data?) {
        super.onClick(view, data)
        when (view?.id) {
            R.id.btnPreview -> {
                mRealVM.storyboard.get()?.imageUrl?.let { PhotoPagerActivity.launch(this, it) }
            }
        }
    }
}