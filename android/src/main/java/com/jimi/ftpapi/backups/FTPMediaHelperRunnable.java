package com.jimi.ftpapi.backups;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.SparseIntArray;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * Created by LZX68 on 2017/10/7.
 */

public class FTPMediaHelperRunnable implements Runnable {
    private String workAction;
    public static final String ACTION_DELETE_FILE = "ACTION_DELETE_FILE";
    public static final String ACTION_COPY_FILE = "ACTION_COPY_FILE";
    public static final String ACTION_SCAN_LOCAL_FILE = "ACTION_SCAN_LOCAL_FILE";
    public static final String RESET_ITEM_INDEX_ARRAY = "RESET_ITEM_INDEX_ARRAY";
    public static final String ACTION_CUSTOMER_COPY_FILE = "ACTION_CUSTOMER_COPY_FILE";
    public static final int ON_CUSTOMER_COPY_TASK_FAILED = 0x1100;
    public static final int ON_CUSTOMER_COPY_SUCCESSFUL = 0x1101;
    private Collection<Serializable> fileCollection;
    private FTPMediaFile mediaFile;
    private Serializable singleValue;
    private Handler mHandler;
    private int[] indexArray;
    private String mSrcFilePath;
    private String mDestFilePath;

    public FTPMediaHelperRunnable(Handler handler) {
        this.mHandler = handler;
    }

    public void setCustomCopyInfo(String srcFilePath, String destFilePath) {
        mSrcFilePath = srcFilePath;
        mDestFilePath = destFilePath;
    }


    public void setWorkAction(String action) {
        this.workAction = action;
    }

    public void setFileCollection(Collection<Serializable> fileCollection) {
        this.fileCollection = fileCollection;
    }

    public void setMediaFile(FTPMediaFile mediaFile) {
        this.mediaFile = mediaFile;
    }

    public void setDeleteValue(Serializable pValue) {
        this.singleValue = pValue;
    }

    /**
     * 复制
     */
    private synchronized void copyFile() {
        String srcFilePath = FTPMediaHelperHandle.MEDIA_PHOTO_PATH + File.separator + mediaFile.name;
        String destFilePath = FTPMediaHelperHandle.MEDIA_PATH + File.separator + mediaFile.name;
        boolean copyResult = FTPFileUtils.copyFile(srcFilePath, destFilePath, FTPMediaHelper.mOnReplaceListener);
        if (copyResult) {
            mediaFile.state = FTPIDownload.STATE_DONE;
            mediaFile.process = 0;
            if (mHandler != null) mHandler.sendEmptyMessage(FTPMediaHelperHandle.ON_FILE_COPY_SUCCESSFUL);
        } else {
            mediaFile.state = FTPIDownload.STATE_INTERRUPT;
            if (mHandler != null) mHandler.sendEmptyMessage(FTPMediaHelperHandle.ON_FILE_COPY_FAILED);
        }
    }

    private void customerCopyFile() {
        boolean copyResult = FTPFileUtils.copyFile(mSrcFilePath, mDestFilePath, FTPMediaHelper.mOnReplaceListener);
        if (copyResult) {
            if (mHandler != null) mHandler.sendEmptyMessage(ON_CUSTOMER_COPY_SUCCESSFUL);
        } else {
            if (mHandler != null) mHandler.sendEmptyMessage(ON_CUSTOMER_COPY_TASK_FAILED);
        }
    }

    /**
     * 删除
     */
    private synchronized void fileDelete() {
        if (fileCollection != null) {
            Iterator<Serializable> vIterator = fileCollection.iterator();
            while (vIterator.hasNext()) {
                Serializable vSerializable = vIterator.next();
                deleteFile(vSerializable);
            }
        }
        deleteFile(singleValue);
        if (mHandler != null) mHandler.sendEmptyMessage(FTPMediaHelperHandle.ON_LOCAL_FILE_DELETE_SUCCESSFUL);
    }

    /**
     * 删除
     * @param pSerializable
     */

    private synchronized void deleteFile(Serializable pSerializable) {
        if (pSerializable instanceof File) {
            File file = (File) pSerializable;
            FTPFileUtils.deleteFile(file);
        } else if (pSerializable instanceof FTPMediaFile) {
            FTPMediaFile vMediaFile = (FTPMediaFile) pSerializable;
            String mediaPath = FTPMediaHelperHandle.MEDIA_PHOTO_PATH + File.separator + vMediaFile.name;
            String localPath = FTPMediaHelperHandle.MEDIA_PATH + File.separator + vMediaFile.name;
            if (FTPFileUtils.isFileExists(localPath)) {
                FTPFileUtils.deleteFile(localPath);
            }
            if (FTPFileUtils.isFileExists(mediaPath)) {
                FTPFileUtils.deleteFile(mediaPath);
            }
        }
    }


    @Override
    public void run() {
        switch (workAction) {
            case ACTION_COPY_FILE:
                copyFile();
                break;
            case ACTION_DELETE_FILE:
                fileDelete();
                break;
            case ACTION_SCAN_LOCAL_FILE:
                scanLocalMediaFile();
                break;
            case RESET_ITEM_INDEX_ARRAY:
                resetItemIndexArray();
                break;
            case ACTION_CUSTOMER_COPY_FILE:
                customerCopyFile();
                break;

        }
    }

