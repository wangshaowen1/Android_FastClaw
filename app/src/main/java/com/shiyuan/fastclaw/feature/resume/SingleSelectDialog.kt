package com.shiyuan.fastclaw.feature.resume

import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentManager
import com.flqy.datepicker.NumberPicker
import com.nhsd.basic.BaseBottomPushDialogFragment
import com.nhsd.basic.ext.setOnSingleClickListener
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.base.BaseBottomDialogFragment
import com.shiyuan.fastclaw.base.BaseWheelSoundBottomDialogFragment
import com.shiyuan.fastclaw.core.network.apis.response.CommonEnumItem
import com.shiyuan.fastclaw.databinding.DialogSingleSelectBinding

/**
 * 单选弹窗（基于滚轮）。
 * 常用于奖励等级、熟练度、公司规模、公司类型、离职原因等枚举型字段。
 */
class SingleSelectDialog : BaseWheelSoundBottomDialogFragment<DialogSingleSelectBinding>() {

    companion object {
        // NumberPicker 选中/未选中文字大小（sp）
        private const val SELECTED_TEXT_SIZE_SP = 16f
        private const val UNSELECTED_TEXT_SIZE_SP = 14f
        // 滚轮可见条目数
        private const val WHEEL_ITEM_COUNT = 5
        const val TAG = "AwardLevelDialog"
        // 透传标题的参数 key
        private const val ARG_TITLE = "title"

        // 待选项列表
        private var awardLevels: List<CommonEnumItem> = emptyList()
        // 外部传入的初值
        private var selectedItem: CommonEnumItem? = null
        // 确认回调
        private var onConfirm: ((CommonEnumItem) -> Unit)? = null

        /**
         * 显示单选弹窗。
         *
         * @param manager FragmentManager。
         * @param title 弹窗标题。
         * @param awardLevels 待选项列表。
         * @param selectedItem 初始选中项。
         * @param onConfirm 确认回调。
         */
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
        // 设置标题 + 初始化滚轮 + 点击事件
        binding.tvTitle.text = arguments?.getString(ARG_TITLE)
        initPicker()
        initClick()
    }

    /**
     * 初始化滚轮内容、样式和默认选中。
     */
    private fun initPicker() {
        // 没有待选项时直接关闭
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
        // 阻止内部 EditText 抢焦点弹键盘
        binding.awardLevelPicker.descendantFocusability = NumberPicker.FOCUS_BLOCK_DESCENDANTS
        binding.awardLevelPicker.setWrapSelectorWheel(false)
        // 还原初值：按 code 匹配，命中则用其索引，否则回退到首位
        binding.awardLevelPicker.setValue(
            awardLevels.indexOfFirst { it.code == selectedItem?.code }.takeIf { it >= 0 } ?: 0
        )
    }

    /**
     * 初始化取消 / 确认按钮。
     */
    private fun initClick() {
        binding.tvCancel.setOnSingleClickListener {
            dismissAllowingStateLoss()
        }
        binding.tvConfirm.setOnSingleClickListener {
            // 按当前索引取出选项并回调
            awardLevels.getOrNull(binding.awardLevelPicker.value)?.let {
                onConfirm?.invoke(it)
            }
            dismissAllowingStateLoss()
        }
    }
}
