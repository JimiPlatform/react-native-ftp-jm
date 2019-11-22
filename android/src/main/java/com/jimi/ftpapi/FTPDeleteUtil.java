package com.jimi.ftpapi;


import android.os.CountDownTimer;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by yzy on 17-9-22.
 */

public class FTPDeleteUtil {

    private ExecutorService mExecutorService;
    private static FTPDeleteUtil instance;
    private FTPDeleteCallBack mDeleteCallBack;

    public static FTPDeleteUtil getInstance(){
        if(instance == null) {
            instance = new FTPDeleteUtil();
        }
        return instance;
    }

    private FTPDeleteUtil() {
        if (mExecutorService == null) {
            mExecutorService = Executors.newFixedThreadPool(1);
        }
    }

    public void setCallback(FTPDeleteCallBack pDeleteCallBack){
        mDeleteCallBack = pDeleteCallBack;
    }

    public DeleteRunnable delete(FTPMediaFile pFile) {
        DeleteRunnable vDeleteRunnable = new DeleteRunnable(pFile);
        mExecutorService.execute(vDeleteRunnable);
        return vDeleteRunnable;
    }

    private class DeleteRunnable implements Runnable {
        private FTPMediaFile vFile;
        private boolean isTimeing = false;

        public DeleteRunnable(FTPMediaFile pFile) {
            vFile = pFile;
        }

        private CountDownTimer timer = new CountDownTimer(3000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {

            }

            @Override
            public void onFinish() {
                isTimeing = false;
                if(mDeleteCallBack!=null) {
                    mDeleteCallBack.onDeleteFail(vFile);
                }
            }
        };

        @Override
        public void run() {
            try {
                if(mDeleteCallBack!=null) {
                    mDeleteCallBack.onDeleteing(vFile);
                }
                timer.start();
                isTimeing = true;
                boolean res = FTPUtil2.getInstance().getClient().deleteFile(vFile.url);
                if (isTimeing) {
                    timer.cancel();
                    isTimeing = false;
                    if(mDeleteCallBack!=null) {
                        if (res) {
                            mDeleteCallBack.onDeleteSuccess(vFile);
                        } else {
                            mDeleteCallBack.onDeleteFail(vFile);
                        }
                    }
                }
            } catch (IOException pE) {
                if(mDeleteCallBack!=null) {
                    mDeleteCallBack.onDeleteFail(vFile);
                }
            }
        }
    }
}
