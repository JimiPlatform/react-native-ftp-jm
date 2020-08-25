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
import com.jimi.ftpapi.listener.JMBaseListener;
import com.jimi.ftpapi.listener.ListeningKye;
import com.jimi.ftpapi.model.ToJSBean;
import com.jimi.ftpapi.model.ToJSData;
import com.jimi.ftpapi.model.socket.ErrorToJsBean;
import com.jimi.ftpapi.udp.JMUDPSoket;
import com.jimi.ftpapi.utils.MyGson;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.HashMap;
import java.util.Map;


public class JMUDPScoketManager extends ReactContextBaseJavaModule{
    private String TAG = "JMRNFTP";
    private ReactContext mReactContext;
    private static final String kRNJMUDPScoketManager = "kRNJMUDPSocketManager";
    private JMUDPSoket jmudpSoket=new JMUDPSoket();


    public JMUDPScoketManager(ReactApplicationContext reactContext) {
        super(reactContext);
        mReactContext=reactContext;
        reactContext.addLifecycleEventListener(mLifecycleEventListener);
    }

    @Override
    public Map<String, Object> getConstants() {
        Map<String, Object> constants = new HashMap<>();
        constants.put(kRNJMUDPScoketManager, kRNJMUDPScoketManager);
        return constants;
    }

    @Override
    public String getName() {
        return "JMUDPScoketManager";
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
        Log.i(TAG, "JMUDPScoketManager sendEvent(" +methodName + ") JS：" + resultJson);
        mReactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class).emit(kRNJMUDPScoketManager, resultJson);
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
            jmudpSoket.destroy();
        }
    };

    @ReactMethod
    public void configUDPSocket(String host, int port, int timeout, Promise promise){
        Log.i(TAG, "configUDPSocket host：" + host + " port:" + port + " timeout:" + timeout);
        jmudpSoket.configUDPSocket(host, port, timeout, new JMBaseListener() {
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
    public void send(String data, int tag, Promise promise){
        Log.i(TAG, "send:" + data + " potagrt:" + tag);
        jmudpSoket.send(data, tag, new JMBaseListener() {
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
    public void closeSocket(Promise promise) {
        Log.i(TAG, "closeSocket");
       jmudpSoket.closeSocket(new JMBaseListener() {
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

}
