package com.kgmedia.fastfilm.core.network.apis.request;


import com.kgmedia.fastfilm.core.User;

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
