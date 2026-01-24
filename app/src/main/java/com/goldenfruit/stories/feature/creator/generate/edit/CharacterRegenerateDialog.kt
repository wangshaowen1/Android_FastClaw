package com.kgmedia.fastfilm.feature.creator.generate.edit

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.app.Activity.RESULT_OK
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import com.kgmedia.fastfilm.common.Constants
import com.kgmedia.fastfilm.common.SimpleTextWatcher
import com.kgmedia.fastfilm.common.ext.selectSinglePhotoFromAlbums
import com.kgmedia.fastfilm.common.permissions.requestImages2VideosPermissions
import com.kgmedia.fastfilm.core.network.apis.entity.CharacterInfo
import com.kgmedia.fastfilm.core.network.apis.entity.RoleTagInfo
import com.kgmedia.fastfilm.databinding.DialogCharacterRegenerateBinding
import com.kgmedia.fastfilm.widget.GeneratingDialog
import com.nhsd.basic.BaseBottomPushVMDialogFragment
import com.nhsd.basic.ext.launchNoCrash
import com.nhsd.basic.ext.setOnSingleClickListener
import com.nhsd.basic.ext.showToast
import com.nhsd.basic.utils.Logger
import com.nhsd.mattise.Matisse
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 故事剧本编辑
 */
class CharacterRegenerateDialog : BaseBottomPushVMDialogFragment<DialogCharacterRegenerateBinding, CharacterRegenerateViewModel>() {

    companion object {
        private const val REQUEST_CODE_CHOOSE = 0x14
        fun newInstance(dramasId: String?, role: CharacterInfo? = null, isRoleAdd: Boolean = false): CharacterRegenerateDialog {
            val fragment = CharacterRegenerateDialog()
            fragment.arguments = Bundle().apply {
                putString(Constants.Extra.DRAMAS_ID, dramasId)
                putParcelable(Constants.Extra.DATA, role)
                putBoolean(Constants.Extra.MODE, isRoleAdd)
            }
            return fragment
        }
    }

    private var animator: ObjectAnimator? = null
    var dialog: GeneratingDialog? = null
    var onSuccess: (String?) -> Unit = {}
    private var isPopShowing: Boolean = false
    var popupType = 0

    private val popupView by lazy {
        PopupCharacterGenerateDialog(
            mActivity
        ).apply {
            setOnDismissListener {
                lifecycleScope.launchNoCrash {
                    delay(500)
                    isPopShowing = false
                }
                Logger.i("randomVoice_dismiss 2 poop")
            }
            onPopViewClickedListener = {
                when (popupType) {
                    0 -> {
                        mRealVM.mAgeTagInfo = it
                        binding.btnAge.text = it?.roleTagName
                    }

                    1 -> {
                        mRealVM.mGenderTagInfo = it
                        binding.btnGender.text = it?.roleTagName
                    }

                    2 -> {
                        mRealVM.mTypeTagInfo = it
                        binding.btnType.text = it?.roleTagName
                    }
                }
            }
        }
    }


    override fun onPrepareView() {
        super.onPrepareView()
        initListener()
        mRealVM.input.observe(this) {
            if (mRealVM.input.value != binding.inputer.text) {
                binding.inputer.setTextNoSelection(mRealVM.input.value)
            }
        }
        mRealVM.tags.observe(this) {
            mRealVM.originalTags?.forEach { tag ->
                val tagInfo = it.find { t -> t.roleTagId == tag?.roleTagId }
                when (tagInfo?.roleTagType) {
                    1 -> {
                        mRealVM.mAgeTagInfo = tagInfo
                        binding.btnAge.text = tagInfo.roleTagName
                    }

                    2 -> {
                        mRealVM.mGenderTagInfo = tagInfo
                        binding.btnGender.text = tagInfo.roleTagName
                    }

                    3 -> {
                        mRealVM.mTypeTagInfo = tagInfo
                        binding.btnType.text = tagInfo.roleTagName
                    }
                }
            }
        }
//        binding.inputer.setOnTextOutOfBondsListener {
//            mRealVM.maxInputLength.takeIf { it > 0 }.let {
//                getString(R.string.toast_input_max_characters_exceeded, it).showToast()
//            }
//        }

        binding.inputer.addTextChangedListener(object : SimpleTextWatcher() {
            override fun afterTextChanged(s: Editable?) {
                val text = s.toString()
                mRealVM.input.value = text
                updateConfirmButtonState()
            }
        })

        binding.etRoleName.doAfterTextChanged {
            updateConfirmButtonState()
        }

        mRealVM.promptGenerating.observe(this) {
            if (it.first) {
                dialog?.dismiss()
                dialog = activity?.let { it1 -> GeneratingDialog(it1, it.second) }
                dialog?.show()
            } else {
                dialog?.dismiss()
            }
        }

        mRealVM.isAnalysisLoading.observe(this) { isLoading ->
            if (isLoading) {
                lifecycleScope.launch {
                    animator?.cancel()
                    animator = ObjectAnimator.ofFloat(binding.rotate, "rotation", 0f, 360f).apply {
                        duration = 1000
                        interpolator = LinearInterpolator()
                        repeatCount = ObjectAnimator.INFINITE
                        repeatMode = ObjectAnimator.RESTART
                        start()
                    }
                }
            } else {
                stopAnimationGracefully()
            }
        }
    }

