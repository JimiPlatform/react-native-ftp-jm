package com.jimi.ftpapi.manager;

import android.util.Log;

import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.google.gson.Gson;
import com.jimi.ftpapi.model.FTPDownInfoBean;
import com.jimi.ftpapi.model.ToJSBean;
import com.jimi.ftpapi.model.ToJSData;
import com.jimi.ftpapi.model.socket.ErrorToJsBean;
import com.jimi.ftpapi.utils.MyGson;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;


public class JMFTPSyncFileManager extends ReactContextBaseJavaModule {
    private ReactContext mReactContext;
    private static final String kRNJMFTPSyncFileManager = "kRNJMFTPSyncFileManager";
    private FTPClient pClient;
    private String baseUrl;
    private String mode;
    private String account;
    private String password;
    private int port;

    private Map<String ,FTPDownInfoBean> tagMap=new HashMap<>();


    public JMFTPSyncFileManager(ReactApplicationContext reactContext) {
        super(reactContext);
        mReactContext = reactContext;
        reactContext.addLifecycleEventListener(mLifecycleEventListener);
    }

    @Override
    public Map<String, Object> getConstants() {
        Map<String, Object> constants = new HashMap<>();
        constants.put(kRNJMFTPSyncFileManager, kRNJMFTPSyncFileManager);
        return constants;
    }

    @Override
    public String getName() {
        return "JMFTPSyncFileManager";
    }

