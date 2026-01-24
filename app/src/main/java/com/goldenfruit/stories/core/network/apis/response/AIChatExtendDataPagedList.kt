package com.kgmedia.fastfilm.core.network.apis.response

import com.kgmedia.fastfilm.core.network.apis.PagedList

data class AIChatExtendDataPagedList<T>(
    val extendData: AIChatExtendData?,
) : PagedList<T>()