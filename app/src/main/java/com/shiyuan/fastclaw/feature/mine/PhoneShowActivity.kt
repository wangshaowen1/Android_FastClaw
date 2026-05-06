package com.shiyuan.fastclaw.feature.mine

import android.content.Context
import android.content.Intent
import com.shiyuan.fastclaw.databinding.ActivityPhoneShowBinding
import com.nhsd.basic.BaseViewModelActivity

class PhoneShowActivity : BaseViewModelActivity<ActivityPhoneShowBinding, PhoneShowViewModel>() {

    companion object {
        fun start(activity: Context?) {
            activity ?: return
            val intent = Intent(activity, PhoneShowActivity::class.java)
            activity.startActivity(intent)
        }
    }

}