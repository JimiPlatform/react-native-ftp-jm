package com.jimi.ftpapi.backups;

/**
 * Created by yzy on 17-9-22.
 */

public interface FTPDeleteCallBack {
    void onDeleteing(FTPMediaFile pMediaFile);
    void onDeleteSuccess(FTPMediaFile pMediaFile);
    void onDeleteFail(FTPMediaFile pMediaFile);
}
