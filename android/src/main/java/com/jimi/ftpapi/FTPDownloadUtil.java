package com.jimi.ftpapi;

import android.util.Log;

import org.apache.commons.net.ftp.FTPClient;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.io.Serializable;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by yzy on 17-9-22.
 */

public class FTPDownloadUtil implements Serializable {
    private String mLocalPath;
    private ExecutorService mExecutorService;
    private HashMap<String, FTPDownLoadCallBack> mCallBackMap = new HashMap<>();
    private static FTPDownloadUtil instance;
    private FTPDownLoadCallBack mDownLoadCallBack;

    public static FTPDownloadUtil getInstance() {
        if (instance == null) {
            instance = new FTPDownloadUtil();
        }
        return instance;
    }

    public void initDownLoadUtil(String pLocalPath) {
        File dir = new File(pLocalPath);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        mLocalPath = pLocalPath;
//        mClient = pFTPClient;
    }

    public void setCallback(FTPDownLoadCallBack pDownLoadCallBack) {
        mDownLoadCallBack = pDownLoadCallBack;
    }


    private FTPDownloadUtil() {
        if (mExecutorService == null) {
            mExecutorService = Executors.newFixedThreadPool(3);
        }
    }

    public DownLoadRunnable download(FTPMediaFile pMediaFile) {
        DownLoadRunnable vDownLoadRunnable = new DownLoadRunnable(pMediaFile);
        mExecutorService.execute(vDownLoadRunnable);
        return vDownLoadRunnable;
    }

    //注册预览页面用的监听
    public void regestPreviewCallBack(String fileName, FTPDownLoadCallBack pDownLoadCallBack) {
        if (mCallBackMap.containsKey(fileName)) return;
        mCallBackMap.put(fileName, pDownLoadCallBack);
    }

    //销毁预览页面用的监听
    public void unRegestPreviewCallBack(String fileName) {
        if (mCallBackMap.containsKey(fileName)) mCallBackMap.remove(fileName);
    }

    //销毁所有下载任务，销毁后不能再开启
    public void destortyAll() {
        mExecutorService.shutdown();
        mExecutorService.shutdownNow();
    }

    public void removeAllCallBacks() {
        mDownLoadCallBack = null;
        mCallBackMap.clear();
    }

    public class DownLoadRunnable implements Runnable {
        private FTPMediaFile vMediaFile;
        private FTPClient vClient;
        public boolean isPause = false;

        public void stop() {
            isPause = true;
        }

        public DownLoadRunnable(FTPMediaFile pMediaFile) {
            vMediaFile = pMediaFile;
        }

        @Override
        public void run() {
//            FileOutputStream os = null;
            vClient = FTPUtil2.getInstance().getClient();
            RandomAccessFile vAccessFile = null;
            InputStream is = null;
            try {
                File localFile = new File(mLocalPath, vMediaFile.name);
                vAccessFile = new RandomAccessFile(localFile, "rw");
                long localLen = vAccessFile.length();
                Log.e("yzy", "url----: " + vMediaFile.url);
                if (localLen >= vMediaFile.size) {
                    Log.e("yzy", "run: 文件已存在");

                    if (mDownLoadCallBack != null) {
                        mDownLoadCallBack.onDownLoadSuccess(vMediaFile, false);
                    }
                    if (mCallBackMap.containsKey(vMediaFile.name)) {
                        mCallBackMap.get(vMediaFile.name).onDownLoadSuccess(vMediaFile, false);
                    }
                    FTPTasksManger.getInstance().mDownloadTasks.remove(vMediaFile.name);
                    return;
                } else {
                    if (localLen == 0) {
                        Log.e("yzy", "run: 正常下载");
                    } else if (localLen > 0) {
                        Log.e("yzy", "run: 断点续传");
//                        os = new FileOutputStream(localFile, true);
                    }

                    Log.e("yzy", "localLenOffset: " + localLen);
                    vClient.setRestartOffset(localLen);
                    vAccessFile.seek(localLen);
                    is = vClient.retrieveFileStream(vMediaFile.url);
                    byte[] buffer = new byte[1024];
                    int len;
                    long lastPro = 0;
                    if (is != null) {

                        while (!isPause && ((len = is.read(buffer)) > 0 || localLen < vMediaFile.size)) {
//                            os.write(buffer, 0, len);
                            localLen += len;
                            long process = localLen * 100 / vMediaFile.size;
                            vAccessFile.write(buffer, 0, len);
                            if (process > lastPro) {
                                Log.e("yang", vMediaFile.name + "-----" + process);
                                lastPro = process;
                                vMediaFile.process = localLen;
                                if (mDownLoadCallBack != null) {
                                    mDownLoadCallBack.onDownLoading(vMediaFile);
                                }
                            }
                            if (mCallBackMap.containsKey(vMediaFile.name)) {
                                mCallBackMap.get(vMediaFile.name).onDownLoading(vMediaFile);
                            }
                        }

                        Log.e("yzy", "vMediaFile.size: " + vMediaFile.size);
                    }
                    if (!isPause) {
                        if (mDownLoadCallBack != null) {
                            mDownLoadCallBack.onDownLoadSuccess(vMediaFile, true);
                            Log.e("yzy", "run: " + "onDownLoadSuccess1");
                        }
                        if (mCallBackMap.containsKey(vMediaFile.name)) {
                            mCallBackMap.get(vMediaFile.name).onDownLoadSuccess(vMediaFile, true);
                            Log.e("yzy", "run: " + "onDownLoadSuccess2");
                        }
                        FTPTasksManger.getInstance().mDownloadTasks.remove(vMediaFile.name);
                    }
                    isPause = true;
                }
            } catch (Exception pE) {
                isPause = true;
                if (mDownLoadCallBack != null) {
                    mDownLoadCallBack.onDownLoadFail(vMediaFile);
                    if (mCallBackMap.containsKey(vMediaFile.name)) {
                        mCallBackMap.get(vMediaFile.name).onDownLoadFail(vMediaFile);
                    }
                }
                pE.printStackTrace();
            } finally {
                try {
                    isPause = true;
//                    if (os != null) {
//                        os.flush();
//                        os.close();
//                    }
                    if (vAccessFile != null) {
                        vAccessFile.close();
                    }
                    if (is != null && vClient != null) {
                        is.close();
                        vClient.completePendingCommand();
                    }
                } catch (IOException pE) {
                    pE.printStackTrace();
                }
            }
        }
    }


}
