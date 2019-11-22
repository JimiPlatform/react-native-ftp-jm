package com.jimi.ftpapi.manager;

import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;

import com.jimi.ftpapi.FTPUtil2;
import com.jimi.ftpapi.FTPWifiUtils;

import org.apache.commons.net.ftp.FTPClient;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import static android.os.AsyncTask.Status.RUNNING;

public class JMFTPMediaSyncHelper extends AsyncTask<FTPWifiUtils, Integer, FTPClient> {
    private String outMessage = "jimi";
    private MediaSyncStateListener mSyncStateListener;
    private FTPUtil2 mFtpUtil2;
    private static final String KEY_IP = "ip";
    private static final String KEY_PORT = "port";
    private static final String KEY_RSP_PORT = "rsp_port";
    private static final String KEY_FIR_PORT = "fir_port";
    private static final String KEY_PHOTO_DIRS = "photo_dirs";
    private static final String KEY_VIDEO_DIRS = "video_dirs";
    private FTPClient mClient;
    private boolean destroyTask = false;
    private String errorMessage = "未知错误!";
    private int mPort = 1712;

    public void setDestroyTask(boolean destroyTask) {
        this.destroyTask = destroyTask;
    }

    public JMFTPMediaSyncHelper() {
        mFtpUtil2 = FTPUtil2.getInstance();
    }

    public void setSyncStateListener(MediaSyncStateListener syncStateListener) {
        mSyncStateListener = syncStateListener;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        if (destroyTask) return;
        if (mSyncStateListener != null) mSyncStateListener.onMediaSyncStart();
    }

    @Override
    protected FTPClient doInBackground(FTPWifiUtils... args) {
        FTPWifiUtils wifiUtils = args[0];
        WifiManager.MulticastLock lock = null;
        if (destroyTask) {
            if (mClient != null) mFtpUtil2.logOutFtp(mClient);
            wifiUtils.disConnect();
            mClient = null;
            mFtpUtil2 = null;
            return null;
        }
        boolean connectResult = wifiUtils.connectWifi();
        int connectTimes = 0;
        while (!connectResult && connectTimes < 5) {
            connectResult = wifiUtils.connectWifi();
            Log.e("lzx", "重连次数 ====================" + connectTimes);
            SystemClock.sleep(500);
            connectTimes++;
        }

        if (connectResult) {
            try {
                SystemClock.sleep(5000);
                lock = wifiUtils.createLock("dk.aboaya.pingpong");
                lock.acquire();
                DatagramSocket socket = new DatagramSocket();
                socket.setBroadcast(true);
                InetAddress vAddress = wifiUtils.getBroadcastAddress();
                DatagramPacket datagramPacket = new DatagramPacket(outMessage.getBytes(), outMessage.length(), vAddress, mPort);
                socket.send(datagramPacket);
                byte[] b = new byte[1024];
                DatagramPacket vPacket = new DatagramPacket(b, b.length);
                socket.setSoTimeout(30000);
                socket.receive(vPacket);
                String udpDate = new String(vPacket.getData(), 0, vPacket.getLength());
                Bundle bundle = parseJSON(udpDate, vPacket);
                ftpLogin(bundle);
            } catch (SocketException e) {
                errorMessage = "设备通信故障!";
            } catch (UnknownHostException e) {
                errorMessage = "非法的IP地址";
            } catch (SocketTimeoutException e) {
                errorMessage = "连接设备超时!";
            } catch (IOException e) {
                Log.e("lzx", " ====================" + e.getMessage());
                errorMessage = "IO异常!";
            } finally {
                if (lock != null) lock.release();
            }

        } else {
            errorMessage = "连接热点失败!";
        }

        return mClient;
    }

    private void ftpLogin(Bundle bundle) {
        if (bundle != null) {
            mClient = mFtpUtil2.getClient(bundle.getString(KEY_IP), bundle.getInt(KEY_PORT));
            if (mClient != null) {
                mFtpUtil2.setField(bundle.getString(KEY_PHOTO_DIRS), bundle.getString(KEY_VIDEO_DIRS));
            } else {
                errorMessage = "FTP登陆失败!";
            }
        } else {
            errorMessage = "获取设备信息失败!";
        }
    }

    private Bundle parseJSON(String str, DatagramPacket dp) {
        Bundle bundle = null;
        try {

            JSONObject json = new JSONObject(str);
            int port = json.getInt("port");
            //判断json数组中rtsp_port字段是否为空
            int vRtspPort = 0;
            int vFirPort = 0;
            int vVersion = 0;   //服务端版本

            String vPhotoDirs = "";   //图片路径
            String vVideoDirs = "";   //视频路径

            if (json.has("version")) {
                vVersion = json.getInt("version");
            }

            if (json.has("fir_port") && vVersion > 8) {
                vFirPort = json.getInt("fir_port");//设备端口号
            }

            if (json.has("rtsp_port") && vVersion > 7) {
                vRtspPort = json.getInt("rtsp_port");
            }

            if (json.has("photo_dirs")) {
                vPhotoDirs = json.getString("photo_dirs");
            }

            if (json.has("video_dirs")) {
                vVideoDirs = json.getString("video_dirs");
            }
            bundle = new Bundle();
            String ip = dp.getAddress().toString().substring(1);
            bundle.putString(KEY_IP, ip);
            bundle.putInt(KEY_PORT, port);
            bundle.putInt(KEY_RSP_PORT, vRtspPort);
            bundle.putInt(KEY_FIR_PORT, vFirPort);
            bundle.putString(KEY_PHOTO_DIRS, vPhotoDirs);
            bundle.putString(KEY_VIDEO_DIRS, vVideoDirs);

        } catch (JSONException e) {
            e.printStackTrace();
        }

        return bundle;
    }

    @Override
    protected void onPostExecute(FTPClient client) {
        super.onPostExecute(client);
        if (destroyTask) return;
        if (client == null) {
            if (mSyncStateListener != null) mSyncStateListener.onMediaSyncError(errorMessage);
        } else {
            if (mSyncStateListener != null) mSyncStateListener.onMediaSyncComplete(client);

        }
    }


    public interface MediaSyncStateListener {
        void onMediaSyncStart();

        void onMediaSyncError(String errorMessage);

        void onMediaSyncComplete(FTPClient client);
    }

    public boolean isRunning() {
        return getStatus() == RUNNING;
    }

    public void setClient(FTPClient client) {
        mClient = client;
    }
}
