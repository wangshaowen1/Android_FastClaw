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
import com.nhsd.basic.ext.showToast
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.core.Events
import com.shiyuan.fastclaw.core.Events.TrackCreatedEvent
import com.shiyuan.fastclaw.core.User
import com.shiyuan.fastclaw.core.network.apis.response.AttributeArrayQueryData
import com.shiyuan.fastclaw.core.network.apis.response.AttributeFieldItem
import com.shiyuan.fastclaw.core.network.apis.response.CommonItems
import com.shiyuan.fastclaw.databinding.ActivityPersonaFreeFillInBinding
import com.shiyuan.fastclaw.databinding.ActivityPublicFreeFillInBinding
import com.shiyuan.fastclaw.feature.information.viewModel.InformationViewModel


/**
 * 通用自由填写页：单行文本输入。
 * 适用于字段值不受字典约束、可由用户自由输入的字段。
 */
class PersonaFreeFillInActivity : BaseViewModelActivity<ActivityPersonaFreeFillInBinding, InformationViewModel>() {
    companion object {
        // 是否编辑模式参数 key
        private const val EXTRA_IS_EDIT = "extra_is_edit"
        // 字段数据参数 key
        private const val EXTRA_DATA = "extra_data"
        private const val MIN_LENGTH = 1
        private const val MAX_LENGTH = 50

        private const val FIELD_NAME = "name"
        private const val TEXT = "TEXT"

        /**
         * 启动自由填写页。
         *
         * @param activity 启动源 Activity。
         * @param isEdit 是否编辑模式。
         * @param data 字段数据。
         */
        fun start(activity: Activity?, isEdit: Boolean = true, data: AttributeArrayQueryData) {
            activity ?: return
            Intent(activity, PersonaFreeFillInActivity::class.java).apply {
                putExtra(EXTRA_IS_EDIT, isEdit)
                data?.let { putExtra(EXTRA_DATA, it) }
            }.let(activity::startActivity)
        }
    }
    // 是否编辑模式
    private val isEdit: Boolean by lazy { intent.getBooleanExtra(EXTRA_IS_EDIT, true) }

    // 字段数据缓存，提交时回传
    private  var data: AttributeArrayQueryData = AttributeArrayQueryData()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 读取 Parcelable 字段数据
        data = intent.getParcelableExtra(EXTRA_DATA) ?: AttributeArrayQueryData()
        initView()
    }

    private fun initView() {
        // 确保视图完成布局后再执行
        binding.et.post {
            // 自动获取焦点
            binding.et.requestFocus()
            // 将光标移动到文本末尾（若文本为空则停在起始位置）
            binding.et.setSelection( binding.et.text.length)
            // 弹出软键盘
            val imm = getSystemService(InputMethodManager::class.java)
            imm.showSoftInput( binding.et, InputMethodManager.SHOW_IMPLICIT)
        }
        // 标题 / 提示 / 初值
        mRealVM.mToolbarVM.mTitle.set(data.attributeGroupName.toString())
        mRealVM.mToolbarVM.mMenuText.set(getString(R.string.confirm))
        binding.et.hint = data.attributeGroupName.toString()
        binding.et.setText(data.displayValue ?: "")
        binding.tvWordCount.text = "${binding.et.text?.length ?: 0}/$MAX_LENGTH"
      /*  // 有初始值时显示清空图标
        if (!data.displayValue.isNullOrEmpty())  binding.ivClear.visibility = View.VISIBLE else binding.ivClear.visibility = View.GONE*/
        // 文本变化：清空图标显隐 + 同步到 data
        binding.et.doAfterTextChanged { editable ->
            val showIcon = !editable.isNullOrEmpty()
          /*  if (showIcon)  binding.ivClear.visibility = View.VISIBLE else binding.ivClear.visibility = View.GONE*/
            binding.tvWordCount.text = "${editable?.length ?: 0}/$MAX_LENGTH"
          /*  data.displayValue = editable.toString()
            data.fieldValue =  mapOf(
                "value" to editable.toString() ,
                "displayText" to editable.toString()
            )*/
        }
     /*   // 清空输入
        binding.ivClear.setOnSingleClickListener {
            binding.et.text.clear()
        }*/
        // 确认按钮：空内容提示，否则通过事件总线回传
        mRealVM.mToolbarVM.menuAction = menuAction@{
            val inputLength = binding.et.text?.length ?: 0
            if (inputLength < MIN_LENGTH) {
                "至少输入1个字".showToast()
                return@menuAction
            }
            mRealVM.updateOnlineResumeAttribute(
                attributeGroupCode = data.attributeGroupCode ?: data.metaCode,
                inputMode = data.inputMode ?: TEXT,
                value =  binding.et.text.toString()
            ) {
                syncUserNameIfNeeded(binding.et.text.toString())
                FlowEventBus.postSticky(Events.CommonAttributeArrayQueryData())
                finish()
            }
        }

    }

    private fun syncUserNameIfNeeded(name: String) {
        val fieldCode = data.attributeGroupCode ?: data.metaCode
        if (fieldCode != FIELD_NAME) return
        User.get().storeNickname(name)
        User.get().notifyChange(User.NICK_NAME)
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
