package com.kgmedia.fastfilm.core.manager;

import static androidx.lifecycle.Lifecycle.State.DESTROYED;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;

import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.GenericLifecycleObserver;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;

import com.blankj.utilcode.util.NetworkUtils;
import com.kgmedia.fastfilm.App;
import com.nhsd.basic.ext.ToastExtKt;
import com.nhsd.basic.utils.Logger;
import com.nhsd.share.KS;
import com.nhsd.share.OtherShareMedia;
import com.nhsd.share.ShareContent;
import com.nhsd.share.XHS;
import com.umeng.socialize.ShareAction;
import com.umeng.socialize.UMAuthListener;
import com.umeng.socialize.UMShareAPI;
import com.umeng.socialize.UMShareListener;
import com.umeng.socialize.bean.SHARE_MEDIA;
import com.umeng.socialize.media.UMImage;
import com.umeng.socialize.media.UMVideo;
import com.umeng.socialize.media.UMWeb;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Map;


/**
 * 统一的分享入口
 */
@SuppressWarnings("RestrictedApi")
public class ShareManager {

    private Activity mContext;
    public static final String WECHAT_PACKAGE_NAME = "com.tencent.mm";

    public ShareManager(Activity context) {
        this.mContext = context;
        //initUmengSDK();
        if (context instanceof FragmentActivity) {
            FragmentActivity activity = (FragmentActivity) context;
            activity.getLifecycle().addObserver(new GenericLifecycleObserver() {
                @Override
                public void onStateChanged(LifecycleOwner source, Lifecycle.Event event) {
                    if (activity.getLifecycle().getCurrentState() == DESTROYED) {
                        activity.getLifecycle().removeObserver(this);
                        if (App.getInstance().hasAgree()) {
                            UMShareAPI.get(activity).release();
                        }
                    }
                }
            });
        }
    }


    public void doShare(String title, String description, Bitmap shareImage, @DrawableRes int defaultShareImage, String url, final SHARE_MEDIA platform, final UMShareListener umShareListener) {
        //判断是否有网络
        if (!NetworkUtils.isConnected()) {
            umShareListener.onCancel(platform);
            ToastExtKt.showToast("你的网络状态异常,请稍后再试");
            return;
        }
        UMImage image;
        if (shareImage != null) {
            image = new UMImage(mContext, shareImage);
        } else {
            image = new UMImage(mContext, defaultShareImage);
        }

        final ShareAction shareAction = new ShareAction(mContext);
        switch (platform) {
            // 微信朋友圈
            case WEIXIN_CIRCLE:
                // 支持微信朋友圈
                shareAction.setPlatform(SHARE_MEDIA.WEIXIN_CIRCLE);
                break;
            // 微信
            case WEIXIN:
                // 添加微信平台
                shareAction.setPlatform(SHARE_MEDIA.WEIXIN);
                break;
            // QQ
            case QQ:
//			Utils.isAppInstalled(mContext, "com.tencent.mobileqq", new Utils.Callback() {
//				@Override
//				public void callBack(boolean exist) {
//					if (exist) {
                // 参数1为当前Activity， 参数2为开发者在QQ互联申请的APP
                // ID，参数3为开发者在QQ互联申请的APP kEY.
                shareAction.setPlatform(SHARE_MEDIA.QQ);
//					} else {
//						ToastMaster.shortToast("你还没有安装手机QQ");
//					}
//				}
//			});
                break;
            case QZONE:
                shareAction.setPlatform(SHARE_MEDIA.QZONE);
                break;

            // 新浪微博
            case SINA:
                shareAction.setPlatform(SHARE_MEDIA.SINA);
                break;
            //default
            default:
                shareAction.setPlatform(SHARE_MEDIA.SMS);
                break;
        }
        UMWeb web = new UMWeb(url);
        web.setTitle(title);//标题
        web.setThumb(image);  //缩略图
        web.setDescription(description);//描述

        shareAction.setCallback(new WeakReferenceUMShareListener(umShareListener))
                .withMedia(web)
                .share();
    }

