package com.shiyuan.fastclaw.feature.login.phone

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.text.Spannable
import android.text.SpannableString
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.util.Log
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import com.gyf.immersionbar.ImmersionBar
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.common.ext.topActivity
import com.shiyuan.fastclaw.core.Config
import com.shiyuan.fastclaw.core.Events
import com.shiyuan.fastclaw.databinding.ActivityPhoneLoginBinding
import com.shiyuan.fastclaw.feature.web.SimpleBrowserActivity
import com.shiyuan.fastclaw.widget.GeneratingDialog
import com.nhsd.basic.BaseViewModelActivity
import com.nhsd.basic.bus.FlowEventBus
import com.shiyuan.fastclaw.feature.login.PrivacyPolicyDialog

/**
 * @Introduce: 手机号登录页面
 */
class PhoneLoginActivity : BaseViewModelActivity<ActivityPhoneLoginBinding, PhoneLoginViewModel>() {
    companion object {
        const val TAG = "PhoneLoginActivity"

        var checkedPolicy = false

        fun start(
            showToast: Boolean = false,
            checkedPolicy: Boolean = false
        ) {
            this.checkedPolicy = checkedPolicy
            val activity = topActivity ?: return
            val intent = Intent(activity, PhoneLoginActivity::class.java)
            activity.startActivity(intent)
            /* activity.overridePendingTransition(
                 R.anim.push_bottom_in,
                 R.anim.push_bottom_out
             )*/
        }
    }

    private var dialog: GeneratingDialog? = null

    override fun setTranslucentStatus() {
        ImmersionBar.with(this)
            .statusBarDarkFont(true)
            .navigationBarColor(R.color.login_bg)
            .transparentStatusBar()
            .init()
    }

    override fun onViewModelCreated() {
        super.onViewModelCreated()
        mRealVM.showState.set(PhoneLoginViewModel.SHOW_VERIFY_CODE_LOGIN)
        initView()
        dataUpdate()
        setupClickListeners()
        termsOfService()
    }

    private fun initView() {
        // 如果只需要监听 afterTextChanged，可以这样简化
        binding.etPhone.doAfterTextChanged { editable ->
            mRealVM.mobile.value = editable?.toString()
        }
        binding.etVerifyCode.doAfterTextChanged { editable ->
            mRealVM.code.value = editable?.toString()
        }
        // 初始化：把 EditText 当前内容同步到 ViewModel，避免 Activity 重建时按钮状态丢失
        mRealVM.mobile.value = binding.etPhone.text?.toString()
        mRealVM.code.value = binding.etVerifyCode.text?.toString()
        mRealVM.checkSendCodeEnable()
        mRealVM.checkLoginEnable()
        mRealVM.checkedPolicy.set(checkedPolicy)
        updateCheckboxState()
    }

    private fun updateCheckboxState() {
        val isChecked = mRealVM.checkedPolicy.get()
        binding.ivCheck.setImageResource(
            if (isChecked) R.drawable.icon_checkbox_checked
            else R.drawable.icon_checkbox_unchecked
        )
    }

