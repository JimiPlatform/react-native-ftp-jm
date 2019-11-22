package com.jimi.ftpapi;

import android.support.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.Serializable;
import java.text.SimpleDateFormat;

/**
 * Created by yzy on 17-9-22.
 */

public class FTPMediaFile implements Serializable, Comparable<FTPMediaFile> {
    public long size;//服务器中该文件大小
    public long process;//当前进度值
    public String date;
    public String day;
    public String time;
    public int type;//0代表图片，1代表视频
    public String name;
    public String url;//在服务器中的位置
    private String rex = "\\d{4}(.\\d\\d){5}.*";
    public String path;
    public int state = FTPIDownload.STATE_PAUSE;
    public String imei = "";
    public boolean isDownload = false;

    public FTPDowmLoadInfo info = new FTPDowmLoadInfo();

    public String toJson() {
        JSONObject jsonObj = new JSONObject();
        try {
            jsonObj.put("time", time);
            jsonObj.put("name", name);
            jsonObj.put("totalSize", size);
//            jsonObj.put("url", url);
            jsonObj.put("localUrl", path);
//            jsonObj.put("imei", imei);
            jsonObj.put("fileType", type);
            jsonObj.put("isDownload", isDownload);

            if (size == 0) {
                jsonObj.put("progress", 0.0);
            } else {
                jsonObj.put("progress", isDownload? 1.0 : process*1.0/size);  //内部下载完之后process会被清0
            }
        } catch (JSONException e) {
        }

        return jsonObj.toString();
    }

    public FTPDowmLoadInfo getDowmLoadInfo(){

        info.progress = this.size == 0 ? 0 : ( isDownload? 1.0 : process*1.0/size);
        info.name = this.name;
        if (this.state == FTPIDownload.STATE_PAUSE) {
            info.state = 0; //暂停
        } else if (info.progress == 1.0) {
            info.state = 2; //完成
        } else {
            info.state = 1; //正在下载
        }
        info.state = -1; //下载失败

        return info;
    }

    public FTPMediaFile(String pName, String pPath, long pSize) {
        size = pSize;
        name = pName;
        String vName = pName.substring(pName.length() - 23, pName.length() - 4);
        if (vName.matches(rex)) {
            date = vName.substring(0, 19).replace("_", "-");
            day = vName.substring(0, 10).replace("_", "-");
//            time = date.substring(11, 19).replace("-", ":");
            time = day;
        } else {
            long vMillis = System.currentTimeMillis();
            SimpleDateFormat format1 = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
            SimpleDateFormat format2 = new SimpleDateFormat("yyyy-MM-dd");
            date = format1.format(vMillis);
            day = format2.format(vMillis);
//            time = date.substring(11, 19).replace("-", ":");
            time = day;
        }

        if ("/".equals(pPath.substring(pPath.length() - 1, pPath.length()))) {
            url = pPath + pName;
        } else {
            url = pPath + "/" + pName;
        }
        if (pName.endsWith(".3gp") || pName.endsWith(".mp4")|| pName.endsWith(".avi")) {
            type = 1;
        } else {
            type = 0;
        }

        path = type == 0 ? FTPMediaHelperHandle.MEDIA_PHOTO_PATH + File.separator + name : FTPMediaHelperHandle.MEDIA_PATH + File.separator + name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        if (o instanceof FTPMediaFile) {
            FTPMediaFile mediaFile = (FTPMediaFile) o;
            return url != null ? url.equals(mediaFile.url) : mediaFile.url == null;
        } else if (o instanceof String) {
            return url != null ? url.equals(o) : o == null;
        }

        return false;
    }

    @Override
    public int hashCode() {
        return name != null ? name.hashCode() : 0;
    }

    @Override
    public int compareTo(@NonNull FTPMediaFile another) {
        return url.compareTo(another.url);
    }
}
