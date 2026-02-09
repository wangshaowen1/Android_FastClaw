package com.kgmedia.fastfilm.widget

import android.content.Context
import androidx.appcompat.app.AppCompatDialog
import com.kgmedia.fastfilm.R
import com.kgmedia.fastfilm.databinding.DialogLoadingBinding

class GeneratingDialog @JvmOverloads constructor(context: Context, tips: CharSequence? = null, backEnable: Boolean = false) :
    AppCompatDialog(context, R.style.Dialog_Common) {
    private val binding = DialogLoadingBinding.inflate(layoutInflater)

    constructor(context: Context, resId: Int, backEnable: Boolean = false) : this(context, context.getString(resId), backEnable)

    init {
        setContentView(binding.root)
        setCanceledOnTouchOutside(backEnable)
        setCancelable(backEnable)
        tips?.let { binding.tips.text = it }
//        val anim = AnimationUtils.loadAnimation(context, R.anim.rotate_animation)
//        binding.loadingImage.startAnimation(anim)
    }

}
