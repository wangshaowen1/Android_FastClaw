package com.shiyuan.fastclaw.feature.report.report

import android.view.View
import android.graphics.Color
import android.graphics.Typeface
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


    override fun onPrepareView() {
        super.onPrepareView()
        initView()
        dataMonitoring()
        dataUpdate()
    }

    private fun dataMonitoring() {
        mRealVM.selectIndex.observe(this) {
            setView(it)
        }
    }

    fun setView(index: Int) {
        if(index == 0){
            binding.llTab1.background =  resources.getDrawable(R.drawable.bg_0099ff_r100)
            binding.tvTargetPosition.setTextColor(resources.getColor(R.color.white))
            binding.tvTargetPosition.setTypeface(binding.tvNoTargetPosition.typeface, Typeface.BOLD)
            binding.tvUpJd.visibility = View.VISIBLE
            binding.ivItem1.setImageResource(R.drawable.ic_background_check)
            binding.tvItem11.text = resources.getString(R.string.company_background_check)
            binding.tvItem12.text = resources.getString(R.string.report_home_tip3)




            binding.llTab2.background =  null
            binding.tvNoTargetPosition.setTextColor(resources.getColor(R.color.color_999999))
            binding.tvNoTargetPosition.typeface = Typeface.DEFAULT
            binding.tvNoJd.visibility = View.GONE
            binding.ivItem2.setImageResource(R.drawable.ic_job)
            binding.tvItem21.text = resources.getString(R.string.job_matching_analysis)
            binding.tvItem22.text = resources.getString(R.string.job_home_tip1)



            binding.llJd.visibility = View.VISIBLE
            binding.vLine.visibility = View.VISIBLE



        }else{
            binding.llTab1.background =  null
            binding.tvTargetPosition.setTextColor(resources.getColor(R.color.color_999999))
            binding.tvTargetPosition.typeface = Typeface.DEFAULT
            binding.tvUpJd.visibility = View.GONE
            binding.ivItem1.setImageResource(R.drawable.ic_skill)
            binding.tvItem11.text = resources.getString(R.string.job_matching_analysis)
            binding.tvItem12.text = resources.getString(R.string.job_home_tip1)


            binding.llTab2.background =  resources.getDrawable(R.drawable.bg_0099ff_r100)
            binding.tvNoTargetPosition.setTextColor(resources.getColor(R.color.white))
            binding.tvNoTargetPosition.setTypeface(binding.tvNoTargetPosition.typeface, Typeface.BOLD)
            binding.tvNoJd.visibility = View.VISIBLE
            binding.ivItem2.setImageResource(R.drawable.ic_personal)
            binding.tvItem21.text = resources.getString(R.string.personal_weaknesses)
            binding.tvItem22.text = resources.getString(R.string.personal_home_tip1)


            binding.llJd.visibility = View.GONE
            binding.vLine.visibility = View.GONE

        }
    }
    private fun initView() {
        binding.bannerView.apply {
            setIndicatorGravity(com.zhpan.bannerview.constants.IndicatorGravity.START)
            setIndicatorSliderColor(
                resources.getColor(R.color.color_F6F6F7),
                resources.getColor(R.color.color_0099FF)
            )
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
                if (AppNavigator.checkLoginIntercepted(true)) {
                    //跳转

                }
            }

            R.id.ll_tab1 -> {
                mRealVM.selectIndex.postValue(0)
            }

            R.id.ll_tab2 -> {
                mRealVM.selectIndex.postValue(1)
            }
        }
    }
}
