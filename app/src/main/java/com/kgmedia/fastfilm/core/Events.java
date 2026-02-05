package com.kgmedia.fastfilm.core;


import androidx.annotation.NonNull;

import com.kgmedia.fastfilm.core.network.apis.entity.ShortDramaInfo;
import com.nhsd.basic.bus.AppEvent;

/**
 * Created by ymh on 2016/7/1 0001.
 */
public class Events {


    /**
     * 关闭
     */
    public static class CloseEvent implements AppEvent {
        public static final String FINISH_ALL = "finishAll";
        public String eventType;
        public final String[] ignoreClass;

        public CloseEvent(String eventType, String... ignoreClass) {
            this.eventType = eventType;
            this.ignoreClass = ignoreClass;
        }
    }

    public static class PurchaseEvent implements AppEvent {
        //购买类型会员
        public static int VIP = 1;
        //购买积分
        public static int POINTS = 2;
        //购买能力包
        public static int ABILITY = 3;

        //购买类型
        private int type;

        public PurchaseEvent(int type) {
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


    /**
     * 绘本删除事件
     */
    public static class ShortDeletionEvent implements AppEvent {
        @NonNull
        public final ShortDramaInfo shortDramaInfo;

        public ShortDeletionEvent(@NonNull ShortDramaInfo shortDramaInfo) {
            this.shortDramaInfo = shortDramaInfo;
        }
    }

    /**
     * 绘本信息改变
     */
    public static class ShortChangeEvent implements AppEvent {
        @NonNull
        public final ShortDramaInfo shortDramaInfo;

        public ShortChangeEvent(@NonNull ShortDramaInfo shortDramaInfo) {
            this.shortDramaInfo = shortDramaInfo;
        }
    }

    /**
     * track喜欢事件
     */
    public static class TrackLikeEvent implements AppEvent {
        public final String trackId;
        public final int likeStatus;
        public final int likeNum;


        public TrackLikeEvent(String trackId, int likeStatus, int likeNum) {
            this.trackId = trackId;
            this.likeStatus = likeStatus;
            this.likeNum = likeNum;
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

    /**
     * 生成歌的AI有变化
     */
    public static class ComposeConfigChangeEvent implements AppEvent {
        public int way;

        public ComposeConfigChangeEvent(int way) {
            this.way = way;
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
     * 灵感
     */
    public static class InspirationEvent implements AppEvent {
        public final String inspiration;

        public InspirationEvent(String inspiration) {
            this.inspiration = inspiration;
        }
    }


    /**
     * 手机号绑定成功
     */
    public static class BindingSuccessfulEvent implements AppEvent {

        public BindingSuccessfulEvent() {
        }
    }

    public static class TimbreGeneratedEvent implements AppEvent {

        public final String timbreId;

        public TimbreGeneratedEvent(String timbreId) {
            this.timbreId = timbreId;
        }
    }

    public static class PictureBookGeneratedEvent implements AppEvent {

        public final String pictureBookTemplateId;

        public PictureBookGeneratedEvent(String pictureBookTemplateId) {
            this.pictureBookTemplateId = pictureBookTemplateId;
        }
    }

    public static class PictureBookStartGenerateEvent implements AppEvent {

        public PictureBookStartGenerateEvent() {
        }

        ;
    }

    public static class PictureBookPublishEvent implements AppEvent {

        public final String pictureBookId;

        public PictureBookPublishEvent(String pictureBookId) {
            this.pictureBookId = pictureBookId;
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


    public static class InvitationIncomeEvent implements AppEvent {
        public final int amountOfMoney;

        public InvitationIncomeEvent(int amountOfMoney) {
            this.amountOfMoney = amountOfMoney;
        }
    }

    /**
     * 短剧更新事件
     */
    public static class ShortDramasUpdatedEvent implements AppEvent {
        public final String dramasId;

        public ShortDramasUpdatedEvent(String dramasId) {
            this.dramasId = dramasId;
        }
    }

    /**
     * 修改视频信息
     */
    public static class ShortDramasEditInfoEvent implements AppEvent {
        public final String dramaId;
        public final String dramaName;
        public final String dramaIntro;

        public ShortDramasEditInfoEvent(String dramaId, String dramaName, String dramaIntro) {
            this.dramaId = dramaId;
            this.dramaName = dramaName;
            this.dramaIntro = dramaIntro;
        }
    }

    /**
     * 删除剧集
     */
    public static class DeleteTVSeriesEvent implements AppEvent {
        public final String workId;

        public DeleteTVSeriesEvent(String workId) {
            this.workId = workId;
        }
    }

    /**
     * 字幕配音事件
     */
    public static class DubbingEvent implements AppEvent {
        public final String dramasId;
        public final String timbreId;

        public DubbingEvent(String dramasId, String timbreId) {
            this.dramasId = dramasId;
            this.timbreId = timbreId;
        }
    }

    /**
     * 字幕配音事件
     */
    public static class UseDubbingEvent implements AppEvent {
        public final String dramasId;
        public final String timbreId;

        public UseDubbingEvent(String dramasId, String timbreId) {
            this.dramasId = dramasId;
            this.timbreId = timbreId;
        }
    }

    /**
     * 剧集更新事件
     */
    public static class SeriesUpdatedEvent implements AppEvent {
        public final String seriesId;

        public SeriesUpdatedEvent(String seriesId) {
            this.seriesId = seriesId;
        }
    }

    /**
     * 喜欢
     */
    public static class ShortDramaLikeEvent implements AppEvent {
        public final String shortDramaId;
        public final int likeStatus;

        public ShortDramaLikeEvent(String shortDramaId, int likeStatus) {
            this.shortDramaId = shortDramaId;
            this.likeStatus = likeStatus;
        }
    }

    /**
     * 追更
     */
    public static class SeriesWatchingEvent implements AppEvent {
        public final String seriesId;
        public final int watchingStatus;
        public final String watchingNumStr;

        public SeriesWatchingEvent(String seriesId, int watchingStatus, String watchingNumStr) {
            this.seriesId = seriesId;
            this.watchingStatus = watchingStatus;
            this.watchingNumStr = watchingNumStr;
        }
    }

    public static class UnLockEvent implements AppEvent {

        public final String dramasId;

        public UnLockEvent(String dramasId) {
            this.dramasId = dramasId;
        }
    }

    public static class AutoUnLockSwitchEvent implements AppEvent {

        public final String dramasSeriesId;
        public final boolean autoLock;

        public AutoUnLockSwitchEvent(String dramasSeriesId, boolean autoLock) {
            this.dramasSeriesId = dramasSeriesId;
            this.autoLock = autoLock;
        }
    }

    /**
     * 小说大纲流式输出完成
     */
    public static class ScriptContentGenerateSuccessEvent implements AppEvent {
        public final static int TYPE_INSPIRATION = 1;
        public final static int TYPE_CUSTOMIZED = 2;
        public final static int TYPE_CONTINUE_WRITE = 3;

        public final int type;
        public final String seriesId;

        public ScriptContentGenerateSuccessEvent(int type, String seriesId) {
            this.seriesId = seriesId;
            this.type = type;
        }
    }
}