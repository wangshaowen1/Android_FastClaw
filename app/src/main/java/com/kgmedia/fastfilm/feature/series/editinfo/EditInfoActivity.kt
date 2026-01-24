package com.kgmedia.fastfilm.feature.series.editinfo

import android.content.Intent
import android.os.Bundle
import android.view.View
import com.blankj.utilcode.util.KeyboardUtils
import com.kgmedia.fastfilm.R
import com.kgmedia.fastfilm.common.Constants
import com.kgmedia.fastfilm.common.ext.selectSinglePhotoFromAlbums
import com.kgmedia.fastfilm.common.ext.topActivity
import com.kgmedia.fastfilm.common.permissions.requestImages2VideosPermissions
import com.kgmedia.fastfilm.databinding.ActivitySeriesEditInfoBinding
import com.nhsd.basic.BaseViewModelActivity
import com.nhsd.mattise.Matisse

/**
 * 修改剧集信息
 */
class EditInfoActivity : BaseViewModelActivity<ActivitySeriesEditInfoBinding, EditInfoViewModel>() {
    companion object {
        private const val REQUEST_CODE_CHOOSE = 0x14
        fun start(seriesId: String?) {
            val activity = topActivity ?: return
            activity.startActivity(
                Intent(activity, EditInfoActivity::class.java).apply {
                    putExtra(Constants.Extra.ID, seriesId)
                }
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        KeyboardUtils.registerSoftInputChangedListener(window) {
            mRealVM.onSoftInputChanged(it)
        }
    }

    override fun onDestroy() {
        KeyboardUtils.unregisterSoftInputChangedListener(window)
        super.onDestroy()
    }

    override fun <Data> onClick(view: View?, data: Data?) {
        super.onClick(view, data)
        when (view?.id) {
            R.id.ivCover -> {
                requestAlbumsPermission()
            }
        }
    }

    /**
     * 请求存储权限并选择头像
     */
    private fun requestAlbumsPermission() {
        requestImages2VideosPermissions {
            if (it.not()) {
                //权限申请失败
                return@requestImages2VideosPermissions
            }
            //权限申请成功
            selectSinglePhotoFromAlbums(
                activity = this@EditInfoActivity,
                selectedUri = null,
                requestCode = REQUEST_CODE_CHOOSE,
                crop = true,
                cropWHMax = 750 to 1000,
                cropWHRatio = 3f to 4f
            )
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_CODE_CHOOSE) {
                val result = Matisse.obtainPathResult(data)
                result?.firstOrNull()?.let { mRealVM.changeCover(it) }
            }
        }
    }
}