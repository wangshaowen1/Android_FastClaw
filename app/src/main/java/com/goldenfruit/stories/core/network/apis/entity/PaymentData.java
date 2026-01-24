package com.kgmedia.fastfilm.core.network.apis.entity;

import com.kgmedia.fastfilm.core.network.apis.response.WxPayData;

public  class PaymentData {

    /**
     * orderId : 202007242017529183580079
     * payInfo : alipay_sdk=alipay-sdk-java-dynamicVersionNo&app_id=2021001181662301&biz_content=%7B%22body%22%3A%22%E6%94%AF%E4%BB%98%E5%AE%9E%E5%90%8D%E8%AE%A4%E8%AF%81%E8%B4%B9%E7%94%A8%22%2C%22out_trade_no%22%3A%22202007242017529183580079%22%2C%22product_code%22%3A%22QUICK_MSECURITY_PAY%22%2C%22subject%22%3A%22%E6%94%AF%E4%BB%98%E5%AE%9E%E5%90%8D%E8%AE%A4%E8%AF%81%E8%B4%B9%E7%94%A8%22%2C%22timeout_express%22%3A%2230m%22%2C%22total_amount%22%3A%220.01%22%7D&charset=utf-8&format=json&method=alipay.trade.app.pay&notify_url=http%3A%2F%2F120.25.102.60%3A8686%2Fpay%2FaliPayAsyncCallBack&sign=Qx%2BV%2FWRpzKxMCiIfiiU4VcZqR2hca7Zjbp9BEslxOzQmwKJCJg%2Fo%2Fp0%2Fi%2BjBH8wE%2FcUv9P%2F0fvGiS9OX3qsNmSwd4m%2BaeDoaPjMFuKEUzHx5z4nmzvqmn01WiUxDZvJ8eR6%2BGHav224Akusm7ogcwbKe437TXhNUJo7zo2manR91Rb6mW5aT34XGBcGqkab0fXnc7ylEk1ZW7I5GzyTzH9G5IZbLtw9k1kEslzlxGGyXb9ZGB4SFIpPksHnldluCjqjdEH5xWTiz5wNjZau%2BB0xqpKhWmqs6X8uh%2BbW0vFVzodArfmL3oH50tirWyOyvdtvYb77qvDovJaXUTSIltg%3D%3D&sign_type=RSA2&timestamp=2020-07-24+20%3A17%3A55&version=1.0
     */

    protected String orderNo;
    protected String payInfo;

    public String getOrderNo() {
        return orderNo;
    }

    public String getPayInfo() {
        return payInfo;
    }

    public boolean isBaoFu(){
        if(this instanceof WxPayData){
            return this.isBaoFu();
        }
        return false;
    }
}
