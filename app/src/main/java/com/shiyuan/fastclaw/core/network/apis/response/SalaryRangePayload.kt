package com.shiyuan.fastclaw.core.network.apis.response

import com.blankj.utilcode.util.GsonUtils

/**
 * 期望薪资范围回传载荷。
 *
 * `negotiable=true` 表示选择了面议，此时薪资上下限固定回传 null。
 */
data class SalaryRangePayload(
    val minSalary: Int? = null,
    val maxSalary: Int? = null,
    val negotiable: Boolean = false,
) {
    fun toJson(): String = GsonUtils.toJson(this)

    companion object {
        private const val NEGOTIABLE_TEXT = "面议"
        private const val SALARY_UNIT_SUFFIX = "K"

        fun fromPicker(minSalary: String, maxSalary: String?): SalaryRangePayload {
            val isNegotiable = minSalary == NEGOTIABLE_TEXT
            return SalaryRangePayload(
                minSalary = minSalary.toSalaryAmountOrNull(),
                maxSalary = maxSalary.toSalaryAmountOrNull(),
                negotiable = isNegotiable,
            )
        }

        private fun String?.toSalaryAmountOrNull(): Int? {
            return this
                ?.takeUnless { it == NEGOTIABLE_TEXT }
                ?.removeSuffix(SALARY_UNIT_SUFFIX)
                ?.toIntOrNull()
                ?.times(1000)
        }
    }
}