    /**
     * 充值分割线标记位
     */
    private void resetItemIndexArray() {
        SparseIntArray itemIndexList = new SparseIntArray();
        for (int i = 0; i < FTPMediaHelperHandle.itemList.size(); i++) {
            if (i > 0 && FTPMediaHelperHandle.itemList.get(i) instanceof String) {
                itemIndexList.put(i, i);
            }
        }
        itemIndexList.put(FTPMediaHelperHandle.itemList.size(), FTPMediaHelperHandle.itemList.size());
        int[] itemIndexArray = new int[itemIndexList.size()];
        for (int i = 0; i < itemIndexList.size(); i++) {
            itemIndexArray[i] = itemIndexList.valueAt(i);
        }
        itemIndexList.clear();
        Message vMessage = mHandler.obtainMessage();
        vMessage.what = FTPMediaHelperHandle.SHOULD_REFRESH_UI;
        vMessage.obj = itemIndexArray;
        mHandler.sendMessage(vMessage);
    }

    /**
     * 扫描本地文件
     */
    private void scanLocalMediaFile() {
        ArrayList<FTPMediaDateBean> vSourceMediaFileList = getSourceMediaFileList();
        List<Serializable> vMediaItemList = getMediaItemList(vSourceMediaFileList);
        if (mHandler == null) return;
        Message vMessage = mHandler.obtainMessage();
        vMessage.obj = vMediaItemList;
        Bundle vBundle = new Bundle();
        vBundle.putParcelableArrayList(FTPMediaHelperHandle.MEDIA_DATE_LIST, vSourceMediaFileList);
        vBundle.putIntArray(FTPMediaHelperHandle.MEDIA_FILE_INDEX_ARRAY, indexArray);
        vMessage.setData(vBundle);
        vMessage.what = FTPMediaHelperHandle.SCAN_SUCCESSFUL_FLAG;
        mHandler.sendMessage(vMessage);
    }

    private ArrayList<FTPMediaDateBean> getSourceMediaFileList() {
        ArrayList<FTPMediaDateBean> vMediaDateBeanList = new ArrayList<>();
        File vFile = new File(FTPMediaHelperHandle.MEDIA_PATH);
        if (!vFile.exists()) vFile.mkdir();
        File[] vFiles = vFile.listFiles(FTPFileComparator.MEDIAFILTER);
        if (vFiles == null || vFiles.length == 0) return vMediaDateBeanList;
        Arrays.sort(vFiles, new FTPFileComparator(FTPFileComparator.ACTION_SORT));
        int tempIndex = 0;
        long tempFileDateMillis = FTPFileComparator.getFileTime(vFiles[0]);
        for (int i = 0; i < vFiles.length; i++) {
            long targetFileDateMillis = FTPFileComparator.getFileTime(vFiles[i]);
            if (targetFileDateMillis < tempFileDateMillis) {
                File[] sourceMediaFileArrays = Arrays.copyOfRange(vFiles, tempIndex, i);
                FTPMediaDateBean vMediaDateBean = new FTPMediaDateBean();
                vMediaDateBean.mediaFileList.addAll(Arrays.asList(sourceMediaFileArrays));
                assortmentMediaFile(vMediaDateBean);
                vMediaDateBean.date = FTPFileComparator.generateDateByTime(tempFileDateMillis);
                vMediaDateBeanList.add(vMediaDateBean);
                tempFileDateMillis = targetFileDateMillis;
                tempIndex = i;
            }


            if (i == vFiles.length - 1) {
                FTPMediaDateBean vBean = new FTPMediaDateBean();
                File[] sourceMediaFileArrays = Arrays.copyOfRange(vFiles, tempIndex, vFiles.length);
                vBean.date = FTPFileComparator.generateMediaDate(sourceMediaFileArrays[0]);
                vBean.mediaFileList.addAll(Arrays.asList(sourceMediaFileArrays));
                assortmentMediaFile(vBean);
                vMediaDateBeanList.add(vBean);

            }
        }

        return vMediaDateBeanList;
    }

    /**
     * 归类
     * @param pMediaDateBean
     */
    private void assortmentMediaFile(FTPMediaDateBean pMediaDateBean) {
        for (int i = 0; i < pMediaDateBean.mediaFileList.size(); i++) {
            File mediaFile = pMediaDateBean.mediaFileList.get(i);
            if (FTPMediaHelper.isPhotoFile(mediaFile)) {
                pMediaDateBean.photoFileList.add(mediaFile);
            } else {
                pMediaDateBean.videoFileList.add(mediaFile);
            }
        }
    }

    /**
     * 获取本地媒体文件的原始信息
     * @param pBeanList
     * @return
     */
    private ArrayList<Serializable> getMediaItemList(List<FTPMediaDateBean> pBeanList) {
        ArrayList<Serializable> itemList = new ArrayList<>();
        indexArray = new int[pBeanList.size()];
        for (int i = 0; i < pBeanList.size(); i++) {
            FTPMediaDateBean vBean = pBeanList.get(i);
            itemList.add(vBean.date);
            itemList.addAll(vBean.mediaFileList);
            indexArray[i] = itemList.size();
        }
        return itemList;
    }

}
