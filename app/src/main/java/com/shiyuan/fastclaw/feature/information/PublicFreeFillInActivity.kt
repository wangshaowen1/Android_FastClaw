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
import com.nhsd.basic.ext.setOnSingleClickListener
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.core.Events
import com.shiyuan.fastclaw.core.Events.TrackCreatedEvent
import com.shiyuan.fastclaw.core.network.apis.response.CommonItems
import com.shiyuan.fastclaw.databinding.ActivityPublicFreeFillInBinding
import com.shiyuan.fastclaw.feature.information.viewModel.InformationViewModel


class PublicFreeFillInActivity : BaseViewModelActivity<ActivityPublicFreeFillInBinding, InformationViewModel>() {
    companion object {
        private const val EXTRA_IS_EDIT = "extra_is_edit"
        private const val EXTRA_DATA = "extra_data"

        fun start(activity: Activity?, isEdit: Boolean = true, data: CommonItems) {
            activity ?: return
            Intent(activity, PublicFreeFillInActivity::class.java).apply {
                putExtra(EXTRA_IS_EDIT, isEdit)
                data?.let { putExtra(EXTRA_DATA, it) }
            }.let(activity::startActivity)
        }
    }
    private val isEdit: Boolean by lazy { intent.getBooleanExtra(EXTRA_IS_EDIT, true) }

    private  var data: CommonItems = CommonItems()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        data = intent.getParcelableExtra(EXTRA_DATA)!!
        initView()
    }

    private fun initView() {
        // 确保视图完成布局后再执行
        binding.et.post {
            binding.et.requestFocus()
            // 将光标移动到文本末尾（若文本为空则停在起始位置）
            binding.et.setSelection( binding.et.text.length)
            // 弹出软键盘
            val imm = getSystemService(InputMethodManager::class.java)
            imm.showSoftInput( binding.et, InputMethodManager.SHOW_IMPLICIT)
        }
        binding.tvTitle.text = data.fieldName.toString()
        binding.et.hint = data.fieldName.toString()
        binding.et.setText(data.fieldValue ?: "")
        binding.et.doAfterTextChanged { editable ->
            val showIcon = !editable.isNullOrEmpty()
            if (showIcon)  binding.ivClear.visibility = View.VISIBLE else binding.ivClear.visibility = View.GONE
            data.fieldValue= editable.toString()
        }
        binding.ivClear.setOnSingleClickListener {
            binding.et.text.clear()
        }
        binding.tvQr.setOnSingleClickListener {
            FlowEventBus.postSticky(Events.CommonItemsEvent(data));
            finish()
        }
        binding.tvQx.setOnSingleClickListener {
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