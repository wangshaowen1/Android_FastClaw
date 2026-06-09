package com.shiyuan.fastclaw.core;


import com.nhsd.basic.bus.AppEvent;
import com.shiyuan.fastclaw.core.network.apis.response.CommonItems;

/**
 * Created by ymh on 2016/7/1 0001.
 */
public class Events {

    public static class MainTabEvent implements AppEvent {
        public static int TAB_HOME = 1;
        public static int TAB_MY_WORKS = 2;
        public static int TAB_MINE = 3;

        private int type;

        public MainTabEvent(int type) {
            this.type = type;
        }

        public int getType() {
            return type;
        }

        public void setType(int type) {
            this.type = type;
        }
    }

    public static class UserDataEvent implements AppEvent {
        public final String[] keys;
        public String data;

        public UserDataEvent(String... keys) {
            this.keys = keys;
        }

        public String getData() {
            return data;
        }

        public UserDataEvent setData(String data) {
            this.data = data;
            return this;
        }

        public String keyAt(int index) {
            return keys[index];
        }

        public boolean isUpdateAll() {
            return keys == null || keys.length == 0;
        }

        public boolean contain(String key) {
            if (keys == null || keys.length == 0) {
                return false;
            } else {
                for (int i = 0; i < keys.length; i++) {
                    if (key.equals(keys[i])) {
                        return true;
                    }
                }
                return false;
            }
        }

        public boolean containOnly(String key) {
            if (keys == null) {
                return false;
            }
            if (keys.length == 1 && key.equals(keys[0])) {
                return true;
            }
            return false;
        }
    }


    public static class UserChangedEvent implements AppEvent {
        public static final int SIGN_IN = 0;
        public static final int SIGN_OUT = 1;
        // 新增：区分是用户主动退出，还是被系统踢出/过期
        public static final int TOKEN_EXPIRED = 3;
        public final int eventType;

        public UserChangedEvent(int eventType) {
            this.eventType = eventType;
        }
    }

    public static class DownloadContractEvent implements AppEvent {

        public String pdfFileName;

        public DownloadContractEvent(String pdfFileName) {
            this.pdfFileName = pdfFileName;
        }

        public String getPdfFileName() {
            return pdfFileName;
        }

        public void setPdfFileName(String pdfFileName) {
            this.pdfFileName = pdfFileName;
        }
    }

    /**
     * 配置更改
     */
    public static class ConfigChangeEvent implements AppEvent {
        public ConfigChangeEvent(String[] changedKeys) {
            this.changedKeys = changedKeys;
        }

        public final String[] changedKeys;

        public String keyAt(int index) {
            return changedKeys[index];
        }

        public boolean contain(String key) {
            if (changedKeys == null || changedKeys.length == 0) {
                return true;
            } else {
                for (int i = 0; i < changedKeys.length; i++) {
                    if (key.equals(changedKeys[i])) {
                        return true;
                    }
                }
                return false;
            }
        }

        public boolean containOnly(String key) {
            if (changedKeys == null) {
                return false;
            }
            if (changedKeys.length == 1 && key.equals(changedKeys[0])) {
                return true;
            }
            return false;
        }
    }

    public static class PushEvent implements AppEvent {
        public static final String MSG_CENTER = "push_msg_center";
        public static final String MSG_WARNING = "msg_warning";
        public final String eventType;
        public final String content;
        public final long msgId;

        public PushEvent(String eventType, String content, long msgId) {
            this.eventType = eventType;
            this.content = content;
            this.msgId = msgId;
        }
    }


    /**
     * 歌曲创建事件
     */
    public static class TrackCreatedEvent implements AppEvent {
        public final String musicId;

        public TrackCreatedEvent(String musicId) {
            this.musicId = musicId;
        }

        public TrackCreatedEvent() {
            this(null);
        }
    }

    public static class DownloadProgressEvent implements AppEvent {
        public final int percent;
        public final String url;
        public final String path;

        public DownloadProgressEvent(String url, int percent, String path) {
            this.percent = percent;
            this.url = url;
            this.path = path;
        }
    }

    //apk更新
    public static class ApkRenewal implements AppEvent {

        public Boolean apkRenewal;

        public ApkRenewal(Boolean apkRenewal) {
            this.apkRenewal = apkRenewal;
        }
    }

    /**
     * 手机号绑定成功
     */
    public static class BindingSuccessfulEvent implements AppEvent {

        public BindingSuccessfulEvent() {
        }
    }

    public static class AgreePolicyEvent implements AppEvent {
        /**
         * type 11/21:手机号 12/22:QQ 13/23:微信
         */
        public final int type;

        public AgreePolicyEvent(int type) {
            this.type = type;
        }
    }

    public static class MainTabDoubleClickEvent implements AppEvent {

        public static final int TAB_HOME = 0;
        public static final int TAB_WORKS = 1;
        public static final int TAB_MINE = 2;
        public static final int TAB_MESSAGE = 3;

        public final int tabType;

        public MainTabDoubleClickEvent(int tabType) {
            this.tabType = tabType;
        }
    }


    /**
     * 简历通知
     */
    public static class ResumeEvent implements AppEvent {
        public  Boolean resumeUploadSuccess ;  //简历上传成功
        public ResumeEvent(Boolean resumeUploadSuccess) {
            this.resumeUploadSuccess = resumeUploadSuccess;
        }
    }


    public static class CommonItemsEvent implements AppEvent {
        public CommonItems commonItems ;  //简历上传成功
        public CommonItemsEvent(CommonItems commonItems) {
            this.commonItems = commonItems;
        }
    }

}