package com.shiyuan.fastclaw.widget

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.text.TextUtils
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import androidx.core.content.withStyledAttributes
import com.shiyuan.fastclaw.R

class PersonalInformationFieldView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr) {

    private val labelView: TextView
    private val valueView: TextView
    private val arrowView: ImageView

    private var rawValueText: CharSequence = ""
    private var placeholderText: CharSequence = ""
    private var valueTextColor: Int = 0
    private var placeholderTextColor: Int = 0

    init {
        LayoutInflater.from(context).inflate(R.layout.view_personal_information_field, this, true)
        labelView = findViewById(R.id.tv_label)
        valueView = findViewById(R.id.tv_value)
        arrowView = findViewById(R.id.iv_arrow)

        isClickable = true
        isFocusable = true

        val defaultValueTextColor = ContextCompat.getColor(context, R.color.color_90_000000)
        val defaultPlaceholderTextColor = ContextCompat.getColor(context, R.color.color_25_3c3c43)
        val defaultArrowTint = ContextCompat.getColor(context, R.color.color_55_3c3c43)
        valueTextColor = defaultValueTextColor
        placeholderTextColor = defaultPlaceholderTextColor

        context.withStyledAttributes(attrs, R.styleable.CommonFormFieldView) {
            setLabelText(getText(R.styleable.CommonFormFieldView_formLabelText))
            setValueText(getText(R.styleable.CommonFormFieldView_formValueText))
            setPlaceholderText(getText(R.styleable.CommonFormFieldView_formPlaceholderText))
            setArrowVisible(getBoolean(R.styleable.CommonFormFieldView_formArrowVisible, true))
            getDrawable(R.styleable.CommonFormFieldView_formArrowDrawable)?.let { setArrowDrawable(it) }
            setValueTextColor(
                if (hasValue(R.styleable.CommonFormFieldView_formValueTextColor)) {
                    getColor(R.styleable.CommonFormFieldView_formValueTextColor, defaultValueTextColor)
                } else {
                    defaultValueTextColor
                }
            )
            setPlaceholderTextColor(
                if (hasValue(R.styleable.CommonFormFieldView_formPlaceholderTextColor)) {
                    getColor(R.styleable.CommonFormFieldView_formPlaceholderTextColor, defaultPlaceholderTextColor)
                } else {
                    defaultPlaceholderTextColor
                }
            )
            setArrowTint(
                if (hasValue(R.styleable.CommonFormFieldView_formArrowTint)) {
                    getColor(R.styleable.CommonFormFieldView_formArrowTint, defaultArrowTint)
                } else {
                    defaultArrowTint
                }
            )
        }
        refreshValueState()
    }

    fun setLabelText(text: CharSequence?) {
        labelView.text = text ?: ""
    }

    fun setValueText(text: CharSequence?) {
        rawValueText = text ?: ""
        refreshValueState()
    }

    fun setPlaceholderText(text: CharSequence?) {
        placeholderText = text ?: ""
        refreshValueState()
    }

    fun setArrowVisible(visible: Boolean) {
        arrowView.visibility = if (visible) View.VISIBLE else View.GONE
    }

    fun setArrowDrawable(drawable: Drawable) {
        arrowView.setImageDrawable(drawable)
    }

    fun setValueTextColor(@ColorInt color: Int) {
        valueTextColor = color
        refreshValueState()
    }

    fun setPlaceholderTextColor(@ColorInt color: Int) {
        placeholderTextColor = color
        refreshValueState()
    }

    fun setArrowTint(@ColorInt color: Int) {
        arrowView.imageTintList = ColorStateList.valueOf(color)
    }

    private fun refreshValueState() {
        val hasValue = rawValueText.isNotBlank()
        valueView.text = if (hasValue) rawValueText else placeholderText
        valueView.setTextColor(if (hasValue) valueTextColor else placeholderTextColor)
        valueView.ellipsize = TextUtils.TruncateAt.END
        valueView.maxLines = 1
    }
}
