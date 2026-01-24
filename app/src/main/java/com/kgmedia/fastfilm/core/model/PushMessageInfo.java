package com.kgmedia.fastfilm.core.model;

public class PushMessageInfo {
    public final static int NORMAL = 1;
    public final static int WARNING = 2;
    //推送消息类型 1-普通消息（默认为1） 2-警告弹出消息
    private String pushMsgContent;
    private int pushMsgType;
    private long msgId;

    //jumpUri : topicId=121212 话题动态
    //jumpUri : scriptId=121212  剧本详情页
    //jumpUri : groupId=121212  组局详情页
    private String jumpUri;
    private String pushMsgJumpUri;

    public String getPushMsgJumpUri() {
        return pushMsgJumpUri;
    }

    public void setPushMsgJumpUri(String pushMsgJumpUri) {
        this.pushMsgJumpUri = pushMsgJumpUri;
    }

    public String getJumpUri() {
        return jumpUri;
    }

    public void setJumpUri(String jumpUri) {
        this.jumpUri = jumpUri;
    }

    public String getPushMsgContent() {
        return pushMsgContent;
    }

    public void setPushMsgContent(String pushMsgContent) {
        this.pushMsgContent = pushMsgContent;
    }

    public int getPushMsgType() {
        return pushMsgType;
    }

    public void setPushMsgType(int pushMsgType) {
        this.pushMsgType = pushMsgType;
    }

    public long getMsgId() {
        return msgId;
    }

    public void setMsgId(long msgId) {
        this.msgId = msgId;
    }

    @Override
    public String toString() {
        return "PushMessageInfo{" +
                "pushMsgContent='" + pushMsgContent + '\'' +
                ", pushMsgType=" + pushMsgType +
                ", msgId=" + msgId +
                ", jumpUri='" + jumpUri + '\'' +
                ", pushMsgJumpUri='" + pushMsgJumpUri + '\'' +
                '}';
    }
}
