package com.kgmedia.fastfilm.base.item

import com.nhsd.basic.item.BaseUpdatableItemViewModel
import com.kgmedia.fastfilm.BR


 open class AppItemViewModel<T>(data: T?) : BaseUpdatableItemViewModel<T>(data) {
    override fun id(): Int = BR.item

}