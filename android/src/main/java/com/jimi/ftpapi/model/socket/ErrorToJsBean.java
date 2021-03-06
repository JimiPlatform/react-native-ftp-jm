package com.jimi.ftpapi.model.socket;

import android.util.Log;

import com.google.gson.Gson;

public class ErrorToJsBean {
    private static Gson gson=new Gson();
    private static ErrorToJsBean errorToJsBean;
    public String data;
    public String code;

    private ErrorToJsBean(){}

    public static ErrorToJsBean getInstance(){
        if(errorToJsBean==null){
            errorToJsBean=new ErrorToJsBean();
        }
        return errorToJsBean;
    }

    public String getErrorJson(String code,String data){
        this.data=data;
        this.code=code;
        String msg=gson.toJson(errorToJsBean);
        Log.d("errorMsg", "getErrorJson: "+msg);
        return msg;
    }
}
