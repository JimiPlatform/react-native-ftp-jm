package com.jimi.ftpapi.backups;

import android.text.TextUtils;
import android.util.Log;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Created by yzy on 17-9-22.
 */

public class FTPUtil2 {
    private String rootDir = "/mnt/sdcard2";
    private String[] mDefaultPhotoPaths = new String[]{rootDir + "/DVRMEDIA/CarRecorder/PHOTO",
            rootDir + "/DVRMEDIA/Remote/PHOTO", rootDir + "/HDIT/Remote/PHOTO",
            rootDir + "/Remote/PHOTO", rootDir + "/CarRecorder/PHOTO"};
    private String[] mDefaultVideoPaths = new String[]{rootDir + "/DVRMEDIA/CarRecorder/GENERAL",
            rootDir + "/DVRMEDIA/Remote/GENERAL", rootDir + "/HDIT/Remote/VIDEO",
            rootDir + "/Remote/VIDEO", rootDir + "/CarRecorder/GENERAL",
            rootDir + "/DVRMEDIA/Remote/VIDEO", rootDir + "/DVRMEDIA/CarRecorder/EVENT"};
    private List<String> photoDirs;
    private List<String> vedioDirs;
    private static FTPUtil2 instance = null;
    private String mIp = null;
    private int mPort = 0;

    public static synchronized FTPUtil2 getInstance() {
        if (instance == null) {
            instance = new FTPUtil2();
        }
        return instance;
    }

    private FTPUtil2() {
    }

    public FTPClient getClient() {
        FTPClient vClient = new FTPClient();
        if (mIp != null && mPort != 0) {
            boolean vB = loginFtp(vClient, mIp, mPort, "admin", "admin");
            if (vB) {
                return vClient;
            } else {
                return null;
            }
        }
        return null;
    }

    public FTPClient getClient(String pIp, int pPort) {
        mIp = pIp;
        mPort = pPort;
        FTPClient vClient = new FTPClient();
        if (mIp != null && mPort != 0) {
            boolean vB = loginFtp(vClient, mIp, mPort, "admin", "hao123");
            if (vB) {
                return vClient;
            } else {
                return null;
            }
        }
        return null;
    }

    //设置文件路径
    public void setField(String pthotoPath, String vedioPath) {
        photoDirs = new ArrayList<>();
        vedioDirs = new ArrayList<>();
        if (pthotoPath != null && !"".equals(pthotoPath)) {
            String[] vSplit = pthotoPath.split(",");//这个直接得到目录，不用下面的for循环
            photoDirs.addAll(Arrays.asList(vSplit));
        } else {
            photoDirs.addAll(Arrays.asList(mDefaultPhotoPaths));
        }

        if (vedioPath != null && !"".equals(vedioPath)) {
            String[] vSplit = vedioPath.split(",");
            vedioDirs.addAll(Arrays.asList(vSplit));
        } else {
            vedioDirs.addAll(Arrays.asList(mDefaultVideoPaths));
        }
    }

    private void createDir() {

//        dirs.add(RootDir + "/CarRecorder" + "/EVENT");
//        dirs.add(RootDir + "/CarRecorder" + "/GENERAL");
//        dirs.add(RootDir + "/CarRecorder" + "/PHOTO");
//        dirs.add(RootDir + "/Remote");
//        dirs.add(RootDir + "/DCIM");
//        dirs.add(RootDir + "/Movies");
//        dirs.add(RootDir + "/Pictures");
    }

    /**
     * @author YangZhenYu
     * created at 17-9-26 下午1:48
     * 功能：链接并登录ftp服务器，好事操作，需要在子线程中进行
     */
    private boolean loginFtp(FTPClient pClient, String url, int port, String user, String pwd) {
        try {
            Log.e("yzy", "loginFtp: " + url + ",port:" + port);
            pClient.connect(url, port);
            pClient.login(user, pwd);
            pClient.setFileType(FTPClient.BINARY_FILE_TYPE);//很重要，不然下载的视频不完整
            return true;
        } catch (IOException pE) {
            pE.printStackTrace();
            return false;
        }
    }

    public void logOutFtp(FTPClient pClient) {
        try {
            if (pClient != null) {
                pClient.logout();
                pClient.disconnect();
            }
        } catch (IOException pE) {
            pE.printStackTrace();
        }

    }

    /**
     * 获取资源文件列表
     *
     * @param type         0代表图片，1代表视频
     * @param pFTPCallBack
     */
    public GetListRunnable getMediaList(FTPClient pClient, int type, FTPCallBack pFTPCallBack) {
        if (FTPTasksManger.getInstance().hasDownlodTask()) {
            return null;
        }
        Log.e("yzy", "getMediaList: " + System.currentTimeMillis());
        GetListRunnable vRunnable = new GetListRunnable(pClient, type, pFTPCallBack);
        new Thread(vRunnable).start();
        return vRunnable;
    }