    public void doShare(String imageUrl, final SHARE_MEDIA platform, final UMShareListener umShareListener) {

        UMImage image = new UMImage(mContext, imageUrl);
        image.setThumb(image);
        final ShareAction shareAction = new ShareAction(mContext);
        switch (platform) {
            // 微信朋友圈
            case WEIXIN_CIRCLE:
                // 支持微信朋友圈
                shareAction.setPlatform(SHARE_MEDIA.WEIXIN_CIRCLE);
                break;
            // 微信
            case WEIXIN:
                // 添加微信平台
                shareAction.setPlatform(SHARE_MEDIA.WEIXIN);
                break;
            // QQ
            case QQ:
//			Utils.isAppInstalled(mContext, "com.tencent.mobileqq", new Utils.Callback() {
//				@Override
//				public void callBack(boolean exist) {
//					if (exist) {
                // 参数1为当前Activity， 参数2为开发者在QQ互联申请的APP
                // ID，参数3为开发者在QQ互联申请的APP kEY.
                shareAction.setPlatform(SHARE_MEDIA.QQ);
//					} else {
//						ToastMaster.shortToast("你还没有安装手机QQ");
//					}
//				}
//			});
                break;
            case QZONE:
                shareAction.setPlatform(SHARE_MEDIA.QZONE);
                break;

            // 新浪微博
            case SINA:
                shareAction.setPlatform(SHARE_MEDIA.SINA);
                break;
            //default
            default:
                shareAction.setPlatform(SHARE_MEDIA.SMS);
                break;
        }
        shareAction.setCallback(new WeakReferenceUMShareListener(umShareListener)).withText("谜圈").withMedia(image).share();
    }

    public void doShare(Bitmap bitmap, final SHARE_MEDIA platform, final UMShareListener umShareListener) {
        //判断是否有网络
        if (!NetworkUtils.isConnected()) {
            umShareListener.onCancel(platform);
            ToastExtKt.showToast("你的网络状态异常,请稍后再试");
            return;
        }

        UMImage image = new UMImage(mContext, bitmap);
        image.setThumb(image);
        final ShareAction shareAction = new ShareAction(mContext);
        switch (platform) {
            // 微信朋友圈
            case WEIXIN_CIRCLE:
                // 支持微信朋友圈
                shareAction.setPlatform(SHARE_MEDIA.WEIXIN_CIRCLE);
                break;
            // 微信
            case WEIXIN:
                // 添加微信平台
                shareAction.setPlatform(SHARE_MEDIA.WEIXIN);
                break;
            // QQ
            case QQ:
//			Utils.isAppInstalled(mContext, "com.tencent.mobileqq", new Utils.Callback() {
//				@Override
//				public void callBack(boolean exist) {
//					if (exist) {
                // 参数1为当前Activity， 参数2为开发者在QQ互联申请的APP
                // ID，参数3为开发者在QQ互联申请的APP kEY.
                shareAction.setPlatform(SHARE_MEDIA.QQ);
//					} else {
//						ToastMaster.shortToast("你还没有安装手机QQ");
//					}
//				}
//			});
                break;
            case QZONE:
                shareAction.setPlatform(SHARE_MEDIA.QZONE);
                break;

            // 新浪微博
            case SINA:
                shareAction.setPlatform(SHARE_MEDIA.SINA);
                break;
            //default
            default:
                shareAction.setPlatform(SHARE_MEDIA.SMS);
                break;
        }
        shareAction.setCallback(new WeakReferenceUMShareListener(umShareListener)).withMedia(image).share();
    }

