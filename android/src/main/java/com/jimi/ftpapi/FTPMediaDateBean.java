package com.jimi.ftpapi;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.File;
import java.util.ArrayList;

/**
 * Created by Liuzhixue on 2017/9/21.
 */

public class FTPMediaDateBean implements Parcelable {
    public String date;
    public ArrayList<File> mediaFileList = new ArrayList<>();
    public ArrayList<File> photoFileList = new ArrayList();
    public ArrayList<File> videoFileList = new ArrayList<>();
    private final ClassLoader mClassLoader = getClass().getClassLoader();

    public FTPMediaDateBean() {
    }

    protected FTPMediaDateBean(Parcel in) {
        date = in.readString();
        mediaFileList = in.readArrayList(mClassLoader);
        photoFileList = in.readArrayList(mClassLoader);
        videoFileList = in.readArrayList(mClassLoader);
    }

    public static final Creator<FTPMediaDateBean> CREATOR = new Creator<FTPMediaDateBean>() {
        @Override
        public FTPMediaDateBean createFromParcel(Parcel in) {
            return new FTPMediaDateBean(in);
        }

        @Override
        public FTPMediaDateBean[] newArray(int size) {
            return new FTPMediaDateBean[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(date);
        dest.writeList(mediaFileList);
        dest.writeList(photoFileList);
        dest.writeList(videoFileList);
    }
}
