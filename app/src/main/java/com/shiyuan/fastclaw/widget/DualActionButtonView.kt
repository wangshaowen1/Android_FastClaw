package com.shiyuan.fastclaw.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.content.withStyledAttributes
import androidx.databinding.BindingAdapter
import com.nhsd.basic.ext.setOnSingleClickListener
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.widget.shape.SuperRelativeLayout

class DualActionButtonView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr) {

    private val secondaryLayout: SuperRelativeLayout
    private val primaryLayout: SuperRelativeLayout
    private val secondaryTextView: TextView
    private val primaryTextView: TextView
    private val spacerView: View

    init {
        LayoutInflater.from(context).inflate(R.layout.view_dual_action_button, this, true)
        secondaryLayout = findViewById(R.id.layout_secondary)
        primaryLayout = findViewById(R.id.layout_primary)
        secondaryTextView = findViewById(R.id.tv_secondary)
        primaryTextView = findViewById(R.id.tv_primary)
        spacerView = findViewById(R.id.view_space)

        context.withStyledAttributes(attrs, R.styleable.DualActionButtonView) {
            setPrimaryText(getText(R.styleable.DualActionButtonView_primaryText))
            setSecondaryText(getText(R.styleable.DualActionButtonView_secondaryText))
            setPrimaryVisible(getBoolean(R.styleable.DualActionButtonView_primaryVisible, true))
            setSecondaryVisible(getBoolean(R.styleable.DualActionButtonView_secondaryVisible, true))
        }

        updateSpacing()
    }

    fun setPrimaryText(text: CharSequence?) {
        primaryTextView.text = text ?: ""
    }

    fun setSecondaryText(text: CharSequence?) {
        secondaryTextView.text = text ?: ""
    }

    fun setPrimaryVisible(visible: Boolean) {
        primaryLayout.visibility = if (visible) View.VISIBLE else View.GONE
        updateSpacing()
    }

    fun setSecondaryVisible(visible: Boolean) {
        secondaryLayout.visibility = if (visible) View.VISIBLE else View.GONE
        updateSpacing()
    }

    fun setOnPrimaryClickListener(listener: OnClickListener?) {
        if (listener == null) {
            primaryLayout.setOnClickListener(null)
            return
        }
        primaryLayout.setOnSingleClickListener(listener)
    }

    fun setOnSecondaryClickListener(listener: OnClickListener?) {
        if (listener == null) {
            secondaryLayout.setOnClickListener(null)
            return
        }
        secondaryLayout.setOnSingleClickListener(listener)
    }

    private fun updateSpacing() {
        spacerView.visibility = if (primaryLayout.visibility == View.VISIBLE && secondaryLayout.visibility == View.VISIBLE) {
            View.VISIBLE
        } else {
            View.GONE
        }
    }

    companion object {
        @JvmStatic
        @BindingAdapter("primaryText")
        fun bindPrimaryText(view: DualActionButtonView, text: CharSequence?) {
            view.setPrimaryText(text)
        }

        @JvmStatic
        @BindingAdapter("secondaryText")
        fun bindSecondaryText(view: DualActionButtonView, text: CharSequence?) {
            view.setSecondaryText(text)
        }

        @JvmStatic
        @BindingAdapter("primaryVisible")
        fun bindPrimaryVisible(view: DualActionButtonView, visible: Boolean) {
            view.setPrimaryVisible(visible)
        }

        @JvmStatic
        @BindingAdapter("secondaryVisible")
        fun bindSecondaryVisible(view: DualActionButtonView, visible: Boolean) {
            view.setSecondaryVisible(visible)
        }

        @JvmStatic
        @BindingAdapter("primaryClick")
        fun bindPrimaryClick(view: DualActionButtonView, clickListener: View.OnClickListener?) {
            view.setOnPrimaryClickListener(clickListener)
        }

        @JvmStatic
        @BindingAdapter("secondaryClick")
        fun bindSecondaryClick(view: DualActionButtonView, clickListener: View.OnClickListener?) {
            view.setOnSecondaryClickListener(clickListener)
        }
    }
}
