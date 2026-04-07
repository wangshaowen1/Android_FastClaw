package com.kgmedia.fastfilm.feature.works.assetmanager

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.text.method.ScrollingMovementMethod
import com.kgmedia.fastfilm.common.Constants
import com.kgmedia.fastfilm.core.network.apis.entity.SimpleAsset
import com.kgmedia.fastfilm.databinding.ActivityAssetDtlBinding
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
        fun <T> start(context: Context, workId: String?, storyId: String?, info: T?) where T : SimpleAsset, T : Parcelable {
            val starter = Intent(context, AssetDetailActivity::class.java)
            starter.putExtra(Constants.Extra.DATA, info)
            starter.putExtra(Constants.Extra.WORK_ID, workId)
            starter.putExtra(Constants.Extra.STORY_ID, storyId)
            context.startActivity(starter)
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding.desc.movementMethod = ScrollingMovementMethod()
    }


}