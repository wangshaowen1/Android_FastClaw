package com.shiyuan.fastclaw.feature.information.dialog

import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentManager
import com.flqy.datepicker.NumberPicker
import com.nhsd.basic.ext.setOnSingleClickListener
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.base.BaseBottomDialogFragment
import com.shiyuan.fastclaw.core.network.apis.response.CommonEnumItem
import com.shiyuan.fastclaw.databinding.DialogEducationDegreePickerBinding
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * 学历与学习形式双列选择弹窗。
 */
class EducationDegreePickerDialog :
    BaseBottomDialogFragment<DialogEducationDegreePickerBinding>() {

    companion object {
        private const val SELECTED_TEXT_SIZE_SP = 16f
        private const val UNSELECTED_TEXT_SIZE_SP = 14f
        private const val WHEEL_ITEM_COUNT = 5
        private const val DEFAULT_DEGREE = "本科"
        private const val DEFAULT_STUDY_TYPE = "全日制"
        private val FULL_TIME_SUPPORTED_DEGREES = setOf("大专", "本科", "硕士", "博士")
        private const val ARG_DEGREE_OPTIONS = "degreeOptions"
        private const val ARG_STUDY_TYPE_OPTIONS = "studyTypeOptions"
        private const val ARG_SELECTED_DEGREE = "selectedDegree"
        private const val ARG_SELECTED_STUDY_TYPE = "selectedStudyType"

        const val TAG = "EducationDegreePickerDialog"

        fun show(
            manager: FragmentManager,
            degreeOptions: List<CommonEnumItem>,
            studyTypeOptions: List<CommonEnumItem>,
            selectedDegree: String? = null,
            selectedStudyType: String? = null,
            onConfirm: ((degree: CommonEnumItem, studyType: CommonEnumItem?) -> Unit)? = null,
        ): EducationDegreePickerDialog {
            return EducationDegreePickerDialog().apply {
                arguments = Bundle().apply {
                    putString(ARG_DEGREE_OPTIONS, Json.encodeToString(degreeOptions))
                    putString(ARG_STUDY_TYPE_OPTIONS, Json.encodeToString(studyTypeOptions))
                    putString(ARG_SELECTED_DEGREE, selectedDegree)
                    putString(ARG_SELECTED_STUDY_TYPE, selectedStudyType)
                }
                this.onConfirm = onConfirm
                show(manager)
            }
        }
    }

    private var degreeOptions: List<CommonEnumItem> = emptyList()
    private var studyTypeOptions: List<CommonEnumItem> = emptyList()
    private var onConfirm: ((degree: CommonEnumItem, studyType: CommonEnumItem?) -> Unit)? = null

    override fun onPrepareView() {
        super.onPrepareView()
        degreeOptions = decodeOptions(ARG_DEGREE_OPTIONS)
        studyTypeOptions = decodeOptions(ARG_STUDY_TYPE_OPTIONS)
        if (degreeOptions.isEmpty() || studyTypeOptions.isEmpty()) {
            dismissAllowingStateLoss()
            return
        }
        initPicker(
            picker = binding.degreePicker,
            options = degreeOptions,
            selectedName = arguments?.getString(ARG_SELECTED_DEGREE),
            defaultName = DEFAULT_DEGREE,
        )
        initPicker(
            picker = binding.studyTypePicker,
            options = studyTypeOptions,
            selectedName = arguments?.getString(ARG_SELECTED_STUDY_TYPE),
            defaultName = DEFAULT_STUDY_TYPE,
        )
        binding.degreePicker.setOnValueChangedListener { _, _, newValue ->
            updateStudyTypeVisibility(newValue)
        }
        updateStudyTypeVisibility(binding.degreePicker.value)
        initClick()
    }

    private fun decodeOptions(key: String): List<CommonEnumItem> {
        return Json.decodeFromString(requireNotNull(arguments?.getString(key)))
    }

    private fun initPicker(
        picker: NumberPicker,
        options: List<CommonEnumItem>,
        selectedName: String?,
        defaultName: String,
    ) {
        picker.applyPickerStyle()
        picker.minValue = 0
        picker.maxValue = options.lastIndex
        picker.displayedValues = options.map { it.name.orEmpty() }.toTypedArray()
        picker.value = resolveSelectedIndex(options, selectedName, defaultName)
    }

    private fun resolveSelectedIndex(
        options: List<CommonEnumItem>,
        selectedName: String?,
        defaultName: String,
    ): Int {
        return options.indexOfFirst { it.name == selectedName }
            .takeIf { it >= 0 }
            ?: options.indexOfFirst { it.name == defaultName }.takeIf { it >= 0 }
            ?: 0
    }

    private fun initClick() {
        binding.cancel.setOnSingleClickListener {
            dismissAllowingStateLoss()
        }
        binding.confirm.setOnSingleClickListener {
            val degree = degreeOptions.getOrNull(binding.degreePicker.value)
            val studyType = if (binding.studyTypePicker.visibility == View.VISIBLE) {
                studyTypeOptions.getOrNull(binding.studyTypePicker.value)
            } else {
                null
            }
            if (degree != null) {
                onConfirm?.invoke(degree, studyType)
            }
            dismissAllowingStateLoss()
        }
    }

    /**
     * 只有大专及以上学历可选择学习形式，其他学历隐藏右侧内容但保留列宽。
     */
    private fun updateStudyTypeVisibility(degreeIndex: Int) {
        val degreeName = degreeOptions.getOrNull(degreeIndex)?.name
        binding.studyTypePicker.visibility = if (
            degreeName != null && degreeName in FULL_TIME_SUPPORTED_DEGREES
        ) {
            View.VISIBLE
        } else {
            View.INVISIBLE
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

    override fun onDestroy() {
        onConfirm = null
        super.onDestroy()
    }
}
