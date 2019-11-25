package com.jimi.ftpapi;


/**
 * Created by yzy on 17-9-22.
 */

public interface FTPDownLoadCallBack {
    void onDownLoading(FTPMediaFile pMediaFile);
    void onDownLoadSuccess(FTPMediaFile pMediaFile, boolean isNormal);
    void onDownLoadFail(FTPMediaFile pMediaFile);
}