    private fun setupClickListeners() {
        binding.btnSend.setOnClickListener { mRealVM.sendSmsCode() }
        binding.llAgreement.setOnClickListener {
            mRealVM.checkedPolicy.set(!mRealVM.checkedPolicy.get())
        }
        binding.btVerificationCodeLogin.setOnClickListener {
            if (!mRealVM.checkedPolicy.get()) {
                showPrivacyPolicyDialog()
                return@setOnClickListener
            } else {
                phoneLogin()
            }
//            if (mRealVM.isBind.get() || mRealVM.checkedPolicy.get()) {
//                phoneLogin()
//                return@setOnClickListener
//            }
//            // mRealVM.checkedPolicy.set(true)
//            phoneLogin()
        }
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun dataUpdate() {
        mRealVM.promptGenerating.observe(this) {
            if (it.first) {
                dialog?.dismiss()
                dialog = GeneratingDialog(this, it.second)
                dialog?.show()
                return@observe
            }
            dialog?.dismiss()
        }

        mRealVM.loginResult.observe(this) {
            if (!it) return@observe
            mRealVM.loginResult.value = false
            finishOnly()
        }

        mRealVM.checkedPolicy.addOnPropertyChangedCallback(object :
            androidx.databinding.Observable.OnPropertyChangedCallback() {
            override fun onPropertyChanged(
                sender: androidx.databinding.Observable?,
                propertyId: Int
            ) {
                updateCheckboxState()
            }
        })

        mRealVM.sendCodeText.addOnPropertyChangedCallback(object :
            androidx.databinding.Observable.OnPropertyChangedCallback() {
            override fun onPropertyChanged(
                sender: androidx.databinding.Observable?,
                propertyId: Int
            ) {
                binding.btnSend.text = mRealVM.sendCodeText.get()
            }
        })


        mRealVM.mobile.observe(this) {
            mRealVM.checkSendCodeEnable()
            mRealVM.checkPhoneBg()
        }
        mRealVM.code.observe(this) {
            mRealVM.checkLoginEnable()
            mRealVM.checkCodeBg()
        }

        mRealVM.phoneBg.observe(this){
            binding.llPhone.background =if(it) resources.getDrawable(R.drawable.bg_f7f7f7_r100) else  resources.getDrawable(R.drawable.bg_ffffff_r100_w1_242424)
        }

        mRealVM.codeBg.observe(this){
            binding.llVerifyCode.background =if(it) resources.getDrawable(R.drawable.bg_f7f7f7_r100) else  resources.getDrawable(R.drawable.bg_ffffff_r100_w1_242424)
        }

        mRealVM.codeSendBg.observe(this){
            binding.btnSend.background =if(it) resources.getDrawable(R.drawable.bg_ffffff_r100_w1_242424) else  resources.getDrawable(R.drawable.bg_242424_r100)
            binding.btnSend.setTextColor(if(it) resources.getColor(R.color.color_242424) else resources.getColor(R.color.white))
        }


        mRealVM.sendCodeEnable.observe(this) {
            Log.d("sendCodeEnable",it.toString())
            Log.d("sendCodeEnable",mRealVM.codeSendBg.value.toString())
            if (it) {
                binding.btnSend.isEnabled = true
                binding.btnSend.alpha = 1f
            } else {
                binding.btnSend.isEnabled = false
                if( mRealVM.codeSendBg.value == true){
                    binding.btnSend.alpha =  1f
                }else{
                    binding.btnSend.alpha = 0.3f
                }

            }
        }
        mRealVM.loginEnable.observe(this) {
            if (it) {
                binding.btVerificationCodeLogin.isEnabled = true
                binding.btVerificationCodeLogin.alpha = 1f
            } else {
                binding.btVerificationCodeLogin.isEnabled = false
                binding.btVerificationCodeLogin.alpha = 0.3f
            }
        }

    }

    private fun phoneLogin() {
        mRealVM.phoneLogin { _ ->
            finishOnly()
            if (mRealVM.isBind.get()) {
                FlowEventBus.postSync(Events.BindingSuccessfulEvent())
            }
        }
    }

    private fun finishOnly() {
        finish()
    }

    /**
     * 显示用户协议及隐私政策确认弹窗
     */
    private fun showPrivacyPolicyDialog() {
        val privacyPolicyDialog = PrivacyPolicyDialog.show(supportFragmentManager)
        privacyPolicyDialog.callback = {
            if (it) {
                mRealVM.checkedPolicy.set(true)
                phoneLogin()
            }
        }
    }

    private fun termsOfService() {
        val prefix = getString(R.string.login_agreement_prefix)
        val userAgreement = getString(R.string.login_user_agreement)
        val suffix = getString(R.string.login_agreement_suffix)
        val privacyPolicy = getString(R.string.login_privacy_policy)

        val spannableString = SpannableString(
            prefix + userAgreement + suffix + privacyPolicy
        )

        val userAgreementIndex = prefix.length
        val suffixIndex = userAgreementIndex + userAgreement.length
        val privacyPolicyIndex = suffixIndex + suffix.length

        spannableString.setSpan(
            object : ClickableSpan() {
                override fun onClick(widget: View) {
                    SimpleBrowserActivity.launch(
                        this@PhoneLoginActivity,
                        Config.get().serviceUrl,
                        "用户协议"
                    )
                }

                override fun updateDrawState(ds: TextPaint) {
                    super.updateDrawState(ds)
                    ds.color =
                        ContextCompat.getColor(
                            this@PhoneLoginActivity,
                            R.color.login_agreement_link
                        )
                    ds.isUnderlineText = false
                }
            },
            userAgreementIndex,
            userAgreementIndex + userAgreement.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        spannableString.setSpan(
            object : ClickableSpan() {
                override fun onClick(widget: View) {
                    SimpleBrowserActivity.launch(
                        this@PhoneLoginActivity,
                        Config.get().privacyUrl,
                        "隐私政策"
                    )
                }

                override fun updateDrawState(ds: TextPaint) {
                    super.updateDrawState(ds)
                    ds.color =
                        ContextCompat.getColor(
                            this@PhoneLoginActivity,
                            R.color.login_agreement_link
                        )
                    ds.isUnderlineText = false
                }
            },
            privacyPolicyIndex,
            privacyPolicyIndex + privacyPolicy.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        binding.tvAgreement.movementMethod = LinkMovementMethod.getInstance()
        binding.tvAgreement.highlightColor = Color.TRANSPARENT
        binding.tvAgreement.text = spannableString
    }

    override fun onDestroy() {
        super.onDestroy()
        dialog?.dismiss()
        dialog = null
    }
}
