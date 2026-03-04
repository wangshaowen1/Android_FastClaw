package com.kgmedia.fastfilm.common.utils

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
    ): CharSequence? {
        return null
    }
}