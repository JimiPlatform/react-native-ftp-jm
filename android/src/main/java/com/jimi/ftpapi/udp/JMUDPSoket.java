package com.jimi.ftpapi.udp;

import android.util.Log;

import com.jimi.ftpapi.listener.JMBaseListener;
import com.jimi.ftpapi.listener.ListeningKye;
import com.jimi.ftpapi.model.socket.ErrorToJsBean;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

public class JMUDPSoket implements JMUDPSoketImp{
    private DatagramSocket socket;
    private InetAddress serverAddress = null;
    private int port;
    private boolean isReceive=true;
    private String TAG = "JMRNFTP";
    @Override
    public void configUDPSocket(String host, int port, int timeout, JMBaseListener jmBaseListener) {
        try {
            if(timeout<=0){
                jmBaseListener.onFail("601",ErrorToJsBean.getInstance().getErrorJson("601","链接失败,timeout参数设置不正确"));
                return;
            }

            if(host==null||port<0){
                jmBaseListener.onFail("601",ErrorToJsBean.getInstance().getErrorJson("601","host或者port参数 错误"));
                return;
            }

            String strUrl[]=host.split("//");
            if(strUrl.length==2){
                host=strUrl[1];
            }else{
                host=strUrl[0];
            }

            isReceive=true;
            socket = new DatagramSocket();
            socket.setBroadcast(true);
            serverAddress = InetAddress.getByName(host);
            this.port=port;
            jmBaseListener.onSuccess(null);
        } catch (SocketException e) {
            jmBaseListener.onFail("601",ErrorToJsBean.getInstance().getErrorJson("601","链接失败,设备通信故障"));
            e.printStackTrace();
        } catch (Exception e) {
            jmBaseListener.onFail("601",ErrorToJsBean.getInstance().getErrorJson("601","链接失败,IO异常!"));
            e.printStackTrace();
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                while (isReceive){
                    try{
                        byte[] b = new byte[1024];
                        DatagramPacket vPacket = new DatagramPacket(b, b.length);
                        socket.setSoTimeout(timeout<5000?5000:timeout);
                        socket.receive(vPacket);
                        String udpDate = new String(vPacket.getData(), 0, vPacket.getLength());
                        jmBaseListener.realTimeMessage(ListeningKye.listeningUDPSocketCellBack,udpDate);
                        Log.e(TAG, "run: udp收到回复"+udpDate);
                    }catch (Exception e) {
//                        e.printStackTrace();
                    }

                }

            }
        }).start();
    }

    @Override
    public void send(String data, int tag, JMBaseListener jmBaseListener) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try{
                    DatagramPacket datagramPacket = new DatagramPacket(data.getBytes(), data.length(), serverAddress, port);
                    socket.send(datagramPacket);
                    Log.e(TAG, "run: 发送消息成功"+data);
                    jmBaseListener.onSuccess(null);
                }catch (Exception e){
                    e.printStackTrace();
                    jmBaseListener.onFail("604",ErrorToJsBean.getInstance().getErrorJson("604","发送失败"));
                }
            }
        }).start();
    }

    @Override
    public void closeSocket(JMBaseListener jmBaseListener) {
        isReceive=false;
        new Thread(new Runnable() {
            @Override
            public void run() {
                try{
                    if(socket!=null){
                        socket.disconnect();
                        socket.close();
                    }
                    socket=null;
                }catch (Exception e){
                    e.printStackTrace();
                }
                jmBaseListener.onSuccess(null);
            }
        }).start();
    }

    @Override
    public void destroy() {
        isReceive=false;
    }
}
