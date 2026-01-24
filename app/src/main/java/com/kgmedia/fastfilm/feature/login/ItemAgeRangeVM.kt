package com.kgmedia.fastfilm.feature.login

import androidx.databinding.ObservableField
import androidx.databinding.ObservableInt
import com.kgmedia.fastfilm.base.item.AppItemViewModel
import com.kgmedia.fastfilm.core.network.apis.response.AgeRange

class ItemAgeRangeVM(item: AgeRange, val selectedItem: ObservableField<AgeRange>, val gender: ObservableInt) : AppItemViewModel<AgeRange>(item)