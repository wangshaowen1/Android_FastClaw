package com.shiyuan.fastclaw.core.manager

import android.app.Activity
import android.app.Application
import android.content.Context
import com.bytedance.ads.convert.BDConvert.init
import com.bytedance.ads.convert.callback.BDConvertLifecycleCallback
import com.bytedance.ads.convert.config.BDConvertConfig
import com.nhsd.basic.utils.Logger
import org.json.JSONException
import org.json.JSONObject

/**
 * 巨量sdk初始化
 */
class AdsManager {

    companion object {

        private const val TAG = "AdsReporter"
        var isInitSuccess = false
        fun initAdsReporter(activity: Activity, isDebug: Boolean = false) {
            if (isInitSuccess) return
            val context: Context = activity
            Thread {//文档说不建议用协程。。。
                try {
                    val config = BDConvertConfig()
                    // 在调用init时是否自动上报事件，默认为true，
                    config.autoSendLaunchEvent = true
                    // 是否开启debug日志，默认为false，在排查问题时使用,参考：https://bytedance.larkoffice.com/docx/D99KdC7VXoMmarxIza6cAFKDnhe
                    config.enableLog = isDebug
                    // 是否启用心跳事件，默认为true
                    config.playSessionEnable = true
                    // SDK是否采集OAID，默认为true
                    config.enableOAID = true
                    // 配置自定义OAID获取逻辑，默认不配置；若配置，则SDK自身不会有采集OAID的行为，会调用这个Callback获取
//                config.customOaidCallback = object : CustomOaidCallback {
//                    override fun get(): String {
//                        //"自定义OAID获取"
//                        return ""
//                    }
//                }
//                // 配置自定义Android_id获取逻辑，默认不配置；若配置，则SDK自身不会有采集android_id的行为，会调用这个Callback获取
//                // 请正确回传Android_id值，错误传值会导致归因失败
//                config.customAndroidIDCallback = object : CustomAndroidIDCallback {
//                    override fun get(): String {
//                        //"自定义Android_ID获取"
//                        return ""
//                    }
//                }
                    config.lifecycleCallback = (object : BDConvertLifecycleCallback {
                        override fun onOtherError(reason: Int, throwable: Throwable?) {
                            // 如果有非打断的错误，会触发该回调
                            Logger.e(TAG, "onOtherError: $reason ${throwable?.message}")
                            val params = JSONObject()
                            try {
                                params.put("reason", reason)
                                params.put("error_msg", throwable?.message ?: "")
                                //sendHostLog("bd_convert_other_error", params)
                            } catch (e: JSONException) {
                                e.printStackTrace()
                            }
                        }

                        override fun onEventSendFailure(eventName: String, reason: Int, requestId: String, throwable: Throwable?) {
                            // 事件发送发生错误时，会触发该回调
                            Logger.e(TAG, "onEventSendFailure: $eventName-$reason $requestId  ${throwable?.message}")
                            val params = JSONObject()
                            try {
                                params.put("event_name", eventName)
                                params.put("reason", reason)
                                params.put("request_id", requestId)
                                params.put("error_msg", throwable?.message ?: "")
                                //sendHostLog("bd_convert_event_send_failure", params)
                            } catch (e: JSONException) {
                                e.printStackTrace()
                            }
                        }

                        override fun onInitFailure(reason: Int, throwable: Throwable?) {
                            // 初始化发生错误时，会触发该回调
                            Logger.e(TAG, "onInitFailure: $reason  ${throwable?.message}")
                            isInitSuccess = false
                            val params = JSONObject()
                            try {
                                params.put("reason", reason)
                                params.put("error_msg", throwable?.message ?: "")
                                //sendHostLog("bd_convert_init_failure", params)
                            } catch (e: JSONException) {
                                e.printStackTrace()
                            }
                        }

                        override fun onInitSuccess() {
                            // 初始化成功回调
                            Logger.d(TAG, "onInitSuccess")
                            isInitSuccess = true
                            //sendHostLog("bd_convert_init_success", JSONObject())
                        }

                        override fun onEventSendSuccess(eventName: String, requestId: String) {
                            // 事件发送成功回调
                            Logger.d(TAG, "onEventSendSuccess: $eventName-$requestId")
                            val params = JSONObject()
                            try {
                                params.put("event_name", eventName)
                                params.put("request_id", requestId)
                                //sendHostLog("bd_convert_event_send_success", params)
                            } catch (e: JSONException) {
                                e.printStackTrace()
                            }
                        }
                    })
                    init(context, config, activity)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }.start()
        }
    }
}