    public void doShare(File imageFile, final SHARE_MEDIA platform, final UMShareListener umShareListener) {
        //判断是否有网络
        if (!NetworkUtils.isConnected()) {
            umShareListener.onCancel(platform);
            ToastExtKt.showToast("你的网络状态异常,请稍后再试");
            return;
        }

//        //判断应用是否安装
//        switch (platform) {
//            case WEIXIN:
//            case WEIXIN_CIRCLE:
//                if (!Utils.isPackageInstalled(mContext, "com.tencent.mm")) {
//                    T.showShort("请安装微信客户端");
//                    umShareListener.onCancel(platform);
//                    return;
//                }
//                break;
//
//            case QQ:
//            case QZONE:
//                if (!Utils.isPackageInstalled(mContext, "com.tencent.mobileqq")) {
//                    umShareListener.onCancel(platform);
//                    T.showShort("请安装QQ客户端");
//                    return;
//                }
//                break;
//            case SINA:
//                if (!Utils.isPackageInstalled(mContext, "com.sina.weibo")) {
//                    umShareListener.onCancel(platform);
//                    T.showShort("请安装新浪微博客户端");
//                    return;
//                }
//                break;
//        }
        UMImage image = new UMImage(mContext, imageFile);
        image.setThumb(image);
        final ShareAction shareAction = new ShareAction(mContext);
        switch (platform) {
            // 微信朋友圈
            case WEIXIN_CIRCLE:
                // 支持微信朋友圈
                shareAction.setPlatform(SHARE_MEDIA.WEIXIN_CIRCLE);
                break;
            // 微信
            case WEIXIN:
                // 添加微信平台
                shareAction.setPlatform(SHARE_MEDIA.WEIXIN);
                break;
            // QQ
            case QQ:
//			Utils.isAppInstalled(mContext, "com.tencent.mobileqq", new Utils.Callback() {
//				@Override
//				public void callBack(boolean exist) {
//					if (exist) {
                // 参数1为当前Activity， 参数2为开发者在QQ互联申请的APP
                // ID，参数3为开发者在QQ互联申请的APP kEY.
                shareAction.setPlatform(SHARE_MEDIA.QQ);
//					} else {
//						ToastMaster.shortToast("你还没有安装手机QQ");
//					}
//				}
//			});
                break;
            case QZONE:
                shareAction.setPlatform(SHARE_MEDIA.QZONE);
                break;

            // 新浪微博
            case SINA:
                shareAction.setPlatform(SHARE_MEDIA.SINA);
                break;
            //default
            default:
                shareAction.setPlatform(SHARE_MEDIA.SMS);
                break;
        }
        shareAction.setCallback(new WeakReferenceUMShareListener(umShareListener)).withMedia(image).share();
    }

    public static final String getPlatName(SHARE_MEDIA platform) {
        String platName = "";
        switch (platform) {
            case WEIXIN:
                platName = "微信好友";
                break;
            case WEIXIN_CIRCLE:
                platName = "微信朋友圈";
                break;

            case QQ:
                platName = "QQ好友";
                break;

            case QZONE:
                platName = "QQ空间";
                break;

            case SINA:
                platName = "新浪微博";
                break;

        }
        return platName;
    }

    public void deleteOauth() {
        if (UMShareAPI.get(mContext).isAuthorize(mContext, SHARE_MEDIA.WEIXIN)) {
            UMShareAPI.get(mContext).deleteOauth(mContext, SHARE_MEDIA.WEIXIN, null);
        }
        if (UMShareAPI.get(mContext).isAuthorize(mContext, SHARE_MEDIA.QQ)) {
            UMShareAPI.get(mContext).deleteOauth(mContext, SHARE_MEDIA.QQ, null);
        }
    }

    public void login(final SHARE_MEDIA platform, final UmengLoginListener listener) {
        if (platform == SHARE_MEDIA.QQ || platform == SHARE_MEDIA.SINA) {
            auth(platform, listener);
        } else if (platform == SHARE_MEDIA.WEIXIN) {
            if (UMShareAPI.get(mContext).isInstall(mContext, SHARE_MEDIA.WEIXIN)) {
                auth(platform, listener);
            } else {
                ToastExtKt.showToast("你还没有安装微信，请安装后再使用微信登录");
            }
        }
    }

