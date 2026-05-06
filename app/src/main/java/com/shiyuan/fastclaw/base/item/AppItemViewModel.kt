package com.shiyuan.fastclaw.base.item

import com.nhsd.basic.item.BaseUpdatableItemViewModel
import com.shiyuan.fastclaw.BR


 open class AppItemViewModel<T>(data: T?) : BaseUpdatableItemViewModel<T>(data) {
    override fun id(): Int = BR.item

}