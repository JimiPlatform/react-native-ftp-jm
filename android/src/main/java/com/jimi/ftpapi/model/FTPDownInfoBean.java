package com.jimi.ftpapi.model;

import com.facebook.react.bridge.Promise;

public class FTPDownInfoBean {
    public String url;
    public String fileName;
    public String tempFileName;
    public String locaUrl;
    public String tag;
    public Promise promise;
    public boolean isRun;
    public boolean overWrite;
    public int type;
    public static final int DOWNLOD=0;
    public static final int UPDATA=1;

    public FTPDownInfoBean(String url,String fileName, String locaUrl, String tag, Promise promise,boolean isRun,int type){
        this.url=url;
        this.locaUrl=locaUrl;
        this.tag=tag;
        this.promise=promise;
        this.isRun=isRun;
        this.type=type;
        this.fileName=fileName;
    }
}
