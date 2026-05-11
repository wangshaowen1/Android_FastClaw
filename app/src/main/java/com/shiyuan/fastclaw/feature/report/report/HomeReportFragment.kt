package com.shiyuan.fastclaw.feature.report.report

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import com.gyf.immersionbar.ImmersionBar
import com.shiyuan.fastclaw.R
import com.nhsd.basic.BaseViewModelActivity
import com.nhsd.basic.BaseViewModelFragment
import com.nhsd.basic.bus.observeEvents
import com.nhsd.basic.ext.getViewModelOfActivity
import com.shiyuan.fastclaw.core.Events
import com.shiyuan.fastclaw.core.User
import com.shiyuan.fastclaw.core.ext.onUserStatusChanged
import com.shiyuan.fastclaw.databinding.ActivityGennerReportBinding
import com.shiyuan.fastclaw.databinding.FragmentHomeReportBinding
import com.shiyuan.fastclaw.databinding.FragmentMineBinding
import com.shiyuan.fastclaw.feature.main.MainViewModel
import com.shiyuan.fastclaw.feature.mine.MineViewModel
import com.shiyuan.fastclaw.feature.report.genner.GenerateReportViewModel

/**
 * @Introduce: 历史报告列表页面
 */
class HomeReportFragment : BaseViewModelFragment<FragmentHomeReportBinding, HomeReportViewModel>() {


    private var userId: String? = null

    override fun onPrepareView() {
        super.onPrepareView()
        initView()
        //数据更新
        dataUpdate()
    }

    private fun initView() {
     /*   binding.bannerView.setIndicatorView(binding.indicatorView)
            .setIndicatorColor(Color.parseColor("#888888"),
                Color.parseColor("#118EEA"))
            .setHolderCreator(() -> new ImageResourceViewHolder(BannerUtils.dp2px(5)))
        .create(mDrawableList);*/

    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (hidden) return

    }

    override fun onResume() {
        super.onResume()

    }

    private fun dataUpdate() {

    }


    override fun <Data> onClick(view: View, data: Data?) {
        super.onClick(view, data)
        when (view.id) {
            /*         R.id.siv_avatar,
                     R.id.tv_login,
                     R.id.tv_name -> {
                         //个人资料
                         AppNavigator.checkLoginIntercepted(false)
                     }

                     R.id.ll_qa -> {
                         SimpleBrowserActivity.launch(activity, Config.get().appConfig.appHelpCenterUrl)
                     }

                     R.id.ll_settings -> {
                         //设置
                         if (AppNavigator.checkLoginIntercepted(false)) {
                             return
                         }
                         SettingsActivity.start(requireActivity())
                     }

                     R.id.tv_id, R.id.iv_copy -> {
                         //复制
                         User.get().userUid?.let {
                             ClipboardUtils.copyText(it)
                             getString(R.string.copy_success).showToast()
                         }
                     }

                     R.id.suggestions_feedback -> {
                         if (AppNavigator.checkLoginIntercepted(false)) {
                             return
                         }
                         //建议与反馈
                         FeedbackActivity.start(topActivity)
                     }

                     R.id.btnMyBalance -> {
                         if (AppNavigator.checkLoginIntercepted(false)) {
                             return
                         }
                     }*/
        }
    }
}
