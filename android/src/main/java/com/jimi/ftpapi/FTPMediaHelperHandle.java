package com.jimi.ftpapi;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import com.jimi.ftpapi.manager.FTPSyncFileManager;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


/**
 * Created by Liuzhixue on 2017/10/13.
 */

public class FTPMediaHelperHandle extends Handler implements FTPDownLoadCallBack, FTPCallBack, FTPDeleteCallBack {
    private String mediaType;
    private final FTPSyncFileManager ftpSyncFileManager;
    private FTPUtil2 mFTPUtil;
//    private FTPClient mClient;
    private int[] itemIndexArray;
    public static final int GET_FILE_LIST_SUCCESSFUL = 0x12;
    public static final int GET_FILE_LIST_FAILED = 0x16;
    public static final int ON_FILE_DOWNLOAD_COMPLETE = 0x18;
    public static final int ON_FILE_DOWNLOADING = 0X20;
    public static final int ON_FILE_DOWNLOAD_FAILED = 0X22;
    public static final int ON_DEVICE_FILE_DELETE_SUCCESSFUL = 0x58;
    public static final int ON_DEVICE_FILE_DELETE_FAILED = 0x59;
    public static final int SHOULD_REFRESH_UI = 0X10;
    public static final int ON_FILE_COPY_SUCCESSFUL = 0x32;
    public static final int ON_FILE_COPY_FAILED = 0x36;
    public static final int ON_LOCAL_FILE_DELETE_SUCCESSFUL = 0x38;
    public static final int SCAN_SUCCESSFUL_FLAG = 0x26;

    public static final String MEDIA_DATE_LIST = "media_date_list";
    public static final String MEDIA_FILE_INDEX_ARRAY = "index_array";

    public static final List<Serializable> itemList = new ArrayList<>();//本地所有文件
    private ArrayList<FTPMediaDateBean> mLocalDateList = new ArrayList<>();//本地已归类文件
    private OnFTPMediaFileOperationListener mOnMediaFileOperationListener;
    private int index;
    private List<Serializable> deleteList = new ArrayList<>();
    public static String MEDIA_PATH;
    public static String MEDIA_PHOTO_PATH;
    public String imei = "";

    public FTPMediaHelperHandle(FTPSyncFileManager ftpSyncFileManager, String mediaType) {
        this.mediaType = mediaType;
        this.ftpSyncFileManager = ftpSyncFileManager;
    }

    /**
     * 初始化下载
     */

    public void prepareDownLoadWork() {
        mFTPUtil = FTPUtil2.getInstance();
        if (mediaType.equals(FTPSyncFileManager.MEDIA_PHOTO)) {
            FTPTasksManger.getInstance().initManger(MEDIA_PHOTO_PATH, this);
        } else {
            FTPTasksManger.getInstance().initManger(MEDIA_PATH, this);
        }
        if (mOnMediaFileOperationListener != null) mOnMediaFileOperationListener.onDownloadWorkPrepared();
        FTPMediaHelper.scanLocalMediaFile(new FTPMediaHelperRunnable(this));
    }

    public void setMediaType(String mediaType) {
        this.mediaType = mediaType;
    }

