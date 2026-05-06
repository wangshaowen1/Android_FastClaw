package com.shiyuan.fastclaw.douyinapi

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.bytedance.sdk.open.aweme.common.handler.IApiEventHandler
import com.bytedance.sdk.open.aweme.common.model.BaseReq
import com.bytedance.sdk.open.aweme.common.model.BaseResp
import com.bytedance.sdk.open.douyin.DouYinOpenApiFactory
import com.bytedance.sdk.open.douyin.api.DouYinOpenApi

class DouYinEntryActivity : AppCompatActivity(), IApiEventHandler {


    private var douYinOpenApi: DouYinOpenApi? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        douYinOpenApi = DouYinOpenApiFactory.create(this)
        douYinOpenApi?.handleIntent(intent, this)
    }

    override fun onReq(p0: BaseReq?) {

    }

    override fun onResp(resp: BaseResp?) {
//        if (resp is Authorization.Response) {
//            //处理授权结果
//            DouyinLoginManager.inst().onAuthResponse(resp)
//        } else if (resp is Share.Response) {
//            // 处理分享结果,主要看subErrorCode
//            LogUtils.e(TAG, "errorCode=${resp.errorCode} subErrorCode=${resp.subErrorCode} errorMsg=${resp.errorMsg}")
//        } else if (resp is ShareToContact.Response) {
//            //处理分享到好友结果
//        } else if (resp is CommonAbility.Response) {
//            DouYinCommonAbility.onResponse(resp)
//        } else if (resp is OpenRecord.Response) {
//            //处理拍摄页结果
//            LogUtils.e(TAG, "errorCode=${resp.errorCode} errorMsg=${resp.errorMsg}")
//        }
        finish()
    }

    override fun onErrorIntent(p0: Intent?) {

    }

    companion object {
        private const val TAG = "DouYinEntryActivity"
    }
}