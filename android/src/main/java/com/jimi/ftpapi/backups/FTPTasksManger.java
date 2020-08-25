package com.jimi.ftpapi.backups;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

/**
 * Created by yzy on 17-10-13.
 */

public class FTPTasksManger {
    private FTPDownloadUtil mDownloadUtil = FTPDownloadUtil.getInstance();
    private FTPDeleteUtil mDeleteUtil = FTPDeleteUtil.getInstance();
    public HashMap<String, FTPDownloadUtil.DownLoadRunnable> mDownloadTasks = new HashMap<>();
    private HashMap<String, FTPMediaFile> mediaFiles = new HashMap<>();

    private FTPTasksManger() {
    }

    private static FTPTasksManger instance;

    public static FTPTasksManger getInstance() {
        if (instance == null) {
            instance = new FTPTasksManger();
        }
        return instance;
    }

    /*初始化*/
    public void initManger(String pLocalPath, FTPDownLoadCallBack pDownLoadCallBack) {
        mDownloadUtil.initDownLoadUtil(pLocalPath);
        mDownloadUtil.setCallback(pDownLoadCallBack);
    }

    /*单个下载*/
    public FTPDownloadUtil.DownLoadRunnable download(FTPMediaFile pMediaFile) {
        FTPDownloadUtil.DownLoadRunnable vDownLoadRunnable = mDownloadUtil.download(pMediaFile);
        mDownloadTasks.put(pMediaFile.name, vDownLoadRunnable);
        mediaFiles.put(pMediaFile.name, pMediaFile);
        return vDownLoadRunnable;
    }

    /*批量下载*/
    public void downloadAll(ArrayList<FTPMediaFile> list) {
        if (mDownloadTasks != null && mDownloadTasks.size() != 0) {
            mDownloadTasks.clear();
        }

        for (FTPMediaFile mediaFile : list) {
            //然后开始下载
            download(mediaFile);
        }
    }

    /*单个删除*/
    public void delete(FTPMediaFile pFile, FTPDeleteCallBack pDeleteCallBack) {
        mDeleteUtil.setCallback(pDeleteCallBack);
        if (mediaFiles.containsKey(pFile.name)) {
            mediaFiles.remove(pFile.name);
        }
        stopAll();
        mDeleteUtil.delete(pFile);
    }

    /*批量删除*/
    public void deleteAll(Collection<Serializable> list, FTPDeleteCallBack pDeleteCallBack) {
        mDeleteUtil.setCallback(pDeleteCallBack);
        stopAll();
        for (Serializable vSerializable : list) {
            FTPMediaFile vFile = (FTPMediaFile) vSerializable;
            if (mediaFiles.containsKey(vFile.name)) {
                mediaFiles.remove(vFile.name);
            }
            mDeleteUtil.delete(vFile);
        }
    }

    public void resetDownload() {
        for (FTPMediaFile file : mediaFiles.values()) {
            download(file);
        }
    }

    /*判断是否有下载任务*/
    public boolean hasDownlodTask() {
        if (mDownloadTasks != null) {
            for (FTPDownloadUtil.DownLoadRunnable vRunnable : mDownloadTasks.values()) {
                if (vRunnable != null && !vRunnable.isPause) {
                    return true;
                }
            }
        }
        return false;
    }

    /*停止单个下载任务*/
    public void stop(FTPMediaFile pFile) {
        if (mDownloadTasks != null && mDownloadTasks.get(pFile.name) != null) {
            mDownloadTasks.get(pFile.name).stop();
            mDownloadTasks.remove(pFile.name);
        }
    }

    /*停止所有下载任务*/
    public void stopAll() {
        if (mDownloadTasks != null) {
            for (FTPDownloadUtil.DownLoadRunnable vRunnable : mDownloadTasks.values()) {
                if (vRunnable != null)
                    vRunnable.stop();
            }
            mDownloadTasks.clear();
        }
    }

    //注册预览页面用的监听
    public void regestPreviewCallBack(String fileName, FTPDownLoadCallBack pDownLoadCallBack) {
        mDownloadUtil.regestPreviewCallBack(fileName, pDownLoadCallBack);
    }

    //销毁预览页面用的监听
    public void unRegestPreviewCallBack(String fileName) {
        mDownloadUtil.unRegestPreviewCallBack(fileName);
    }

    /*销毁*/
    public void destoryManger() {
        mDownloadUtil.destortyAll();
        mDownloadTasks.clear();
    }

    public void removeAllCallbacks() {
        stopAll();
        mDownloadUtil.removeAllCallBacks();
    }


}
