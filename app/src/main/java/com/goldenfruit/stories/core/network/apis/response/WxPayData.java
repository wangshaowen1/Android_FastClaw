package com.kgmedia.fastfilm.core.network.apis.response;

import com.kgmedia.fastfilm.core.network.apis.entity.PaymentData;
import com.google.gson.Gson;
import com.tencent.mm.opensdk.modelpay.PayReq;

public class WxPayData extends PaymentData {

    //(目前这里只有微信支付有) 支付方式 0-微信支付 1-宝付
    protected int payWay;

    //(目前这里只有微信支付有) 宝付 跳转小程序原始ID
    protected String originalId;

    public boolean isBaoFu(){
        return payWay == 1;
    }

    public int getPayWay() {
        return payWay;
    }

    public String getOriginalId() {
        return originalId;
    }

    public PayReq toPayReq() {
        // 微信
        PayReq req = new PayReq();
        PayInfo payInfo = new Gson().fromJson(this.payInfo, PayInfo.class);
        req.appId = payInfo.appid;
        req.partnerId = payInfo.partnerId;
        req.prepayId = payInfo.prepayId;
        req.nonceStr = payInfo.noncestr;
        req.timeStamp = payInfo.timestamp;
        req.packageValue = payInfo.packageValue;
        req.sign = payInfo.sign;
        req.extData = "app data";

        return req;
    }

    public static class PayInfo {
        /**
         * sign : E04855FEBDA18EE76ABB0ED416676867
         * prepayId : wx2214355448013268da9e5a4c1428984300
         * partnerId : 1573117521
         * appId : wx0366df6639769878
         * packageValue : Sign=WXPay
         * timeStamp : 1595399753
         * nonceStr : EDMla1QxQSbfQyoy
         */

        private String sign;
        private String prepayId;
        private String partnerId;
        private String appid;
        private String packageValue;
        private String timestamp;
        private String noncestr;

        public String getSign() {
            return sign;
        }

        public void setSign(String sign) {
            this.sign = sign;
        }

        public String getPrepayId() {
            return prepayId;
        }

        public void setPrepayId(String prepayId) {
            this.prepayId = prepayId;
        }

        public String getPartnerId() {
            return partnerId;
        }

        public void setPartnerId(String partnerId) {
            this.partnerId = partnerId;
        }


        public String getPackageValue() {
            return packageValue;
        }

        public void setPackageValue(String packageValue) {
            this.packageValue = packageValue;
        }

        public String getAppid() {
            return appid;
        }

        public void setAppid(String appid) {
            this.appid = appid;
        }

        public String getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(String timestamp) {
            this.timestamp = timestamp;
        }

        public String getNoncestr() {
            return noncestr;
        }

        public void setNoncestr(String noncestr) {
            this.noncestr = noncestr;
        }
    }
}
