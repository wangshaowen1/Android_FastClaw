package com.shiyuan.fastclaw.feature.information.dialog

import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentManager
import com.flqy.datepicker.NumberPicker
import com.nhsd.basic.BaseBottomPushDialogFragment
import com.nhsd.basic.ext.setOnSingleClickListener
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.databinding.DialogBirthYearMonthPickerBinding
import java.util.Calendar

class BirthYearMonthPickerDialog : BaseBottomPushDialogFragment<DialogBirthYearMonthPickerBinding>() {

    companion object {
        private const val SELECTED_TEXT_SIZE_SP = 16f
        private const val UNSELECTED_TEXT_SIZE_SP = 14f
        private const val WHEEL_ITEM_COUNT = 5
        private const val DEFAULT_MIN_YEAR = 1900
        const val TAG = "BirthYearMonthPickerDialog"

        private var selectedYear: Int? = null
        private var selectedMonth: Int? = null
        private var minYear: Int = DEFAULT_MIN_YEAR
        private var maxYear: Int = Calendar.getInstance().get(Calendar.YEAR)
        private var onConfirm: ((year: Int, month: Int) -> Unit)? = null

        fun show(
            manager: FragmentManager,
            selectedYear: Int? = null,
            selectedMonth: Int? = null,
            minYear: Int = DEFAULT_MIN_YEAR,
            maxYear: Int = Calendar.getInstance().get(Calendar.YEAR),
            onConfirm: ((year: Int, month: Int) -> Unit)? = null,
        ): BirthYearMonthPickerDialog {
            this.selectedYear = selectedYear
            this.selectedMonth = selectedMonth
            this.minYear = minYear
            this.maxYear = maxYear
            this.onConfirm = onConfirm
            return BirthYearMonthPickerDialog().apply {
                arguments = Bundle()
                show(manager)
            }
        }
    }

    private val monthDisplayValues by lazy {
        (1..12).map { "$it${getString(R.string.month)}" }.toTypedArray()
    }

    override fun onPrepareView() {
        super.onPrepareView()
        initYearPicker()
        initMonthPicker()
        initClick()
    }

    private fun initYearPicker() {
        val safeMaxYear = maxYear.coerceAtLeast(minYear)
        val years = (minYear..safeMaxYear).map { "${it}年" }.toTypedArray()
        binding.yearPicker.applyPickerStyle()
        binding.yearPicker.minValue = 0
        binding.yearPicker.maxValue = years.lastIndex
        binding.yearPicker.displayedValues = years
        binding.yearPicker.value = resolveSelectedYearIndex(safeMaxYear)
    }

    private fun initMonthPicker() {
        binding.monthPicker.applyPickerStyle()
        binding.monthPicker.minValue = 0
        binding.monthPicker.maxValue = monthDisplayValues.lastIndex
        binding.monthPicker.displayedValues = monthDisplayValues
        binding.monthPicker.value = resolveSelectedMonthIndex()
    }

    private fun initClick() {
        binding.tvCancel.setOnSingleClickListener {
            dismissAllowingStateLoss()
        }
        binding.tvConfirm.setOnSingleClickListener {
            val year = minYear + binding.yearPicker.value
            val month = binding.monthPicker.value + 1
            onConfirm?.invoke(year, month)
            dismissAllowingStateLoss()
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

    private fun resolveSelectedYearIndex(safeMaxYear: Int): Int {
        val defaultYear = Calendar.getInstance().get(Calendar.YEAR).coerceIn(minYear, safeMaxYear)
        val targetYear = (selectedYear ?: defaultYear)
            .coerceIn(minYear, safeMaxYear)
        return targetYear - minYear
    }

    private fun resolveSelectedMonthIndex(): Int {
        val defaultMonth = Calendar.getInstance().get(Calendar.MONTH) + 1
        val targetMonth = (selectedMonth ?: defaultMonth).coerceIn(1, 12)
        return targetMonth - 1
    }
}