    fun initListener() {
        binding.btnOptimize.setOnSingleClickListener {
            if (mRealVM.input.value.isNullOrEmpty()) {
                "请输入角色形象描述".showToast()
                return@setOnSingleClickListener
            }
            mRealVM.optimizeRoleImageDesc()
        }
        binding.btnAge.setOnClickListener {
            if (mRealVM.tags.value.isNullOrEmpty()) {
                mRealVM.getTagList {
                    binding.btnAge.performClick()
                }
                return@setOnClickListener
            }
            popupType = 0
            showPopup(mRealVM.tags.value?.filter { tag -> tag.roleTagType == 1 }, it)
        }
        binding.btnGender.setOnClickListener {
            if (mRealVM.tags.value.isNullOrEmpty()) {
                mRealVM.getTagList {
                    binding.btnGender.performClick()
                }
                return@setOnClickListener
            }
            popupType = 1
            showPopup(mRealVM.tags.value?.filter { tag -> tag.roleTagType == 2 }, it)
        }
        binding.btnType.setOnClickListener {
            if (mRealVM.tags.value.isNullOrEmpty()) {
                mRealVM.getTagList {
                    binding.btnType.performClick()
                }
                return@setOnClickListener
            }
            popupType = 2
            showPopup(mRealVM.tags.value?.filter { tag -> tag.roleTagType == 3 }, it)
        }

        binding.btnModifyPic.setOnSingleClickListener {
            chooseAvatar()
        }
        binding.btnUpload.setOnSingleClickListener {
            chooseAvatar()
        }

        binding.btnConfirm.setOnClickListener {
            if (mRealVM.isRoleAddModel && binding.etRoleName.text.isNullOrEmpty()) {
                "请填写角色名称".showToast()
                return@setOnClickListener
            }
            if (binding.inputer.text.isNullOrEmpty()) {
                "请填写角色形象描述".showToast()
                return@setOnClickListener
            }
            if (mRealVM.isRoleAddModel) {
                if (mRealVM.mAgeTagInfo == null) {
                    "请选择角色年龄".showToast()
                    return@setOnClickListener
                }
                if (mRealVM.mGenderTagInfo == null) {
                    "请选择角色性别".showToast()
                    return@setOnClickListener
                }
                if (mRealVM.mTypeTagInfo == null) {
                    "请选择角色类别".showToast()
                    return@setOnClickListener
                }
                mRealVM.roleAdd {
                    onSuccess.invoke(null)
                    dismissAllowingStateLoss()
                }
                return@setOnClickListener
            }
            mRealVM.roleImageRegenerate {
                onSuccess.invoke(it)
                dismissAllowingStateLoss()
            }
        }
    }

    private fun updateConfirmButtonState() {
        val text1 = binding.inputer.text.toString()
        val text2 = binding.etRoleName.text.toString()

        val isReady = if (mRealVM.isRoleAddModel) {
            text1.isNotEmpty() && text2.isNotEmpty()
        } else {
            text1.isNotEmpty()
        }
        binding.btnConfirm.alpha = if (isReady) 1f else 0.5f
    }

    fun showPopup(tags: List<RoleTagInfo>?, view: View) {
        if (isPopShowing) {
            popupView.dismiss()
            Logger.i("randomVoice_dismiss poop")
        } else {
            if (tags.isNullOrEmpty()) return
            popupView.show(tags, view)
            isPopShowing = true
        }
    }

    fun chooseAvatar() {
        //选择图片
        requestImages2VideosPermissions { granted ->
            if (granted) {
                selectSinglePhotoFromAlbums(
                    fragment = this,
                    selectedUri = null,
                    requestCode = REQUEST_CODE_CHOOSE,
                    crop = false,
                )
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK) return
        when (requestCode) {
            REQUEST_CODE_CHOOSE -> {
                val result = Matisse.obtainPathResult(data)
                result?.firstOrNull()?.let { mRealVM.uploadAvatar(it) }
            }
        }
    }

    private fun stopAnimationGracefully() {
        // 获取当前旋转角度
        val currentRotation = binding.rotate.rotation % 360
        // 计算需要额外旋转的角度（至少完成一圈）
        val remainingRotation = if (currentRotation > 0) 360f - currentRotation else 0f

        animator?.cancel()

        ObjectAnimator.ofFloat(
            binding.rotate,
            "rotation",
            binding.rotate.rotation,
            binding.rotate.rotation + remainingRotation
        ).apply {
            duration = (400 * (remainingRotation / 360f)).toLong()
            interpolator = LinearInterpolator()
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    bindingNullable?.rotate?.rotation = 0f
                }
            })
            start()
        }
    }
}