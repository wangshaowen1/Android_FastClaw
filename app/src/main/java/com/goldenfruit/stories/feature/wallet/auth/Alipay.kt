package com.kgmedia.fastfilm.feature.wallet.auth

import android.text.TextUtils
import com.alipay.sdk.app.AuthTask
import com.alipay.sdk.app.OpenAuthTask
import com.kgmedia.fastfilm.common.ext.topActivity
import com.kuaige.payment.alipay.AuthResult
import com.nhsd.basic.ext.launchNoCrash
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.withContext

/**
 * 支付宝授权
 */
object Alipay {
    private const val appId = "2021004159620739"
    fun auth(authInfo: String?, listener: AuthListener) {
        val activity = topActivity ?: return
        // 支付宝回跳到您的应用时使用的 Intent Scheme。
        // 请设置为一个不和其它应用冲突的值，并在 AndroidManifest.xml 中为 AlipayResultActivity 的 android:scheme 属性
        // 指定相同的值。实际使用时请勿设置为 __alipaysdkdemo__ 。
        // 如果不设置，OpenAuthTask.execute() 在用户未安装支付宝，使用网页完成业务流程后，将无法回跳至您的应用。
        // 防止在支付宝客户端被强行退出等意外情况下，OpenAuthTask.Callback 一定时间内无法释放，导致
        // 唤起授权业务
        if (!authInfo.isNullOrEmpty()) {
            MainScope().launchNoCrash {
                withContext(Dispatchers.IO) {
                    val authTask = AuthTask(activity)
                    val result = authTask.authV2(authInfo, true)

                    val authResult =
                        AuthResult(
                            result as MutableMap<String?, String?>?,
                            true
                        )
                    val resultStatus = authResult.resultStatus
                    // 判断resultStatus 为“9000”且result_code
                    // 为“200”则代表授权成功，具体状态码代表含义可参考授权接口文档
                    //Logging.d("授权码--->" + resultStatus + "---->" + authResult.getResultCode())
                    if (TextUtils.equals(resultStatus, "9000") && TextUtils.equals(authResult.resultCode, "200")) {
                        // 获取alipay_open_id，调支付时作为参数extern_token 的value
                        // 传入，则支付账户为该授权账户
                        listener.onSuccess(authResult.authCode)
                    } else {
                        // 其他状态值则为授权失败
                        listener.onFailed("授权失败")
                    }
                }
            }
            return
        }
        // 传递给支付宝应用的业务参数
        val scheme = "__kuaige__"
        val bizParams = mapOf(
            "url" to "https://authweb.alipay.com/auth?auth_type=PURE_OAUTH_SDK&app_id=$appId&scope=auth_user&state=init",
            "mqpScheme" to scheme
        )
        val task = OpenAuthTask(activity)
        task.execute(
            scheme,  // Intent Scheme
            OpenAuthTask.BizType.AccountAuth,  // 业务类型
            bizParams,  // 业务参数
            { i, s, bundle ->
                /**
                 * entry[0] = {AbstractMap$SimpleEntry@27318} "app_id" -> "2021004159620739"
                 * entry[1] = {AbstractMap$SimpleEntry@27321} "result_code" -> "SUCCESS"
                 * entry[2] = {AbstractMap$SimpleEntry@27324} "scope" -> "auth_user"
                 * entry[3] = {AbstractMap$SimpleEntry@27327} "state" -> "init"
                 * entry[4] = {AbstractMap$SimpleEntry@27330} "auth_code" -> "1acf88523c4c400287d0a1d4e7e9HX10"
                 */
                val code = bundle?.getString("auth_code")
                if (code.isNullOrEmpty()) {
                    listener.onFailed("授权失败")
                } else {
                    listener.onSuccess(code)
                }
            },  // 业务结果回调
            true // 是否需要在用户未安装支付宝客户端时，使用 H5 中间页中转。建议设置为 true。
        )
    }
}