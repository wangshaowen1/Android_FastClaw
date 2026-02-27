@file:JvmName("DataUtils")

package com.kgmedia.fastfilm.core.ext

import android.app.Activity
import android.view.View
import android.widget.ImageView
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentActivity
import com.blankj.utilcode.util.AppUtils
import com.blankj.utilcode.util.EncryptUtils
import com.kgmedia.fastfilm.R
import com.kgmedia.fastfilm.common.OssHelp
import com.kgmedia.fastfilm.common.ext.showConfirmPrompt
import com.kgmedia.fastfilm.common.ext.topActivity
import com.kgmedia.fastfilm.common.utils.WXUtils
import com.kgmedia.fastfilm.core.Config
import com.kgmedia.fastfilm.core.Global
import com.kgmedia.fastfilm.core.User
import com.kgmedia.fastfilm.core.databinding.loadHQThumbnailUri
import com.kgmedia.fastfilm.core.databinding.loadUri
import com.kgmedia.fastfilm.core.model.PhotoItem
import com.kgmedia.fastfilm.core.network.apis.entity.BannerInfo
import com.kgmedia.fastfilm.core.network.apis.entity.PaymentData
import com.kgmedia.fastfilm.core.network.apis.entity.ShortDramaInfo
import com.kgmedia.fastfilm.core.network.apis.entity.SimpleStoryboardInfo
import com.kgmedia.fastfilm.core.network.apis.response.UserHomeData
import com.kgmedia.fastfilm.feature.web.SimpleBrowserActivity
import com.kgmedia.fastfilm.router.AppRouter
import com.kuaige.payment.PaymentClient
import com.kuaige.payment.entity.AliPayParams
import com.kuaige.payment.entity.PaymentParams
import com.kuaige.payment.entity.WxPayParams
import com.nhsd.basic.ext.dp
import com.nhsd.basic.ext.getResColor
import com.nhsd.basic.ext.getResDrawable
import com.nhsd.basic.ext.setOnSingleClickListener
import com.nhsd.basic.ext.showToastDelay
import com.nhsd.basic.utils.Logger
import com.nhsd.share.ShareContent
import com.zhpan.bannerview.BannerViewPager
import com.zhpan.bannerview.BaseBannerAdapter
import com.zhpan.bannerview.BaseViewHolder
import com.zhpan.indicator.enums.IndicatorStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jcodec.containers.mp4.boxes.MetaValue
import org.jcodec.movtool.MetadataEditor
import org.json.JSONObject
import java.io.File
import java.net.URLEncoder


fun UserHomeData?.loginToast() {
    val newRegister = this?.isNewRegistration() == true
    Global.needCompleteProfile = newRegister
    if (!newRegister) {
        "登录成功，开始创作吧~".showToastDelay(100)
    }
}

/**
 *
 * @param trackId String
 * @param commentsId String? 评论id  如果是通过消息列表跳转的就会传这个值
 * @param repliesId String?  回复id  如果是通过消息列表跳转的就会传这个值
 * @param downloadAccompany Boolean 是否下载伴奏
 */
fun startMusic(
    trackId: String?,

    ) {

}

/**
 * 上报异常至bugly
 */
fun Throwable.report() {
    //CrashReport.postCatchedException(this)
    printStackTrace()
}


fun Activity.openTheCustomerServiceCenter() {
    Config.get().appConfig.customerServiceUrl?.let { customerCenterH5Url ->
        SimpleBrowserActivity.launch(
            this@openTheCustomerServiceCenter,
            customerCenterH5Url,
        )
    }
}

fun ShortDramaInfo?.shortDramaVideoShareContent(): ShareContent {
    val title = "快来看看我的AI短剧${this?.dramasName ?: ""}"
    val content =
        "我用AI做了个短剧${this?.dramasName ?: ""}，还挺有意思的，快来看看！AI一键生成短剧太厉害了#快出片"
    return ShareContent(title, content)
}

/**
 * 打开微信并到客服聊天
 */
fun Activity.openTheCustomerServiceChat() {
    Config.get().appConfig.customerServiceInfo?.apply {
        showConfirmPrompt("确认跳转到微信打开客服？", confirm = {
            customerServiceTransfer()
        })
    }
}

fun Activity.customerServiceTransfer() {
    Config.get().appConfig.customerServiceInfo?.apply {
        WXUtils.customerServiceChat(
            this@customerServiceTransfer,
            customerServiceJmpUrl,
            enterpriseId
        )
    }
}


suspend fun File.putAIGCMetadata(shortDrama: SimpleStoryboardInfo): File {
    withContext(Dispatchers.IO) {
        runCatching {
            val contentProducer = AppUtils.getAppName().let { URLEncoder.encode(it) }
            val produceID = shortDrama.storyboardId
            val reservedCode1 = EncryptUtils.encryptMD5ToString("${contentProducer}${produceID}")
            val contentPropagator = User.get().nickName?.let { URLEncoder.encode(it) }
            val propagateID = User.get().getId()
            val reservedCode2 = EncryptUtils.encryptMD5ToString("${contentPropagator}${propagateID}")
            val aigc = JSONObject()
                .put("Label", "3")
                .put("ContentProducer", contentProducer)
                .put("ProduceID", produceID)
                .put("ReservedCode1", reservedCode1)
                .put("ContentPropagator", contentPropagator)
                .put("PropagateID", propagateID)
                .put("ReservedCode2", reservedCode2)
                .toString()
            val metadataEditor = MetadataEditor.createFrom(this@putAIGCMetadata)
            metadataEditor.keyedMeta?.put("AIGC", MetaValue.createString(aigc))
            metadataEditor.save(true)
        }.onFailure {
            Logger.e("putAIGCMetadata", it.message)
            it.printStackTrace()
        }
    }
    return this
}


