package com.jimi.ftpapi;


import java.util.ArrayList;

/**
 * Created by yzy on 17-9-22.
 */

public interface FTPCallBack {
    void onResponseMedias(ArrayList<FTPDayBean> pDayBeans);
}
