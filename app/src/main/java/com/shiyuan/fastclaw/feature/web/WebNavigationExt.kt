package com.shiyuan.fastclaw.feature.web

import android.app.Activity
import com.shiyuan.fastclaw.core.Config

/** Web 功能页面的导航扩展。 */
fun Activity.openTheCustomerServiceCenter() {
    Config.get().appConfig.customerServiceUrl?.let { customerCenterH5Url ->
        SimpleBrowserActivity.launch(
            this,
            customerCenterH5Url,
        )
    }
}
