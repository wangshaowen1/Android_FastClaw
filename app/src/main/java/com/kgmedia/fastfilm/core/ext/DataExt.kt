@file:JvmName("DataUtils")

package com.kgmedia.fastfilm.core.ext

import android.app.Activity
import com.blankj.utilcode.util.AppUtils
import com.blankj.utilcode.util.EncryptUtils
import com.kgmedia.fastfilm.common.OssHelp
import com.kgmedia.fastfilm.common.ext.showConfirmPrompt
import com.kgmedia.fastfilm.common.ext.topActivity
import com.kgmedia.fastfilm.common.utils.WXUtils
import com.kgmedia.fastfilm.core.Config
import com.kgmedia.fastfilm.core.Global
import com.kgmedia.fastfilm.core.User
import com.kgmedia.fastfilm.core.model.PhotoItem
import com.kgmedia.fastfilm.core.network.apis.entity.PaymentData
import com.kgmedia.fastfilm.core.network.apis.entity.ShortDramaInfo
import com.kgmedia.fastfilm.core.network.apis.entity.SimpleStoryboardInfo
import com.kgmedia.fastfilm.core.network.apis.response.UserHomeData
import com.kgmedia.fastfilm.core.network.apis.response.WxPayData
import com.kgmedia.fastfilm.feature.web.SimpleBrowserActivity
import com.kuaige.payment.PaymentClient
import com.kuaige.payment.entity.AliPayParams
import com.kuaige.payment.entity.PaymentParams
import com.kuaige.payment.entity.WxPayParams
import com.nhsd.basic.ext.showToastDelay
import com.nhsd.basic.utils.Logger
import com.nhsd.share.ShareContent
import kotlinx.coroutines.Dispatchers
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

fun PaymentData.toPaymentParams(method: PaymentClient.PaymentMethod, isBaoFu: Boolean = false): PaymentParams {
    return if (method == PaymentClient.PaymentMethod.Alipay) AliPayParams(
        this.payInfo ?: ""
    ) else {
        val wxPayParams = WxPayParams()
        if (isBaoFu) {
            (this as? WxPayData)?.apply {
                wxPayParams.let {
                    it.miniprogramType = Config.get().appConfig.clientWxMiniShareMode
                    it.payWay = payWay
                    it.originalId = originalId
                    it.miniProgramPath = payInfo
                }
            }
        } else {
            wxPayParams.apply {
                val json = JSONObject(payInfo)
                this.appId = json.optString("appid")
                this.partnerId = json.optString("partnerId")
                this.prepayId = json.optString("prepayId")
                this.packageValue = json.optString("packageValue") // 注意字段名可能不同
                this.nonceStr = json.optString("noncestr")
                this.timeStamp = json.optString("timestamp")
                this.sign = json.optString("sign")
            }
        }
        wxPayParams
    }
}
