package com.shiyuan.fastclaw.feature.report.report

import android.graphics.Typeface
import android.view.View
import com.nhsd.basic.BaseViewModelFragment
import com.nhsd.basic.ext.dp
import com.nhsd.basic.ext.setOnSingleClickListener
import com.shiyuan.fastclaw.App.Companion.instance
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.core.network.apis.entity.ResumeData
import com.shiyuan.fastclaw.databinding.FragmentHomeReportBinding
import com.shiyuan.fastclaw.feature.mine.InterviewReportActivity
import com.shiyuan.fastclaw.feature.report.ResumeListDialog
import com.shiyuan.fastclaw.feature.report.UploadResumeDialog
import com.shiyuan.fastclaw.feature.report.genner.GenerateReportActivity
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
        mRealVM.resumeData.observe(this) {
            it?.let {
                if(it.hasLatestResume){
                    //显示简历
                    ResumeListDialog.show( manager = this.parentFragmentManager,it,{
                        uploadResume()
                    },{
                        GenerateReportActivity.start(requireActivity(),it.apply {
                            this.isJd = (mRealVM.selectIndex.value == 0)
                        })
                    })
                }else{
                    uploadResume()
                }
            }
        }
    }

    fun  uploadResume(){
        //上传简历
        UploadResumeDialog.show(manager = this.parentFragmentManager,false,{ fileInfo ->
            if (fileInfo != null) {
                mRealVM.submitResume(fileInfo.uri) {
                    GenerateReportActivity.start(
                        requireActivity(),
                        ResumeData(
                            resumeFileUrl = it,
                            isJd = (mRealVM.selectIndex.value == 0),
                            resumeFileName = fileInfo.fileName,
                            resumeFileType = fileInfo.mimeType,
                        ))
                }
            }
        })
    }

    fun setView(index: Int) {
        if(index == 0){
            binding.llTab1.background =  resources.getDrawable(R.drawable.bg_0099ff_r100)
            binding.tvTargetPosition.setTextColor(resources.getColor(R.color.white))
            binding.tvTargetPosition.setTypeface(binding.tvNoTargetPosition.typeface, Typeface.BOLD)
            binding.ivTargetPosition.visibility = View.VISIBLE
            binding.ivItem1.setImageResource(R.drawable.ic_background_check)
            binding.tvItem11.text = resources.getString(R.string.company_background_check)
            binding.tvItem12.text = resources.getString(R.string.report_home_tip3)

            binding.ivAvatar.setImageResource(R.drawable.ic_avatar5)
            binding.tvReportHomeName.text = resources.getString(R.string.report_home_name4)
            binding.tvReportHomeTip.text = resources.getString(R.string.report_home_name4_tip)
            binding.tvReportHomeTip1.text = resources.getString(R.string.report_home_name4_tip1)

            binding.ivAvatar2.setImageResource(R.drawable.ic_avatar6)
            binding.tvReportHomeName1.text = resources.getString(R.string.report_home_name5)
            binding.tvReportHomeTip2.text = resources.getString(R.string.report_home_name5_tip)
            binding.tvReportHomeTip3.text = resources.getString(R.string.report_home_name5_tip1)


            binding.ivAvatar3.setImageResource(R.drawable.ic_avatar7)
            binding.tvReportHomeName2.text = resources.getString(R.string.report_home_name6)
            binding.tvReportHomeTip4.text = resources.getString(R.string.report_home_name6_tip)
            binding.tvReportHomeTip5.text = resources.getString(R.string.report_home_name6_tip1)

            binding.tvMyd.text= resources.getString(R.string.string_98)
            binding.tvCustomerReviews.text= resources.getString(R.string.Customer_reviews)

            binding.rlHave.visibility = View.VISIBLE
            binding.rlNo.visibility = View.GONE

            binding.llTab2.background =  null
            binding.tvNoTargetPosition.setTextColor(resources.getColor(R.color.color_999999))
            binding.tvNoTargetPosition.typeface = Typeface.DEFAULT
            binding.ivNoTargetPosition.visibility = View.GONE
            binding.ivItem2.setImageResource(R.drawable.ic_job)
            binding.tvItem21.text = resources.getString(R.string.job_matching_analysis)
            binding.tvItem22.text = resources.getString(R.string.job_home_tip1)


            binding.llJd.visibility = View.VISIBLE
            binding.vLine.visibility = View.VISIBLE

            binding.tvStep.text = resources.getString(R.string.report_home_tip4_2)
        }else{
            binding.llTab1.background =  null
            binding.tvTargetPosition.setTextColor(resources.getColor(R.color.color_999999))
            binding.tvTargetPosition.typeface = Typeface.DEFAULT
            binding.ivTargetPosition.visibility = View.GONE
            binding.ivItem1.setImageResource(R.drawable.ic_skill)
            binding.tvItem11.text = resources.getString(R.string.skill_Interview)
            binding.tvItem12.text = resources.getString(R.string.skill_home_tip1)

            binding.ivAvatar.setImageResource(R.drawable.ic_avatar2)
            binding.tvReportHomeName.text = resources.getString(R.string.report_home_name1)
            binding.tvReportHomeTip.text = resources.getString(R.string.report_home_name1_tip)
            binding.tvReportHomeTip1.text = resources.getString(R.string.report_home_name1_tip1)

            binding.ivAvatar2.setImageResource(R.drawable.ic_avatar3)
            binding.tvReportHomeName1.text = resources.getString(R.string.report_home_name2)
            binding.tvReportHomeTip2.text = resources.getString(R.string.report_home_name2_tip)
            binding.tvReportHomeTip3.text = resources.getString(R.string.report_home_name2_tip1)


            binding.ivAvatar3.setImageResource(R.drawable.ic_avatar4)
            binding.tvReportHomeName2.text = resources.getString(R.string.report_home_name3)
            binding.tvReportHomeTip4.text = resources.getString(R.string.report_home_name3_tip)
            binding.tvReportHomeTip5.text = resources.getString(R.string.report_home_name3_tip1)

            binding.tvMyd.text= resources.getString(R.string.string_93)
            binding.tvCustomerReviews.text= resources.getString(R.string.Customer_reviews2)

            binding.rlHave.visibility = View.GONE
            binding.rlNo.visibility = View.VISIBLE

            binding.llTab2.background =  resources.getDrawable(R.drawable.bg_0099ff_r100)
            binding.tvNoTargetPosition.setTextColor(resources.getColor(R.color.white))
            binding.tvNoTargetPosition.setTypeface(binding.tvNoTargetPosition.typeface, Typeface.BOLD)
            binding.ivNoTargetPosition.visibility = View.VISIBLE
            binding.ivItem2.setImageResource(R.drawable.ic_personal)
            binding.tvItem21.text = resources.getString(R.string.personal_weaknesses)
            binding.tvItem22.text = resources.getString(R.string.personal_home_tip1)

            binding.llJd.visibility = View.GONE
            binding.vLine.visibility = View.GONE

            binding.tvStep.text = resources.getString(R.string.report_home_tip4)
        }
    }
    private fun initView() {
        binding.bannerView.apply {
            setIndicatorGravity(com.zhpan.bannerview.constants.IndicatorGravity.START)
            setIndicatorSliderColor(
                resources.getColor(R.color.color_F6F6F7),
                resources.getColor(R.color.color_0099FF)
            )
            .setIndicatorMargin(16.dp, 0.dp, 0.dp, 16.dp)
            setIndicatorStyle(IndicatorStyle.CIRCLE)
            setIndicatorSliderWidth(6.dp, 6.dp)
            setIndicatorHeight(6.dp)
            setScrollDuration(550)
            setInterval(3000)
            setLifecycleRegistry(lifecycle)
        }

        mRealVM.bannerList.get()?.let { imageList ->
            val adapter = ImageBannerAdapter()
            binding.bannerView.adapter = adapter
            binding.bannerView.create(imageList)
        }
        binding.llViewReport.setOnSingleClickListener {
            if (!AppNavigator.checkLoginIntercepted(true)) {
                //跳转
                InterviewReportActivity.start(requireActivity())
            }
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
            R.id.rl_generate -> {
                if (!AppNavigator.checkLoginIntercepted(true)) {
                    //跳转
                    mRealVM.queryLatestResume()
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
