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

/**
 * 薪资范围选择底部弹窗。
 * 支持“面议”或最小薪资到最大薪资区间；当最小薪资选择“面议”时隐藏最大薪资滚轮。
 */
class SalaryRangePickerDialog : BaseBottomPushDialogFragment<DialogSalaryRangePickerBinding>() {

    companion object {
        // NumberPicker 选中/未选中文字大小（sp）
        private const val SELECTED_TEXT_SIZE_SP = 16f
        private const val UNSELECTED_TEXT_SIZE_SP = 14f
        // 滚轮可见条目数
        private const val WHEEL_ITEM_COUNT = 5
        const val TAG = "SalaryRangePickerDialog"

        // 最小薪资候选项（首位为"面议"）
        private val minSalaryList = listOf(
            "面议", "1K", "2K", "3K", "4K", "5K", "6K", "7K", "8K", "9K", "10K", "11K", "12K"
        )
        // 最大薪资候选项（不含"面议"）
        private val allSalaryList = listOf(
            "1K", "2K", "3K", "4K", "5K", "6K", "7K", "8K", "9K", "10K", "11K", "12K"
        )
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
        // 初始化最小/最大薪资滚轮 + 点击 + 默认选中
        initMinSalaryPicker()
        initClick()
        applyDefaultSelection()
        binding.title.text = dialogTitle ?: "薪资范围"
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
        binding.wheelMinSalary.value = minSalaryList.indexOf(selectedMinSalary).takeIf { it >= 0 } ?: 0
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
            // 最大薪资只有在滚轮可见时才有意义（最小薪资不是"面议"）
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
            // 最小薪资为"面议"时，隐藏最大薪资滚轮
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
        // 命中初值用初值，否则选最后一项
        return if (selectedIndex >= 0) {
            selectedIndex
        } else {
            maxSalaryList.lastIndex
        }
    }

    /**
     * 根据当前最小薪资计算最大薪资可选列表。
     * 最小薪资为"面议"时返回空；否则取从最小薪资开始到末尾的子列表。
     */
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
