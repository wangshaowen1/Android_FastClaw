package com.shiyuan.fastclaw.core.im;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;

import com.netease.nimlib.sdk.mixpush.MixPushMessageHandler;
import com.shiyuan.fastclaw.router.AppLaunchJump;
import com.tulele.im.push.ImMixPushMessageHandler;


import java.util.Map;

/**
 * 云信离线推送点击处理：业务通知按 jumpUri 跳转，聊天通知沿用 IM 模块默认逻辑。
 */
public final class FastClawMixPushMessageHandler implements MixPushMessageHandler {

    private static final String PAYLOAD_JUMP_URI = "pushMsgJumpUri";
    private final ImMixPushMessageHandler defaultHandler = new ImMixPushMessageHandler();

    @Override
    public boolean onNotificationClicked(Context context, Map<String, String> payload) {
        String jumpUri = payload == null ? null : payload.get(PAYLOAD_JUMP_URI);
        if (!TextUtils.isEmpty(jumpUri)) {
            AppLaunchJump.launchJump(Uri.parse(jumpUri));
            return true;
        }
        return defaultHandler.onNotificationClicked(context, payload);
    }

    @Override
    public boolean cleanMixPushNotifications(int pushType) {
        return defaultHandler.cleanMixPushNotifications(pushType);
    }
}
