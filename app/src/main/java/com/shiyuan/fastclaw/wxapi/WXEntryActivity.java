package com.shiyuan.fastclaw.wxapi;

import android.util.Log;

import com.alibaba.fastjson.JSON;
import com.nhsd.basic.bus.FlowEventBus;
import com.shiyuan.fastclaw.core.Events;
import com.tencent.mm.opensdk.constants.ConstantsAPI;
import com.tencent.mm.opensdk.modelbase.BaseResp;
import com.tencent.mm.opensdk.modelbiz.WXLaunchMiniProgram;
import com.umeng.socialize.weixin.view.WXCallbackActivity;

import org.json.JSONObject;

import kotlinx.serialization.json.Json;

public class WXEntryActivity extends WXCallbackActivity {
    public static  int resumeUploadSource = -1;  //0 是我的简历   1是首页的
    // 3. 核心回调方法
    @Override
    public void onResp(BaseResp resp) {
        Log.d("onResp", JSON.toJSONString(resp));
        // 判断是否为“拉起小程序”的响应[reference:10][reference:11]
        if (resp.getType() == ConstantsAPI.COMMAND_LAUNCH_WX_MINIPROGRAM) {
            WXLaunchMiniProgram.Resp launchResp = (WXLaunchMiniProgram.Resp) resp;
            // 获取小程序返回的数据[reference:12][reference:13]
            String extraData = launchResp.extMsg;
            FlowEventBus.postSync(new Events.ResumeEvent(resumeUploadSource, extraData));
        }
        // 4. 处理完后必须关闭当前 Activity[reference:14][reference:15]
        finish();
    }
}