fun toPhotoItem(list: MutableList<String?>?): ArrayList<PhotoItem?>? {
    if (list == null || list.size <= 0) {
        return null
    }
    val photoItems: ArrayList<PhotoItem?> = ArrayList<PhotoItem?>(list.size)
    if (list.size == 1) {
        val str = list.get(0)
        val thumbnail = OssHelp.getOssHighQualityThumbnail(str)
        photoItems.add(PhotoItem(thumbnail, str))
    } else {
        for (str in list) {
            val thumbnail = OssHelp.getOssThumbnail(str)
            photoItems.add(PhotoItem(thumbnail, str))
        }
    }
    return photoItems
}


fun createAppPayment(): PaymentClient {
    return PaymentClient(topActivity)
}

fun PaymentData.toPaymentParams(method: PaymentClient.PaymentMethod): PaymentParams {
    return if (method == PaymentClient.PaymentMethod.Alipay)
        AliPayParams(this.orderStr ?: "")
    else
        WxPayParams().apply {
            if (isBaoFu) {
                this.miniprogramType = Config.get().appConfig.clientWxMiniShareMode
                this.payWay = payWay
                this.originalId = originalId
                this.miniProgramPath = orderStr
            } else {
                /**
                 * {
                 *   "appid" : "wxc4c951d1cd1816fc",
                 *   "noncestr" : "5uTXPyYSYsOVrsFGCpBElYg7AT11DP2Y",
                 *   "orderNo" : "2026022618300191381042664",
                 *   "packageValue" : "Sign=WXPay",
                 *   "partnerId" : "1718052044",
                 *   "prepayId" : "wx27090925805964170b9d30499b82b90001",
                 *   "sign" : "K3dov5YFnAdaeRPVda3Fbx1tc9X6Z/NjFSL+FJuLOCwdqjmgp/V9vxA4STr5UuUe3oHQGIbxDD5vi0mHhEAW1/6PyO8agrEjG5/GLNfwYdva02TDclclNxI2x4y+KuiVSOERtzHYDdLiEgU6ZPdRv1i3cp50VBbJZvdByvqr4LXAOu2GrxYnIncdoBbpuwnnz4hY8ABVY769tRiLt9NCeKRIfo5XUXI+/eLMMaHY6Ynmy4qsHiTUzdeVa+YTvQytxYdrHvHPlkHIjXRNlgIEW4QS4vovZmUaI+Zc0DWhCb/gt0FYQTSwUYLHk05Zrh3U1nkmV565tiPdHcvk4J3ZWg==",
                 *   "timestamp" : "1772154565"
                 * }
                 */
                val json = JSONObject(orderStr)
                this.appId = json.optString("appid")
                this.partnerId = json.optString("partnerId")
                this.prepayId = json.optString("prepayId")
                this.packageValue = json.optString("packageValue") // 注意字段名可能不同
                this.nonceStr = json.optString("noncestr")
                this.timeStamp = json.optString("timestamp")
                this.sign = json.optString("sign")
            }
        }
}

fun setupCommonBanner(
    container: View,
    pager: BannerViewPager<BannerInfo>,
    getBanners: suspend () -> List<BannerInfo>?
) {
    MainScope().launch {
        val banners = runCatching { getBanners.invoke() }.getOrNull()
        if (banners.isNullOrEmpty()) {
            container.isVisible = false
        } else {
            container.isVisible = true
            pager.setAdapter(
                object : BaseBannerAdapter<BannerInfo>() {
                    override fun bindData(holder: BaseViewHolder<BannerInfo>?, data: BannerInfo, position: Int, pageSize: Int) {
                        holder?.apply {
                            val image = findViewById<ImageView>(R.id.image)
                            image.setOnSingleClickListener { AppRouter.jump(data.jumpCode) }
                            loadHQThumbnailUri(image, data.imageUrl, R.drawable.img_default_cover_16_9.getResDrawable())
                        }
                    }

                    override fun getLayoutId(viewType: Int): Int {
                        return R.layout.item_common_banner
                    }
                })
                .apply {
                    (pager.context as? FragmentActivity)?.lifecycle?.let { setLifecycleRegistry(it) }
                }
                .setAutoPlay(true)
                .setIndicatorSliderRadius(2f.dp)
                .setIndicatorSliderGap(4.dp)
                .setIndicatorHeight(4.dp)
                .setIndicatorSliderWidth(4.dp, 16.dp)
                .setIndicatorSliderColor(R.color.color_60_ffffff.getResColor(), R.color.white.getResColor()).setIndicatorStyle(IndicatorStyle.ROUND_RECT)
                .setIndicatorMargin(0, 0, 0, 4.dp)
                .setIndicatorVisibility(View.VISIBLE)
                .create(banners)
        }
    }
}