    private <T> void sendEvent(String methodName, T data) {
        ToJSBean bean = new ToJSBean<T>();
        bean.method = methodName;
        bean.data = data;
        String resultJson;
        if (data instanceof ToJSData) {
            resultJson = MyGson.getJsonByMethodName(methodName, bean);
        } else {
            resultJson = new Gson().toJson(bean);
        }
        Log.i(kRNJMFTPSyncFileManager, "JMUDPScoketManager send JS：" + resultJson);
        mReactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class).emit(kRNJMFTPSyncFileManager, resultJson);
    }

    //监听Activity的生命周期
    private final LifecycleEventListener mLifecycleEventListener = new LifecycleEventListener() {
        @Override
        public void onHostResume() {

        }

        @Override
        public void onHostPause() {
        }

        @Override
        public void onHostDestroy() {

        }
    };

    @ReactMethod
    public void configFtpSyncFile(String baseUrl, String mode, int port, String account, String password, Promise promise) {
        Log.d(kRNJMFTPSyncFileManager, "配置: baseUrl:" +baseUrl+"\n mode"+mode+"\n port" +port+"\n account"+account+"\n password"+password);
        if (!"passive".equals(mode) && !"active".equals(mode)) {
            Log.d(kRNJMFTPSyncFileManager, "模式错误");
            promise.reject("800", ErrorToJsBean.getInstance().getErrorJson("800", "mode不正确"));
            return;
        }
        this.baseUrl = baseUrl;
        this.mode = mode;
        this.port = port;
        this.account = account;
        this.password = password;
        promise.resolve(null);
    }

    @ReactMethod
    public void connectFTP(Promise promise) {
        try {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
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
                        Log.d(kRNJMFTPSyncFileManager, "FTP链接成功");
                        promise.resolve(null);
                    } catch (Exception e) {
                        e.printStackTrace();
                        Log.d(kRNJMFTPSyncFileManager, "FTP链接失败");
                        promise.reject("809",ErrorToJsBean.getInstance().getErrorJson("809","FTP链接失败"));
                    }

                }
            }).start();
        } catch (Exception e) {
            e.printStackTrace();
            Log.d(kRNJMFTPSyncFileManager, "FTP链接失败");
            promise.reject("809",ErrorToJsBean.getInstance().getErrorJson("809","FTP链接失败"));
        }
    }

    @ReactMethod
    public void findFTPFlies(String subPath,boolean recursive, Promise promise) {

       if(!isConnect(promise)){
           return;
       }

        try {
            FTPFile[] ftpFiles = pClient.listFiles(subPath);
            if (ftpFiles == null || ftpFiles.length <= 0) {
                promise.resolve("[]");
                return;
            }

            JSONArray jsonArray = new JSONArray();
            for (int index = 0; index < ftpFiles.length; index++) {
                try {
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("name", ftpFiles[index].getName());
                    jsonArray.put(jsonObject);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            Log.d(kRNJMFTPSyncFileManager, "findFTPFlies: "+jsonArray.toString());
            promise.resolve(jsonArray.toString());


        } catch (IOException e) {
            e.printStackTrace();
            Log.d(kRNJMFTPSyncFileManager, "获取文件失败");
            promise.reject("803", "获取文件失败");
        }

    }


    @ReactMethod
    public void downFTPFile(String url, String locaUrl, String fileName,String tag, Promise promise) {
        if(!isConnect(promise)){
            return;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    tagMap.put(tag,new FTPDownInfoBean(url,fileName,locaUrl,tag,promise,true,FTPDownInfoBean.DOWNLOD));
                    FTPFile[] files = pClient.listFiles(url);
                    if (files == null || files.length <= 0) {
                        promise.resolve("[]");
                    }

                    File localFile = new File(locaUrl);
                    long localBeginSize=0;
                    if (localFile.exists()) {
                        localBeginSize = localFile.length();
                        if (localBeginSize == files[0].getSize()) {
                            Log.d(kRNJMFTPSyncFileManager, "文件已经存在");
                            JSONObject jsonObject = new JSONObject();
                            jsonObject.put("tag", tag);
                            promise.resolve(jsonObject.toString());
                            return;
                        } else if (localBeginSize > files[0].getSize()) {
                            localFile.delete();
                            localFile.mkdirs();
                            localBeginSize=0;
                        }
                    }
                    if(downloadByUnit(url,fileName, locaUrl, localBeginSize, files[0].getSize(),tag)){
                        JSONObject jsonObject=new JSONObject();
                        jsonObject.put("tag",tag);
                        promise.resolve(jsonObject.toString());
                    }else{
//                        Log.d(kRNJMFTPSyncFileManager, "文件下载错误");
//                        promise.reject("804",ErrorToJsBean.getInstance().getErrorJson("804","下载错误"));
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    Log.d(kRNJMFTPSyncFileManager, "文件下载错误");
                    promise.reject("804",ErrorToJsBean.getInstance().getErrorJson("804","下载错误"));
                }
            }
        }).start();
    }

    private Boolean downloadByUnit(String url,String fileName, String locaUrl, long beginSize, long endSize,String tag) throws Exception {

        if(!isConnect(tagMap.get(tag).promise)){
            return false;
        }

        File localFile = new File(locaUrl+fileName);
        long waitSize = endSize - beginSize;

        //切换服务器空间
        pClient.changeWorkingDirectory(url);

        //进行断点续传，并记录状态
        FileOutputStream out = new FileOutputStream(localFile, true);
        //把文件指针移动到 开始位置
        pClient.setRestartOffset(beginSize);
        InputStream in = pClient.retrieveFileStream(new String(url.getBytes("GBK"), "iso-8859-1"));
        byte[] bytes = new byte[1024];
        int c;
        double finishSize = 0;
        double finishPercent = 0;
        while ((c = in.read(bytes)) != -1&&tagMap.get(tag).isRun) {
            out.write(bytes, 0, c);
            finishSize += c;
            if (finishSize > waitSize) {
                Log.d(kRNJMFTPSyncFileManager, "下载完成");
                JSONObject jsonObject=new JSONObject();
                jsonObject.put("path",locaUrl+fileName);
                jsonObject.put("progress",1);
                jsonObject.put("tag",tag);
                sendEvent("listeningFTPProgressCellBack",jsonObject.toString());
            }
            if ((finishSize / waitSize) - finishPercent > 0.01) {
                finishPercent = finishSize / waitSize;
                Log.d(kRNJMFTPSyncFileManager, "文件下载进度"+finishPercent);
                JSONObject jsonObject=new JSONObject();
                jsonObject.put("path",locaUrl+fileName);
                jsonObject.put("progress",finishPercent);
                jsonObject.put("tag",tag);
                sendEvent("listeningFTPProgressCellBack",jsonObject.toString());
            }
        }
        in.close();
        out.close();
        return pClient.completePendingCommand();
    }

    /**
     *
     * @param remoteFolder  FTP文件夹路径
     * @param localFilePath  本地文件路径
     * @param remoteFileName FTP文件名字
     * @param tag
     * @param promise
     */
    @ReactMethod
    public void uploadFTPFile(String remoteFolder,String localFilePath,String remoteFileName,boolean overWrite,String tag,Promise promise){
        if(!isConnect(promise)){
            return;
        }
        FTPDownInfoBean ftpDownInfoBean=new FTPDownInfoBean(remoteFolder,remoteFileName,localFilePath,tag,promise,true,FTPDownInfoBean.UPDATA);
        ftpDownInfoBean.tempFileName=System.currentTimeMillis()+remoteFileName;
        ftpDownInfoBean.overWrite=overWrite;

        tagMap.put(tag,ftpDownInfoBean);
        upload(remoteFolder,localFilePath,ftpDownInfoBean.tempFileName,tag,promise);
    }

    private void upload(String remoteFolder,String localFilePath,String remoteFileName,String tag,Promise promise){
        if(!isConnect(promise)){
            return;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                //读取文件
                try {

                    Log.d(kRNJMFTPSyncFileManager, "upload path上传地址:"+remoteFolder+"     \n本地文件路径:"+localFilePath+"\n本地文件名:" +remoteFileName);

                    //创建远程路径
                    pClient.makeDirectory(remoteFolder);

                    Log.d(kRNJMFTPSyncFileManager, "创建目录"+remoteFolder);

                    //设置上传路径,远程站点路径
                    boolean falg = pClient.changeWorkingDirectory(remoteFolder);

                    Log.d(kRNJMFTPSyncFileManager, "跳转到站点");

                    File LocalFile = new File(localFilePath);
                    Log.d(kRNJMFTPSyncFileManager, "本地文件"+localFilePath);
                    int n = -1;
                    long localSize = LocalFile.length();
                    long remoteSiez=getRemoteFileSize(remoteFolder,remoteFileName);
                    if(localSize==remoteSiez&&localSize!=0){
                        Log.d(kRNJMFTPSyncFileManager, "文件已经存在");
                        JSONObject jsonObject = new JSONObject();
                        jsonObject.put("tag", tag);
                        promise.resolve(jsonObject.toString());
                    }else if(localSize<remoteSiez){
                        Log.d(kRNJMFTPSyncFileManager, "");
                        //先删除服务器的文件,然后重命名
                        pClient.dele(remoteFolder+remoteFileName);
                    }


                    long waitSize = localSize - remoteSiez;
                    Log.d(kRNJMFTPSyncFileManager, "远程文件大小"+remoteSiez+"本地文件大小: "+localSize+" 偏移量: "+waitSize);



                    //把文件指针移动到 开始位置
                    pClient.setRestartOffset(remoteSiez);

                    int bufferSize = pClient.getBufferSize();
                    byte[] buffer = new byte[bufferSize];


                    double finishSize = 0;
                    double finishPercent = 0;

                    FileInputStream fileInputStream = new FileInputStream(LocalFile);
                    fileInputStream.skip(remoteSiez);

                    OutputStream outputstream = pClient.storeFileStream(remoteFolder+remoteFileName);

                    while ((n = fileInputStream.read(buffer)) != -1&&tagMap.get(tag).isRun) {
                        outputstream.write(buffer);
                        finishSize += n;
                        FTPDownInfoBean ftpDownInfoBean=tagMap.get(tag);
                        if (finishSize > waitSize) {
                            Log.d(kRNJMFTPSyncFileManager, "上传完成");
                            JSONObject jsonObject=new JSONObject();
                            //如果覆盖文件
                            if(ftpDownInfoBean.overWrite){
                                jsonObject.put("path",ftpDownInfoBean.fileName);
                                //先删除服务器的文件,然后重命名
                                pClient.dele(remoteFolder+remoteFileName);
                                //重命名文件
                                pClient.rename(remoteFolder+ftpDownInfoBean.tempFileName,remoteFolder+ftpDownInfoBean.fileName);
                            }else{
                                //不覆盖
                                jsonObject.put("path",ftpDownInfoBean.tempFileName);
                            }
                            jsonObject.put("progress",1);
                            jsonObject.put("tag",tag);
                            sendEvent("listeningFTPProgressCellBack",jsonObject.toString());
                        }

                        if ((finishSize / waitSize) - finishPercent > 0.01) {
                            finishPercent = finishSize / waitSize;
                            Log.d(kRNJMFTPSyncFileManager, "文件上传进度"+finishPercent);
                            JSONObject jsonObject=new JSONObject();
                            jsonObject.put("path",ftpDownInfoBean.fileName);
                            jsonObject.put("progress",finishPercent);
                            jsonObject.put("tag",tag);
                            sendEvent("listeningFTPProgressCellBack",jsonObject.toString());
                        }
                    }
                    fileInputStream.close();
                    outputstream.flush();
                    outputstream.close();

                } catch (Exception e) {
                    e.printStackTrace();
                    Log.d(kRNJMFTPSyncFileManager, "上传报异常了");
                }
            }
        }).start();
    }

    private long getRemoteFileSize(String remote,String fileName){
        //获取远程文件大小,方面做断点续传
        try {
            FTPFile[] ftpFiles = pClient.listFiles(remote);
            if(ftpFiles.length>0){
                return ftpFiles[0].getSize();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
       return 0;
    }
    @ReactMethod
    public void deleteFTPFile(String path,Promise promise){
        if(!isConnect(promise)){
            return;
        }
        try {
            pClient.dele(path);
            promise.resolve(null);
        } catch (IOException e) {
            e.printStackTrace();
            promise.reject("806",ErrorToJsBean.getInstance().getErrorJson("806","删除失败"));
        }
    }

    @ReactMethod
    public void moveFTPFile(){
//        if(!isConnect(promise)){
//            return;
//        }
//        pClient.
    }

    @ReactMethod
    public void ftpPause(String tag,Promise promise){
        if(!isConnect(promise)){
            return;
        }
        FTPDownInfoBean ftpDownInfoBean=tagMap.get(tag);
        ftpDownInfoBean.isRun=false;
        tagMap.put(tag,ftpDownInfoBean);
        promise.resolve(null);
        Log.d(kRNJMFTPSyncFileManager, "ftp暂停操作 : tag: "+tag);
    }

    @ReactMethod
    public void ftpResume(String tag,Promise promise){
        if(!isConnect(promise)){
            return;
        }
        FTPDownInfoBean ftpDownInfoBean=tagMap.get(tag);
        ftpDownInfoBean.isRun=true;
        tagMap.put(tag,ftpDownInfoBean);
        promise.resolve(null);
        if(ftpDownInfoBean.type==FTPDownInfoBean.DOWNLOD){
            downFTPFile(ftpDownInfoBean.url,ftpDownInfoBean.fileName,ftpDownInfoBean.locaUrl,ftpDownInfoBean.tag,ftpDownInfoBean.promise);
        }else{
            upload(ftpDownInfoBean.url,ftpDownInfoBean.locaUrl,ftpDownInfoBean.tempFileName,tag,promise);
        }
        Log.d(kRNJMFTPSyncFileManager, "ftp恢复操作 : tag: "+tag);
    }

    private boolean isConnect(Promise promise){
        if(!pClient.isConnected()){
            promise.reject("810",ErrorToJsBean.getInstance().getErrorJson("810","FTP链接断开"));
            Log.d(kRNJMFTPSyncFileManager, "FTP链接断开");
            return false;
        }
        return true;
    }

}
