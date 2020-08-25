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
import com.jimi.ftpapi.ftp.JMFtp;
import com.jimi.ftpapi.listener.JMBaseListener;
import com.jimi.ftpapi.model.ToJSBean;
import com.jimi.ftpapi.model.ToJSData;
import com.jimi.ftpapi.utils.MyGson;

import java.util.HashMap;
import java.util.Map;


public class JMFTPSyncFileManager extends ReactContextBaseJavaModule {
    private String TAG = "JMRNFTP";
    private ReactContext mReactContext;
    private static final String kRNJMFTPSyncFileManager = "kRNJMFTPSyncFileManager";
    private JMFtp jmFtp=new JMFtp();

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
        Log.i(TAG, "JMFTPSyncFileManager sendEvent JS：" + resultJson);
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
            jmFtp.destroy();
        }
    };

    @ReactMethod
    public void configFtpSyncFile(String baseUrl, String mode, int port, String account, String password, Promise promise) {
        Log.i(TAG, "configFtpSyncFile:" + baseUrl + " mode:" + mode + " port:" + port +
                " account:" + account + " password:" + password);
        jmFtp.configFtpSyncFile(baseUrl, mode, port, account, password, new JMBaseListener() {
            @Override
            public void realTimeMessage(String key, String data) {

            }

            @Override
            public void onSuccess(String data) {

                promise.resolve(data);
            }

            @Override
            public void onFail(String code, String errorMsg) {
                promise.reject(code,errorMsg);
            }
        });
    }

    @ReactMethod
    public void connectFTP(Promise promise) {
        Log.i(TAG, "connectFTP");
        jmFtp.connectFTP(new JMBaseListener() {
            @Override
            public void realTimeMessage(String key, String data) {

            }

            @Override
            public void onSuccess(String data) {
                promise.resolve(data);
            }

            @Override
            public void onFail(String code, String errorMsg) {
                promise.reject(code,errorMsg);
            }
        });
    }

    @ReactMethod
    public void findFTPFlies(String subPath, Promise promise) {
//        subPath = "/mnt/sdcard2/Remote/Photo";
        Log.i(TAG, "findFTPFlies:" + subPath);
        jmFtp.findFTPFlies(subPath, new JMBaseListener() {
            @Override
            public void realTimeMessage(String key, String data) {

            }

            @Override
            public void onSuccess(String data) {
                Log.i(TAG, "findFTPFlies-onSuccess:" + data);
                promise.resolve(data);
            }

            @Override
            public void onFail(String code, String errorMsg) {
                Log.i(TAG, "findFTPFlies-onFail:" + code + "," + errorMsg);
                promise.reject(code,errorMsg);
            }
        });
    }


    @ReactMethod
    public void downFTPFile(String url, String locaUrl, String fileName, String tag, Promise promise) {
        Log.i(TAG, "downFTPFile:" + url + " locaUrl:" + locaUrl + " fileName:" + fileName + " tag:" + tag);
        jmFtp.downFTPFile(url, locaUrl, fileName, tag, new JMBaseListener() {
            @Override
            public void realTimeMessage(String key, String data) {
                sendEvent(key,data);
            }

            @Override
            public void onSuccess(String data) {
                promise.resolve(data);
            }

            @Override
            public void onFail(String code, String errorMsg) {
                promise.reject(code,errorMsg);
            }
        });
    }



    /**
     * @param remoteFolder   FTP文件夹路径
     * @param localFilePath  本地文件路径
     * @param remoteFileName FTP文件名字
     * @param tag
     * @param promise
     */
    @ReactMethod
    public void uploadFTPFile(String remoteFolder, String localFilePath, String remoteFileName, boolean overWrite, String tag, Promise promise) {
        jmFtp.uploadFTPFile(remoteFolder, localFilePath, remoteFileName, overWrite, tag, new JMBaseListener() {
            @Override
            public void realTimeMessage(String key, String data) {
                sendEvent(key,data);
            }

            @Override
            public void onSuccess(String data) {
                promise.resolve(data);
            }

            @Override
            public void onFail(String code, String errorMsg) {
                promise.reject(code,errorMsg);
            }
        });
    }


    @ReactMethod
    public void deleteFTPFile(String path, Promise promise) {
       jmFtp.deleteFTPFile(path, new JMBaseListener() {
           @Override
           public void realTimeMessage(String key, String data) {
               sendEvent(key,data);
           }

           @Override
           public void onSuccess(String data) {
               promise.resolve(data);
           }

           @Override
           public void onFail(String code, String errorMsg) {
               promise.reject(code,errorMsg);
           }
       });
    }

    @ReactMethod
    public void moveFTPFile(String from, String to, boolean overWrite, Promise promise) {
        jmFtp.moveFTPFile(from, to, overWrite, new JMBaseListener() {
            @Override
            public void realTimeMessage(String key, String data) {
                sendEvent(key,data);
            }

            @Override
            public void onSuccess(String data) {
                promise.resolve(data);
            }

            @Override
            public void onFail(String code, String errorMsg) {
                promise.reject(code,errorMsg);
            }
        });

    }



    @ReactMethod
    public void ftpPause(String tag, Promise promise) {
      jmFtp.ftpPause(tag, new JMBaseListener() {
          @Override
          public void realTimeMessage(String key, String data) {
              sendEvent(key,data);
          }

          @Override
          public void onSuccess(String data) {
              promise.resolve(data);
          }

          @Override
          public void onFail(String code, String errorMsg) {
              promise.reject(code,errorMsg);
          }
      });

    }

    @ReactMethod
    public void ftpResume(String tag, Promise promise) {
       jmFtp.ftpResume(tag, new JMBaseListener() {
           @Override
           public void realTimeMessage(String key, String data) {
               sendEvent(key,data);
           }

           @Override
           public void onSuccess(String data) {
               promise.resolve(data);
           }

           @Override
           public void onFail(String code, String errorMsg) {
               promise.reject(code,errorMsg);
           }
       });

    }

    @ReactMethod
    public void closeFTP(Promise promise) {
        Log.i(TAG, "closeFTP:");
       jmFtp.closeFTP(new JMBaseListener() {
           @Override
           public void realTimeMessage(String key, String data) {
               sendEvent(key,data);
           }

           @Override
           public void onSuccess(String data) {
               promise.resolve(data);
           }

           @Override
           public void onFail(String code, String errorMsg) {
               promise.reject(code,errorMsg);
           }
       });

    }

    @ReactMethod
    public void ftpCancel(String tag, Promise promise) {
        Log.i(TAG, "ftpCancel:" + tag);
        jmFtp.ftpCancel(tag, new JMBaseListener() {
            @Override
            public void realTimeMessage(String key, String data) {
                sendEvent(key,data);
            }

            @Override
            public void onSuccess(String data) {
                promise.resolve(data);
            }

            @Override
            public void onFail(String code, String errorMsg) {
                promise.reject(code,errorMsg);
            }
        });

    }



}
