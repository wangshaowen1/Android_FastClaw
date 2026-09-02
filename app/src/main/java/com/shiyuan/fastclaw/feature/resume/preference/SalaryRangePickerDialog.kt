package com.shiyuan.fastclaw.feature.resume.preference

import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentManager
import com.flqy.datepicker.NumberPicker
import com.nhsd.basic.ext.setOnSingleClickListener
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.base.BaseBottomDialogFragment
import com.shiyuan.fastclaw.base.BaseWheelSoundBottomDialogFragment
import com.shiyuan.fastclaw.databinding.DialogSalaryRangePickerBinding

/**
 * 薪资范围选择底部弹窗。
 * 支持“面议”或有效薪资档位内的最小薪资与最大薪资区间。
 */
class SalaryRangePickerDialog : BaseWheelSoundBottomDialogFragment<DialogSalaryRangePickerBinding>() {

    companion object {
        // NumberPicker 选中/未选中文字大小（sp）
        private const val SELECTED_TEXT_SIZE_SP = 16f
        private const val UNSELECTED_TEXT_SIZE_SP = 14f
        // 滚轮可见条目数
        private const val WHEEL_ITEM_COUNT = 5
        private const val NEGOTIABLE_TEXT = "面议"
        const val TAG = "SalaryRangePickerDialog"
        private const val MIN_SALARY_K = 1
        private const val LEFT_MAX_SALARY_K = 250
        private const val RIGHT_MAX_SALARY_K = 260
        private const val LOW_STEP_BOUNDARY_K = 30
        private const val MID_STEP_BOUNDARY_K = 100

        // 1K-29K 步进 1K，30K-95K 步进 5K，100K 起步进 10K。
        private val salaryValueList = buildSalaryValueList(RIGHT_MAX_SALARY_K)
        private val minSalaryList = listOf(NEGOTIABLE_TEXT) +
            salaryValueList
                .filter { it <= LEFT_MAX_SALARY_K }
                .map(::formatSalaryValue)
        // 外部传入的初值与回调
        private var selectedMinSalary: String? = null
        private var selectedMaxSalary: String? = null
        private var dialogTitle: String? = null
        private var onConfirm: ((minSalary: String, maxSalary: String?) -> Unit)? = null

        /**
         * 显示薪资范围选择弹窗。
         *
         * @param manager FragmentManager。
         * @param minSalary 初始最小薪资。
         * @param maxSalary 初始最大薪资。
         * @param title 弹窗标题。
         * @param onConfirm 确认回调，回传最小薪资和最大薪资（可能为 null）。
         */
        fun show(
            manager: FragmentManager,
            minSalary: String? = "6K",
            maxSalary: String? = "7K",
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

        private fun buildSalaryValueList(maxSalaryK: Int): List<Int> {
            val values = mutableListOf<Int>()
            var salary = MIN_SALARY_K
            while (salary < LOW_STEP_BOUNDARY_K && salary <= maxSalaryK) {
                values.add(salary)
                salary += 1
            }

            salary = LOW_STEP_BOUNDARY_K
            while (salary < MID_STEP_BOUNDARY_K && salary <= maxSalaryK) {
                values.add(salary)
                salary += 5
            }

            salary = MID_STEP_BOUNDARY_K
            while (salary <= maxSalaryK) {
                values.add(salary)
                salary += 10
            }
            return values
        }

        private fun formatSalaryValue(salaryValue: Int): String {
            return "${salaryValue}K"
        }

        private fun parseSalaryValue(salary: String?): Int? {
            return salary
                ?.takeUnless { it == NEGOTIABLE_TEXT }
                ?.removeSuffix("K")
                ?.toIntOrNull()
        }
    }

    override fun onPrepareView() {
        super.onPrepareView()
        // 初始化最小/最大薪资滚轮 + 点击 + 默认选中
        initMinSalaryPicker()
        initClick()
        applyDefaultSelection()
        binding.title.text = dialogTitle ?: getString(R.string.salary_range)
    }

    /**
     * 初始化最小薪资滚轮，监听变化后刷新最大薪资选项。
     */
    private fun initMinSalaryPicker() {
        val displayValues = minSalaryList.toTypedArray()
        binding.wheelMinSalary.applyPickerStyle()
        binding.wheelMinSalary.minValue = 0
        binding.wheelMinSalary.maxValue = displayValues.lastIndex
        binding.wheelMinSalary.displayedValues = displayValues
        // 还原初值，未命中时回到首位
        binding.wheelMinSalary.value = resolveSalaryIndex(
            salaryList = minSalaryList,
            salary = selectedMinSalary,
            fallbackIndex = 0,
        )
        // 切换最小薪资后，最大薪资范围需要重算
        binding.wheelMinSalary.setOnValueChangedListener { _, _, _ ->
            updateMaxSalaryPicker(resetSelection = true)
        }
    }

    /**
     * 初始化取消 / 确认按钮。
     */
    private fun initClick() {
        binding.cancel.setOnSingleClickListener {
            dismissAllowingStateLoss()
        }
        binding.confirm.setOnSingleClickListener {
            // 从最小薪资滚轮读取最小值
            val minSalary = minSalaryList[binding.wheelMinSalary.value]
            // 从当前可选范围读取最大值
            val maxSalary = getCurrentMaxSalaryList().getOrNull(binding.wheelMaxSalary.value)
                ?.takeIf { binding.wheelMaxSalary.visibility == View.VISIBLE }
            onConfirm?.invoke(minSalary, maxSalary)
            dismissAllowingStateLoss()
        }
    }

    /**
     * 应用默认选中态。
     */
    private fun applyDefaultSelection() {
        updateMaxSalaryPicker(resetSelection = false)
    }

    /**
     * 根据当前最小薪资计算最大薪资可选范围，并刷新最大薪资滚轮。
     *
     * @param resetSelection 是否重置最大薪资到首位（最小薪资变化时为 true）。
     */
    private fun updateMaxSalaryPicker(resetSelection: Boolean) {
        val maxSalaryList = getCurrentMaxSalaryList()
        if (maxSalaryList.isEmpty()) {
            binding.wheelMaxSalary.visibility = View.INVISIBLE
            return
        }
        binding.wheelMaxSalary.visibility = View.VISIBLE
        val displayValues = maxSalaryList.toTypedArray()
        binding.wheelMaxSalary.applyPickerStyle()
        // 必须先清空再设置 displayedValues，避免 NumberPicker 缓存旧索引报错
        binding.wheelMaxSalary.displayedValues = null
        binding.wheelMaxSalary.minValue = 0
        binding.wheelMaxSalary.maxValue = displayValues.lastIndex
        binding.wheelMaxSalary.displayedValues = displayValues
        binding.wheelMaxSalary.value = resolveMaxSalaryIndex(maxSalaryList, resetSelection)
    }

    /**
     * 解析最大薪资滚轮的初始索引。
     */
    private fun resolveMaxSalaryIndex(maxSalaryList: List<String>, resetSelection: Boolean): Int {
        if (resetSelection) {
            // 最小薪资变化时，重置为首位
            return 0
        }
        val selectedIndex = maxSalaryList.indexOf(selectedMaxSalary)
        return if (selectedIndex >= 0) {
            // 命中初值用初值
            selectedIndex
        } else {
            resolveSalaryIndex(
                salaryList = maxSalaryList,
                salary = selectedMaxSalary,
                fallbackIndex = maxSalaryList.lastIndex,
            )
        }
    }

    /**
     * 根据当前最小薪资计算最大薪资可选列表。
     * 最小薪资为“面议”时返回空；否则取大于最小薪资且不超过右侧上限的有效档位。
     */
    private fun getCurrentMaxSalaryList(): List<String> {
        val minSalaryValue = parseSalaryValue(minSalaryList.getOrNull(binding.wheelMinSalary.value))
            ?: return emptyList()
        val maxSalaryValue = getRightMaxSalaryValue(minSalaryValue)
        return salaryValueList
            .filter { it > minSalaryValue && it <= maxSalaryValue }
            .map(::formatSalaryValue)
    }

    /**
     * 右侧最大值：min(左侧当前值 * 2, 260K)，再向下取最近有效档位。
     */
    private fun getRightMaxSalaryValue(minSalaryValue: Int): Int {
        val cappedMaxSalaryValue = (minSalaryValue * 2).coerceAtMost(RIGHT_MAX_SALARY_K)
        return salaryValueList.lastOrNull { it <= cappedMaxSalaryValue } ?: cappedMaxSalaryValue
    }

    /**
     * 初值命中旧的非有效档位时，向下兼容到最近的有效档位。
     */
    private fun resolveSalaryIndex(
        salaryList: List<String>,
        salary: String?,
        fallbackIndex: Int,
    ): Int {
        val exactIndex = salaryList.indexOf(salary)
        if (exactIndex >= 0) {
            return exactIndex
        }
        val salaryValue = parseSalaryValue(salary) ?: return fallbackIndex
        return salaryList.indexOfLast {
            val optionValue = parseSalaryValue(it)
            optionValue != null && optionValue <= salaryValue
        }.takeIf { it >= 0 } ?: fallbackIndex
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
}
