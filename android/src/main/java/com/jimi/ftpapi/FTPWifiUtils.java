package com.jimi.ftpapi;

import android.content.Context;
import android.net.DhcpInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.SystemClock;
import android.text.TextUtils;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

public class FTPWifiUtils {
    private final String mSSID;
    private final String mPassword;
    private final WifiManager mWifiManager;
    private int mNetID;
    private String mTargetSSID;

    public FTPWifiUtils(Context context, String ssId, String password) {
        this.mSSID = ssId;
        this.mPassword = password;
        this.mWifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        mTargetSSID = "\"" + ssId + "\"";
    }

    //判断是否是我们要连接的设备wifi
    public boolean isTargetWifi() {
        String currentSSID = mWifiManager.getConnectionInfo().getSSID();
        return (mTargetSSID).equals(currentSSID);
    }

    public String currentWifi() {
        String currentSSID = mWifiManager.getConnectionInfo().getSSID();
        return currentSSID;
    }

    public String getSSID() {
        return mSSID;
    }

    /**
     * @author YangZhenYu
     * created at 17-9-21 上午11:40
     * 功能：创建一个wifi配置文件
     */
    private WifiConfiguration createWifiInfo(String ssID, String password, int type) {
        WifiConfiguration config = new WifiConfiguration();
        config.allowedAuthAlgorithms.clear();
        config.allowedGroupCiphers.clear();
        config.allowedKeyManagement.clear();
        config.allowedPairwiseCiphers.clear();
        config.allowedProtocols.clear();
        config.SSID = "\"" + ssID + "\"";
        //config.SSID = ssID;//切记不要用上面那行代码，否则很多wifi会连不上


        WifiConfiguration tempConfig = isExistence(ssID);
        if (tempConfig != null) {
            mWifiManager.removeNetwork(tempConfig.networkId);
        }


        // nopass
        if (type == 0) {
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
        }
        // wep
        if (type == 1) {
            if (!TextUtils.isEmpty(password)) {
                if (isHexWepKey(password)) {
                    config.wepKeys[0] = password;
                } else {
                    config.wepKeys[0] = "\"" + password + "\"";
                }
            }
            config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
            config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.SHARED);
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
            config.wepTxKeyIndex = 0;
        }
        // wpa
        if (type == 2) {
            config.preSharedKey = "\"" + password + "\"";
            config.hiddenSSID = true;
            config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
            config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
            // 此处需要修改否则不能自动重联
            // config.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
            config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
            config.status = WifiConfiguration.Status.ENABLED;
        }
        return config;
    }

    // 查看以前是否也配置过这个网络
    private WifiConfiguration isExistence(String SSID) {

        List<WifiConfiguration> existingConfigs = mWifiManager.getConfiguredNetworks();
        if (existingConfigs != null && existingConfigs.size() != 0) {

            for (WifiConfiguration existingConfig : existingConfigs) {
                if (existingConfig.SSID.equals("\"" + SSID + "\"")) {
                    return existingConfig;
                }
            }
        }
        return null;
    }

    private static boolean isHexWepKey(String wepKey) {
        final int len = wepKey.length();
        return (len == 10 || len == 26 || len == 58) && isHex(wepKey);

    }

    private static boolean isHex(String key) {
        for (int i = key.length() - 1; i >= 0; i--) {
            final char c = key.charAt(i);
            if (!(c >= '0' && c <= '9' || c >= 'A' && c <= 'F' || c >= 'a' && c <= 'f')) {
                return false;
            }
        }

        return true;
    }

    private boolean openWifi() {

        switch (mWifiManager.getWifiState()) {
            case WifiManager.WIFI_STATE_DISABLED://已经关闭
                mWifiManager.setWifiEnabled(true);
                return false;
            case WifiManager.WIFI_STATE_ENABLED://已打开
                return true;
            case WifiManager.WIFI_STATE_DISABLING://正在关
                mWifiManager.setWifiEnabled(true);
                return false;
            case WifiManager.WIFI_STATE_ENABLING://正在开
                return false;
        }
        return false;
    }

    public void disConnect() {
        mWifiManager.disableNetwork(mNetID);
        mWifiManager.disconnect();
        mWifiManager.removeNetwork(mNetID);
    }

    public boolean connectWifi() {
        while (!openWifi()) {
            SystemClock.sleep(500);
        }
        if (isTargetWifi()) return true;
        WifiConfiguration wifiConfig = createWifiInfo(mSSID, mPassword, 2);
        if (wifiConfig == null) return false;
        mNetID = mWifiManager.addNetwork(wifiConfig);
        if (mNetID == -1) return false;
        boolean enableNetwork = mWifiManager.enableNetwork(mNetID, true);
        boolean reconnect = mWifiManager.reconnect();
        if (!reconnect) mWifiManager.reassociate();
        return isTargetWifi();
    }

    public InetAddress getBroadcastAddress() throws UnknownHostException {
        DhcpInfo dhcpInfo = mWifiManager.getDhcpInfo();
        if (dhcpInfo == null) {
            return InetAddress.getByName("255.255.255.255");
        }
        int broadcast = (dhcpInfo.ipAddress & dhcpInfo.netmask) | ~dhcpInfo.netmask;
        byte[] quads = new byte[4];
        for (int k = 0; k < 4; k++)
            quads[k] = (byte) ((broadcast >> k * 8) & 0xFF);
        return InetAddress.getByAddress(quads);
    }

    public boolean isExistSSID() {
        List<ScanResult> scanResults = mWifiManager.getScanResults();
        if (scanResults == null || scanResults.isEmpty()) return false;
        for (ScanResult result : scanResults) {

            if (mTargetSSID.equals(result.SSID)) return true;
        }
        return false;
    }

    public WifiManager.MulticastLock createLock(String tag) {
        return mWifiManager.createMulticastLock(tag);
    }
}
