package com.shiyuan.fastclaw.core;

import android.app.Application;
import android.content.Context;

import com.shiyuan.fastclaw.BuildConfig;
import com.shiyuan.fastclaw.core.manager.IRouter;
import com.shiyuan.fastclaw.core.manager.PushManager;
import com.nhsd.basic.Session;
import com.nhsd.share.KS;
import com.nhsd.share.XHS;
import com.umeng.analytics.MobclickAgent;
import com.umeng.commonsdk.UMConfigure;
import com.umeng.message.PushAgent;
import com.umeng.socialize.PlatformConfig;


public class UmInitConfig {

    private PushManager pushManager;
    private Context context;

    public UmInitConfig(Context context) {
        this.context = context;
        pushManager = new PushManager(context);
        UMConfigure.setLogEnabled(BuildConfig.DEBUG);
        UMConfigure.preInit(context, BuildConfig.UMENG_APP_KEY, Session.getInstance().getChannel());
    }

    public void init(Application application, IRouter router) {
        UMConfigure.init(context, BuildConfig.UMENG_APP_KEY, Session.getInstance().getChannel(), UMConfigure.DEVICE_TYPE_PHONE, BuildConfig.UMENG_SECRET);
        PushAgent.getInstance(context).setPackageListenerEnable(false);
        String fileProvider = BuildConfig.APPLICATION_ID + ".fileprovider";
        PlatformConfig.setFileProvider(fileProvider);
        //初始化分享
        //微信 appid appsecret
        PlatformConfig.setWeixin(BuildConfig.WX_APP_ID, BuildConfig.WX_APP_SECRET);
        //抖音
        PlatformConfig.setBytedance(BuildConfig.DY_CLIENT_KEY, BuildConfig.DY_CLIENT_KEY, "", BuildConfig.APPLICATION_ID + ".fileprovider");
        PlatformConfig.setWXFileProvider(fileProvider);
        //新浪微博 appkey appsecret
        PlatformConfig.setSinaWeibo(BuildConfig.WB_APP_KEY, BuildConfig.WB_APP_SECRET, BuildConfig.WB_URL);
        PlatformConfig.setSinaFileProvider(fileProvider);
        // QQ和Qzone appid appkey
        //qq官方sdk3.5.7需要调用授权api才可以正常使用授权和分享功能 //QQ官方sdk授权 Tencent.setIsPermissionGranted(true);
        //Tencent.setIsPermissionGranted(true);
        PlatformConfig.setQQZone(BuildConfig.QQ_APP_ID, BuildConfig.QQ_APP_KEY);
        PlatformConfig.setQQFileProvider(BuildConfig.APPLICATION_ID + ".fileprovider");

        //小红书
        XHS.init(context, "d2f14980e0471b57a09c6a3471e4b933", BuildConfig.APPLICATION_ID + ".fileprovider");
        //快手
        KS.init(application);

        //PushSDK初始化(如使用推送SDK，必须调用此方法)
        initUpush(context, router);

        //统计SDK是否支持采集在子进程中打点的自定义事件，默认不支持
        UMConfigure.setProcessEvent(true);//支持多进程打点

        // 页面数据采集模式
        // setPageCollectionMode接口参数说明：
        // 1. MobclickAgent.PageMode.AUTO: 建议大多数用户使用本采集模式，SDK在此模式下自动采集Activity
        // 页面访问路径，开发者不需要针对每一个Activity在onResume/onPause函数中进行手动埋点。在此模式下，
        // 开发者如需针对Fragment、CustomView等自定义页面进行页面统计，直接调用MobclickAgent.onPageStart/
        // MobclickAgent.onPageEnd手动埋点即可。此采集模式简化埋点工作，唯一缺点是在Android 4.0以下设备中
        // 统计不到Activity页面数据和各类基础指标(提示：目前Android 4.0以下设备市场占比已经极小)。

        // 2. MobclickAgent.PageMode.MANUAL：对于要求在Android 4.0以下设备中也能正常采集数据的App,可以使用
        // 本模式，开发者需要在每一个Activity的onResume函数中手动调用MobclickAgent.onResume接口，在Activity的
        // onPause函数中手动调用MobclickAgent.onPause接口。在此模式下，开发者如需针对Fragment、CustomView等
        // 自定义页面进行页面统计，直接调用MobclickAgent.onPageStart/MobclickAgent.onPageEnd手动埋点即可。

        // 如下两种LEGACY模式不建议首次集成友盟统计SDK的新用户选用。
        // 如果您是友盟统计SDK的老用户，App需要从老版本统计SDK升级到8.0.0版本统计SDK，
        // 并且：您的App之前MobclickAgent.onResume/onPause接口埋点分散在所有Activity
        // 中，逐个删除修改工作量很大且易出错。
        // 若您的App符合以上特征，可以选用如下两种LEGACY模式，否则不建议继续使用LEGACY模式。
        // 简单来说，升级SDK的老用户，如果不需要手动统计页面路径，选用LEGACY_AUTO模式。
        // 如果需要手动统计页面路径，选用LEGACY_MANUAL模式。
        // 3. MobclickAgent.PageMode.LEGACY_AUTO: 本模式适合不需要对Fragment、CustomView
        // 等自定义页面进行页面访问统计的开发者，SDK仅对App中所有Activity进行页面统计，开发者需要在
        // 每一个Activity的onResume函数中手动调用MobclickAgent.onResume接口，在Activity的
        // onPause函数中手动调用MobclickAgent.onPause接口。此模式下MobclickAgent.onPageStart
        // ,MobclickAgent.onPageEnd这两个接口无效。

        // 4. MobclickAgent.PageMode.LEGACY_MANUAL: 本模式适合需要对Fragment、CustomView
        // 等自定义页面进行手动页面统计的开发者，开发者如需针对Fragment、CustomView等
        // 自定义页面进行页面统计，直接调用MobclickAgent.onPageStart/MobclickAgent.onPageEnd
        // 手动埋点即可。开发者还需要在每一个Activity的onResume函数中手动调用MobclickAgent.onResume接口，
        // 在Activity的onPause函数中手动调用MobclickAgent.onPause接口。
        MobclickAgent.setPageCollectionMode(MobclickAgent.PageMode.AUTO);

    }

    /**
     * 主进程和子进程channel都需要进行初始化和注册
     */
    private void initUpush(Context context, IRouter router) {
        PushManager.initRouter(router);
        pushManager.initPushManager();
    }

    public PushManager getPushManager() {
        return pushManager;
    }
}
