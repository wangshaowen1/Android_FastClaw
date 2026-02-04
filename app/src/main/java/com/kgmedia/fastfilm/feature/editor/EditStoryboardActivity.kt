package com.kgmedia.fastfilm.feature.editor

import android.app.Activity
import android.content.Intent
import android.view.View
import com.kgmedia.fastfilm.R
import com.kgmedia.fastfilm.common.Constants
import com.kgmedia.fastfilm.common.ext.showCommonDialog
import com.kgmedia.fastfilm.common.ext.topActivity
import com.kgmedia.fastfilm.databinding.ActivityEditStoryboardVideoBinding
import com.nhsd.basic.BaseViewModelActivity
import com.nhsd.basic.ext.showToast

/**
 * 分镜重绘
 */
class EditStoryboardActivity : BaseViewModelActivity<ActivityEditStoryboardVideoBinding, EditStoryboardVideoViewModel>() {
    companion object {

        @JvmStatic
        fun launchIntent(activity: Activity, workId: String?): Intent {
            val intent = Intent(activity, EditStoryboardActivity::class.java)
            intent.putExtra(Constants.Extra.WORK_ID, workId)
            return intent
        }

        @JvmStatic
        fun start(workId: String?) {
            topActivity?.let { it.startActivity(launchIntent(it, workId)) }
        }
    }

    override fun <Data> onClick(view: View?, data: Data?) {
        super.onClick(view, data)
        when (view?.id) {
            R.id.btnRegenerate -> {
                val current = mRealVM.currentStoryboard.get()?.data?.get() ?: return
                if (current.generating) {
                    "视频正在生成中，请稍后再试".showToast()
                    return
                }
            }

            R.id.btnDownload -> {
                val current = mRealVM.currentStoryboard.get()?.data?.get() ?: return
                if (current.generating) {
                    "视频正在生成中，请稍后再试".showToast()
                    return
                }
                if (current.failed) {
                    "视频生成失败，请重新生成".showToast()
                    return
                }
                if (current.videoGenerated) {
                    current.videoUrl?.let { mRealVM.download(it) }
                }
            }

            R.id.btnDelete -> {
                showCommonDialog(tip = "删除后，该分镜视频将永久移除，无法恢复，并可能影响后续视频合成，是否确认删除", confirmText = "确认删除") {
                    mRealVM.deleteCurrentStoryboard()
                }
            }

            R.id.btnGenerate -> {

            }
        }
    }

}