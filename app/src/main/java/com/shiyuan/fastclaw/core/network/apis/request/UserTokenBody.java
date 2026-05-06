package com.shiyuan.fastclaw.core.network.apis.request;


import com.shiyuan.fastclaw.core.User;

public class UserTokenBody {
    private String userToken;

    public UserTokenBody(String userToken) {
        this.userToken = userToken;
    }

    public UserTokenBody() {
        userToken = User.get().getId();
    }

    public String getUserToken() {
        return userToken;
    }

    public void setUserToken(String userToken) {
        this.userToken = userToken;
    }
}
