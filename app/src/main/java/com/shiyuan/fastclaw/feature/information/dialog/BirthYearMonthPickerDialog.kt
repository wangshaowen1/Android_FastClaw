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

/**
 * 年月选择底部弹窗。
 * 适用于出生年月、获奖时间等场景。月份以两位字符串（"01"-"12"）回传。
 */
class BirthYearMonthPickerDialog : BaseBottomPushDialogFragment<DialogBirthYearMonthPickerBinding>() {

    companion object {
        // NumberPicker 选中/未选中文字大小（sp）
        private const val SELECTED_TEXT_SIZE_SP = 16f
        private const val UNSELECTED_TEXT_SIZE_SP = 14f
        // 滚轮可见条目数
        private const val WHEEL_ITEM_COUNT = 5
        // 默认最小年份
        private const val DEFAULT_MIN_YEAR = 1900
        const val TAG = "BirthYearMonthPickerDialog"

        // 外部传入的初值与回调
        private var selectedYear: Int? = null
        private var selectedMonth: Int? = null
        private var minYear: Int = DEFAULT_MIN_YEAR
        private var maxYear: Int = Calendar.getInstance().get(Calendar.YEAR)
        private var onConfirm: ((year: Int, month: String) -> Unit)? = null

        /**
         * 显示年月选择弹窗。
         *
         * @param manager FragmentManager。
         * @param selectedYear 初始年份。
         * @param selectedMonth 初始月份（1-12）。
         * @param minYear 最小可选年份。
         * @param maxYear 最大可选年份。
         * @param onConfirm 确认回调，回传年份和两位月份字符串。
         */
        fun show(
            manager: FragmentManager,
            selectedYear: Int? = null,
            selectedMonth: Int? = null,
            minYear: Int = DEFAULT_MIN_YEAR,
            maxYear: Int = Calendar.getInstance().get(Calendar.YEAR),
            onConfirm: ((year: Int, month: String) -> Unit)? = null,
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

    // 1-12 月的可显示文本（带"月"字后缀）
    private val monthDisplayValues by lazy {
        (1..12).map { "$it${getString(R.string.month)}" }.toTypedArray()
    }

    override fun onPrepareView() {
        super.onPrepareView()
        // 初始化年份、月份滚轮 + 点击事件
        initYearPicker()
        initMonthPicker()
        initClick()
    }

    /**
     * 初始化年份滚轮。
     */
    private fun initYearPicker() {
        // 防御 maxYear < minYear 的情况
        val safeMaxYear = maxYear.coerceAtLeast(minYear)
        val years = (minYear..safeMaxYear).map { "${it}年" }.toTypedArray()
        binding.yearPicker.applyPickerStyle()
        binding.yearPicker.minValue = 0
        binding.yearPicker.maxValue = years.lastIndex
        binding.yearPicker.displayedValues = years
        binding.yearPicker.value = resolveSelectedYearIndex(safeMaxYear)
    }

    /**
     * 初始化月份滚轮。
     */
    private fun initMonthPicker() {
        binding.monthPicker.applyPickerStyle()
        binding.monthPicker.minValue = 0
        binding.monthPicker.maxValue = monthDisplayValues.lastIndex
        binding.monthPicker.displayedValues = monthDisplayValues
        binding.monthPicker.value = resolveSelectedMonthIndex()
    }

    /**
     * 初始化取消 / 确认按钮。
     */
    private fun initClick() {
        binding.tvCancel.setOnSingleClickListener {
            dismissAllowingStateLoss()
        }
        binding.tvConfirm.setOnSingleClickListener {
            // 从滚轮索引换算到年份（= minYear + 索引）和月份（= 索引 + 1）
            val year = minYear + binding.yearPicker.value
            val month = binding.monthPicker.value + 1
            val monthStr = String.format("%02d", month)   // 补零为两位字符串
            onConfirm?.invoke(year, monthStr)
            dismissAllowingStateLoss()
        }
    }

    /**
     * 应用 NumberPicker 统一样式：选中/未选中字号、颜色、可见行数、循环关闭。
     */
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
        // 阻止内部 EditText 抢焦点弹键盘
        descendantFocusability = NumberPicker.FOCUS_BLOCK_DESCENDANTS
        setWrapSelectorWheel(false)
    }

    /**
     * 计算年份滚轮默认索引：优先使用外部传入的 selectedYear，否则用当前系统年。
     */
    private fun resolveSelectedYearIndex(safeMaxYear: Int): Int {
        val defaultYear = Calendar.getInstance().get(Calendar.YEAR).coerceIn(minYear, safeMaxYear)
        val targetYear = (selectedYear ?: defaultYear)
            .coerceIn(minYear, safeMaxYear)
        return targetYear - minYear
    }

    /**
     * 计算月份滚轮默认索引：优先使用外部传入的 selectedMonth，否则用当前系统月。
     */
    private fun resolveSelectedMonthIndex(): Int {
        val defaultMonth = Calendar.getInstance().get(Calendar.MONTH) + 1
        val targetMonth = (selectedMonth ?: defaultMonth).coerceIn(1, 12)
        return targetMonth - 1
    }
}
