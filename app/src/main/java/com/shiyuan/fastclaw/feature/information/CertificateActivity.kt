package com.shiyuan.fastclaw.feature.information

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.google.common.base.Joiner.on
import com.gyf.immersionbar.ImmersionBar
import com.nhsd.basic.BaseViewModelActivity
import com.nhsd.basic.bus.observeEvents
import com.nhsd.basic.ext.isActive
import com.nhsd.basic.ext.setOnSingleClickListener
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.core.Events
import com.shiyuan.fastclaw.core.User
import com.shiyuan.fastclaw.core.network.apis.response.AttributeArrayQueryData
import com.shiyuan.fastclaw.core.network.apis.response.CommonEnumItem
import com.shiyuan.fastclaw.core.network.apis.response.CommonItems
import com.shiyuan.fastclaw.core.network.apis.response.FieldItems
import com.shiyuan.fastclaw.databinding.ActivityAwardBinding
import com.shiyuan.fastclaw.databinding.ActivityCertificateBinding
import com.shiyuan.fastclaw.databinding.ActivitySkillBinding
import com.shiyuan.fastclaw.feature.information.viewModel.InformationViewModel
import com.shiyuan.fastclaw.router.AppNavigator
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class CertificateActivity : BaseViewModelActivity<ActivityCertificateBinding, InformationViewModel>() {
    companion object {
        private const val EXTRA_IS_EDIT = "extra_is_edit"
        private const val EXTRA_ATTRIBUTE_DATA = "extra_attribute_data"

        fun start(activity: Activity?, isEdit: Boolean = true, attributeData: AttributeArrayQueryData? = null) {
            activity ?: return
            val intent = Intent(activity, CertificateActivity::class.java).apply {
                putExtra(EXTRA_IS_EDIT, isEdit)
                attributeData?.let { putExtra(EXTRA_ATTRIBUTE_DATA, it) }
            }
            activity.startActivity(intent)
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initView()
        observeEvents()
    }

    private fun initView() {
        val isEdit = intent?.getBooleanExtra(EXTRA_IS_EDIT, true) ?: true
        if (isEdit) {
            binding.tvTitle.text = getString(R.string.edit_certificate)
            binding.viewActionButtons.setSecondaryText(getString(R.string.del))
            binding.viewActionButtons.setPrimaryText(getString(R.string.complete))
            binding.viewActionButtons.setOnSecondaryClickListener {
                finish()
            }
            binding.viewActionButtons.setOnPrimaryClickListener {
                finish()
            }
        } else {
            binding.tvTitle.text = getString(R.string.add_certificate)
            binding.viewActionButtons.setPrimaryText(getString(R.string.save))
            binding.viewActionButtons.setOnPrimaryClickListener {
                finish()
            }
        }
        binding.vForm1.setOnSingleClickListener {
            PublicFreeFillInActivity.start(this,isEdit, CommonItems(fieldName = "证书名称"))
        }

        binding.vForm2.setOnSingleClickListener {
            PublicFreeFillInActivity.start(this,isEdit, CommonItems(fieldName = "颁发机构"))
        }
        binding.viewActionButtons.setSecondaryVisible(isEdit)
    }

    private fun observeEvents() {
        observeEvents {
            on<Events.CommonItemsEvent> { event ->
                binding.vForm1.setData(event.commonItems)
            }
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
