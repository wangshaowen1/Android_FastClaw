package com.shiyuan.fastclaw.feature.report

import android.view.View
import com.nhsd.basic.BaseViewModelFragment
import com.nhsd.basic.ext.getResString
import com.nhsd.basic.ext.showToast
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.core.network.apis.entity.ResumeData
import com.shiyuan.fastclaw.databinding.FragmentHomeTreasuryBinding
import com.shiyuan.fastclaw.feature.report.genner.GenerateReportActivity
import com.shiyuan.fastclaw.feature.report.report.HomeReportViewModel
import com.shiyuan.fastclaw.router.AppNavigator

class HomeTreasuryFragment : BaseViewModelFragment<FragmentHomeTreasuryBinding, HomeReportViewModel>() {

    override fun onPrepareView() {
        super.onPrepareView()
        dataMonitoring()
    }

    private fun dataMonitoring() {
        mRealVM.resumeData.observe(viewLifecycleOwner) { resumeData ->
            resumeData?.let {
                if (it.hasLatestResume) {
                    ResumeListDialog.show(
                        parentFragmentManager,
                        it,
                        {
                            uploadResume()
                        },
                        {
                            GenerateReportActivity.start(
                                requireActivity(),
                                it.apply {
                                    isJd = mRealVM.selectIndex.value == REPORT_TYPE_COMPANY
                                }
                            )
                        }
                    )
                } else {
                    uploadResume()
                }
            }
        }
    }

    private fun uploadResume() {
        UploadResumeDialog.show(manager = parentFragmentManager) { fileInfo ->
            if (fileInfo != null) {
                mRealVM.submitResume(fileInfo.uri) { resumeFileUrl ->
                    GenerateReportActivity.start(
                        requireActivity(),
                        ResumeData(
                            resumeFileUrl = resumeFileUrl,
                            isJd = mRealVM.selectIndex.value == REPORT_TYPE_COMPANY,
                            resumeFileName = fileInfo.fileName,
                            resumeFileType = fileInfo.mimeType,
                        )
                    )
                }
            }
        }
    }

    private fun startGenerate(isCompanyReport: Boolean) {
        if (AppNavigator.checkLoginIntercepted(true)) return
        mRealVM.selectIndex.value = if (isCompanyReport) REPORT_TYPE_COMPANY else REPORT_TYPE_INTERVIEW
        mRealVM.queryLatestResume()
    }

    override fun <Data> onClick(view: View, data: Data?) {
        super.onClick(view, data)
        when (view.id) {
            R.id.card_company,
            R.id.ll_company_action -> startGenerate(true)

            R.id.card_interview,
            R.id.ll_interview_action -> startGenerate(false)

            R.id.ll_resume_check_tool,
            R.id.ll_salary_tool -> R.string.home_treasury_coming_soon.getResString().showToast()
        }
    }

    private companion object {
        const val REPORT_TYPE_COMPANY = 0
        const val REPORT_TYPE_INTERVIEW = 1
    }
}
