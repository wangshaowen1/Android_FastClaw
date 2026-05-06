package com.shiyuan.fastclaw.feature

import android.os.Bundle
import com.nhsd.basic.BaseActivity

/**
 * 将应用前台显示
 */
class BringForegroundActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        finish()
    }
}