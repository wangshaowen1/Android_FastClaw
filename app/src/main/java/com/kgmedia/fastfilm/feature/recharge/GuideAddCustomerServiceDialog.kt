package com.kgmedia.fastfilm.feature.recharge

import android.os.Bundle
import android.view.Gravity
import androidx.fragment.app.FragmentActivity
import com.blankj.utilcode.util.SPUtils
import com.blankj.utilcode.util.ScreenUtils
import com.kgmedia.fastfilm.R
import com.kgmedia.fastfilm.common.Constants
import com.kgmedia.fastfilm.common.ext.topActivity
import com.kgmedia.fastfilm.common.utils.DateUtil
import com.kgmedia.fastfilm.core.Global
import com.kgmedia.fastfilm.core.User
import com.kgmedia.fastfilm.core.databinding.loadGifUrl
import com.kgmedia.fastfilm.core.network.apis.response.SimpleActivityInfo
import com.kgmedia.fastfilm.databinding.DialogGuideAddCustomerServiceBinding
import com.kgmedia.fastfilm.router.AppRouter
import com.nhsd.basic.BaseDialogFragment
import com.nhsd.basic.ext.getParcelableValue
import com.nhsd.basic.ext.getResDrawable
import com.nhsd.basic.ext.setOnSingleClickListener
import kotlin.math.roundToInt

/**
 * 引导添加客服弹窗
 */
class GuideAddCustomerServiceDialog : BaseDialogFragment<DialogGuideAddCustomerServiceBinding>() {

    companion object {
        //弹窗场景标识
        const val SCENE_FIRST_RECHARGE = "SCENE_FIRST_RECHARGE"

        //弹窗场景标识
        const val SCENE_HOME = "SCENE_HOME"

        //是否跳转过微信小程序，跳过就不用弹了
        private const val HAS_JUMPED = "has_jumped_wechat"

        fun isHasJumped(): Boolean {
            return SPUtils.getInstance().getBoolean(HAS_JUMPED, false)
        }

        fun markHasJumped() {
            SPUtils.getInstance().put(HAS_JUMPED, true)
        }

        /**
         * @param scene 场景标识，确保不同场景互不干扰
         */
        fun checkAndShow(scene: String = SCENE_HOME, isPaid: Boolean? = null, callback: ((GuideAddCustomerServiceDialog?) -> Unit)? = null) {
            val activity = topActivity as? FragmentActivity ?: return
            // 1，已经跳转过小程序的话 就所有场景都不需要显示
            if (isHasJumped()) return
            val uid = User.get().userId
            // 组合 Key：日期 + 场景名 + 用户 ID
            val showKey = "${DateUtil.getDateTime()}${scene}_${uid}"
            // 2，判断该场景下今天是否已经显示过
            if (SPUtils.getInstance().getBoolean(showKey, false)) return

            Global.getGuideLinkCustomerInfo { list ->
                if (list.isNullOrEmpty()) return@getGuideLinkCustomerInfo
                // 确定需要查找的 targetCode
                val targetCode = if (isPaid == true || User.get().hasPaid()) {
                    SimpleActivityInfo.POPUP_PAID
                } else {
                    SimpleActivityInfo.POPUP_UNPAID
                }
                // 统一查找和弹窗逻辑，消除 if/else 里的重复代码
                list.find { it.businessCode == targetCode && it.isEnable() }?.let { info ->
                    val dialog = GuideAddCustomerServiceDialog().apply {
                        arguments = Bundle().apply {
                            putString(Constants.Extra.MODE, scene)
                            putParcelable(Constants.Extra.DATA, info)
                        }
                    }
                    dialog.show(activity.supportFragmentManager, "GuideAddCustomerServiceDialog") // 建议加上 tag
                    callback?.invoke(dialog)
                    SPUtils.getInstance().put(showKey, true)
                }
            }
        }
    }

    val info: SimpleActivityInfo? by lazy { arguments?.getParcelableValue(Constants.Extra.DATA, SimpleActivityInfo::class.java) }

    override fun onStart() {
        super.onStart()
        dialog?.window?.let { dialogWindow ->
            dialogWindow.attributes?.let { attr ->
                attr.width = ScreenUtils.getScreenWidth()
                attr.height = (ScreenUtils.getScreenWidth() / 375f * 415f).roundToInt()
                attr.gravity = Gravity.CENTER
                dialogWindow.attributes = attr
            }
        }
    }

    override fun onPrepareView() {
        super.onPrepareView()
        binding.image.minimumHeight = (ScreenUtils.getScreenWidth() / 375f * 415f).roundToInt()
        loadGifUrl(binding.image, info?.imageUrl, R.drawable.img_default_cover_1_1.getResDrawable())

        binding.btnCancel.setOnClickListener {
            dismissAllowingStateLoss()
        }

        binding.btnAdd.setOnSingleClickListener {
            markHasJumped()
            AppRouter.jump(info?.jumpUrl)
            dismissAllowingStateLoss()
        }
    }
}