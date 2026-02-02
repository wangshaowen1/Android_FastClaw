package com.kgmedia.fastfilm.feature.settings.customerservice

import androidx.databinding.ObservableField
import com.kgmedia.fastfilm.base.viewmodel.BaseShareViewModel
import com.kgmedia.fastfilm.core.AppRepository
import com.kgmedia.fastfilm.core.Config

/**
 * 微信客服二维码
 */
class CustomerServiceViewModel : BaseShareViewModel<AppRepository>() {

    val customerServiceUrl= ObservableField<String>(Config.get().appConfig.customerServiceUrl)

//    fun save2Album() {
//        launchWithLoading {
//            withContext(Dispatchers.IO) {
//                val bitmap = ImageUtils.getBitmap(R.drawable.img_customer_service)
//                val file = ImageUtils.save2Album(bitmap, Bitmap.CompressFormat.PNG)
//                if (file?.exists() == true) {
//                    "保存成功".showToast()
//                }
//            }
//        }
//    }
}