    private void auth(final SHARE_MEDIA platform, final UmengLoginListener listener) {
        if (UMShareAPI.get(mContext).isAuthorize(mContext, platform)) {
            UMShareAPI.get(mContext).deleteOauth(mContext, platform, new UMAuthListener() {
                @Override
                public void onStart(SHARE_MEDIA share_media) {
                    Logger.i("AUTH_", "onstart");
                }

                @Override
                public void onComplete(SHARE_MEDIA share_media, int i, Map<String, String> map) {
                    Logger.i("AUTH_", "onComplete");
                    doAuth(platform, listener);
                }

                @Override
                public void onError(SHARE_MEDIA share_media, int i, Throwable throwable) {
                    Logger.i("AUTH_", "onError");
                }

                @Override
                public void onCancel(SHARE_MEDIA share_media, int i) {
                    Logger.i("AUTH_", "onCancel");
                }
            });
        } else {
            doAuth(platform, listener);
        }
    }

    private void doAuth(final SHARE_MEDIA platform, final UmengLoginListener listener) {
        UMShareAPI.get(mContext).getPlatformInfo(mContext, platform, new UMAuthListener() {
            @Override
            public void onStart(SHARE_MEDIA share_media) {
                if (listener != null) {
                    listener.onStart(share_media);
                }
            }

            @Override
            public void onComplete(SHARE_MEDIA share_media, int i, Map<String, String> map) {
                if (listener != null) {
                    listener.onAuthed(share_media, map);
                }
            }

            @Override
            public void onError(SHARE_MEDIA share_media, int i, Throwable throwable) {
                if (listener != null) {
                    listener.onError(new Exception(throwable), share_media, false);
                }
            }

            @Override
            public void onCancel(SHARE_MEDIA share_media, int i) {
                if (listener != null) {
                    listener.onError(new Exception("On Cancel"), share_media, true);
                }
            }
        });
    }

    public static void onActivityResult(int requestCode, int resultCode, Intent data) {
        UMShareAPI.get(App.getInstance()).onActivityResult(requestCode, resultCode, data);
    }

    public void release() {
        if (App.getInstance().hasAgree()) {
            UMShareAPI.get(mContext).release();
        }
    }

    public void shareImage(@NotNull ShareContent shareContent, @NotNull File file, @Nullable SHARE_MEDIA media, @Nullable OtherShareMedia otherShareMedia, @Nullable UMShareListener shareListener) {
        if (otherShareMedia != null) {
            switch (otherShareMedia) {
                case XHS:
                    XHS.shareImage(shareContent, file, shareListener);
                    break;
                case KS:
                    KS.shareImage(shareContent, file, shareListener);
                    break;
            }
            return;
        }
        new ShareAction(mContext)
                .setPlatform(media)
                .withMedia(new UMImage(App.getInstance(), file))
                .withText(shareContent.getContent())
                .setCallback(new WeakReferenceUMShareListener(shareListener))
                .share();
    }

    /**
     * @param shareContent   分享内容
     * @param file           分享的视频文件
     * @param screenshotFile 分享的视频封面
     * @param media          分享渠道
     * @param otherMedia     Umeng默认不支持的其他渠道
     * @param shareListener  分享回调
     */
    public void shareVideo(@NotNull ShareContent shareContent, @NotNull File file, @Nullable File screenshotFile, @Nullable SHARE_MEDIA media, @Nullable OtherShareMedia otherMedia, @Nullable UMShareListener shareListener) {
        if (otherMedia != null) {
            switch (otherMedia) {
                case XHS:
                    XHS.shareVideo(shareContent, file, screenshotFile, shareListener);
                    break;
                case KS:
                    KS.shareVideo(shareContent, file, screenshotFile, shareListener);
                    break;
            }
            return;
        }
        new ShareAction(mContext)
                .setPlatform(media)
                .withMedia(new UMVideo(file))
                .withText(shareContent.getContent())
                .setCallback(new WeakReferenceUMShareListener(shareListener))
                .share();
    }

    public interface UmengLoginListener {


        void onStart(SHARE_MEDIA platform);

        /**
         * @param data 第三方用户信息
         */
        void onAuthed(SHARE_MEDIA platform, Map<String, String> data);

        /**
         * @param e        异常
         * @param platform 授权平台
         * @param onCancel 是否用户取消
         */
        void onError(Exception e, SHARE_MEDIA platform, boolean onCancel);
    }

}
