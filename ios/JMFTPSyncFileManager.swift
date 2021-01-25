//
// ProjectName:  JiMiPlatfromProject
// FileName:     JMFTPSyncFileManager.swift
// Description:  <#Description#>
//
// Created by Jason on 2019/11/11.
// COPYRIGHT. ShenZhen JiMi Technology Co., Ltd. 2018.
// ALL RIGHTS RESERVED.
//
// No part of this publication may be reproduced, stored in a retrieval system, or transmitted,
// on any form or by any means, electronic, mechanical, photocopying, recording,
// or otherwise, without the prior written permission of ShenZhen JiMi Network Technology Co., Ltd.
//

import Foundation
import React
import JMSmartFTPUtils

struct JMRealDownUpdataFtpModel {
    
    var ftpClient:JMFTPClient? = JMFTPClient()
    var resolver: RCTPromiseResolveBlock
    var rejecter: RCTPromiseRejectBlock
    var isDown : Bool
    var progress : Double = 0
    var requetUrl : String
    var tag : String
    func pause() {
        ftpClient?.pause()
    }
    func resume()  {
        ftpClient?.resume()
    }
    func cancel()  {
        ftpClient?.cancel()
    }
}

@objc(JMFTPSyncFileManager)
class JMFTPSyncFileManager: RCTEventEmitter {
    
    let kRNFTPProgressCallback = "kRNJMFTPSyncFileManager"
    var isHasListeners = false      //JS是否有监听事件
    
    var baseUrl: String = ""
    var port: Int?
    var account: String?
    var password: String?
    
    var ftpClient: JMFTPClient?
    var userInfo: JMFTPUserInfo?
    
    var realFtpDic : [String:JMRealDownUpdataFtpModel] = [String:JMRealDownUpdataFtpModel]()
    var timer : DispatchSourceTimer?

    deinit {
        debugPrint("FTP->deinit")
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
        return [kRNFTPProgressCallback]
    }
    
    override func constantsToExport() -> [AnyHashable : Any]! {
        return ["kRNFTPProgressCallback": kRNFTPProgressCallback]
    }
    func sendEvent(_ model: JMRealDownUpdataFtpModel) -> Void {
        if (isHasListeners) {
            let param : [String:Any] = ["path":model.requetUrl,"tag":model.tag,"progress":model.progress]
            let jsonStr = JMFTPSyncFileTools.getJSONStringFrom(param)
            let dataStr = JMFTPSyncFileTools.getJSONStringFrom(["data":jsonStr])
            self.sendEvent(withName: kRNFTPProgressCallback, body: dataStr)
        }
    }
    
    //MARK:配置ftp
    @objc(configFtpSyncFile:mode:port:account:password:resolver:rejecter:)
    func configFtpSyncFile(baseUrl:String,
                           mode:String,
                           port:Int,
                           account:String,
                           password:String,
                           resolver:@escaping RCTPromiseResolveBlock,
                           rejecter:@escaping RCTPromiseRejectBlock)
    {
        self.baseUrl =  baseUrl
        self.port = port
        self.account = account
        self.password = password
        resolver(nil)
    }
    
    //MARK:发起ftp链接
    @objc(connectFTP:rejecter:)
    func connectFTP(resolver:@escaping RCTPromiseResolveBlock, rejecter:@escaping RCTPromiseRejectBlock) {
        guard let port = self.port,
            let account = self.account,
            let password = self.password else {
            rejecter("801","未配置ftp参数",nil)
            return
        }
        
        userInfo = JMFTPUserInfo.init(domain: self.baseUrl, port: Int32(port), account: account, password: password)
        ftpClient = JMFTPClient.init(userInfo: userInfo!)
        ftpClient?.delegate = self
        resolver(nil)
    }
    
//    "/mnt/sdcard2/DVRMEDIA/CarRecorder/PHOTO/2019_02_23"
    //MARK:获取指点文件夹下文件
    @objc(findFTPFlies:resolver:rejecter:)
    func findFTPFlies(subPath:String,
                      resolver:@escaping RCTPromiseResolveBlock,
                      rejecter:@escaping RCTPromiseRejectBlock)  {
        if subPath.count == 0 {
             resolver(JMFTPSyncFileTools.getJSONStringFrom([Any]()));
             return;
         }
        guard let ftpClient = ftpClient else {
            rejecter("801","未配置ftp参数", nil)
            return
        }
        var files = [[String: Any]]()
        
        ftpClient.findFlies(withDirPath: baseUrl + subPath, completion: { (infos) in
            for info in infos {
                files.append(["fileName": info.fileName,
                              "fileSize": info.isDir ? 0 : info.fileSize,
                              "filePath": info.filePath,
                              "fileType": info.isDir ? 1 : 0])
            }
            resolver(JMFTPSyncFileTools.getJSONStringFrom(files))
        })
    }
    
    //MARK:下载ftp文件
    @objc(downFTPFile:locaUrl:fileName:tag:resolver:rejecter:)
    func downFTPFile(url:String,
                     locaUrl:String,
                     fileName:String,
                     tag:String,
                     resolver:@escaping RCTPromiseResolveBlock,
                     rejecter:@escaping RCTPromiseRejectBlock)
    {
        guard let userInfo = userInfo else {
            rejecter("801","未配置ftp参数", nil)
            return
        }
        let ftpClient = JMFTPClient.init(userInfo: userInfo)
        ftpClient.delegate = self
        ftpClient.tag = Int(tag) ?? 0
        
        realFtpDic[tag] = JMRealDownUpdataFtpModel(ftpClient: ftpClient, resolver: resolver, rejecter: rejecter, isDown: true, progress: 0,  requetUrl: url, tag: tag)
        ftpClient.downloadFile(withRemotePath: url, localDir: locaUrl, localFileName: fileName)
        NSLog("下载开始+++++++++++")
    }
    
