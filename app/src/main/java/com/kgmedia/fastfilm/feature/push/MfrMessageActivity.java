package com.kgmedia.fastfilm.feature.push;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;

import com.kgmedia.fastfilm.R;
import com.kgmedia.fastfilm.common.jsbridge.BridgeDataUtils;
import com.kgmedia.fastfilm.core.model.PushMessageInfo;
import com.kgmedia.fastfilm.core.model.UmengPushBody;
import com.kgmedia.fastfilm.feature.main.MainActivity;
import com.kgmedia.fastfilm.router.AppRouter;
import com.nhsd.basic.utils.Logger;
import com.umeng.message.UmengNotifyClick;
import com.umeng.message.entity.UMessage;

public class MfrMessageActivity extends Activity {
    private static final String TAG = "MfrMessageActivity";

    private final UmengNotifyClick mNotificationClick = new UmengNotifyClick() {
        @Override
        public void onMessage(UMessage msg) {
            final String body = msg.getRaw().toString();
            Logger.d(TAG, "body: " + body);
            if (!TextUtils.isEmpty(body)) {//没有验证过
                UmengPushBody umengPushBody = BridgeDataUtils.parseJson(body, UmengPushBody.class);
                if (umengPushBody == null) {
                    toMainActivity();
                    finish();
                    return;
                }
                UmengPushBody.BodyDTO bodyDTO = umengPushBody.getBody();
                if (bodyDTO == null) {
                    toMainActivity();
                    finish();
                    return;
                }
                toMainActivity();
                String messageInfoString = bodyDTO.getCustom();
                PushMessageInfo messageInfo = BridgeDataUtils.parseJson(messageInfoString, PushMessageInfo.class);
                if (messageInfo != null && messageInfo.getPushMsgType() == PushMessageInfo.NORMAL) {
                    String jumpUri = messageInfo.getPushMsgJumpUri();
                    if (!TextUtils.isEmpty(jumpUri)) {
                        AppRouter.jump(jumpUri);
                    }
                }
            } else {
                toMainActivity();
            }
        }
    };

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.activity_push);
        mNotificationClick.onCreate(this, getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        mNotificationClick.onNewIntent(intent);
    }


    private void toMainActivity() {
        Intent intent2 = new Intent(this, MainActivity.class);
        intent2.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent2);
    }
}
