package com.kgmedia.fastfilm.core.manager;


import android.app.Notification;
import android.app.NotificationChannel;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;
import android.widget.RemoteViews;

import com.kgmedia.fastfilm.App;
import com.kgmedia.fastfilm.R;
import com.kgmedia.fastfilm.common.Constants;
import com.kgmedia.fastfilm.common.jsbridge.BridgeDataUtils;
import com.kgmedia.fastfilm.core.Config;
import com.kgmedia.fastfilm.core.Events;
import com.kgmedia.fastfilm.core.User;
import com.kgmedia.fastfilm.core.model.PushMessageInfo;
import com.kgmedia.fastfilm.feature.main.MainActivity;
import com.nhsd.basic.bus.FlowEventBus;
import com.nhsd.basic.utils.Logger;
import com.umeng.message.MsgConstant;
import com.umeng.message.PushAgent;
import com.umeng.message.UTrack;
import com.umeng.message.UmengMessageHandler;
import com.umeng.message.UmengNotificationClickHandler;
import com.umeng.message.api.UPushRegisterCallback;
import com.umeng.message.common.UPushNotificationChannel;
import com.umeng.message.entity.UMessage;

import org.android.agoo.honor.HonorRegister;
import org.android.agoo.huawei.HuaWeiRegister;
import org.android.agoo.oppo.OppoRegister;
import org.android.agoo.vivo.VivoRegister;
import org.android.agoo.xiaomi.MiPushRegistar;

/*推送tag和alias管理*/
public class PushManager {
    public static final String ALIAS_TYPE = "FastFilmAliasType";
    private static final String TAG = PushManager.class.getSimpleName();

    private final Context mContext;
    private PushAgent mPushAgent;
    private static IRouter routerImpl;

    // 2. 提供注入方法 (在 App 初始化时调用)
    public static void initRouter(IRouter router) {
        routerImpl = router;
    }
    public PushManager(Context context) {
        this.mContext = context;
        mPushAgent = PushAgent.getInstance(mContext);

    }

    public static PushManager getInstance() {
        return App.getInstance().getPushManager();
    }

    public void initPushManager() {
        //UMConfigure.init(this, UMConfigure.DEVICE_TYPE_PHONE, "替换为秘钥信息,服务后台位置：应用管理 -> 应用信息 -> Umeng Message Secret");
        //获取消息推送代理示例
        //sdk开启通知声音
        //pushAgent.setNotificationPlaySound(MsgConstant.NOTIFICATION_PLAY_SDK_ENABLE);
        // sdk关闭通知声音
        // mPushAgent.setNotificationPlaySound(MsgConstant.NOTIFICATION_PLAY_SDK_DISABLE);

        // 通知声音由服务端控制
        mPushAgent.setNotificationPlaySound(MsgConstant.NOTIFICATION_PLAY_SERVER);
        //注册推送服务，每次调用register方法都会回调该接口
        mPushAgent.register(new UPushRegisterCallback() {

            @Override
            public void onSuccess(String deviceToken) {
                Logger.e(TAG, "注册deviceToken：-------->  " + ",deviceToken:" + deviceToken);
                Config.get().storeDeviceToken(deviceToken);
                //注册成功会返回deviceToken deviceToken是推送消息的唯一标志
                if (!User.get().isVisitor()) {
                    setAlias(User.get().getId());
                    addDeviceTags();
                }
            }

            @Override
            public void onFailure(String s, String s1) {
                Logger.e(TAG, "注册失败：-------->  " + "s:" + s + ",s1:" + s1);
            }
        });
        mPushAgent.setMessageHandler(messageHandler);
        /*
         * 自定义行为的回调处理，参考文档：高级功能-通知的展示及提醒-自定义通知打开动作
         * UmengNotificationClickHandler是在BroadcastReceiver中被调用，故
         * 如果需启动Activity，需添加Intent.FLAG_ACTIVITY_NEW_TASK
         */
        mPushAgent.setNotificationClickHandler(notificationClickHandler);
        mPushAgent.setDisplayNotificationNumber(5);
        mPushAgent.setThirdTokenCallback((type, token) -> Logger.e(TAG, "厂商token：-------->  " + ",token:" + token));
        UPushNotificationChannel.getDefaultMode(mContext);
        MiPushRegistar.register(mContext, "2882303761520335315", "5282033524315", false);
        HuaWeiRegister.register(mContext);
        OppoRegister.register(mContext, "63c2404b8ef9487aa86c6a6d196635c0", "4e2460ea3a574abcb122f81c88289f7a");
        VivoRegister.register(mContext);
        HonorRegister.register(mContext);
    }


    public void setAlias(String id) {
        //别名绑定，将某一类型的别名ID绑定至某设备，老的绑定设备信息被覆盖，别名ID和deviceToken是一对一的映射关系
        mPushAgent.setAlias(id, ALIAS_TYPE, new UTrack.ICallBack() {
            @Override
            public void onMessage(boolean isSuccess, String message) {
                Logger.e(TAG, "注册设置别名：-------->  " + ",isSuccess:" + isSuccess + ",message:" + message);
            }
        });
    }

