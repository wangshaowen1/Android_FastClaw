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


/**
 * 通用长文本/简介填写页。
 * 适用于工作内容、项目描述、自我介绍等可输入 3000 字以内的字段。
 */
class PublicDescriptionActivity : BaseViewModelActivity<ActivityPublicDescriptionBinding, InformationViewModel>() {
    companion object {
        // 是否编辑模式参数 key
        private const val EXTRA_IS_EDIT = "extra_is_edit"
        // 字段数据参数 key
        private const val EXTRA_DATA = "extra_data"

        // 描述文本最大长度
        private val MAX_LENGTH = 3000

        /**
         * 启动通用描述填写页。
         *
         * @param activity 启动源 Activity。
         * @param isEdit 是否编辑模式。
         * @param data 字段数据。
         */
        fun start(activity: Activity?, isEdit: Boolean = true, data: AttributeFieldItem) {
            activity ?: return
            Intent(activity, PublicDescriptionActivity::class.java).apply {
                putExtra(EXTRA_IS_EDIT, isEdit)
                data?.let { putExtra(EXTRA_DATA, it) }
            }.let(activity::startActivity)
        }
    }
    // 是否编辑模式
    private val isEdit: Boolean by lazy { intent.getBooleanExtra(EXTRA_IS_EDIT, true) }

    // 字段数据缓存，提交时回传
    private  var data: AttributeFieldItem = AttributeFieldItem()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 读取 Parcelable 字段数据
        data = intent.getParcelableExtra(EXTRA_DATA) ?: AttributeFieldItem()
        initView()
    }

    private fun initView() {
        // 确保视图完成布局后再执行
        binding.et.post {
            // 自动获取焦点
            binding.et.requestFocus()
            // 将光标移动到文本末尾（若文本为空则停在起始位置）
            binding.et.setSelection(binding.et.text?.length ?:0)
            // 弹出软键盘
            val imm = getSystemService(InputMethodManager::class.java)
            imm.showSoftInput( binding.et, InputMethodManager.SHOW_IMPLICIT)
        }
        // 标题 / 提示 / 初值
        binding.tvTitle.text = data.fieldName.toString()
        binding.tvTip.text = data.fieldName.toString()
        binding.et.setText(data.displayValue ?: "")
        binding.tvWordCount.text = if(binding.et.text?.length.isNull()) "0" else  binding.et.text?.length.toString()
        // 文本变化：实时更新计数、达到上限提示、同步到 data
        binding.et.doAfterTextChanged { editable ->
            val currentLength = editable?.length ?: 0
            binding.tvWordCount.text = "$currentLength"
            // 达到上限时提示
            if (currentLength == MAX_LENGTH) {
                "已达最大输入字数 $MAX_LENGTH 字".showToast()
            }
            data.displayValue= editable.toString()
            data.fieldValue = mapOf(
                "value" to editable.toString() ,
                "displayText" to editable.toString()
            )
            // 冗余调用 mapOf，无副作用，保留原写法以避免回退用户代码
            mapOf(
                "value" to editable.toString() ,
                "displayText" to editable.toString()
            )
        }
        // 返回
        binding.rlBack.setOnSingleClickListener {
            finish()
        }
        // 确认按钮：空内容提示，否则通过事件总线回传
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
        // 设置沉浸式状态栏
        ImmersionBar.with(this)
            .statusBarDarkFont(true)
            .navigationBarColor(R.color.white)
            .transparentStatusBar()
            .init()
    }

}
