package com.shiyuan.fastclaw.feature.report
import android.view.View
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.databinding.DialogUploadResumeBinding
import com.nhsd.basic.BaseBottomPushVMDialogFragment
import com.nhsd.basic.ext.getResString
import com.nhsd.basic.ext.setOnSingleClickListener
import com.nhsd.basic.ext.showToast



/**
 * *****************************************
 * 项目名称: FastFilm
 * @Author: SZYMN
 * @Introduce:上传简历弹窗
 * @Data: 2026/2/10/周二  17:09
 * @Version: 1.0
 * *****************************************
 */
class UploadResumeDialog : BaseBottomPushVMDialogFragment<DialogUploadResumeBinding, ReportViewModel>(){

    companion object {
        const val TAG = "UploadResumeDialog"

        fun show(manager: androidx.fragment.app.FragmentManager): UploadResumeDialog {
            val dialog = UploadResumeDialog()
            dialog.show(manager)
            return dialog
        }
    }


    override fun onViewModelCreated() {
        super.onViewModelCreated()
        initView()
        dataUpdate()
    }



    private fun initView() {
        binding.ivClose.setOnSingleClickListener {
            dismiss()
        }

        binding.llWx.setOnSingleClickListener {
            performWxUpload()
        }

        binding.llFolder.setOnSingleClickListener {

        }
    }




    /**
     * 微信上传逻辑（需要微信 SDK 集成）
     */
    private fun performWxUpload() {
        R.string.wx_upload_developing.getResString().showToast()
    }

    private fun dataUpdate() {}

    override fun <Data> onClick(view: View, data: Data?) {
        super.onClick(view, data)
    }
}