    public void deleteAlias() {
        //final String uniqueId = Config.get().getRole() == Config.ROLE_EMPLOYEE ? User.get().getEmployeeId() : User.get().getId();
        //Logger.e(TAG, "删除推送别名：-------->  " + "uniqueId:" + uniqueId);
        //别名删除，将某一类型的别名ID绑定至某设备，老的绑定设备信息被覆盖，别名ID和deviceToken是一对一的映射关系
        mPushAgent.deleteAlias(User.get().getId(), ALIAS_TYPE, (UTrack.ICallBack) (isSuccess, message) -> Logger.e(TAG, "删除推送别名：-------->  " + "isSuccess:" + isSuccess + ",message:" + message));
    }

    UmengMessageHandler messageHandler = new UmengMessageHandler() {

        /**
         * 自定义消息的回调方法
         */
        @Override
        public void dealWithCustomMessage(final Context context, final UMessage msg) {
            Logger.e(TAG, "dealWithCustomMessage:" + msg.custom);
        }

        @Override
        public Notification getNotification(Context context, UMessage msg) {
            Logger.e(TAG, "收到消息：-------->  " + "msg.custom:" + msg.custom + "," + msg.title);
            if (!TextUtils.isEmpty(msg.custom)) {
                PushMessageInfo messageInfo = BridgeDataUtils.parseJson(msg.custom, PushMessageInfo.class);
                if (messageInfo.getPushMsgType() == PushMessageInfo.WARNING) {
                    FlowEventBus.postSync(new Events.PushEvent(Events.PushEvent.MSG_WARNING, messageInfo.getPushMsgContent(), messageInfo.getMsgId()));
                }
                try {
                    Uri uri = Uri.parse(messageInfo.getPushMsgJumpUri());
                    final String host = uri.getHost();
                    if (Constants.APP_SCHEME.equals(uri.getScheme()) && !TextUtils.isEmpty(host)) {
                        switch (host) {
                            case "musicGenerationFailed":
                                FlowEventBus.postSync(new Events.TrackCreatedEvent());
                                break;
                            case "bookGenerated":
                                //歌曲生成弹窗跳转链接 xiaoyuanmc://musicGenerated?musicId=...&musicTitle=...
                                routerImpl.openFromPush(context,messageInfo.getPushMsgJumpUri());

                                break;
                            default:
                                break;
                        }
                    }
                } catch (Throwable e) {
                }
            }
            switch (msg.builder_id) {
                case 1:
                    Notification.Builder builder;
                    if (Build.VERSION.SDK_INT >= 26) {
                        NotificationChannel channel = UPushNotificationChannel.getDefaultMode(context);
                        builder = new Notification.Builder(context, channel.getId());
                    } else {
                        builder = new Notification.Builder(context);
                    }
                    RemoteViews myNotificationView = new RemoteViews(context.getPackageName(), R.layout.notification_view);
                    myNotificationView.setTextViewText(R.id.notification_title, msg.title);
                    myNotificationView.setTextViewText(R.id.notification_text, msg.text);
                    myNotificationView.setImageViewBitmap(R.id.notification_large_icon, getLargeIcon(context, msg));
                    myNotificationView.setImageViewResource(R.id.notification_small_icon, getSmallIconId(context, msg));
                    builder.setContent(myNotificationView)
                            .setSmallIcon(getSmallIconId(context, msg))
                            .setTicker(msg.ticker)
                            .setAutoCancel(true);
                    return builder.getNotification();
                default:
                    //默认为0，若填写的builder_id并不存在，也使用默认。
                    return super.getNotification(context, msg);
            }
        }
    };

    UmengNotificationClickHandler notificationClickHandler = new UmengNotificationClickHandler() {
        @Override
        public void launchApp(Context context, UMessage msg) {
            super.launchApp(context, msg);
            if (!TextUtils.isEmpty(msg.custom)) {
                PushMessageInfo messageInfo = BridgeDataUtils.parseJson(msg.custom, PushMessageInfo.class);
                Logger.e(TAG, "点击通知栏消息：-------->  " + "messageInfo:" + messageInfo.toString());
                if (messageInfo.getPushMsgType() == PushMessageInfo.NORMAL) {
                    String jumpUri = messageInfo.getPushMsgJumpUri();
                    if (!TextUtils.isEmpty(jumpUri)) {
                        routerImpl.navigate(context,jumpUri);
                    } else {
                        Intent intent = new Intent(context, MainActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        context.startActivity(intent);
                    }
                }
            } else {
                Intent intent = new Intent(context, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            }
        }

        @Override
        public void openUrl(Context context, UMessage msg) {
            super.openUrl(context, msg);
        }

        @Override
        public void openActivity(Context context, UMessage msg) {
            super.openActivity(context, msg);
        }

        @Override
        public void dealWithCustomAction(Context context, UMessage msg) {
            Logger.e(TAG, "dealWithCustomAction:" + msg.custom);
            if (!TextUtils.isEmpty(msg.custom)) {
                PushMessageInfo messageInfo = BridgeDataUtils.parseJson(msg.custom, PushMessageInfo.class);
                if (messageInfo.getPushMsgType() == PushMessageInfo.NORMAL) {
                    String jumpUri = messageInfo.getPushMsgJumpUri();
                    if (!TextUtils.isEmpty(jumpUri)) {
                        routerImpl.navigate(context,jumpUri);
                    }
                }
            }
        }
    };

    public void addDeviceTags() {
        PushTagManager.updateTags();
    }

}
