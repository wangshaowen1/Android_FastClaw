package com.shiyuan.fastclaw.feature.information

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.gyf.immersionbar.ImmersionBar
import com.nhsd.basic.BaseViewModelActivity
import com.nhsd.basic.bus.observeEvents
import com.nhsd.basic.ext.setOnSingleClickListener
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.core.Events
import com.shiyuan.fastclaw.core.network.apis.response.CommonEnumItem
import com.shiyuan.fastclaw.core.network.apis.response.CommonItems
import com.shiyuan.fastclaw.databinding.ActivityEducationalBinding
import com.shiyuan.fastclaw.databinding.ActivitySkillBinding
import com.shiyuan.fastclaw.databinding.ActivityWorkBinding
import com.shiyuan.fastclaw.feature.information.dialog.SingleSelectDialog
import com.shiyuan.fastclaw.feature.information.viewModel.InformationViewModel

class WorkActivity : BaseViewModelActivity<ActivityWorkBinding, InformationViewModel>() {
    companion object {
        private const val EXTRA_IS_EDIT = "extra_is_edit"

        fun start(activity: Activity?, isEdit: Boolean = true) {
            activity ?: return
            val intent = Intent(activity, WorkActivity::class.java).apply {
                putExtra(EXTRA_IS_EDIT, isEdit)
            }
            activity.startActivity(intent)
        }
    }

    private val fakeProficiencyList = listOf(
        CommonEnumItem(code = "expert", name = "精通"),
        CommonEnumItem(code = "skilled", name = "熟练"),
        CommonEnumItem(code = "beginner_1", name = "入门1"),
        CommonEnumItem(code = "beginner_2", name = "入门2"),
        CommonEnumItem(code = "beginner_3", name = "入门3"),
        CommonEnumItem(code = "beginner_4", name = "入门4")
    )

    private var selectedProficiencyCode: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initView()
        observeEvents()
    }

    private fun initView() {
        val isEdit = intent?.getBooleanExtra(EXTRA_IS_EDIT, true) ?: true
        if (isEdit) {
            binding.tvTitle.text = getString(R.string.edit_working)
            binding.viewActionButtons.setSecondaryText(getString(R.string.del))
            binding.viewActionButtons.setPrimaryText(getString(R.string.complete))
            binding.viewActionButtons.setOnSecondaryClickListener {
                finish()
            }
            binding.viewActionButtons.setOnPrimaryClickListener {
                finish()
            }
        } else {
            binding.tvTitle.text = getString(R.string.add_working)
            binding.viewActionButtons.setPrimaryText(getString(R.string.save))
            binding.viewActionButtons.setOnPrimaryClickListener {
                finish()
            }
        }
        binding.vForm1.setOnSingleClickListener {
            PublicFreeFillInActivity.start(this,isEdit, CommonItems(fieldName = "证书名称"))
        }

        binding.vForm2.setOnSingleClickListener {
            SingleSelectDialog.show(
                manager = supportFragmentManager,
                awardLevels = fakeProficiencyList,
                selectedItem = fakeProficiencyList.firstOrNull { it.code == selectedProficiencyCode },
            ) { selectedItem ->
                selectedProficiencyCode = selectedItem.code
                binding.vForm2.setData(CommonItems(fieldName = "熟练度", fieldValue = selectedItem.name))
            }
        }
        binding.vForm9.setOnSingleClickListener {
            PublicDescriptionActivity.start(this,isEdit, CommonItems(fieldName = "项目描述"))
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
