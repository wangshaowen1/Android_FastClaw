package com.shiyuan.fastclaw.feature.information.dialog

import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentManager
import com.flqy.datepicker.NumberPicker
import com.nhsd.basic.BaseBottomPushDialogFragment
import com.nhsd.basic.ext.setOnSingleClickListener
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.databinding.DialogSalaryRangePickerBinding

class SalaryRangePickerDialog : BaseBottomPushDialogFragment<DialogSalaryRangePickerBinding>() {

    companion object {
        private const val SELECTED_TEXT_SIZE_SP = 16f
        private const val UNSELECTED_TEXT_SIZE_SP = 14f
        private const val WHEEL_ITEM_COUNT = 5
        const val TAG = "SalaryRangePickerDialog"

        private val minSalaryList = listOf(
            "面议", "1K", "2K", "3K", "4K", "5K", "6K", "7K", "8K", "9K", "10K", "11K", "12K"
        )
        private val allSalaryList = listOf(
            "1K", "2K", "3K", "4K", "5K", "6K", "7K", "8K", "9K", "10K", "11K", "12K"
        )
        private var selectedMinSalary: String? = null
        private var selectedMaxSalary: String? = null
        private var dialogTitle: String? = null
        private var onConfirm: ((minSalary: String, maxSalary: String?) -> Unit)? = null

        fun show(
            manager: FragmentManager,
            minSalary: String? = "10K",
            maxSalary: String? = "12K",
            title: String? = null,
            onConfirm: ((minSalary: String, maxSalary: String?) -> Unit)? = null,
        ): SalaryRangePickerDialog {
            selectedMinSalary = minSalary
            selectedMaxSalary = maxSalary
            dialogTitle = title
            this.onConfirm = onConfirm
            return SalaryRangePickerDialog().apply {
                arguments = Bundle()
                show(manager)
            }
        }
    }

    override fun onPrepareView() {
        super.onPrepareView()
        initMinSalaryPicker()
        initClick()
        applyDefaultSelection()
        binding.title.text = dialogTitle ?: "薪资范围"
    }

    private fun initMinSalaryPicker() {
        val displayValues = minSalaryList.toTypedArray()
        binding.wheelMinSalary.applyPickerStyle()
        binding.wheelMinSalary.minValue = 0
        binding.wheelMinSalary.maxValue = displayValues.lastIndex
        binding.wheelMinSalary.displayedValues = displayValues
        binding.wheelMinSalary.value = minSalaryList.indexOf(selectedMinSalary).takeIf { it >= 0 } ?: 0
        binding.wheelMinSalary.setOnValueChangedListener { _, _, _ ->
            updateMaxSalaryPicker(resetSelection = true)
        }
    }

    private fun initClick() {
        binding.cancel.setOnSingleClickListener {
            dismissAllowingStateLoss()
        }
        binding.confirm.setOnSingleClickListener {
            val minSalary = minSalaryList[binding.wheelMinSalary.value]
            val maxSalary = getCurrentMaxSalaryList().getOrNull(binding.wheelMaxSalary.value)
                ?.takeIf { binding.wheelMaxSalary.visibility == View.VISIBLE }
            onConfirm?.invoke(minSalary, maxSalary)
            dismissAllowingStateLoss()
        }
    }

    private fun applyDefaultSelection() {
        updateMaxSalaryPicker(resetSelection = false)
    }

    private fun updateMaxSalaryPicker(resetSelection: Boolean) {
        val maxSalaryList = getCurrentMaxSalaryList()
        if (maxSalaryList.isEmpty()) {
            binding.wheelMaxSalary.visibility = View.INVISIBLE
            return
        }
        binding.wheelMaxSalary.visibility = View.VISIBLE
        val displayValues = maxSalaryList.toTypedArray()
        binding.wheelMaxSalary.applyPickerStyle()
        binding.wheelMaxSalary.displayedValues = null
        binding.wheelMaxSalary.minValue = 0
        binding.wheelMaxSalary.maxValue = displayValues.lastIndex
        binding.wheelMaxSalary.displayedValues = displayValues
        binding.wheelMaxSalary.value = resolveMaxSalaryIndex(maxSalaryList, resetSelection)
    }

    private fun resolveMaxSalaryIndex(maxSalaryList: List<String>, resetSelection: Boolean): Int {
        if (resetSelection) {
            return 0
        }
        val selectedIndex = maxSalaryList.indexOf(selectedMaxSalary)
        return if (selectedIndex >= 0) {
            selectedIndex
        } else {
            maxSalaryList.lastIndex
        }
    }

    private fun getCurrentMaxSalaryList(): List<String> {
        val minSalary = minSalaryList.getOrNull(binding.wheelMinSalary.value) ?: return allSalaryList
        if (minSalary == "面议") {
            return emptyList()
        }
        val minSalaryIndex = allSalaryList.indexOf(minSalary)
        return if (minSalaryIndex >= 0) {
            allSalaryList.subList(minSalaryIndex, allSalaryList.size)
        } else {
            allSalaryList
        }
    }

    private fun NumberPicker.applyPickerStyle() {
        setSelectedTextSizeSp(SELECTED_TEXT_SIZE_SP)
        setSelectedTextBold(true)
        setUnselectedTextSizeSp(UNSELECTED_TEXT_SIZE_SP)
        setSelectedTextColor(
            ContextCompat.getColor(requireContext(), R.color.color_90_000000)
        )
        setUnselectedTextColor(
            ContextCompat.getColor(requireContext(), R.color.color_55_3c3c43)
        )
        setSelectorWheelItemCount(WHEEL_ITEM_COUNT)
        descendantFocusability = NumberPicker.FOCUS_BLOCK_DESCENDANTS
        setWrapSelectorWheel(false)
    }
}
