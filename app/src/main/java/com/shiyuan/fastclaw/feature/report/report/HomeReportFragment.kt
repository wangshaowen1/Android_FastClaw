package com.shiyuan.fastclaw.feature.report.report

import android.view.View
import android.graphics.Color
import com.shiyuan.fastclaw.R
import com.nhsd.basic.BaseViewModelFragment
import com.nhsd.basic.ext.dp
import com.shiyuan.fastclaw.databinding.FragmentHomeReportBinding
import com.shiyuan.fastclaw.router.AppNavigator
import com.zhpan.indicator.enums.IndicatorStyle

/**
 * @Introduce: 历史报告列表页面
 */
class HomeReportFragment : BaseViewModelFragment<FragmentHomeReportBinding, HomeReportViewModel>() {


    private var userId: String? = null

    override fun onPrepareView() {
        super.onPrepareView()
        initView()
        dataUpdate()
    }

    private fun initView() {
        binding.bannerView.apply {
            setIndicatorGravity(com.zhpan.bannerview.constants.IndicatorGravity.START)
            setIndicatorSliderColor(resources.getColor(R.color.color_F6F6F7),resources.getColor(R.color.color_0099FF))
            setIndicatorStyle(IndicatorStyle.CIRCLE)
            setIndicatorSliderWidth(6.dp, 6.dp)
            setIndicatorHeight(6.dp)
            setScrollDuration(750)
            setInterval(3000)
            setLifecycleRegistry(lifecycle)
        }

        mRealVM.bannerList.get()?.let { imageList ->
            val adapter = ImageBannerAdapter()
            binding.bannerView.adapter = adapter
            binding.bannerView.create(imageList)
        }
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
            R.id.tv_generate -> {
                if(AppNavigator.checkLoginIntercepted(true)){
                    //跳转

                }
            }

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