    public class GetListRunnable implements Runnable {
        private int type;
        private FTPCallBack vFTPCallBack;
        private List<String> tempDirs;
        private ArrayList<String> vPaths;//所有图片或视频的上级目录
        private ArrayList<FTPMediaFile> vImages;
        private ArrayList<FTPMediaFile> vVedios;
        private FTPClient mClient;
        private boolean stop = false;

        public GetListRunnable(FTPClient pClient, int type, FTPCallBack pFTPCallBack) {
            this.type = type;
            this.vFTPCallBack = pFTPCallBack;
            this.mClient = pClient;
            vPaths = new ArrayList<>();//所有图片或视频的上级目录
            vImages = new ArrayList<>();
            vVedios = new ArrayList<>();
        }

        /*如果中途取消获取文件列表需要调用*/
        public void cancle() {
            stop = true;
        }

        public void run() {
            try {
            /*找到所有资源的直接父目录*/
                if (type == 0) {
                    //图片目录
                    tempDirs = photoDirs;
                } else if (type == 1) {
                    //视频目录
                    tempDirs = vedioDirs;
                }

                if (tempDirs == null) throw new IOException();

                for (String vDir : tempDirs) {
                    if (mClient == null) {
                        throw new IOException();
                    }

                    FTPFile[] vFTPFiles = mClient.listFiles(vDir);

                    if (vFTPFiles == null) continue;

                    for (FTPFile vFTPFile : vFTPFiles) {
                        if (vFTPFile == null) continue;
                        if ("/".equals(vDir.substring(vDir.length() - 1, vDir.length()))) {
                            vPaths.add(vDir + vFTPFile.getName());
                        } else {
                            vPaths.add(vDir + "/" + vFTPFile.getName());
                        }
                    }
                }

                for (final String vPath : vPaths) {
                    mClient.listFiles(vPath, pFTPFile -> {
                        if (stop) {
                            return false;
                        }
                        if (pFTPFile == null) {
                            return false;
                        }

                        String vName = pFTPFile.getName();
                        if (TextUtils.isEmpty(vName)) {
                            return false;
                        }
                        if (vName.endsWith(".jpg") || vName.endsWith(".png")) {
                            vImages.add(new FTPMediaFile(vName, vPath, pFTPFile.getSize()));
                            return true;
                        }
                        if (vName.endsWith(".3gp") || vName.endsWith(".mp4")|| vName.endsWith(".avi")) {
                            vVedios.add(new FTPMediaFile(vName, vPath, pFTPFile.getSize()));
                            return true;
                        }
                        return false;
                    });
                    if (stop) {
                        return;
                    }
                }

                if (!stop) {
                    if (type == 0) {
                        sortData(vImages);
                        vFTPCallBack.onResponseMedias(formatData(vImages));
                    } else if (type == 1) {
                        sortData(vVedios);
                        vFTPCallBack.onResponseMedias(formatData(vVedios));
                    }
                }
            } catch (IOException pE) {
                if (!stop) {
                    vFTPCallBack.onResponseMedias(null);
                }
                Log.e("yzy", "run: " + pE);
            }

        }
    }

    /**
     * 根据时间来排序
     *
     * @param pMediaFiles
     */
    private void sortData(ArrayList<FTPMediaFile> pMediaFiles) {
        Collections.sort(pMediaFiles, (file1, file2) -> compareDate(file1.date, file2.date));
    }

    /**
     * 格式化数据，类似于json的形式
     *
     * @param pMediaFiles
     * @return
     */
    private ArrayList<FTPDayBean> formatData(ArrayList<FTPMediaFile> pMediaFiles) {
        ArrayList<String> days = new ArrayList<>();
        ArrayList<FTPDayBean> vDayBeans = new ArrayList<>();
        FTPDayBean vDayBean = null;
        for (FTPMediaFile vFile : pMediaFiles) {
            String vDay = vFile.day;
            if (!days.contains(vDay)) {
                days.add(vDay);
                vDayBean = new FTPDayBean(vDay);
                vDayBeans.add(vDayBean);
            }
            if (vDayBean != null) {
                vDayBean.mediaFiles.add(vFile);
            }
        }
        return vDayBeans;
    }

    /**
     * 日期大小比较器
     *
     * @param date1
     * @param date2
     * @return
     */
    private int compareDate(String date1, String date2) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
        Date vParse1;
        Date vParse2;
        try {
            vParse1 = format.parse(date1);
        } catch (ParseException pE) {
            return 1;
        }

        try {
            vParse2 = format.parse(date2);
        } catch (ParseException pE) {
            return -1;
        }
        return vParse1.before(vParse2) ? 1 : -1;
    }
}
