package com.jimi.ftpapi.ftp;

import com.facebook.react.bridge.Promise;
import com.jimi.ftpapi.listener.JMBaseListener;

public interface JMFtpImp {
    void configFtpSyncFile(String baseUrl, String mode, int port, String account, String password,JMBaseListener jmBaseListener);
    void connectFTP(JMBaseListener jmBaseListener);
    void findFTPFlies(String subPath,JMBaseListener jmBaseListener);
    void downFTPFile(String url, String locaUrl, String fileName, String tag,JMBaseListener jmBaseListener);
    void uploadFTPFile(String remoteFolder, String localFilePath, String remoteFileName, boolean overWrite, String tag,JMBaseListener jmBaseListener);
    void deleteFTPFile(String path,JMBaseListener jmBaseListener);
    void moveFTPFile(String from, String to, boolean overWrite,JMBaseListener jmBaseListener);
    void ftpPause(String tag,JMBaseListener jmBaseListener);
    void ftpResume(String tag,JMBaseListener jmBaseListener);
    void closeFTP(JMBaseListener jmBaseListener);
    void ftpCancel(String tag,JMBaseListener jmBaseListener);
    void destroy();
}
