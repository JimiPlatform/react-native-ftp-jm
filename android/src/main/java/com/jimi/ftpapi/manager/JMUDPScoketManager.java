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
import com.jimi.ftpapi.model.ToJSBean;
import com.jimi.ftpapi.model.ToJSData;
import com.jimi.ftpapi.model.socket.ErrorToJsBean;
import com.jimi.ftpapi.utils.MyGson;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.HashMap;
import java.util.Map;


public class JMUDPScoketManager extends ReactContextBaseJavaModule{
    private DatagramSocket socket;
    private ReactContext mReactContext;
    private static final String kRNJMUDPScoketManager = "kRNJMUDPScoketManager";
    private InetAddress serverAddress = null;
    private int port;
    private boolean isReceive=true;

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
        Log.i(kRNJMUDPScoketManager, "JMUDPScoketManager send JS：" + resultJson);
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
            isReceive=false;
        }
    };

    @ReactMethod
    public void configUDPSocket(String host,int port, int timeout,Promise promise){
        try {
            socket = new DatagramSocket();
            socket.setBroadcast(true);
            serverAddress = InetAddress.getByName(host);
            this.port=port;
            promise.resolve(null);
            Log.d(kRNJMUDPScoketManager, "configUDPSocket: host:"+host+" port:"+port+" timeout"+timeout);
        } catch (SocketException e) {
            promise.reject("601",ErrorToJsBean.getInstance().getErrorJson("601","链接失败,设备通信故障"));
            e.printStackTrace();
        } catch (Exception e) {
            promise.reject("601",ErrorToJsBean.getInstance().getErrorJson("601","链接失败,IO异常!"));
            e.printStackTrace();
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                while (isReceive){
                    try{
                        byte[] b = new byte[1024];
                        DatagramPacket vPacket = new DatagramPacket(b, b.length);
                        socket.setSoTimeout(timeout<=0?15:timeout);
                        socket.receive(vPacket);
                        String udpDate = new String(vPacket.getData(), 0, vPacket.getLength());
                        sendEvent("listeningUDPScoketCellBack",udpDate);
                    }catch (Exception e){
                        e.printStackTrace();
                    }

                }

            }
        }).start();

    }

    @ReactMethod
    public void send(String data,int tag,int timeout,Promise promise){
        new Thread(new Runnable() {
            @Override
            public void run() {
                try{
                    DatagramPacket datagramPacket = new DatagramPacket(data.getBytes(), data.length(), serverAddress, port);
                    socket.send(datagramPacket);
                    Log.d(kRNJMUDPScoketManager, "run: 发送消息成功"+data);
                    promise.resolve(null);
                }catch (Exception e){
                    e.printStackTrace();
                    promise.reject("604",ErrorToJsBean.getInstance().getErrorJson("604","发送失败"));
                }
            }
        }).start();
    }

    @ReactMethod
    public void closeSocket(Promise promise){
        isReceive=false;
        try{
            if(socket!=null){
                socket.disconnect();
                socket.close();
            }
            socket=null;
        }catch (Exception e){
            e.printStackTrace();
        }
        promise.resolve(null);
    }

}
