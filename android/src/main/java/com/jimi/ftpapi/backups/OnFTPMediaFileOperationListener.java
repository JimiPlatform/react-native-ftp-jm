package com.jimi.ftpapi.backups;

import java.io.Serializable;
import java.util.List;

/**
 * Created by Liuzhixue on 2017/9/25.
 * 下载管理监听
 */

public interface OnFTPMediaFileOperationListener {
    void onLocalMediaFileScanSuccessful(List<Serializable> itemList, int[] itemIndexArray);//当本地媒体库扫描完成时回掉

    void onLocalMediaFileCopySuccessful();//复制文件完成时回掉

    void onLocalMediaFileCopyFailed();//复制失败时回掉

    void onLocalMediaFileDeleteSuccessful(int[] itemIndexArray, List<Serializable> itemList);//删除成功时回掉

    void onLocalMediaFileDeleteFailed(int[] itemIndexArray, List<Serializable> itemList);

    void onDeviceFileListGetSuccessful(List<Serializable> itemList, int[] itemIndexArray);//设备文件获取成功时回掉

    void onDeviceFileListGetFailed();//设备文件获取失败时回掉

    void onProgressUpdated(int position, int progress);//更新下载进度时回掉

    void onDeviceFileDownloadSuccessful(int position);//设备文件下载成功时回掉

    void onDeviceFileDownloadFailed(int position, FTPMediaFile pMediaFile);//下载失败时回掉

    void onDownloadWorkPrepared();//准备下载时回掉

    void onUiRefreshed(int[] itemIndexArray, List<Serializable> itemList);//当需要更新界面数据时回掉
}