    /**
     * 主线程中更新下载信息
     * @param msg
     */
    @Override
    public void handleMessage(Message msg) {
        super.handleMessage(msg);
        switch (msg.what) {
            case GET_FILE_LIST_SUCCESSFUL:
                if (mOnMediaFileOperationListener != null) mOnMediaFileOperationListener.onDeviceFileListGetSuccessful(itemList, itemIndexArray);
                break;
            case SCAN_SUCCESSFUL_FLAG:
                List<Serializable> itemMediaList = (List<Serializable>) msg.obj;
                Bundle vData = msg.getData();
                fillItemDate(itemMediaList, vData);
                break;
            case ON_FILE_DOWNLOAD_COMPLETE:
                int completePosition = msg.arg1;
                if (mOnMediaFileOperationListener != null) mOnMediaFileOperationListener.onDeviceFileDownloadSuccessful(completePosition);
                break;
            case ON_FILE_DOWNLOADING:
                if (mOnMediaFileOperationListener != null) mOnMediaFileOperationListener.onProgressUpdated(msg.arg1, msg.arg2);
                break;
            case ON_FILE_DOWNLOAD_FAILED:
                if (mOnMediaFileOperationListener != null) mOnMediaFileOperationListener.onDeviceFileDownloadFailed(msg.arg1, (FTPMediaFile) msg.obj);
                break;
            case ON_FILE_COPY_SUCCESSFUL:
                if (mOnMediaFileOperationListener != null) mOnMediaFileOperationListener.onLocalMediaFileCopySuccessful();
                break;
            case ON_FILE_COPY_FAILED:
                if (mOnMediaFileOperationListener != null) mOnMediaFileOperationListener.onLocalMediaFileCopyFailed();
                break;
            case ON_LOCAL_FILE_DELETE_SUCCESSFUL:
//                if (mOnMediaFileOperationListener != null) mOnMediaFileOperationListener.onLocalMediaFileDeleteSuccessful();
                break;
            case GET_FILE_LIST_FAILED:
                if (mOnMediaFileOperationListener != null) mOnMediaFileOperationListener.onDeviceFileListGetFailed();
                break;
            case ON_DEVICE_FILE_DELETE_SUCCESSFUL:
                deleteLocalFiles(deleteList);   //lzj add
                itemList.removeAll(deleteList);
                deleteList.clear();
                resetItemIndexArray();
                itemIndexArray = (int[]) msg.obj;
                if (mOnMediaFileOperationListener != null) mOnMediaFileOperationListener.onLocalMediaFileDeleteSuccessful(itemIndexArray, FTPMediaHelperHandle.itemList);
                break;
            case ON_DEVICE_FILE_DELETE_FAILED:
                itemIndexArray = (int[]) msg.obj;
                if (mOnMediaFileOperationListener != null) mOnMediaFileOperationListener.onLocalMediaFileDeleteFailed(itemIndexArray, FTPMediaHelperHandle.itemList);
                break;
            case SHOULD_REFRESH_UI:
                itemIndexArray = (int[]) msg.obj;
                if (mOnMediaFileOperationListener != null) mOnMediaFileOperationListener.onUiRefreshed(itemIndexArray, FTPMediaHelperHandle.itemList);
                break;
        }
    }

    /**
     * 更新进度时，发消息切换到主线程
     * @param pMediaFile
     */
    @Override
    public void onDownLoading(FTPMediaFile pMediaFile) {
        int position = FTPMediaHelper.findPositionByTag(itemList, pMediaFile);
        if (pMediaFile.type == 0 || !ftpSyncFileManager.isVisible() || position == -1) return;
        Message vMessage = obtainMessage();
        vMessage.arg1 = position;
        vMessage.arg2 = (int) pMediaFile.process;
        vMessage.what = ON_FILE_DOWNLOADING;
        sendMessage(vMessage);
    }

    /**
     * 下载成功时发消息切换到主线程
     * @param pMediaFile
     * @param isNormal
     */
    @Override
    public void onDownLoadSuccess(FTPMediaFile pMediaFile, boolean isNormal) {
        int position = FTPMediaHelper.findPositionByTag(itemList, pMediaFile);
        if (position == -1) return;

        pMediaFile.process = 0;
        pMediaFile.isDownload = true;
        if (pMediaFile.type == 1) {
            pMediaFile.state = FTPIDownload.STATE_NORMAL;
        } else if (pMediaFile.type == 0) {
            if (pMediaFile.state != FTPIDownload.STATE_DONE) pMediaFile.state = FTPIDownload.STATE_INTERRUPT;
        }

        if (!ftpSyncFileManager.isVisible()
//                || (!isNormal && pMediaFile.type != 1)
                ) return;
        Message vMessage = obtainMessage();
        vMessage.arg1 = position;
        vMessage.what = ON_FILE_DOWNLOAD_COMPLETE;  //下载成功
        sendMessage(vMessage);
    }

