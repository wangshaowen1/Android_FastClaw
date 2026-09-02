package com.shiyuan.fastclaw.feature.report.generation

import android.app.Dialog
import android.os.Bundle
import androidx.annotation.NonNull
import com.nhsd.basic.BaseDialogFragment
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.databinding.DialogLoadingAnalysisBinding

class AnalysisLoadingDialog : BaseDialogFragment<DialogLoadingAnalysisBinding>() {

    @NonNull
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return Dialog(requireActivity(), R.style.LoadingDialog).apply {
            setCanceledOnTouchOutside(false)
        }
    }
}