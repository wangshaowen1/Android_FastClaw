package com.shiyuan.fastclaw.feature.information.dialog

import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentManager
import com.flqy.datepicker.NumberPicker
import com.nhsd.basic.ext.setOnSingleClickListener
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.base.BaseWheelSoundBottomDialogFragment
import com.shiyuan.fastclaw.databinding.DialogBirthYearMonthPickerBinding
import java.util.Calendar

/**
 * 年月选择底部弹窗。
 * 适用于出生年月、获奖时间等场景。月份以两位字符串（"01"-"12"）回传。
 */
class BirthYearMonthPickerDialog : BaseWheelSoundBottomDialogFragment<DialogBirthYearMonthPickerBinding>() {

    companion object {
        // NumberPicker 选中/未选中文字大小（sp）
        private const val SELECTED_TEXT_SIZE_SP = 16f
        private const val UNSELECTED_TEXT_SIZE_SP = 14f
        // 滚轮可见条目数
        private const val WHEEL_ITEM_COUNT = 5
        // 默认最小年份
        private const val DEFAULT_MIN_YEAR = 1900
        private const val PRESENT_TEXT = "至今"
        const val TAG = "BirthYearMonthPickerDialog"

        // 外部传入的初值与回调
        private var dialogTitle: String? = null
        private var selectedYear: Int? = null
        private var selectedMonth: Int? = null
        private var minYear: Int = DEFAULT_MIN_YEAR
        private var maxYear: Int = Calendar.getInstance().get(Calendar.YEAR)
        private var maxTimestamp: Long? = null
        private var includePresentOption: Boolean = false
        private var selectedPresent: Boolean = false
        private var onConfirmWithTimestamp: ((year: Int, month: String, timestamp: Long) -> Unit)? = null
        private var onConfirm: ((year: Int, month: String) -> Unit)? = null
        private var onConfirmPresent: (() -> Unit)? = null

        /**
         * 显示年月选择弹窗。
         *
         * @param manager FragmentManager。
         * @param title 弹窗标题，传 null 则使用默认标题。
         * @param selectedYear 初始年份。
         * @param selectedMonth 初始月份（1-12）。
         * @param minYear 最小可选年份。
         * @param maxYear 最大可选年份。
         * @param includePresentOption 是否在年份滚轮加入“至今”选项。
         * @param selectedPresent 初始是否选中“至今”。
         * @param onConfirmWithTimestamp 确认回调，回传年份、两位月份字符串和该月 1 日 00:00:00 的毫秒时间戳。
         * @param onConfirm 确认回调，回传年份和两位月份字符串。
         * @param onConfirmPresent 选择“至今”时的确认回调。
         */
        fun show(
            manager: FragmentManager,
            title: String? = null,
            selectedYear: Int? = null,
            selectedMonth: Int? = null,
            minYear: Int = DEFAULT_MIN_YEAR,
            maxYear: Int = Calendar.getInstance().get(Calendar.YEAR),
            maxTimestamp: Long? = null,
            includePresentOption: Boolean = false,
            selectedPresent: Boolean = false,
            onConfirmWithTimestamp: ((year: Int, month: String, timestamp: Long) -> Unit)? = null,
            onConfirm: ((year: Int, month: String) -> Unit)? = null,
            onConfirmPresent: (() -> Unit)? = null,
        ): BirthYearMonthPickerDialog {
            this.dialogTitle = title
            this.selectedYear = selectedYear
            this.selectedMonth = selectedMonth
            this.minYear = minYear
            this.maxYear = maxYear
            this.maxTimestamp = maxTimestamp
            this.includePresentOption = includePresentOption
            this.selectedPresent = selectedPresent
            this.onConfirmWithTimestamp = onConfirmWithTimestamp
            this.onConfirm = onConfirm
            this.onConfirmPresent = onConfirmPresent
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
        // 设置标题
        dialogTitle?.let { binding.tvTitle.text = it }
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
        val safeMaxYear = getEffectiveMaxYear().coerceAtLeast(minYear)
        val years = buildYearDisplayValues(safeMaxYear)
        binding.yearPicker.applyPickerStyle()
        binding.yearPicker.minValue = 0
        binding.yearPicker.maxValue = years.lastIndex
        binding.yearPicker.displayedValues = years
        binding.yearPicker.value = resolveSelectedYearIndex(safeMaxYear)
        binding.yearPicker.setOnValueChangedListener { _, _, newVal ->
            if (isPresentYearIndex(newVal)) {
                binding.monthPicker.visibility = View.INVISIBLE
            } else {
                binding.monthPicker.visibility = View.VISIBLE
                updateMonthPicker(getYearByIndex(newVal), binding.monthPicker.value + 1)
            }
        }
    }

    /**
     * 初始化月份滚轮。
     */
    private fun initMonthPicker() {
        binding.monthPicker.applyPickerStyle()
        if (isPresentYearIndex(binding.yearPicker.value)) {
            binding.monthPicker.visibility = View.INVISIBLE
        } else {
            binding.monthPicker.visibility = View.VISIBLE
            updateMonthPicker(getYearByIndex(binding.yearPicker.value))
        }
    }

    private fun updateMonthPicker(year: Int, preferredMonth: Int? = null) {
        val maxMonth = getEffectiveMaxMonth(year)
        val monthValues = monthDisplayValues.take(maxMonth).toTypedArray()
        binding.monthPicker.displayedValues = null
        binding.monthPicker.minValue = 0
        binding.monthPicker.maxValue = monthValues.lastIndex
        binding.monthPicker.displayedValues = monthValues
        binding.monthPicker.value = resolveSelectedMonthIndex(maxMonth, preferredMonth)
    }

    /**
     * 初始化取消 / 确认按钮。
     */
    private fun initClick() {
        binding.tvCancel.setOnSingleClickListener {
            dismissAllowingStateLoss()
        }
        binding.tvConfirm.setOnSingleClickListener {
            if (isPresentYearIndex(binding.yearPicker.value)) {
                onConfirmPresent?.invoke()
                dismissAllowingStateLoss()
                return@setOnSingleClickListener
            }
            // 从滚轮索引换算到年份和月份
            val year = getYearByIndex(binding.yearPicker.value)
            val month = binding.monthPicker.value + 1
            val monthStr = String.format("%02d", month)   // 补零为两位字符串
            val timestamp = buildMonthTimestamp(year, month)
            onConfirmWithTimestamp?.invoke(year, monthStr, timestamp)
            onConfirm?.invoke(year, monthStr)
            dismissAllowingStateLoss()
        }
    }

    /**
     * 年月选择没有具体日期，统一取所选月份 1 日 00:00:00 的本地毫秒时间戳。
     */
    private fun buildMonthTimestamp(year: Int, month: Int): Long {
        return Calendar.getInstance().apply {
            clear()
            set(year, month - 1, 1, 0, 0, 0)
        }.timeInMillis
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
        if (includePresentOption && selectedPresent) {
            return safeMaxYear - minYear + 1
        }
        val defaultYear = Calendar.getInstance().get(Calendar.YEAR).coerceIn(minYear, safeMaxYear)
        val targetYear = (selectedYear ?: defaultYear)
            .coerceIn(minYear, safeMaxYear)
        return targetYear - minYear
    }

    /**
     * 计算月份滚轮默认索引：优先使用外部传入的 selectedMonth，否则用当前系统月。
     */
    private fun resolveSelectedMonthIndex(maxMonth: Int, preferredMonth: Int? = null): Int {
        val defaultMonth = Calendar.getInstance().get(Calendar.MONTH) + 1
        val targetMonth = (preferredMonth ?: selectedMonth ?: defaultMonth).coerceIn(1, maxMonth)
        return targetMonth - 1
    }

    private fun getEffectiveMaxYear(): Int {
        return maxTimestamp?.let { timestamp ->
            Calendar.getInstance().apply {
                timeInMillis = timestamp
            }.get(Calendar.YEAR)
        } ?: maxYear
    }

    private fun getEffectiveMaxMonth(year: Int): Int {
        val timestamp = maxTimestamp ?: return 12
        val calendar = Calendar.getInstance().apply {
            timeInMillis = timestamp
        }
        return if (calendar.get(Calendar.YEAR) == year) {
            calendar.get(Calendar.MONTH) + 1
        } else {
            12
        }
    }

    private fun buildYearDisplayValues(safeMaxYear: Int): Array<String> {
        val yearValues = (minYear..safeMaxYear).map { "${it}年" }
        return if (includePresentOption) {
            (yearValues + PRESENT_TEXT).toTypedArray()
        } else {
            yearValues.toTypedArray()
        }
    }

    private fun isPresentYearIndex(index: Int): Boolean {
        return includePresentOption && index == binding.yearPicker.maxValue
    }

    private fun getYearByIndex(index: Int): Int {
        return minYear + index
    }
}