    /**
     * 下载失败时发消息切换到主线程
     * @param pMediaFile
     */
    @Override
    public void onDownLoadFail(FTPMediaFile pMediaFile) {
        int position = FTPMediaHelper.findPositionByTag(itemList, pMediaFile);
        if (position == -1) return;
        pMediaFile.state = FTPIDownload.STATE_PAUSE;
        if (!ftpSyncFileManager.isVisible() || pMediaFile.type == 0) return;
        Message vMessage = obtainMessage();
        vMessage.what = ON_FILE_DOWNLOAD_FAILED;
        vMessage.arg1 = position;
        vMessage.obj = pMediaFile;
        sendMessage(vMessage);
    }


    /**
     * 填充本地媒体库信息
     *
     */
    private void fillItemDate(List<Serializable> pItemMediaList, Bundle pData) {
        mLocalDateList.clear();
        mLocalDateList.addAll(pData.getParcelableArrayList(MEDIA_DATE_LIST));
        if (mediaType.equals(FTPSyncFileManager.MEDIA_ALL)) {
            itemIndexArray = pData.getIntArray(MEDIA_FILE_INDEX_ARRAY);
            itemList.clear();
            itemList.addAll(pItemMediaList);
            if (mOnMediaFileOperationListener != null) mOnMediaFileOperationListener.onLocalMediaFileScanSuccessful(itemList, itemIndexArray);
        } else {
            getNetDate();
        }
    }


    /**
     * 初始化下载文件类型
     */
    private void getNetDate() {
        new Thread(){
            public void run(){
                if (mediaType.equals(FTPSyncFileManager.MEDIA_VIDEO)) {
                    mFTPUtil.getMediaList(mFTPUtil.getClient(),1, FTPMediaHelperHandle.this);

                } else if (mediaType.equals(FTPSyncFileManager.MEDIA_PHOTO)) {
                    mFTPUtil.getMediaList(mFTPUtil.getClient(),0, FTPMediaHelperHandle.this);//获取文件列表,0代表图片，1代表视频
                }
            }
        }.start();


    }

    /**
     * 获取列表响应时发消息切换到主线程
     * @param pDayBeans
     */
    @Override
    public void onResponseMedias(ArrayList<FTPDayBean> pDayBeans) {
        if (pDayBeans == null) {
            sendEmptyMessage(GET_FILE_LIST_FAILED);
        } else {
            fillDate(pDayBeans);
            sendEmptyMessage(GET_FILE_LIST_SUCCESSFUL);
            if (mediaType.equals(FTPSyncFileManager.MEDIA_PHOTO)) {    //下载文件
                for (FTPDayBean bean : pDayBeans) {
                    FTPTasksManger.getInstance().downloadAll(bean.mediaFiles);
                }
            }

        }
    }

    /**
     * 刷新item数据
     */
    public void refreshItemDate() {
        FTPMediaHelper.scanLocalMediaFile(new FTPMediaHelperRunnable(this));
    }

    public void downLoadDeviceFile(FTPMediaFile pMediaFile) {
        FTPTasksManger.getInstance().download(pMediaFile);
    }


    /**
     * 填充获取的列表文件信息
     * @param pDayBeans
     */
    private void fillDate(ArrayList<FTPDayBean> pDayBeans) {
        itemIndexArray = new int[pDayBeans.size()];
        itemList.clear();
        for (int i = 0; i < pDayBeans.size(); i++) {
            FTPDayBean vDayBean = pDayBeans.get(i);
            String date = vDayBean.day;
            itemList.add(date);
            for (int j = 0; j < vDayBean.mediaFiles.size(); j++) {
                FTPMediaFile vMediaFile = vDayBean.mediaFiles.get(j);
                vMediaFile.imei = imei;
                FTPMediaHelper.fillLocationMediaInfo(mLocalDateList, vMediaFile);
            }
            itemList.addAll(vDayBean.mediaFiles);
            itemIndexArray[i] = itemList.size();
        }
    }

