package com.kgmedia.fastfilm.feature.mine

import android.content.Context
import android.content.Intent
import com.kgmedia.fastfilm.databinding.ActivityPhoneShowBinding
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