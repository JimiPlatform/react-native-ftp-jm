package com.jimi.ftpapi.ftp;

import android.util.Log;

import com.jimi.ftpapi.listener.JMBaseListener;
import com.jimi.ftpapi.listener.ListeningKye;
import com.jimi.ftpapi.model.FTPDownInfoBean;
import com.jimi.ftpapi.model.socket.ErrorToJsBean;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPClientConfig;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.Map;

public class JMFtp implements JMFtpImp{
    private String TAG = "JMRNFTP";
    private static final String temp="backups";
    private FTPClient pClient;
    private String baseUrl;
    private String mode;
    private String account;
    private String password;
    private int port;
    private Map<String, FTPDownInfoBean> tagMap = new HashMap<>();

    @Override
    public void configFtpSyncFile(String baseUrl, String mode, int port, String account, String password, JMBaseListener jmBaseListener) {
        Log.e(TAG, "配置: baseUrl:" + baseUrl + "\n mode " + mode + "\n port " + port + "\n account " + account + "\n password " + password);

        if (baseUrl == null || mode == null || port < 0 || account == null || password == null) {
            jmBaseListener.onFail("800", ErrorToJsBean.getInstance().getErrorJson("800", "请检查参数是否正确,请看文档"));
            return;
        }

        if (!"passive".equals(mode) && !"active".equals(mode)) {
            jmBaseListener.onFail("800", ErrorToJsBean.getInstance().getErrorJson("800", "mode不正确"));
            return;
        }

        if (baseUrl.trim().startsWith("ftp://")) {
            String strUrl[] = baseUrl.split("//");
            if (strUrl.length == 2) {
                baseUrl = strUrl[1];
            } else {
                baseUrl = strUrl[0];
            }
        }

        this.baseUrl = baseUrl;
        this.mode = mode;
        this.port = port;
        this.account = account;
        this.password = password;
        jmBaseListener.onSuccess(null);
    }

