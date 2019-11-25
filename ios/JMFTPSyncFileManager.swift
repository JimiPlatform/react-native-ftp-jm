//
// ProjectName:  JMSmallAppEngine
// FileName:     JMUDPScoketManager.swift
// Description:  <#Description#>
//
// Created by LiuLuJia on 2019/11/19.
// COPYRIGHT. ShenZhen JiMi Technology Co., Ltd. 2018.
// ALL RIGHTS RESERVED.
//
// No part of this publication may be reproduced, stored in a retrieval system, or transmitted,
// on any form or by any means, electronic, mechanical, photocopying, recording,
// or otherwise, without the prior written permission of ShenZhen JiMi Network Technology Co., Ltd.
//

import Foundation
import React
import CocoaAsyncSocket
@objc(JMUDPScoketManager)
class JMUDPScoketManager: RCTEventEmitter {
    //发送回调结果给小程序方法名
    let onRNUDPSocketCallback = "listeningUDPScoketCellBack"
    var isHasListeners = false      //JS是否有监听事件
    var host : String?
    var port : UInt16?
    var sendTimeout : Double?
    var socket : GCDAsyncUdpSocket?
    var sendSuccessCallback : RCTPromiseResolveBlock?
    var sendFailCallback : RCTPromiseRejectBlock?
    var sendDataTag = [Int]()//待发送的指令tag
    deinit {
        debugPrint("UDPScoket->deinit")

    }
    /// JS开启监听事件
    override func startObserving() {
        isHasListeners = true
    }
    
    /// JS停止监听事件
    override func stopObserving() {
        isHasListeners = false
    }
    
    override func supportedEvents() -> [String]! {
        return [onRNUDPSocketCallback]
    }
    
    override func constantsToExport() -> [AnyHashable : Any]! {
        return [onRNUDPSocketCallback: onRNUDPSocketCallback]
    }
    
    func sendEvent(_ code: Int, _ data: Any? = nil) -> Void {
        if (isHasListeners) {
            var param : [String:Any] = ["code":code]
            if data != nil {
                param["data"] = data!
            }
            let jsonStr = JMFTPSyncFileTools.getJSONStringFrom(param)
            self.sendEvent(withName: onRNUDPSocketCallback, body: jsonStr)
        }
    }
        ///   - appKey: appKey  测试用：449A7D0E9C1911E7BEDB00219B9A2EF3
    ///   - secret: secret  测试用：695c1a459c1911e7bedb00219b9a2ef3
    ///   - token: 服务器登录Token，测试用：2FCB70C6A1EE00CF688F5E9C54C3D502
    @objc(openWifi:rejecter:)
    func openWifi(resolver:@escaping RCTPromiseResolveBlock,
                  rejecter:@escaping RCTPromiseRejectBlock) {
            let parameters = NSMutableDictionary.init(dictionary: [
                "method":"jimi.smarthome.device.custom.instruct.send",
                "imei":"357730090015752",
                "instruct":"WIFI,ON",
                "accessToken": "2FCB70C6A1EE00CF688F5E9C54C3D502",
                "timestamp": Date.timestamp,
                "app_key": "449A7D0E9C1911E7BEDB00219B9A2EF3"
                ])
            parameters["sign"] = ParamsEncryption.signToParams(parameters as! [String : Any], secret: "695c1a459c1911e7bedb00219b9a2ef3")

            FTPSyncFileTools.openDeviceWIFI(url: "http://smarthome.jimicloud.com/route/app", parameters: parameters as! [String : Any], success: {(msg) in
                resolver(nil)
      
            }) {(msg) in
                rejecter("24214",msg,nil)

            }
    }

    //MARK:配置scoket参数
    @objc(configUDPSocket:port:timeout:resolver:rejecter:)
    func configUDPSocket(host:String,
                         port:Int,
                         timeout:Int,
                         resolver:@escaping RCTPromiseResolveBlock,
                         rejecter:@escaping RCTPromiseRejectBlock) {
        self.host = host
        self.port = UInt16(port)
        self.sendTimeout = Double(timeout)
        socket = GCDAsyncUdpSocket(delegate: self , delegateQueue: DispatchQueue.main)
        do {
            try socket?.bind(toPort: UInt16(port))
                
            resolver(nil)

        } catch  {
            rejecter("811","配置参数失败",nil)
        }
        

    }
    //MARK:发送指令
    @objc(send:tag:resolver:rejecter:)
    func send(data:String,
              tag:Int,
              resolver:@escaping RCTPromiseResolveBlock,
              rejecter:@escaping RCTPromiseRejectBlock) {
        guard let socket = self.socket,
            let dataStr = data.data(using: .ascii),
            let host = self.host,
            let port = self.port,
            let sendTimeout = self.sendTimeout else {
                rejecter("605","未进行配置参数或data不正确",nil)
            return
        }
        do {
            try socket.enableBroadcast(true)
            socket.send(dataStr, toHost: host, port: port, withTimeout: sendTimeout, tag: tag)
            sendDataTag.append(tag)
            // receive
            resolver(nil)

        } catch {
            print("dfdsg error=\(error)")
            rejecter("604","发送失败",nil)
        }
    }
    //MARK:关闭socket
    @objc(closeSocket:rejecter:)
    func closeSocket(resolver:@escaping RCTPromiseResolveBlock,
                     rejecter:@escaping RCTPromiseRejectBlock) {
        socket?.close()
        socket = nil
        resolver(nil)
    }
}
extension JMUDPScoketManager: GCDAsyncUdpSocketDelegate{
    //连接失败
    func udpSocket(_ sock: GCDAsyncUdpSocket, didNotConnect error: Error?) {
        debugPrint("didNotConnect tag:\(error.debugDescription)")
        sendEvent(601)
    }
    //发送带有tag数据
    func udpSocket(_ sock: GCDAsyncUdpSocket, didSendDataWithTag tag: Int) {
        debugPrint("didSendDataWithTag tag:\(tag)")
//        if sendDataTag.contains(tag) {
//            sendSuccessCallback?("")
//            sendSuccessCallback = nil
//            sendDataTag = sendDataTag.filter{$0 != tag}
//        }
    }
    //关闭链接
    func udpSocketDidClose(_ sock: GCDAsyncUdpSocket, withError error: Error?) {
        debugPrint("udpSocketDidClose:\(String(describing: sock))")
        sendEvent(602)
    }
    //连接成功
    func udpSocket(_ sock: GCDAsyncUdpSocket, didConnectToAddress address: Data) {
        debugPrint("didConnectToAddress")
        sendEvent(600)
    }
    //发送tag数据失败
    func udpSocket(_ sock: GCDAsyncUdpSocket, didNotSendDataWithTag tag: Int, dueToError error: Error?) {
        debugPrint("didNotSendData error:\(String(describing: error)), tag\(tag)")
//        if sendDataTag.contains(tag) {
//            sendFailCallback?("605","发送数据失败",nil)
//            sendDataTag.remove(at: tag)
//        }
    }
    //接受到数据
    func udpSocket(_ sock: GCDAsyncUdpSocket, didReceive data: Data, fromAddress address: Data, withFilterContext filterContext: Any?) {
        
        if let dict = try? JSONSerialization.jsonObject(with: data, options: .mutableContainers) {
            sendEvent(603,dict)
        }
    }
}
