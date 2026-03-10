package com.kgmedia.fastfilm.common.utils

import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import com.kgmedia.fastfilm.R
import com.nhsd.basic.ext.getResColor

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

    fun highlight(
        text: String?,
        keywords: List<String>?,
        color: Int = R.color.func_failure_dark.getResColor(),
    ): SpannableStringBuilder {
        if (text.isNullOrEmpty() || keywords.isNullOrEmpty()) {
            return SpannableStringBuilder(text ?: "")
        }
        val spannable = SpannableStringBuilder(text)

        keywords
            .filter { it.isNotBlank() } // 防御：过滤纯空格
            .distinct()
            .forEach { keyword ->
                var startIndex = 0
                while (true) {
                    // 修复：忽略大小写，防止命中词和输入词大小写不一致导致高亮失败
                    val index = text.indexOf(keyword, startIndex, ignoreCase = true)
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