    @Override
    public void connectFTP(JMBaseListener jmBaseListener) {
        try {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        int reply;
                        if (pClient == null) {
                            pClient = new FTPClient();
                        }
                        if (mode.equals("passive")) {
                            pClient.enterLocalPassiveMode();
                        } else {
                            pClient.enterLocalActiveMode();
                        }
                        pClient.connect(baseUrl, port);
                        pClient.login(account, password);
                        pClient.setFileType(FTPClient.BINARY_FILE_TYPE);//很重要，不然下载的视频不完整

                        // 看返回的值是不是230，如果是，表示登陆成功
                        reply = pClient.getReplyCode();
                        if (!FTPReply.isPositiveCompletion(reply)) {
                            // 断开
                            pClient.disconnect();
                            Log.e(TAG, "FTP链接失败");
                            jmBaseListener.onFail("809", ErrorToJsBean.getInstance().getErrorJson("809", "FTP链接失败"));
                        }else {
                            Log.e(TAG, "FTP链接成功");
                            jmBaseListener.onSuccess(null);
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                        Log.e(TAG, "FTP链接失败");
                        jmBaseListener.onFail("809", ErrorToJsBean.getInstance().getErrorJson("809", "FTP链接失败"));
                    }

                }
            }).start();
        } catch (Exception e) {
            e.printStackTrace();
            Log.d(TAG, "FTP链接失败");
            jmBaseListener.onFail("809", ErrorToJsBean.getInstance().getErrorJson("809", "FTP链接失败"));
        }
    }

    @Override
    public void findFTPFlies(String subPath, JMBaseListener jmBaseListener) {
        if (subPath == null) {
            jmBaseListener.onFail("803",ErrorToJsBean.getInstance().getErrorJson("803", "获取文件失败"));
            return;
        }

        if (!isConnect(jmBaseListener)) {
            return;
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
//                    changeWorkingDirectory(subPath);

//                    pClient.configure(new FTPClientConfig("com.jimi.ftpapi.utils.UnixFTPEntryParser"));
//                    pClient.configure();

                    FTPFile[] ftpFiles = pClient.listFiles(subPath);
                    if (ftpFiles == null || ftpFiles.length <= 0) {
                        jmBaseListener.onSuccess("[]");
                        return;
                    }

                    JSONArray jsonArray = new JSONArray();
                    for (int index = 0; index < ftpFiles.length; index++) {
                        try {
                            JSONObject jsonObject = new JSONObject();
                            jsonObject.put("fileName", ftpFiles[index].getName());
                            jsonObject.put("fileSize", ftpFiles[index].getSize());
                            jsonObject.put("filePath", subPath + "/" + ftpFiles[index].getName());
                            jsonObject.put("fileType", ftpFiles[index].getType());
                            jsonArray.put(jsonObject);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    Log.d(TAG, "findFTPFlies: 长度"+jsonArray.length() + "数据"+jsonArray.toString());
                    jmBaseListener.onSuccess(jsonArray.toString());
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.d(TAG, "获取文件失败");
                    jmBaseListener.onFail("803",ErrorToJsBean.getInstance().getErrorJson("803", "获取文件失败"));
                }
            }
        }).start();
    }

    @Override
    public void downFTPFile(String url, String locaUrl, String fileName, String tag, JMBaseListener jmBaseListener) {
        if (url == null || locaUrl == null || fileName == null) {
            jmBaseListener.onFail("804", ErrorToJsBean.getInstance().getErrorJson("804", "下载错误"));
            return;
        }

        if (!isConnect(jmBaseListener)) {
            return;
        }


        StringBuffer stringBuffer = new StringBuffer();

        if (!locaUrl.endsWith("/")) {
            locaUrl = locaUrl + "/";
        }
        stringBuffer.append(locaUrl);



        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    tagMap.put(tag, new FTPDownInfoBean(url, fileName, stringBuffer.toString(), tag, jmBaseListener, true, FTPDownInfoBean.DOWNLOD));
                    FTPFile[] files = pClient.listFiles(url);
                    if (files == null || files.length <= 0) {
                        jmBaseListener.onFail("804", ErrorToJsBean.getInstance().getErrorJson("804", "没有该文件"));
                    }

                    File localFile = new File(stringBuffer.toString()+fileName);
                    long localBeginSize = 0;
                    if (localFile.exists()) {
                        localBeginSize = localFile.length();
                        if (localBeginSize == files[0].getSize()) {
                            Log.d(TAG, "文件已经存在");
                            JSONObject jsonObject = new JSONObject();
                            jsonObject.put("tag", tag);
                            removeTag(tag);
                            jmBaseListener.onSuccess(jsonObject.toString());
                            return;
                        } else{
                            localFile.delete();
                            localBeginSize = 0;
                        }
                    }
                    pClient.setFileType(FTPClient.BINARY_FILE_TYPE);//很重要，不然下载的视频不完整
                    if (downloadByUnit(url, fileName, stringBuffer.toString(), localBeginSize, files[0].getSize(), tag,jmBaseListener)) {
                        JSONObject jsonObject = new JSONObject();
                        jsonObject.put("tag", tag);
                        jmBaseListener.onSuccess(jsonObject.toString());
                        removeTag(tag);
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    Log.d(TAG, "文件下载错误");
                    jmBaseListener.onFail("804", ErrorToJsBean.getInstance().getErrorJson("804", "下载错误"));
                }
            }
        }).start();
    }

    private Boolean downloadByUnit(String url, String fileName, String locaUrl, long beginSize, long endSize, String tag,JMBaseListener jmBaseListener) throws Exception {

        if (!isConnect(jmBaseListener)) {
            return false;
        }

        File dirFile=new File(locaUrl);

        if(!dirFile.exists()){
            dirFile.mkdirs();
        }

        File localFile = new File(locaUrl + fileName);

        long waitSize = endSize - beginSize;
        Log.d(TAG, "waitSize"+waitSize+"endSize "+endSize+"beginSize"+beginSize);
        //切换服务器空间
        changeWorkingDirectory(url);

        //进行断点续传，并记录状态
        FileOutputStream out = new FileOutputStream(localFile, true);
        //把文件指针移动到 开始位置
        pClient.setRestartOffset(beginSize);
        pClient.setBufferSize(1024*1024);
        InputStream in = pClient.retrieveFileStream(new String(url.getBytes("GBK"), "iso-8859-1"));
        byte[] bytes = new byte[1024];
        int c;
        double finishSize = 0;
        double finishPercent = 0;
        long startTime = System.currentTimeMillis();
        while ((c = in.read(bytes)) != -1 && tagMap.get(tag).isRun) {
            out.write(bytes, 0, c);
            finishSize += c;
            Log.d(TAG, "下载大小"+finishSize);
            Log.d(TAG, "总大小"+waitSize);
            Log.d(TAG, "百分比"+finishSize/waitSize);
            if (finishSize >= waitSize) {
                Log.d(TAG, "下载完成,下载大小MB："+finishSize/1024/1024);
                Log.d(TAG, "下载所用时间 秒："+(System.currentTimeMillis()-startTime)/1000);
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("path", locaUrl + fileName);
                jsonObject.put("progress", 1);
                jsonObject.put("tag", tag);
                jmBaseListener.realTimeMessage(ListeningKye.listeningFTPProgressCellBack,jsonObject.toString());
            }
            if ((finishSize / waitSize) - finishPercent > 0.01) {
                finishPercent = finishSize / waitSize;
                Log.d(TAG, "文件下载进度" + finishPercent);
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("path", locaUrl + fileName);
                jsonObject.put("progress", finishPercent);
                jsonObject.put("tag", tag);
                jmBaseListener.realTimeMessage(ListeningKye.listeningFTPProgressCellBack,jsonObject.toString());
            }
        }
        in.close();
        out.close();
        Log.d(TAG, "文件最后大小"+localFile.length());

        return pClient.completePendingCommand();
    }

    @Override
    public void uploadFTPFile(String remoteFolder, String localFilePath, String remoteFileName, boolean overWrite, String tag, JMBaseListener jmBaseListener) {
        if (remoteFileName == null || remoteFolder == null || localFilePath == null || tag == null) {
            jmBaseListener.onFail("808", ErrorToJsBean.getInstance().getErrorJson("808", "上传参数错误"));
            return;
        }

        if (!isConnect(jmBaseListener)) {
            return;
        }
        FTPDownInfoBean ftpDownInfoBean = new FTPDownInfoBean(remoteFolder, remoteFileName, localFilePath, tag, jmBaseListener, true, FTPDownInfoBean.UPDATA);
        ftpDownInfoBean.overWrite=overWrite;
        ftpDownInfoBean.tempFileName=remoteFileName+temp;
        ftpDownInfoBean.fileName=remoteFileName;

        tagMap.put(tag, ftpDownInfoBean);

        new Thread(new Runnable() {
            @Override
            public void run() {
                upload(remoteFolder, localFilePath, ftpDownInfoBean.tempFileName, tag, jmBaseListener);
            }
        }).start();
    }

    private void upload(String remoteFolder, String localFilePath, String remoteFileName, String tag, JMBaseListener jmBaseListener) {
        try {
            FTPDownInfoBean ftpDownInfoBean = tagMap.get(tag);
            FTPFile[] files = pClient.listFiles(remoteFolder + remoteFileName);
            File localFile = new File(localFilePath);
            long remoteSize = 0;
            long localSize = localFile.length();
            if (files.length >= 1) {
                //判断文件是否存在
                remoteSize = files[0].getSize();
                if (remoteSize == localSize) {
                    Log.d(TAG, "文件已经存在");
                    pClient.rename(remoteFolder + remoteFileName, remoteFolder + ftpDownInfoBean.fileName);
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("tag", tag);
                    jsonObject.put("path", remoteFolder + ftpDownInfoBean.fileName);
                    jmBaseListener.onSuccess(jsonObject.toString());
                    removeTag(tag);
                    return;
                }else if(remoteSize>localSize){
                    remoteSize=0;
                    pClient.dele(remoteFolder + remoteFileName);
                    Log.d(TAG, "文件异常");
                }
            } else {
                pClient.makeDirectory(remoteFolder);
            }

            changeWorkingDirectory(remoteFolder);

            if (remoteSize < 0) {
                remoteSize = 0;
            }

            Log.d(TAG, "remoteSize" + remoteSize);

            //等待写入的文件大小
            long writeSize = localSize - remoteSize;
            if (writeSize <= 0) {
                return;
            }
            //获取百分单位是 1-100
            RandomAccessFile raf = new RandomAccessFile(localFile, "r");
            OutputStream out = pClient.appendFileStream(new String((remoteFolder + remoteFileName).getBytes("GBK"), "iso-8859-1"));
//            OutputStream out = pClient.storeFileStream(new String((remoteFolder + remoteFileName).getBytes("GBK"), "iso-8859-1"));
            //把文件指针移动到 开始位置
            pClient.setRestartOffset(remoteSize);
            raf.seek(remoteSize);
            //定义最小移动单位是 1024字节 也就是1kb
            byte[] bytes = new byte[1024 ];
            int c;
            double finishSize = 0;
            double finishPercent = 0;
            boolean isOk=false;
            //存在一个bug 当分布移动的时候  可能会出现下载重复的问题 后期需要修改
            while ((c = raf.read(bytes)) != -1 && tagMap.get(tag).isRun) {
                out.write(bytes, 0, c);
                finishSize += c;

                if ((finishSize / writeSize) - finishPercent > 0.01||finishSize==writeSize) {
                    finishPercent = finishSize / writeSize;

                    Log.d(TAG, "finshSize" + finishSize  +"write"+writeSize);
                    Log.d(TAG, "文件上传进度" + finishPercent);
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("path", ftpDownInfoBean.fileName);
                    jsonObject.put("progress", finishPercent);
                    jsonObject.put("tag", tag);
                    jmBaseListener.realTimeMessage(ListeningKye.listeningFTPProgressCellBack,jsonObject.toString());

                    if (finishPercent >= 1) {
                        isOk=true;
                        Log.d(TAG, "上传完成");
                        out.flush();
                        raf.close();
                        out.close();
                        pClient.completePendingCommand();

                        changeWorkingDirectory(remoteFolder);
                        //如果覆盖文件
                        if (ftpDownInfoBean.overWrite) {
                            jsonObject.put("path", ftpDownInfoBean.fileName);
                            //先删除服务器的文件,然后重命名
                            if (pClient.listFiles(remoteFolder + ftpDownInfoBean.fileName).length > 0) {
                                Log.d(TAG, "文件存在,先删除");
                                pClient.dele(remoteFolder + ftpDownInfoBean.fileName);
                            }
                            Log.d(TAG, "重命名");
                            //重命名文件
                            pClient.rename(remoteFolder + ftpDownInfoBean.tempFileName, remoteFolder + ftpDownInfoBean.fileName);

                        } else {
                            String createName=remoteFolder+System.currentTimeMillis()+ftpDownInfoBean.fileName;
                            //重命名文件
                            pClient.rename(remoteFolder + ftpDownInfoBean.tempFileName,createName);

                            //不覆盖
                            jsonObject.put("path",createName);
                        }

                        jmBaseListener.realTimeMessage(ListeningKye.listeningFTPProgressCellBack,jsonObject.toString());
                        jmBaseListener.onSuccess(null);
                        removeTag(tag);
                        Log.d(TAG, "上传完成1"+pClient.printWorkingDirectory());

                        break;
                    }
                }
            }

            if(!isOk){
                out.flush();
                raf.close();
                out.close();
                pClient.completePendingCommand();
            }

            Log.d(TAG, "上传完成2");
        } catch (Exception e) {
            e.printStackTrace();
            Log.d(TAG, "上传异常");
            try{
                pClient.completePendingCommand();
            }catch (Exception e1){
                e1.printStackTrace();
            }
        }
    }

    @Override
    public void deleteFTPFile(String path, JMBaseListener jmBaseListener) {
        if (!isConnect(jmBaseListener)) {
            return;
        }

        try {
            pClient.dele(path);
            jmBaseListener.onSuccess(null);
        } catch (IOException e) {
            e.printStackTrace();
            jmBaseListener.onFail("806", ErrorToJsBean.getInstance().getErrorJson("806", "删除失败"));
        }
    }

    @Override
    public void moveFTPFile(String from, String to, boolean overWrite, JMBaseListener jmBaseListener) {
        if (from == null || to == null) {
            jmBaseListener.onFail("807", ErrorToJsBean.getInstance().getErrorJson("807", "移动失败"));
        }

        if (!isConnect(jmBaseListener)) {
            return;
        }

        try {
            String parentPath = to;
            try {
                parentPath = to.substring(0, to.lastIndexOf("/"));
            } catch (Exception e) {
                e.printStackTrace();
            }
            pClient.makeDirectory(parentPath);
            pClient.changeWorkingDirectory(parentPath);

            //如果是覆盖移动
            if (overWrite) {
                //先将目标文件夹的文件重命名,做一个备份
                if (pClient.listFiles(to).length > 0) {
                    pClient.dele(to + temp);
                    if (pClient.rename(to, to + temp)) {
                        if (pClient.rename(from, to)) {
                            Log.d(TAG, "moveFTPFile: 移动成功");
                            pClient.dele(to + temp);
                            moveFileSucees(to, jmBaseListener);
                        } else {
                            Log.d(TAG, "moveFTPFile: 移动失败,还原备份");
                            pClient.rename(to + temp, to);
                            moveFileFail(jmBaseListener);
                        }
                    }
                } else {
                    if (pClient.rename(from, to)) {
                        Log.d(TAG, "moveFTPFile: 移动成功");
                        moveFileSucees(to, jmBaseListener);
                    } else {
                        Log.d(TAG, "moveFTPFile: 移动失败");
                        moveFileFail(jmBaseListener);
                    }
                }

            } else {
                //先判断本地有没有,如果有需要重命名一下.
                if (pClient.listFiles(to).length > 0) {
                    StringBuilder sb = new StringBuilder(to);
                    sb.insert(to.lastIndexOf("/") + 1, System.currentTimeMillis());
                    String path = sb.toString();
                    if (pClient.rename(from, path)) {
                        Log.d(TAG, "moveFTPFile: 移动成功");
                        moveFileSucees(path, jmBaseListener);
                    } else {
                        Log.d(TAG, "moveFTPFile: 移动失败");
                        moveFileFail(jmBaseListener);
                    }

                } else {
                    if (pClient.rename(from, to)) {
                        Log.d(TAG, "moveFTPFile: 移动成功");
                        moveFileSucees(to, jmBaseListener);
                    } else {
                        Log.d(TAG, "moveFTPFile: 移动失败");
                        moveFileFail(jmBaseListener);
                    }
                }
            }

            Log.d(TAG, "moveFTPFile: " + parentPath);
            Log.d(TAG, "moveFTPFile: " + pClient.printWorkingDirectory());
            pClient.rename(from, to);
        } catch (Exception e) {
            e.printStackTrace();
            moveFileFail(jmBaseListener);
        }
    }

    @Override
    public void ftpPause(String tag, JMBaseListener jmBaseListener) {
        if (!isConnect(jmBaseListener)) {
            return;
        }
        try {
            FTPDownInfoBean ftpDownInfoBean = tagMap.get(tag);
            ftpDownInfoBean.isRun = false;
            tagMap.put(tag, ftpDownInfoBean);
            jmBaseListener.onSuccess(null);
            Log.d(TAG, "ftp暂停操作 : tag: " + tag);
        } catch (Exception e) {
            jmBaseListener.onFail("805", ErrorToJsBean.getInstance().getErrorJson("805", "没有当前tag"));
            e.printStackTrace();
        }
    }

    @Override
    public void ftpResume(String tag, JMBaseListener jmBaseListener) {
        if (!isConnect(jmBaseListener)) {
            return;
        }
        try {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    FTPDownInfoBean ftpDownInfoBean = tagMap.get(tag);
                    ftpDownInfoBean.isRun = true;
                    tagMap.put(tag, ftpDownInfoBean);
                    jmBaseListener.onSuccess(null);
                    changeWorkingDirectory(ftpDownInfoBean.url);
                    if (ftpDownInfoBean.type == FTPDownInfoBean.DOWNLOD) {
                        downFTPFile(ftpDownInfoBean.url,ftpDownInfoBean.locaUrl, ftpDownInfoBean.fileName, ftpDownInfoBean.tag, ftpDownInfoBean.jmBaseListener);
                    } else {
                        upload(ftpDownInfoBean.url, ftpDownInfoBean.locaUrl, ftpDownInfoBean.tempFileName, tag, ftpDownInfoBean.jmBaseListener);
                    }
                    Log.d(TAG, "ftp恢复操作 : tag: " + tag);
                }
            }).start();

        } catch (Exception e) {
            jmBaseListener.onFail("805", ErrorToJsBean.getInstance().getErrorJson("805", "没有当前tag"));
            e.printStackTrace();
        }
    }

    @Override
    public void closeFTP(JMBaseListener jmBaseListener) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (pClient != null) {
                    try {
                        if (pClient.logout()) {
                            pClient.disconnect();
                            pClient = null;
                            jmBaseListener.onSuccess(null);
                        } else {
                            jmBaseListener.onFail("810", ErrorToJsBean.getInstance().getErrorJson("810", "断开失败"));
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        jmBaseListener.onFail("810", ErrorToJsBean.getInstance().getErrorJson("810", "断开失败"));
                    }
                }
            }
        }).start();
    }

    @Override
    public void ftpCancel(String tag, JMBaseListener jmBaseListener) {
        if (!isConnect(jmBaseListener)) {
            return;
        }
        removeTag(tag);
        jmBaseListener.onSuccess(null);
    }

    private void moveFileSucees(String toPath, JMBaseListener jmBaseListener) {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("path", toPath);
            jmBaseListener.onSuccess(jsonObject.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void moveFileFail(JMBaseListener jmBaseListener) {
        jmBaseListener.onFail("807", ErrorToJsBean.getInstance().getErrorJson("807", "移动失败"));
    }




    @Override
    public void destroy() {
        try{
            for (String key : tagMap.keySet()) {
                try{
                    FTPDownInfoBean ftpDownInfoBean = tagMap.get(key);
                    ftpDownInfoBean.isRun=false;
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
            tagMap.clear();
            tagMap=null;
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private boolean isConnect(JMBaseListener jmBaseListener) {
        if (pClient==null||!pClient.isConnected()) {
            jmBaseListener.onFail("810", ErrorToJsBean.getInstance().getErrorJson("810", "FTP链接断开"));
            Log.d(TAG, "FTP链接断开");
            return false;
        }
        return true;
    }

    private void changeWorkingDirectory(String path){
        try{
            if (mode.equals("passive")) {
                pClient.enterLocalPassiveMode();
            } else {
                pClient.enterLocalActiveMode();
            }
            pClient.setFileType(FTPClient.BINARY_FILE_TYPE);//很重要，不然下载的视频不完整

            try {
                //切换服务器空间
                pClient.changeWorkingDirectory(path);
            } catch (Exception e) {
                e.printStackTrace();
            }
            Log.d(TAG, "空间路径：" + pClient.printWorkingDirectory());
        }catch (Exception e){
            e.printStackTrace();
        }

    }

    private long getRemoteFileSize(String remote, String fileName) {
        //获取远程文件大小,方面做断点续传
        try {
            FTPFile[] ftpFiles = pClient.listFiles(remote);
            if (ftpFiles.length > 0) {
                return ftpFiles[0].getSize();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return 0;
    }

    private void removeTag(String tag){
        try{
            FTPDownInfoBean ftpDownInfoBean=tagMap.get(tag);
            ftpDownInfoBean.isRun=false;
            tagMap.remove(tag);
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