    //MARK:暂停下载
    @objc(ftpPause:resolver:rejecter:)
    func ftpPause(tag:String,
               resolver:@escaping RCTPromiseResolveBlock,
               rejecter:@escaping RCTPromiseRejectBlock) {
         guard let model = realFtpDic[tag] else {
            rejecter("-805","没有当前tag",nil)
            return
        }
        model.pause()
    }
    
    //MARK:恢复下载
    @objc(ftpResume:resolver:rejecter:)
    func ftpResume(tag:String,
               resolver:@escaping RCTPromiseResolveBlock,
               rejecter:@escaping RCTPromiseRejectBlock) {
         guard let model = realFtpDic[tag] else {
            rejecter("805","没有当前tag",nil)
            return
        }
        model.resume()
    }
    
    //MARK:取消下载
    @objc(ftpCancel:resolver:rejecter:)
    func ftpCancel(tag:String,
               resolver:@escaping RCTPromiseResolveBlock,
               rejecter:@escaping RCTPromiseRejectBlock) {
         guard let model = realFtpDic[tag] else {
            rejecter("805","没有当前tag",nil)
            return
        }
        model.cancel()
    }
    
    //MARK:上传文件
    @objc(uploadFTPFile:locaUrl:fileName:overwrite:tag:resolver:rejecter:)
    func uploadFTPFile(path:String,
                       locaUrl:String,
                       fileName:String,
                       overwrite:Bool,
                       tag:String,
                       resolver:@escaping RCTPromiseResolveBlock,
                       rejecter:@escaping RCTPromiseRejectBlock)  {
        guard let userInfo = userInfo else {
            rejecter("801","未配置ftp参数", nil)
            return
        }
        
        let ftpClient = JMFTPClient.init(userInfo: userInfo)
        ftpClient.delegate = self
        ftpClient.tag = Int(tag) ?? 0
        
        realFtpDic[tag] = JMRealDownUpdataFtpModel(ftpClient: ftpClient, resolver: resolver, rejecter: rejecter, isDown: false, progress: 0,  requetUrl: path, tag: tag)
        ftpClient.uploadFile(withRemotePath: path, loaclPath: locaUrl + fileName)
    }
    
    //MARK:删除文件
    @objc(deleteFTPFile:resolver:rejecter:)
    func deleteFTPFile(path:String,
                       resolver:@escaping RCTPromiseResolveBlock,
                       rejecter:@escaping RCTPromiseRejectBlock)  {
        guard let ftpClient = ftpClient else {
            rejecter("801","未配置ftp参数", nil)
            return
        }
        
        ftpClient.deleteFile(withPath: path) { (success) in
            if success {
                resolver(nil)
            } else {
                rejecter("806", "删除失败", nil)
            }
        }
    }
    
    //MARK:移动文件
    @objc(moveFTPFile:toPath:overwrite:resolver:rejecter:)
    func moveFTPFile(path:String,
                     toPath:String,
                     overwrite:Bool,
                     resolver:@escaping RCTPromiseResolveBlock,
                     rejecter:@escaping RCTPromiseRejectBlock)  {
        guard let ftpClient = ftpClient else {
            rejecter("801","未配置ftp参数", nil)
            return
        }
        
        ftpClient.moveFile(fromPath: path, toPath: toPath) { (success) in
            if success {
                resolver(JMFTPSyncFileTools.getJSONStringFrom(["path":toPath]))
            } else {
                rejecter("807","移动文件失败失败", nil)
            }
        }
    }
    
    //MARK:关闭FTP
    @objc(closeFTP:rejecter:)
    func closeFTP(resolver:@escaping RCTPromiseResolveBlock,
                  rejecter:@escaping RCTPromiseRejectBlock) {
        ftpClient?.delegate = nil
        ftpClient = nil
        userInfo = nil
        realFtpDic.removeAll()
        resolver(nil)
    }

}

extension JMFTPSyncFileManager: JMFTPClientDelegate {
    func didFtpClient(_ client: JMFTPClient, progress: Progress) {
        if realFtpDic.keys.contains(String(client.tag)),
            var model = realFtpDic[String(client.tag)]{
            model.progress = progress.fractionCompleted / Double(progress.totalUnitCount)
            self.sendEvent(model)
        }
    }
    
    func didFtpClient(_ client: JMFTPClient, state: JMFTPClientState) {
        if realFtpDic.keys.contains(String(client.tag)), let model = realFtpDic[String(client.tag)] {
            if state == .success {
                NSLog("下载结束+++++++++++")
                model.resolver(JMFTPSyncFileTools.getJSONStringFrom(["tag": String(client.tag)]))
            } else {
                model.rejecter("804", JMFTPSyncFileTools.getJSONStringFrom(["tag": String(client.tag), "errCode": state]), nil)
            }
            realFtpDic.removeValue(forKey: String(client.tag));
        }
    }
}
