package com.jimi.ftpapi;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;

/**
 * Created by yzy on 17-9-22.
 */

public class FTPDayBean implements Parcelable{
    public String day;
    public ArrayList<FTPMediaFile> mediaFiles= new ArrayList<>();
    private final ClassLoader mClassLoader = getClass().getClassLoader();
    public FTPDayBean(String pDay) {
        day = pDay;
    }

    protected FTPDayBean(Parcel in) {
        day = in.readString();
        mediaFiles = in.readArrayList(mClassLoader);
    }

    public static final Creator<FTPDayBean> CREATOR = new Creator<FTPDayBean>() {
        @Override
        public FTPDayBean createFromParcel(Parcel in) {
            return new FTPDayBean(in);
        }

        @Override
        public FTPDayBean[] newArray(int size) {
            return new FTPDayBean[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(day);
        dest.writeList(mediaFiles);
    }
}
