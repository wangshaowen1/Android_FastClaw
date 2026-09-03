@file:JvmName("DataUtils")

package com.shiyuan.fastclaw.core.ext

import android.app.Activity
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.common.CloudImageHelper
import com.shiyuan.fastclaw.common.ext.showConfirmPrompt
import com.shiyuan.fastclaw.common.ext.topActivity
import com.shiyuan.fastclaw.common.utils.WXUtils
import com.shiyuan.fastclaw.core.Config
import com.shiyuan.fastclaw.core.Global
import com.shiyuan.fastclaw.core.model.PhotoItem
import com.shiyuan.fastclaw.core.network.apis.entity.PaymentData
import com.shiyuan.fastclaw.core.network.apis.entity.ShortDramaInfo
import com.shiyuan.fastclaw.core.network.apis.response.UserHomeData
import com.kuaige.payment.PaymentClient
import com.kuaige.payment.entity.AliPayParams
import com.kuaige.payment.entity.PaymentParams
import com.kuaige.payment.entity.WxPayParams
import com.nhsd.basic.ext.getResString
import com.nhsd.basic.ext.showToastDelay
import com.nhsd.share.ShareContent
import org.json.JSONObject


fun UserHomeData?.loginToast() {
    val newRegister = this?.isNewRegistration() == true
    Global.needCompleteProfile = newRegister
    if (!newRegister) {
    }
    R.string.login_success_start_creating.getResString().showToastDelay(100)
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

fun toPhotoItem(list: MutableList<String?>?): ArrayList<PhotoItem?>? {
    if (list == null || list.size <= 0) {
        return null
    }
    val photoItems: ArrayList<PhotoItem?> = ArrayList<PhotoItem?>(list.size)
    if (list.size == 1) {
        val str = list.get(0)
        val thumbnail = CloudImageHelper.getHighQualityThumbnail(str)
        photoItems.add(PhotoItem(thumbnail, str))
    } else {
        for (str in list) {
            val thumbnail = CloudImageHelper.getThumbnail(str)
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
