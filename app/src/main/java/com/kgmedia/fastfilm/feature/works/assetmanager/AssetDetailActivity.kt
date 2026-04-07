package com.kgmedia.fastfilm.feature.works.assetmanager

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.text.method.ScrollingMovementMethod
import android.view.View
import com.kgmedia.fastfilm.R
import com.kgmedia.fastfilm.common.Constants
import com.kgmedia.fastfilm.common.ext.showCommonDialog
import com.kgmedia.fastfilm.core.network.apis.entity.RoleImageInfo
import com.kgmedia.fastfilm.core.network.apis.entity.SimpleAsset
import com.kgmedia.fastfilm.core.network.apis.entity.SimpleRoleInfo
import com.kgmedia.fastfilm.databinding.ActivityAssetDtlBinding
import com.kgmedia.fastfilm.feature.editor.info.InputDialog
import com.nhsd.basic.BaseViewModelActivity

/**
 * 资产详情页 可修改资产描述 重新生成图片
 */
class AssetDetailActivity : BaseViewModelActivity<ActivityAssetDtlBinding, AssetDetailViewModel>() {


    companion object {
        const val TAG = "AssetManagerActivity"

//        @JvmStatic
//        fun start(context: Context, info: SimpleRoleInfo?) {
//            val starter = Intent(context, AssetDetailActivity::class.java)
//            starter.putExtra(Constants.Extra.DATA, info)
//            context.startActivity(starter)
//        }
//
//        @JvmStatic
//        fun start(context: Context, info: SimpleSceneInfo?) {
//            val starter = Intent(context, AssetDetailActivity::class.java)
//            starter.putExtra(Constants.Extra.DATA, info)
//            context.startActivity(starter)
//        }
//
//        @JvmStatic
//        fun start(context: Context, info: SimplePropInfo?) {
//            val starter = Intent(context, AssetDetailActivity::class.java)
//            starter.putExtra(Constants.Extra.DATA, info)
//            context.startActivity(starter)
//        }

        @JvmStatic
        fun <T> start(context: Context, workId: String?,info: T?) where T : SimpleAsset, T : Parcelable {
            val starter = Intent(context, AssetDetailActivity::class.java)
            starter.putExtra(Constants.Extra.DATA, info)
            starter.putExtra(Constants.Extra.WORK_ID, workId)
            context.startActivity(starter)
        }

        @JvmStatic
        fun start(context: Context, workId: String?, roleInfo: SimpleRoleInfo?, info: RoleImageInfo?) {
            val starter = Intent(context, AssetDetailActivity::class.java)
            starter.putExtra(Constants.Extra.ROLE_IMAGE, info)
            starter.putExtra(Constants.Extra.DATA, roleInfo)
            starter.putExtra(Constants.Extra.WORK_ID, workId)
            context.startActivity(starter)
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding.desc.movementMethod = ScrollingMovementMethod()

        mRealVM.hasChanged.observe(this) {
            mRealVM.mToolbarVM.mMenuEnable.set(it)
        }
    }

    override fun onBackPressed() {
        if (mRealVM.generating.get()) {
            showCommonDialog(getString(R.string.exit_during_generation_warning), confirmText = getString(R.string.continue_exit), confirm = {
                super.onBackPressed()
            })
            return
        } else if (mRealVM.hasChanged.value == true) {
            showCommonDialog(
                getString(R.string.save_and_overwrite_asset_warning), cancelText = getString(R.string.exit_without_saving), confirmText = getString(
                    R.string.save_and_overwrite
                ), cancel = {
                    super.onBackPressed()
                }, confirm = {
                    mRealVM.save {
                        super.onBackPressed()
                    }
                })
            return
        }
        super.onBackPressed()
    }

    override fun <Data> onClick(view: View?, data: Data?) {
        super.onClick(view, data)

        when (view?.id) {
            R.id.btnRegenerate -> {
                mRealVM.reGenerate()
            }

            R.id.desc -> {
                InputDialog.show(InputDialog.TYPE_ASSET_DESC, content = mRealVM.prompt.get()) {
                    mRealVM.prompt.set(it)
                }
            }
        }
    }


}