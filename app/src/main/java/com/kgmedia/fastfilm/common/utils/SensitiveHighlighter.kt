package com.kgmedia.fastfilm.common.utils

import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan

/**
 * *****************************************
 * 项目名称: FastFilm
 * @Author: SZYMN
 * @Introduce:敏感词高亮工具类
 * @Data: 2026/2/28/周六  15:39
 * @Version: 1.0
 * *****************************************
 */
object SensitiveHighlighter {


    /**
     * 高亮文本中的敏感词
     *
     * @param text 原始文本
     * @param keywords 敏感词数组
     * @param color 高亮颜色（例如 ContextCompat.getColor(context, R.color.red)）
     *
     * @return SpannableStringBuilder
     */
    fun highlight(
        text: String?,
        keywords: List<String>?,
        color: Int
    ): SpannableStringBuilder {

        if (text.isNullOrEmpty() || keywords.isNullOrEmpty()) {
            return SpannableStringBuilder(text ?: "")
        }
        val spannable = SpannableStringBuilder(text)

        keywords
            .filter { it.isNotEmpty() }
            .distinct()
            .forEach { keyword ->
                var startIndex = 0
                while (true) {
                    val index = text.indexOf(keyword, startIndex)
                    if (index == -1) break

                    val end = index + keyword.length

                    spannable.setSpan(
                        ForegroundColorSpan(color),
                        index,
                        end,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                    startIndex = end
                }
            }
        return spannable
    }
}