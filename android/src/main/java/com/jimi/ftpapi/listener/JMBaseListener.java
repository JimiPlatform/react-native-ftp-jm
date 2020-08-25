package com.jimi.ftpapi.listener;

public interface JMBaseListener {
    void realTimeMessage(String key,String data);
    void onSuccess(String data);
    void onFail(String code,String errorMsg);
}
