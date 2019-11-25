package com.jimi.ftpapi;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.SparseArray;

import com.jimi.ftpapi.manager.FTPSyncFileManager;

import java.io.File;
import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;



/**
 * Created by Liuzhixue on 2017/9/28.
 */

public class FTPMediaHelper {
    private FTPMediaHelper() {

    }

    private static final ExecutorService singleThreadExecutor = Executors.newSingleThreadExecutor();

    /**
     * 根据当前下载的文件找到在界面显示的位置
     * @param dateList
     * @param pMediaFile
     * @return
     */
    public static final int findPositionByTag(List<Serializable> dateList, FTPMediaFile pMediaFile) {
        int tempIndex = -1;
        for (int i = 0; i < dateList.size(); i++) {
            Serializable obj = dateList.get(i);
            if (obj instanceof FTPMediaFile && pMediaFile.name.equals(((FTPMediaFile) obj).name)) {
                tempIndex = i;
                break;
            }
        }
        return tempIndex;
    }

    public static final FTPFileUtils.OnReplaceListener mOnReplaceListener = () -> true;

    private SparseArray<FTPMediaDateBean> getSourceMediaDateBeanByFile(List<FTPMediaDateBean> sourceMediaList, File targetFile) {
        SparseArray<FTPMediaDateBean> targetMediaFile = new SparseArray<>();
        for (int i = 0; i < sourceMediaList.size(); i++) {
            FTPMediaDateBean vMediaDateBean = sourceMediaList.get(i);
            int index = Collections.binarySearch(vMediaDateBean.mediaFileList, targetFile, new
                    FTPFileComparator(FTPFileComparator.ACTION_GROUP));
            if (index == -1) {
                targetMediaFile.put(i, vMediaDateBean);
                return targetMediaFile;
            }

        }
        return null;
    }


    /**
     * 填充本地下载的文件信息，断点续传要用
     * @param list
     * @param pMediaFile
     */
    public static void fillLocationMediaInfo(List<FTPMediaDateBean> list, FTPMediaFile pMediaFile) {
        for (int i = 0; i < list.size(); i++) {
            FTPMediaDateBean vBean = list.get(i);
            for (int j = 0; j < vBean.mediaFileList.size(); j++) {
                File vFile = vBean.mediaFileList.get(j);
                if (vFile.getName().equals(pMediaFile.name)) {
                    if (vFile.length() == pMediaFile.size) {
                        pMediaFile.process = 0;
                        pMediaFile.isDownload = true;
                        if (pMediaFile.type == 0) {
                            pMediaFile.state = FTPIDownload.STATE_DONE;
                        } else {
                            pMediaFile.state = FTPIDownload.STATE_NORMAL;
                        }
                    } else {
                        pMediaFile.isDownload = false;
                        pMediaFile.process = vFile.length();
                        pMediaFile.state = FTPIDownload.STATE_PAUSE;
                    }
                }
            }
        }
    }

    /**
     * 是否时照片
     * @param pSerializable
     * @return
     */

    public static final boolean isPhotoFile(Serializable pSerializable) {
        if (pSerializable instanceof File) {
            return "jpg".equals(FTPFileUtils.getFileExtension((File) pSerializable));
        } else if (pSerializable instanceof FTPMediaFile) {
            return ((FTPMediaFile) pSerializable).type == 0;
        }
        return false;
    }

    /**
     * 是否时本地文件
     * @param mediaType
     * @return
     */
    public static boolean isLocalMediaType(String mediaType) {
        return FTPSyncFileManager.MEDIA_ALL.equals(mediaType);
    }

    public static void deleteSingleLocalFile(FTPMediaHelperRunnable pHelperThread, Serializable singleValue) {
        pHelperThread.setWorkAction(FTPMediaHelperRunnable.ACTION_DELETE_FILE);
        pHelperThread.setDeleteValue(singleValue);
        singleThreadExecutor.submit(pHelperThread);
    }

    /**
     * 批量删除
     * @param pHelperThread
     * @param fileCollection
     */
    public static void deleteLocalFiles(FTPMediaHelperRunnable pHelperThread, Collection<Serializable> fileCollection) {
        pHelperThread.setWorkAction(FTPMediaHelperRunnable.ACTION_DELETE_FILE);
        pHelperThread.setFileCollection(fileCollection);
        singleThreadExecutor.execute(pHelperThread);
    }
    /**
     * 复制本地文件
     */

    public static void copyFile(FTPMediaHelperRunnable pHelperThread, FTPMediaFile pMediaFile) {
        pHelperThread.setMediaFile(pMediaFile);
        pHelperThread.setWorkAction(FTPMediaHelperRunnable.ACTION_COPY_FILE);
        singleThreadExecutor.execute(pHelperThread);
    }

    /**
     * 扫面本地媒体库
     */
    public static void scanLocalMediaFile(FTPMediaHelperRunnable pFileThread) {
        pFileThread.setWorkAction(FTPMediaHelperRunnable.ACTION_SCAN_LOCAL_FILE);
        singleThreadExecutor.execute(pFileThread);
    }


    /**
     * 充值分割线数组
     * @param pFileThread
     */
    public static void resetItemIndexArray(FTPMediaHelperRunnable pFileThread) {
        pFileThread.setWorkAction(FTPMediaHelperRunnable.RESET_ITEM_INDEX_ARRAY);
        singleThreadExecutor.execute(pFileThread);
    }

    public static void resetHelper() {
        singleThreadExecutor.shutdownNow();
    }

    /**
     * 复制文件
     * @param pHelperThread
     * @param srcFilePath
     * @param destFilePath
     */
    public static void customCopyFile(FTPMediaHelperRunnable pHelperThread, String srcFilePath, String destFilePath) {
        pHelperThread.setWorkAction(FTPMediaHelperRunnable.ACTION_CUSTOMER_COPY_FILE);
        pHelperThread.setCustomCopyInfo(srcFilePath, destFilePath);
        singleThreadExecutor.execute(pHelperThread);
    }

    /**
     *
     * 分享文件
     * @param context
     * @param targetValue
     */

    public static void shareMediaFile(Context context, Serializable targetValue) {
        if (context == null || targetValue == null) return;
        File shareFile = null;
        if (targetValue instanceof File) shareFile = (File) targetValue;
        if (targetValue instanceof FTPMediaFile) shareFile = new File(((FTPMediaFile) targetValue).path);
        Intent intent = new Intent(Intent.ACTION_SEND);
        Uri shareUri = Uri.fromFile(shareFile);
        if (FTPMediaHelper.isPhotoFile(targetValue)) {
            intent.setType("image/*");
        } else {
            intent.setType("video/*");
        }

        intent.putExtra(Intent.EXTRA_STREAM, shareUri);
        context.startActivity(Intent.createChooser(intent, "分享到:"));
    }

}
