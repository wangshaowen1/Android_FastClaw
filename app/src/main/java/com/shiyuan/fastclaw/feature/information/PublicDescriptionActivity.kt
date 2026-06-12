package com.shiyuan.fastclaw.feature.information

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.core.widget.doAfterTextChanged
import com.gyf.immersionbar.ImmersionBar
import com.nhsd.basic.BaseViewModelActivity
import com.nhsd.basic.bus.FlowEventBus
import com.nhsd.basic.ext.isNull
import com.nhsd.basic.ext.setOnSingleClickListener
import com.nhsd.basic.ext.showToast
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.core.Events
import com.shiyuan.fastclaw.core.Events.TrackCreatedEvent
import com.shiyuan.fastclaw.core.network.apis.response.AttributeFieldItem
import com.shiyuan.fastclaw.core.network.apis.response.CommonItems
import com.shiyuan.fastclaw.databinding.ActivityPublicDescriptionBinding
import com.shiyuan.fastclaw.databinding.ActivityPublicFreeFillInBinding
import com.shiyuan.fastclaw.feature.information.viewModel.InformationViewModel


class PublicDescriptionActivity : BaseViewModelActivity<ActivityPublicDescriptionBinding, InformationViewModel>() {
    companion object {
        private const val EXTRA_IS_EDIT = "extra_is_edit"
        private const val EXTRA_DATA = "extra_data"

        private val MAX_LENGTH = 3000

        fun start(activity: Activity?, isEdit: Boolean = true, data: AttributeFieldItem) {
            activity ?: return
            Intent(activity, PublicDescriptionActivity::class.java).apply {
                putExtra(EXTRA_IS_EDIT, isEdit)
                data?.let { putExtra(EXTRA_DATA, it) }
            }.let(activity::startActivity)
        }
    }
    private val isEdit: Boolean by lazy { intent.getBooleanExtra(EXTRA_IS_EDIT, true) }

    private  var data: AttributeFieldItem = AttributeFieldItem()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        data = intent.getParcelableExtra(EXTRA_DATA) ?: AttributeFieldItem()
        initView()
    }

    private fun initView() {
        // 确保视图完成布局后再执行
        binding.et.post {
            binding.et.requestFocus()
            // 将光标移动到文本末尾（若文本为空则停在起始位置）
            binding.et.setSelection(binding.et.text?.length ?:0)
            // 弹出软键盘
            val imm = getSystemService(InputMethodManager::class.java)
            imm.showSoftInput( binding.et, InputMethodManager.SHOW_IMPLICIT)
        }
        binding.tvTitle.text = data.fieldName.toString()
        binding.tvTip.text = data.fieldName.toString()
        binding.et.setText(data.displayValue ?: "")
        binding.tvWordCount.text = if(binding.et.text?.length.isNull()) "0" else  binding.et.text?.length.toString()
        binding.et.doAfterTextChanged { editable ->
            val currentLength = editable?.length ?: 0
            binding.tvWordCount.text = "$currentLength"
            // 达到2000字时提示
            if (currentLength == MAX_LENGTH) {
                "已达最大输入字数 $MAX_LENGTH 字".showToast()
            }
            data.displayValue= editable.toString()
            mapOf(
                "value" to editable.toString() ,
                "displayText" to editable.toString()
            )
        }
        binding.rlBack.setOnSingleClickListener {
            finish()
        }
        binding.tvQr.setOnSingleClickListener {
            if(binding.et.text.isNullOrEmpty()){
                "请输入".showToast()
                return@setOnSingleClickListener
            }
            FlowEventBus.postSticky(Events.CommonAttributeFieldItemEvent(data));
            finish()
        }
    }

    override fun setTranslucentStatus() {
        ImmersionBar.with(this)
            .statusBarDarkFont(true)
            .navigationBarColor(R.color.white)
            .transparentStatusBar()
            .init()
    }

}