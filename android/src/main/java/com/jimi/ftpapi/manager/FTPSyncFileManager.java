package com.jimi.ftpapi.manager;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.google.gson.Gson;
import com.jimi.ftpapi.FTPDowmLoadInfo;
import com.jimi.ftpapi.FTPIDownload;
import com.jimi.ftpapi.FTPMediaFile;
import com.jimi.ftpapi.FTPMediaHelperHandle;
import com.jimi.ftpapi.FTPMediaSyncHelper;
import com.jimi.ftpapi.FTPTasksManger;
import com.jimi.ftpapi.FTPToJSBean;
import com.jimi.ftpapi.FTPWifiUtils;
import com.jimi.ftpapi.OnFTPMediaFileOperationListener;
import com.jimi.ftpapi.model.ToJSBean;
import com.jimi.ftpapi.model.ToJSData;
import com.jimi.ftpapi.utils.MyGson;

import org.apache.commons.net.ftp.FTPClient;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import okhttp3.Call;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class FTPSyncFileManager extends ReactContextBaseJavaModule implements FTPMediaSyncHelper.MediaSyncStateListener, OnFTPMediaFileOperationListener {
    private final String TAG = "FTPSyncFile";
    private ReactContext mReactContext = null;
    private String mImei = null;
    private boolean mIsVisible = false;         //当前界面是否有效
    private boolean mIsConnectWiFi = false;     //是否已连接设备WIFI
    private boolean mIsConnectDevice = false;   //是否已连接设备FTP
    private int connectTimes = 0;

    public static String mSmallRootDirPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/JMSmallApp/";
    public static String smallAppRootDirPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/JMSmallApp/"; //小程序的根目录

    private FTPWifiUtils mWifiUtils = null;
    private FTPMediaSyncHelper mSyncHelper = null;
    private FTPClient mFTPClient = null;

    private List<Serializable> mMediaFilesList = null;
    private FTPMediaHelperHandle mMediaHelperHandle = null;
    private String mMediaType = MEDIA_PHOTO;


    public static final String kRNFTPSyncFileManager = "kRNFTPSyncFileManager";
    public static final String MEDIA_PHOTO = "media_photo";
    public static final String MEDIA_VIDEO = "media_video";
    public static final String MEDIA_ALL = "media_all";

    @Override
    public String getName() {
        return "FTPSyncFileManager";
    }

    public FTPSyncFileManager(ReactApplicationContext reactContext) {
        super(reactContext);
        mReactContext = reactContext;
        mIsVisible = true;

        reactContext.addLifecycleEventListener(mLifecycleEventListener);
    }

    @Override
    public Map<String, Object> getConstants() {
        Map<String, Object> constants = new HashMap<>();
        constants.put(kRNFTPSyncFileManager, kRNFTPSyncFileManager);
        return constants;
    }

    //监听Activity的生命周期
    private final LifecycleEventListener mLifecycleEventListener = new LifecycleEventListener() {
        @Override
        public void onHostResume() {
            checkDeviceConnect(mImei);
        }

        @Override
        public void onHostPause() {
        }

        @Override
        public void onHostDestroy() {
            mIsVisible = false;
            closeFTP();
        }
    };

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
        Log.i(TAG, "FTPSyncFile send JS：" + resultJson);
        mReactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class).emit(kRNFTPSyncFileManager, resultJson);
    }

    public boolean isVisible() {
        return mIsVisible;
    }

    @ReactMethod
    public void showFtp(){
     Log.d("hdyipftp","ftp");

    }

    /**
     * 获取当前WIFI信息
     *
     * @param callback RN回调接口
     */
    @ReactMethod
    public void currentWiFi(Callback callback) {
        WifiManager wifiManager = (WifiManager) mReactContext.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        String currentSSID = wifiManager.getConnectionInfo().getSSID();
        callback.invoke(currentSSID);
        Log.i(TAG, "Current SSID：" + currentSSID);
    }

    /**
     * 发送打开设备WIFI热点的指令
     *
     * @param url 服务器地址
     * @param imei 设备IMEI
     * @param appKey 可能需要几米圈1.0 AppKey
     * @param secret 可能需要几米圈1.0 secret
     * @param token 登录Token
     */
    @ReactMethod
    public void openDeviceWIFI(String url, String imei, String appKey, String secret ,String token) {
        if (imei.length() != 15) {
            return;
        }
        this.mImei = imei;

        if (!checkDeviceConnect(imei)) {
            Map<String, Object> map = new HashMap();
            map.put("method", "jimi.smarthome.device.custom.instruct.send");
            map.put("imei", imei);
            map.put("instruct", "WIFI,ON");
            map.put("accessToken", token);
            map.put("app_key", appKey);     //449A7D0E9C1911E7BEDB00219B9A2EF3
            map.put("timestamp", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(System.currentTimeMillis())));
            map.put("sign", signToParams(map, secret));     //695c1a459c1911e7bedb00219b9a2ef3

            FormBody.Builder formBodyBuilder = new FormBody.Builder();
            for (String key: map.keySet()){
                formBodyBuilder.add(key, map.get(key).toString());
            }
            RequestBody body = formBodyBuilder.build();

            OkHttpClient okHttpClient = new OkHttpClient();
            final Request request = new Request.Builder().url(url).post(body).build();
            okHttpClient.newCall(request).enqueue(new okhttp3.Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    FTPToJSBean bean = new FTPToJSBean();
                    bean.success = false;
                    bean.msg = "设备WIFI打开失败";
                    bean.errMsg = e.getMessage();
                    sendEvent("onOpenDeviceWIFI", bean);
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    try {
                        JSONObject json = new JSONObject(response.body().string());
                        Log.i(TAG, "Open WiFi result：" + json.toString());
                        int code = json.getInt("code");
                        FTPToJSBean bean = new FTPToJSBean();
                        bean.code = code;
                        if (code == 0) {
                            bean.success = true;
                            bean.msg = "设备WIFI打开成功,请连接wifi";
                        } else {
                            bean.success = false;
                            bean.msg = "设备WIFI打开失败";
                        }
                        sendEvent("onOpenDeviceWIFI", bean);
                    } catch (JSONException e) {
                        FTPToJSBean bean = new FTPToJSBean();
                        bean.success = false;
                        bean.msg =  "设备WIFI打开失败";
                        sendEvent("onOpenDeviceWIFI", bean);
                    }
                }
            });
        } else {
            FTPToJSBean bean = new FTPToJSBean();
            bean.success = true;
            bean.msg = "设备WIFI打开成功,wifi已连接";
            sendEvent("onOpenDeviceWIFI", bean);
        }
    }

    /**
     * 连接设备FTP服务
     *
     * @param imei 设备IMEI
     */
    @ReactMethod
    public void connectFTP(String imei) {
        if (imei.length() != 15) {
            return;
        }

        this.mImei = imei;

        connectTimes = 0;
        boolean result = startSyscConnectDevice();
        if (!result) {
            FTPToJSBean bean = new FTPToJSBean();
            bean.success = false;
            bean.msg = "未连接设备WIFI";
            sendEvent("onConnectFTP", bean);
        }
    }

    /**
     * 断开与设备FTP服务的连接
     */
    @ReactMethod
    public void closeFTP() {
        mIsConnectWiFi = false;
        mIsConnectDevice = false;
        connectTimes = 0;

        if (mSyncHelper != null) {
            if (mSyncHelper.isRunning()) {
                mSyncHelper.setSyncStateListener(null);
                mSyncHelper.setDestroyTask(true);
                mSyncHelper.cancel(true);
            }
        }
        mWifiUtils = null;
        mFTPClient = null;

        if (mMediaHelperHandle != null) {
            mMediaHelperHandle.resetHandleHelper();
            mMediaHelperHandle = null;
        }
    }

    /**
     * 检索所有的图片或视频
     *
     * @param isVideo 是否检索视频
     */
    @ReactMethod
    public void findAllFile(boolean isVideo) {
        if (!mIsConnectDevice) {
            FTPToJSBean bean = new FTPToJSBean();
            bean.success = false;
            bean.msg = "未连接设备WIFI";
            sendEvent("onConnectFTP", bean);
            return;
        }

        mMediaType = isVideo ? MEDIA_VIDEO : MEDIA_PHOTO;
        Log.i("FTPSyncFileManager","收到findAllFile指令");
        initMediaDownloadHelper();
    }

    /**
     * 下载文件
     *
     * @param fileArray 文件列表
     */
    @ReactMethod
    public void downloadFile(ReadableArray fileArray) {
        if (!mIsConnectDevice || mMediaHelperHandle == null) {
            return;
        }
        ArrayList<Object> fileList = fileArray.toArrayList();
        for (Object name : fileList) {
            for (Serializable mediaFileT : mMediaFilesList) {
                if (mediaFileT instanceof FTPMediaFile) {
                    FTPMediaFile mediaFile = (FTPMediaFile) mediaFileT;
                    if (mediaFile.name.equals(name)) {
                        if (mediaFile.isDownload) {
                            FTPDowmLoadInfo info = mediaFile.getDowmLoadInfo();
                            info.progress = 1.0;
                            sendEvent("onDownloadFile", info);
                        } else {
                            mediaFile.state = FTPIDownload.STATE_WAIT;
                            mMediaHelperHandle.downLoadDeviceFile(mediaFile);
                        }
                        break;
                    }
                }
            }
        }
    }

    /**
     * 暂停文件下载
     *
     * @param name 文件的名称
     */
    @ReactMethod
    public void pauseFile(String name) {
        for (Serializable mediaFileT : mMediaFilesList) {
            if (mediaFileT instanceof FTPMediaFile) {
                FTPMediaFile mediaFile = (FTPMediaFile) mediaFileT;
                if (mediaFile.name.equals(name)) {
                    FTPTasksManger.getInstance().stop(mediaFile);
                    mediaFile.state = FTPIDownload.STATE_PAUSE;
                    break;
                }
            }
        }
    }

    /**
     * 删除设备及本地文件
     *
     * @param fileArray 文件列表
     */
    @ReactMethod
    public void deleteFile(ReadableArray fileArray) {
        if (!mIsConnectDevice || mMediaHelperHandle == null) {
            return;
        }
        ArrayList<Object> fileList = fileArray.toArrayList();
        Collection<Serializable> deleteList = new TreeSet<>();
        for (Object obj : fileList) {
            for (Serializable mediaFileT : mMediaFilesList) {
                if (mediaFileT instanceof FTPMediaFile) {
                    FTPMediaFile mediaFile = (FTPMediaFile) mediaFileT;
                    if (mediaFile.name.equals(obj)) {
                        deleteList.add(mediaFile);
                    }
                }
            }
        }

        if (deleteList.size() > 0) {
            mMediaHelperHandle.deleteDeviceFiles(deleteList);
        }
    }

    /**
     * 对发送命令参数进行编码
     *
     * @param map 命令参数字典
     * @param secret App Secret
     */
    private String signToParams(Map<String, Object> map, String secret) {
        Set<String> keysSet = new TreeSet<String>(new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                return -o2.compareTo(o1);
            }
        });
        keysSet.addAll(map.keySet());

        String signStr = secret;

        Iterator<String> iterator = keysSet.iterator();
        while (iterator.hasNext()){
            String key = iterator.next();
            String value = (String) map.get(key);
            if (!value.isEmpty()) {
                signStr += key + value;
            }
        }
        signStr += secret;

        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(signStr.getBytes());
            byte[] encryption = md5.digest();
            signStr = bytesToHexString(encryption);
        } catch (NoSuchAlgorithmException e) {
            return "";
        }

        return signStr.toUpperCase();
    }

    /**
     * 将byte数组转换为十六进制的字符串
     * @param data
     * @return
     */
    public String bytesToHexString(byte[] data){
        String result = "";
        for( byte b : data ){
            result += String.format("%02x",b);
        }
        return  result;
    }

    /**
     * 检测是否已连接设备
     *
     * @param imei 设备IMEI
     */
    public boolean checkDeviceConnect(String imei) {
        if (imei == null || imei.length() != 15) {
            mIsConnectWiFi = false;
            return false;
        }

        if (mWifiUtils == null || !mWifiUtils.getSSID().equals(imei)) {
            String wifiPwd = imei.substring(7, 15);
            mWifiUtils = new FTPWifiUtils(mReactContext, imei, wifiPwd);
        }

        mIsConnectWiFi =  mWifiUtils.isTargetWifi();
        Log.i("checkDeviceConnect","检测是否已连接设备WiFi:" + mIsConnectWiFi);
        return mIsConnectWiFi;
    }

    /**
     * 启动连接设备FTP服务
     */
    public boolean startSyscConnectDevice() {
        mIsConnectDevice = false;

        if (mSyncHelper != null) {
            if (mSyncHelper.isRunning()) {
                mSyncHelper.setSyncStateListener(null);
                mSyncHelper.setDestroyTask(true);
                mSyncHelper.cancel(true);
            }
        }
        mIsConnectWiFi = checkDeviceConnect(mImei);
        if (mIsConnectWiFi) {
            mSyncHelper = new FTPMediaSyncHelper();
            mSyncHelper.setSyncStateListener(this);
            mSyncHelper.execute(mWifiUtils);
            return true;
        }

        return false;
    }

    /**
     * 初始化媒体下载器
     */
    private void initMediaDownloadHelper() {
        FTPMediaHelperHandle.MEDIA_PATH =mSmallRootDirPath + File.separator + mImei + File.separator + "FTPMedia";
        FTPMediaHelperHandle.MEDIA_PHOTO_PATH =mSmallRootDirPath + File.separator + mImei + File.separator + "FTPMedia";
        Log.i("FTPSyncFileManager","下载路径：" + FTPMediaHelperHandle.MEDIA_PATH);
        if (mMediaHelperHandle != null) {
            mMediaHelperHandle.resetHandleHelper();
            mMediaHelperHandle.setMediaType(mMediaType);
        } else {
            mMediaHelperHandle = new FTPMediaHelperHandle(this, mMediaType);
        }
        mMediaHelperHandle.setOnMediaFileOperationListener(this);
        mMediaHelperHandle.prepareDownLoadWork();
        mMediaHelperHandle.imei = mImei;
    }

    @Override
    public void onMediaSyncStart() {
        Log.i(TAG, "onMediaSyncStart");
    }

    @Override
    public void onMediaSyncError(String errorMessage) {
        Log.i(TAG, "onMediaSyncError:" + errorMessage);
        mIsConnectDevice = false;
        FTPToJSBean bean = new FTPToJSBean();
        bean.success = false;
        bean.msg = "与设备连接异常!";
        bean.errMsg = errorMessage;
        sendEvent("onConnectFTP", bean);
    }

    @Override
    public void onMediaSyncComplete(FTPClient client) {
        Log.i(TAG, "onMediaSyncComplete:" + client.getPassiveHost());
        mIsConnectDevice = true;
        this.mFTPClient = client;

        FTPToJSBean bean = new FTPToJSBean();
        bean.success = true;
        bean.msg = "已经成功连接设备!";
        sendEvent("onConnectFTP", bean);
    }

    @Override
    public void onLocalMediaFileScanSuccessful(List<Serializable> itemList, int[] itemIndexArray) {
        Log.i(TAG, "onLocalMediaFileScanSuccessful:" + itemList.toString());
    }

    @Override
    public void onLocalMediaFileCopySuccessful() {
        Log.i(TAG, "onLocalMediaFileCopySuccessful");
    }

    @Override
    public void onLocalMediaFileCopyFailed() {
        Log.i(TAG, "onLocalMediaFileCopyFailed");
    }

    @Override
    public void onLocalMediaFileDeleteSuccessful(int[] itemIndexArray,List<Serializable>itemList) {
        Log.i(TAG, "onLocalMediaFileDeleteSuccessful");

        mMediaFilesList = itemList;      //保存当前的文件列表

        FTPToJSBean bean = new FTPToJSBean();
        bean.success = true;
        sendEvent("onDeleteFile", bean);
    }

    @Override
    public void onLocalMediaFileDeleteFailed(int[] itemIndexArray,List<Serializable>itemList) {
        FTPToJSBean bean = new FTPToJSBean();
        bean.success = false;
        sendEvent("onDeleteFile", bean);
    }

    @Override
    public void onDeviceFileListGetSuccessful(List<Serializable> itemList, int[] itemIndexArray) {

        mMediaFilesList = itemList;      //保存当前的文件列表
        ArrayList<ArrayList> fileFirstList = new ArrayList<ArrayList>();  //总的列表
        ArrayList<String> fileSecondList = new ArrayList<String>();  //分级列表
        for (int i = 1, index = 0; i <= itemList.size(); i++) {  //列表中的第一个是时间戳，所以需要i从第二开始
            if (i != itemIndexArray[index]) {
                if(i < itemList.size()){
                    FTPMediaFile mediaFile = (FTPMediaFile) itemList.get(i);
                    fileSecondList.add(mediaFile.toJson());
                }
            } else {
                index ++;
                fileFirstList.add(fileSecondList);
                fileSecondList = new ArrayList<String>();
            }

        }

        sendEvent("onFindAllFile", fileFirstList);
    }

    @Override
    public void onDeviceFileListGetFailed() {
        Log.i(TAG, "onDeviceFileListGetFailed");
    }

    @Override
    public void onProgressUpdated(int position, int progress) {
        Log.i(TAG, "onProgressUpdated:" + position + "," + progress);

        FTPMediaFile mediaFile = (FTPMediaFile) mMediaHelperHandle.getItemList().get(position);
        FTPDowmLoadInfo info = mediaFile.getDowmLoadInfo();
        info.state = 1;
        sendEvent("onDownloadFile", info);
    }

    @Override
    public void onDeviceFileDownloadSuccessful(int position) {
        FTPMediaFile mediaFile = (FTPMediaFile) mMediaHelperHandle.getItemList().get(position);
        mediaFile.isDownload = true;
        FTPDowmLoadInfo info = mediaFile.getDowmLoadInfo();
        info.progress = 1;
        info.state = 2;
        sendEvent("onDownloadFile", info);
    }

    @Override
    public void onDeviceFileDownloadFailed(int position, FTPMediaFile pMediaFile) {
        Log.i(TAG, "onDeviceFileDownloadFailed:" + pMediaFile.url);

        FTPMediaFile mediaFile = (FTPMediaFile) mMediaHelperHandle.getItemList().get(position);
        FTPDowmLoadInfo info = mediaFile.getDowmLoadInfo();
        info.progress = mediaFile.process;
        info.state = -1;    //下载失败

        sendEvent("onDownloadFile", info);
    }

    @Override
    public void onDownloadWorkPrepared() {
        Log.i(TAG, "onDownloadWorkPrepared");
    }

    @Override
    public void onUiRefreshed(int[] itemIndexArray, List<Serializable> itemList) {
    }
}
