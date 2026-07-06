package com.shiyuan.fastclaw.feature.report

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.view.View
import android.widget.Toast
import androidx.fragment.app.FragmentManager
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.databinding.DialogUploadResumeBinding
import com.nhsd.basic.BaseBottomPushVMDialogFragment
import com.nhsd.basic.ext.getResString
import com.nhsd.basic.ext.setOnSingleClickListener
import com.nhsd.basic.ext.showToast
import com.shiyuan.fastclaw.base.BaseBottomDialogVmFragment

/**
 * 上传简历dialog
 * 新增：通过 tvTrue 选择本地文件（PDF/Word/图片）并回调
 */
class UploadResumeDialog : BaseBottomDialogVmFragment<DialogUploadResumeBinding, ReportViewModel>() {

    companion object {
        const val TAG = "UploadResumeDialog"
        var onFileSelected: ((SelectedFileInfo?) -> Unit)? = null

        fun show(
            manager: FragmentManager,
            canSelectImage: Boolean = true,
            fileSelectedListener: ((SelectedFileInfo?) -> Unit)? = null
        ): UploadResumeDialog {
            val dialog = UploadResumeDialog()
            dialog.canSelectImage = canSelectImage
            onFileSelected = fileSelectedListener
            dialog.show(manager)
            return dialog
        }
    }

    private lateinit var fileChooser: FileChooser
    private var canSelectImage = true

    override fun onViewModelCreated() {
        super.onViewModelCreated()
        initView()
        dataUpdate()
        initFileChooser()
    }

    private fun initView() {
        binding.tvUploadTypeTip.text = if (canSelectImage) {
            R.string.upload_type_tip.getResString()
        } else {
            R.string.upload_type_tip_without_image.getResString()
        }
        binding.ivClose.setOnSingleClickListener { dismiss() }
        binding.llWx.setOnSingleClickListener { performWxUpload() }
        binding.llFolder.setOnSingleClickListener {  fileChooser.openFileChooser()}
    }

    private fun initFileChooser() {
        fileChooser = FileChooser(
            fragment = this,
            maxSizeBytes = 10 * 1024 * 1024, // 10MB
            canSelectImage = canSelectImage,
            onResult = { fileInfo ->
                if (fileInfo != null) {
                    onFileSelected?.invoke(fileInfo)
                  // "已选择: ${fileInfo.fileName}".showToast()
                    dismiss()
                } else {
                    // 这里只处理非大小超限的错误（例如无法读取文件）
                    "文件选择失败".showToast()
                }
            },
            onSizeExceeded = {
                "文件大小不能超过10MB".showToast()
            }
        )
    }

    private fun performWxUpload() {

        mRealVM.getScheme()
      //  onWxSelected?.invoke()
       // R.string.wx_upload_developing.getResString().showToast()
    }

    private fun dataUpdate() {
        mRealVM.schemeData.observe(this){
            openInWeChat(it.scheme ?:"")
         /*   //val intent = Intent(Intent.ACTION_VIEW, Uri.parse("weixin://dl/businessMiniProgram/?appid=" + "wx1234567890abcdef" + "&path=" + "pages/wx/resumeUpload/index?code=AUTH_0b4be6459e5441728acd845a611bdda4"))
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(it.scheme))
            startActivity(intent)*/
        }
    }

    fun openInWeChat(urlScheme: String): Boolean {
        // 确保传入的是 weixin:// 开头的小程序专用链接
        if (!urlScheme.startsWith("weixin://")) {
            "仅支持 weixin:// 格式的链接".showToast()
            return false
        }
        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(urlScheme)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                setPackage("com.tencent.mm")
            }
            startActivity(intent)
            true
        } catch (e: ActivityNotFoundException) {
            "请先安装微信".showToast()
            false
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    override fun <Data> onClick(view: View, data: Data?) {
        super.onClick(view, data)
    }
}
