package com.shiyuan.fastclaw.wxapi;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.shiyuan.fastclaw.BuildConfig;
import com.kuaige.payment.wxpay.WXPay;
import com.nhsd.basic.utils.Logger;
import com.tencent.mm.opensdk.constants.ConstantsAPI;
import com.tencent.mm.opensdk.modelbase.BaseReq;
import com.tencent.mm.opensdk.modelbase.BaseResp;
import com.tencent.mm.opensdk.modelbiz.WXLaunchMiniProgram;
import com.tencent.mm.opensdk.openapi.IWXAPI;
import com.tencent.mm.opensdk.openapi.IWXAPIEventHandler;
import com.tencent.mm.opensdk.openapi.WXAPIFactory;

public class WXPayEntryActivity extends Activity implements IWXAPIEventHandler {

    private static final String TAG = "WXPayEntryActivity";
    private IWXAPI api;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Logger.i(TAG, "onReq: ------");
        api = WXAPIFactory.createWXAPI(this, BuildConfig.WX_APP_ID);
        api.handleIntent(getIntent(), this);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        api.handleIntent(intent, this);
    }


    @Override
    public void onReq(BaseReq baseReq) {
        Logger.i(TAG, "onReq: " + baseReq.getType());
    }

    @Override
    public void onResp(BaseResp resp) {
        Logger.i(TAG, "getType::" + resp.getType());
        switch (resp.getType()) {
            case ConstantsAPI.COMMAND_PAY_BY_WX:
                Intent intent = new Intent(WXPay.WX_PAY_RESULT_RECEIVER_ACTION);
                intent.putExtra("result", resp.errCode);
                sendBroadcast(intent);
                finish();
                break;
            case ConstantsAPI.COMMAND_LAUNCH_WX_MINIPROGRAM:
                //启动微信小程序回调
                WXLaunchMiniProgram.Resp launchResp = (WXLaunchMiniProgram.Resp) resp;
                // 接收小程序返回的JSON字符串
                String extraData = launchResp.extMsg;
                //try {
                // 解析数据示例：
                //JSONObject json = new JSONObject(extraData);
                //String result = json.getString("resultKey");
                Intent intentXCX = new Intent(WXPay.WX_PAY_RESULT_RECEIVER_ACTION);
                intentXCX.putExtra("result", resp.errCode);
                sendBroadcast(intentXCX);
                //} catch (JSONException e) {
                //    throw new RuntimeException(e);
                //}
                finish();
                break;
        }
    }
}