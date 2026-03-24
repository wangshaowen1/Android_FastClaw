package com.kgmedia.fastfilm.feature.recharge

import android.view.Gravity
import androidx.fragment.app.FragmentActivity
import com.blankj.utilcode.util.SPUtils
import com.blankj.utilcode.util.ScreenUtils
import com.kgmedia.fastfilm.R
import com.kgmedia.fastfilm.common.ext.topActivity
import com.kgmedia.fastfilm.core.Config
import com.kgmedia.fastfilm.core.User
import com.kgmedia.fastfilm.core.databinding.loadGifUrl
import com.kgmedia.fastfilm.databinding.DialogPaySuccessBinding
import com.kgmedia.fastfilm.router.AppRouter
import com.nhsd.basic.BaseDialogFragment
import com.nhsd.basic.ext.getResDrawable
import com.nhsd.basic.ext.setOnSingleClickListener
import kotlin.math.roundToInt

/**
 * 支付成功弹窗
 */
class PaySuccessDialog : BaseDialogFragment<DialogPaySuccessBinding>() {

    companion object {
        private const val KEY_HAS_ADDED = "has_added_service_"
        private const val KEY_SHOW_COUNT = "show_count_"

        fun checkAndShow() {
            val activity = topActivity as? FragmentActivity ?: return
            val uid = User.get().userId

            // 1. 判断是否已经点击过“立即添加”
            if (SPUtils.getInstance().getBoolean(KEY_HAS_ADDED + uid, false)) return

            // 2. 判断弹出次数是否超过 3 次
            val count = SPUtils.getInstance().getInt(KEY_SHOW_COUNT + uid, 0)
            if (count >= 3) return

            // 符合条件，显示弹窗并累加次数
            SPUtils.getInstance().put(KEY_SHOW_COUNT + uid, count + 1)
            PaySuccessDialog().show(activity.supportFragmentManager)
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.let { dialogWindow ->
            dialogWindow.attributes?.let { attr ->
                attr.width = (ScreenUtils.getScreenWidth() * 0.78).roundToInt()
                attr.gravity = Gravity.CENTER
                dialogWindow.attributes = attr
            }
        }
    }

    override fun onPrepareView() {
        super.onPrepareView()
        val appConfig = Config.get().appConfig
        val uid = User.get().userId

        loadGifUrl(binding.image, appConfig.appPopupPointGuideImageUrl, R.drawable.img_default_cover_3_4.getResDrawable())

        // 暂不添加/关闭
        binding.btnCancel.setOnClickListener {
            dismissAllowingStateLoss()
        }

        // 立即添加
        binding.btnAdd.setOnSingleClickListener {
            // 标记为永久不再弹出
            SPUtils.getInstance().put(KEY_HAS_ADDED + uid, true)

            // 执行跳转逻辑
            AppRouter.jump(appConfig.appJumpMiniProgramCustomerServiceUrl)
            dismissAllowingStateLoss()
        }
    }
}