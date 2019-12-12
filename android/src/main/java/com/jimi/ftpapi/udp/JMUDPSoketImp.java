package com.jimi.ftpapi.udp;

import com.jimi.ftpapi.listener.JMBaseListener;

public interface JMUDPSoketImp {
    void configUDPSocket(String host, int port, int timeout, JMBaseListener jmBaseListener);
    void send(String data,int tag,JMBaseListener jmBaseListener);
    void closeSocket(JMBaseListener jmBaseListener);
    void destroy();
}