    public void resetItemIndexArray() {
        FTPMediaHelper.resetItemIndexArray(new FTPMediaHelperRunnable(this));
    }

    /**
     * 删除本地文件
     * @param fileCollection
     */
    public void deleteDeviceFiles(Collection<Serializable> fileCollection) {
        if (mediaType.equals(FTPSyncFileManager.MEDIA_ALL)) {
            FTPMediaHelperRunnable deleteRunnable = new FTPMediaHelperRunnable(this);
            FTPMediaHelper.deleteLocalFiles(deleteRunnable, fileCollection);
        } else {
            deleteList.addAll(fileCollection);
            index = deleteList.size();
            FTPTasksManger.getInstance().deleteAll(fileCollection, this);
        }
    }


    /**
     * 文件下载状态监听
     * @param pListener
     */
    public void setOnMediaFileOperationListener(OnFTPMediaFileOperationListener pListener) {
        this.mOnMediaFileOperationListener = pListener;
    }

    @Override
    public void onDeleteing(FTPMediaFile pMediaFile) {

    }

    @Override
    public void onDeleteSuccess(FTPMediaFile pMediaFile) {
        index--;
        if (index == 0) {
            addDateString();
            index = -1;
            sendEmptyMessage(ON_DEVICE_FILE_DELETE_SUCCESSFUL);
        }
    }

    @Override
    public void onDeleteFail(FTPMediaFile pMediaFile) {
        index--;
        if (index == 0) {
            addDateString();
            index = -1;
            sendEmptyMessage(ON_DEVICE_FILE_DELETE_FAILED);
        }
    }

    /**
     * 重置下载管理
     */
    public void resetHandleHelper() {
        removeCallbacksAndMessages(null);
        FTPTasksManger.getInstance().removeAllCallbacks();
    }

    /**
     * 添加当天的数据
     */
    public void addDateString() {
        List<Serializable> tempList;
        int tempIndex = 0;
        for (int i = 0; i < itemList.size(); i++) {
            Serializable serializable = itemList.get(i);
            if (serializable instanceof String && i > 0) {
                tempList = itemList.subList(tempIndex + 1, i);
                if (deleteList.containsAll(tempList)) {
                    deleteList.add(itemList.get(tempIndex));
                }
                tempIndex = i;
            }
        }

        tempList = itemList.subList(tempIndex + 1, itemList.size());
        if (deleteList.containsAll(tempList)) {
            deleteList.add(itemList.get(tempIndex));
        }
    }


    public void deleteDeviceSingleFile(FTPMediaFile pMediaFile) {
        FTPTasksManger.getInstance().delete(pMediaFile, this);
    }

    /**
     * 批量删除本地问价
     * @param fileCollection
     */
    public void deleteLocalFiles(Collection<Serializable> fileCollection) {
        FTPMediaHelperRunnable vHelperRunnable = new FTPMediaHelperRunnable(this);
        FTPMediaHelper.deleteLocalFiles(vHelperRunnable, fileCollection);
    }

    public void deleteLocalSingleFile(Serializable pSerializable) {
        FTPMediaHelperRunnable vHelperRunnable = new FTPMediaHelperRunnable(this);
        FTPMediaHelper.deleteSingleLocalFile(vHelperRunnable, pSerializable);
    }

    /**
     * 复制本地文件
     * @param pMediaFile
     */
    public void copyLocalFile(FTPMediaFile pMediaFile) {
        FTPMediaHelperRunnable vHelperRunnable = new FTPMediaHelperRunnable(this);
        FTPMediaHelper.copyFile(vHelperRunnable, pMediaFile);
    }

    public List<Serializable> getItemList() {
        return itemList;
    }

    public int[] getItemIndexArray() {
        return itemIndexArray;
    }

    /**
     * 重置下载管理
     */
    public void resetDownload() {
        FTPTasksManger.getInstance().resetDownload();
    }

}
