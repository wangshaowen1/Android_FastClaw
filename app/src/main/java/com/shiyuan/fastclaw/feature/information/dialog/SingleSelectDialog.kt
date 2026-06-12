package com.shiyuan.fastclaw.feature.information.dialog

import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentManager
import com.flqy.datepicker.NumberPicker
import com.nhsd.basic.BaseBottomPushDialogFragment
import com.nhsd.basic.ext.setOnSingleClickListener
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.core.network.apis.response.CommonEnumItem
import com.shiyuan.fastclaw.databinding.DialogSingleSelectBinding

class SingleSelectDialog : BaseBottomPushDialogFragment<DialogSingleSelectBinding>() {

    companion object {
        private const val SELECTED_TEXT_SIZE_SP = 16f
        private const val UNSELECTED_TEXT_SIZE_SP = 14f
        private const val WHEEL_ITEM_COUNT = 5
        const val TAG = "AwardLevelDialog"
        private const val ARG_TITLE = "title"

        private var awardLevels: List<CommonEnumItem> = emptyList()
        private var selectedItem: CommonEnumItem? = null
        private var onConfirm: ((CommonEnumItem) -> Unit)? = null

        fun show(
            manager: FragmentManager,
            title: String? = null,
            awardLevels: List<CommonEnumItem>,
            selectedItem: CommonEnumItem? = null,
            onConfirm: ((CommonEnumItem) -> Unit)? = null,
        ): SingleSelectDialog {
            this.awardLevels = awardLevels
            this.selectedItem = selectedItem
            this.onConfirm = onConfirm
            return SingleSelectDialog().apply {
                arguments = Bundle().apply {
                    putString(ARG_TITLE, title)
                }
                show(manager)
            }
        }
    }

    override fun onPrepareView() {
        super.onPrepareView()
        binding.tvTitle.text = arguments?.getString(ARG_TITLE)
        initPicker()
        initClick()
    }

    private fun initPicker() {
        if (awardLevels.isEmpty()) {
            dismissAllowingStateLoss()
            return
        }
        val displayValues = awardLevels.map { it.name.orEmpty() }.toTypedArray()
        binding.awardLevelPicker.setSelectedTextSizeSp(SELECTED_TEXT_SIZE_SP)
        binding.awardLevelPicker.setSelectedTextBold(true)
        binding.awardLevelPicker.setUnselectedTextSizeSp(UNSELECTED_TEXT_SIZE_SP)
        binding.awardLevelPicker.setSelectedTextColor(
            ContextCompat.getColor(requireContext(), R.color.color_90_000000)
        )
        binding.awardLevelPicker.setUnselectedTextColor(
            ContextCompat.getColor(requireContext(), R.color.color_55_3c3c43)
        )
        binding.awardLevelPicker.setMinValue(0)
        binding.awardLevelPicker.setMaxValue(displayValues.lastIndex)
        binding.awardLevelPicker.setDisplayedValues(displayValues)
        binding.awardLevelPicker.setSelectorWheelItemCount(5)
        binding.awardLevelPicker.descendantFocusability = NumberPicker.FOCUS_BLOCK_DESCENDANTS
        binding.awardLevelPicker.setWrapSelectorWheel(false)
        binding.awardLevelPicker.setValue(
            awardLevels.indexOfFirst { it.code == selectedItem?.code }.takeIf { it >= 0 } ?: 0
        )
    }

    private fun initClick() {
        binding.tvCancel.setOnSingleClickListener {
            dismissAllowingStateLoss()
        }
        binding.tvConfirm.setOnSingleClickListener {
            awardLevels.getOrNull(binding.awardLevelPicker.value)?.let {
                onConfirm?.invoke(it)
            }
            dismissAllowingStateLoss()
        }
    }
}
