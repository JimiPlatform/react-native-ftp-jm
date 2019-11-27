//
// ProjectName:  JiMiPlatfromProject
// FileName:     JMFTPSyncFileManager.swift
// Description:  <#Description#>
//
// Created by LiuLuJia on 2019/11/11.
// COPYRIGHT. ShenZhen JiMi Technology Co., Ltd. 2018.
// ALL RIGHTS RESERVED.
//
// No part of this publication may be reproduced, stored in a retrieval system, or transmitted,
// on any form or by any means, electronic, mechanical, photocopying, recording,
// or otherwise, without the prior written permission of ShenZhen JiMi Network Technology Co., Ltd.
//

import Foundation
import React
import FilesProvider

struct JMRealDownUpdataFtpModel {
//    var downManager: JMFTPDownloadManager = JMFTPDownloadManager()
    var resolver: RCTPromiseResolveBlock
    var rejecter: RCTPromiseRejectBlock
    var isDown : Bool
    var progress : Double = 0
    var manager : Progress = Progress()
    var requetUrl : String
    var tag : String
    func pause() {
        manager.pause()
    }
    func resume()  {
        manager.pause()
    }
    func cancel()  {
        manager.pause()
    }
}
@objc(JMFTPSyncFileManager)
class JMFTPSyncFileManager: RCTEventEmitter {
    enum JMFTPMode : String {
        case passive = "passive"
        case active = "active"
        case non = "default"
        func getFTPFileProviderMode()->FTPFileProvider.Mode {
            switch self {
            case .passive:
                return FTPFileProvider.Mode.passive
            case .active:
                return FTPFileProvider.Mode.active
            case .non:
                return FTPFileProvider.Mode.default
            }
        }
    }
    let onRNFTPProgressCallback = "listeningFTPProgressCellBack"

    var isHasListeners = false      //JS是否有监听事件
    
    var baseUrl : String?
    var ftpMode : JMFTPMode?
    var account : String?
    var password : String?
    var ftpManager : FTPFileProvider?
    var realFtpDic : [String:JMRealDownUpdataFtpModel] = [String:JMRealDownUpdataFtpModel]()
    var timer : DispatchSourceTimer?

    deinit {
        debugPrint("FTP->deinit")
        self.cencelTimer()
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
        return [onRNFTPProgressCallback]
    }
    
    override func constantsToExport() -> [AnyHashable : Any]! {
        return [onRNFTPProgressCallback: onRNFTPProgressCallback]
    }
    func sendEvent(_ model: JMRealDownUpdataFtpModel) -> Void {
        if (isHasListeners) {
            let param : [String:Any] = ["path":model.requetUrl,"tag":model.tag,"progress":model.progress]
            let jsonStr = JMFTPSyncFileTools.getJSONStringFrom(param)
            self.sendEvent(withName: onRNFTPProgressCallback, body: jsonStr)
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
                           rejecter:@escaping RCTPromiseRejectBlock) {
        guard let state = JMFTPMode(rawValue: "active") else {
            rejecter("-800","mode不正确",nil)
            return
        }
        let portStr = port == 0 ? "" : (":" + String(port))
        if !baseUrl.contains("ftp://") {
            self.baseUrl = "ftp://" + baseUrl + portStr
        }else{
            self.baseUrl =  baseUrl + portStr
        }
//        "ftp://192.168.43.1:10011"
        self.ftpMode = state
        self.account = account
        self.password = password
        resolver(nil)
    }
    //MARK:发起ftp链接
    @objc(connectFTP:rejecter:)
    func connectFTP(resolver:@escaping RCTPromiseResolveBlock,
                    rejecter:@escaping RCTPromiseRejectBlock) {
        guard let baseUrl = self.baseUrl,
            let ftpMode = self.ftpMode,
            let account = self.account,
            let password = self.password else {
            rejecter("801","未配置ftp参数",nil)
            return
        }
        guard let url = URL(string: baseUrl) else {
            rejecter("801","baseUrl不正确",nil)
            return
        }
        let credential = URLCredential(user: account, password: password, persistence: URLCredential.Persistence.none)
        if let manager = FTPFileProvider(baseURL: url, mode: ftpMode.getFTPFileProviderMode(), credential: credential, cache: nil) {
            ftpManager = manager
            ftpManager?.delegate = self
            resolver(nil)
        }else{
            rejecter("809","连接失败",nil)
        }
        
    }
//    "/mnt/sdcard2/DVRMEDIA/CarRecorder/PHOTO/2019_02_23"
    //MARK:获取指点文件夹下文件
    @objc(findFTPFlies:resolver:rejecter:)
    func findFTPFlies(subPath:String,
                      resolver:@escaping RCTPromiseResolveBlock,
                      rejecter:@escaping RCTPromiseRejectBlock)  {
        guard let provider = ftpManager else {
            rejecter("802","未进行连接操作",nil)
            return
        }
//        "/mnt/sdcard2/DVRMEDIA/CarRecorder/PHOTO/2019_02_23"
        DispatchQueue.main.async {
         provider.contentsOfDirectory(path: subPath) { (FileObjects, error) in
               guard error == nil else{
                 debugPrint("provider.searchFile = \(error.debugDescription)")
                       rejecter("803","获取文件错误",error!)
                       return
             }
             var files = [[String:String]]()
             for obj in FileObjects{
                files.append(["fileName":obj.name,"fileSize":String(obj.size),"filePath":obj.path])
             }
             print("files = \(files.debugDescription)")
             resolver(JMFTPSyncFileTools.getJSONStringFrom(files))
         }
        }
    }
    //MARK:下载ftp文件
    @objc(downFTPFile:locaUrl:fileName:tag:resolver:rejecter:)
    func downFTPFile(url:String,
                     locaUrl:String,
                     fileName:String,
                     tag:String,
                     resolver:@escaping RCTPromiseResolveBlock,
                     rejecter:@escaping RCTPromiseRejectBlock) {
        var locaUrlStr = ""
        var downfileName = fileName
        if locaUrl.last != "/" {
            var arr = locaUrl.components(separatedBy: "/")
            if let ss = arr.last {
                if ss.contains(".") {
                    downfileName = arr.removeLast()
                    locaUrlStr = arr.joined(separator: "/") + "/"
                }else{
                    locaUrlStr = locaUrl + "/"
                }
            }else{
                rejecter("802","未进行连接操作",nil)
            }
        }else{
            locaUrlStr = locaUrl
        }
        
        guard let provider = ftpManager,
            JMFTPSyncFileTools.createDicFile(filePath: locaUrlStr) else {
            rejecter("802","未进行连接操作",nil)
            return
        }
//        "/mnt/sdcard2/DVRMEDIA/CarRecorder/PHOTO/2019_02_23/2019_02_23_14_28_10.jpg"
//        FileHandleManager.getSmallAppCache() + "/JMSmallApp/fs4324vv/gsdgs/4324.jpg"
        startTimer()//开启计时器
        let prossess = provider.copyItem(path: url, toLocalURL: URL(fileURLWithPath: locaUrlStr + downfileName))  { [weak self](error) in
            if error == nil {
                resolver(JMFTPSyncFileTools.getJSONStringFrom(["tag":tag]))
            }else{
                print("copyItem(path error = \(error.debugDescription)")
                rejecter("804","下载失败",error)
            }
            self?.realFtpDic.removeValue(forKey: tag)
            if self?.realFtpDic.count == 0 {
                self?.cencelTimer()
            }
        }
        guard let pro =  prossess else{
            rejecter("804","下载失败",nil)
            return
        }
        realFtpDic[tag] = JMRealDownUpdataFtpModel( resolver: resolver, rejecter: rejecter, isDown: true, manager: pro, requetUrl: url, tag: tag)
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
        guard let provider = ftpManager else {
            rejecter("802","未进行连接操作",nil)
            return
        }
        guard let data = FileManager.default.contents(atPath: locaUrl) else {
            rejecter("808","没有上传文件",nil)
            return
        }
        startTimer()
        let progressManger = provider.writeContents(path: path, contents: data, atomically: false, overwrite: true) { [weak self](error) in
            guard let weakSelf = self else{return}
            if error == nil{
                resolver(JMFTPSyncFileTools.getJSONStringFrom(["tag":tag]))
            }else{
                rejecter("808","上传失败",error)
            }
            weakSelf.realFtpDic.removeValue(forKey: tag)
            if weakSelf.realFtpDic.count == 0 {
                weakSelf.cencelTimer()
            }
        }
        guard let pro =  progressManger else{
            rejecter("808","上传失败",nil)
            return
        }
        realFtpDic[tag] = JMRealDownUpdataFtpModel.init( resolver: resolver, rejecter: rejecter, isDown: false, manager: pro, requetUrl: path, tag: tag)

    }
    //MARK:删除文件
    @objc(deleteFTPFile:resolver:rejecter:)
    func deleteFTPFile(path:String,
                       resolver:@escaping RCTPromiseResolveBlock,
                       rejecter:@escaping RCTPromiseRejectBlock)  {
        guard let provider = ftpManager else {
            rejecter("802","未进行连接操作",nil)
            return
        }
        provider.removeItem(path: path) { (error) in
            if error != nil {
                rejecter("806","删除失败",error)
            }else{
                resolver(nil)
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
        guard let provider = ftpManager else {
            rejecter("802","未进行连接操作",nil)
            return
        }
        provider.moveItem(path: path, to: toPath, overwrite: overwrite) { (error) in
            if error != nil {
                rejecter("807","移动文件失败失败",error)
            }else{
                resolver(JMFTPSyncFileTools.getJSONStringFrom(["path":toPath]))
            }
        }
    }
    //MARK:关闭FTP
    @objc(closeFTP:rejecter:)
    func closeFTP(resolver:@escaping RCTPromiseResolveBlock,
                  rejecter:@escaping RCTPromiseRejectBlock) {
        self.cencelTimer()
        ftpManager?.delegate = nil
        ftpManager = nil
    }

}
extension JMFTPSyncFileManager:JMFTPDownloadManagerDelegate{
    func ftpManagerDownloadProgressDidChange(process: Double, tag: String) {
        guard var model = self.realFtpDic[tag] else{return}
        model.progress = process
        self.realFtpDic[tag] = model
    }

    func ftpDownloadStatus(destinationURL: String?, errorMsg: String?, tag: String) {
        guard let model = realFtpDic[tag] else {
            return
        }
        if let errorMessage = errorMsg {
            model.rejecter("804",errorMessage,nil)
        }
        model.resolver(JMFTPSyncFileTools.getJSONStringFrom(["tag":tag,"destinationURL":destinationURL ?? ""]))
        realFtpDic.removeValue(forKey: tag)
        if realFtpDic.count == 0 {
            self.cencelTimer()
        }
    }

    
}
extension JMFTPSyncFileManager{
    func startTimer()  {
        if timer != nil {
            return
        }
        timer = DispatchSource.makeTimerSource(queue:DispatchQueue.global())
        timer?.schedule(deadline: .now(), repeating: .seconds(1))
        timer?.setEventHandler(handler: {[weak self] in
            guard let weakSelf = self else{return}
            for (_,value) in weakSelf.realFtpDic{
                weakSelf.sendEvent(value)
            }
        })
        timer?.resume()
    }
    func cencelTimer()  {
        timer?.cancel()
        timer = nil
    }
}
extension JMFTPSyncFileManager:FileProviderDelegate{
    //操作成功后回调
    func fileproviderSucceed(_ fileProvider: FileProviderOperations, operation: FileOperationType) {
        
    }
    //操作失败后d回调
    func fileproviderFailed(_ fileProvider: FileProviderOperations, operation: FileOperationType, error: Error) {
        
    }
    //文件操作进度回调
    func fileproviderProgress(_ fileProvider: FileProviderOperations, operation: FileOperationType, progress: Float) {
        switch operation {
        case .copy(source: let source, destination: let dest)://下载进度
            realFtpDic.keys.forEach { (key) in
                guard var model = self.realFtpDic[key] else{return}
                if source == model.requetUrl{
                    model.progress = Double(progress)
                    self.realFtpDic[key] = model
                }
            }
            print("Copy \(source) to \(dest): \(progress * 100) completed.")
        case .modify(path: let path)://上传进度
            print("path = \(path) progress = \(progress)")
            let keys = realFtpDic.keys
            keys.forEach { (key) in
                guard var model = self.realFtpDic[key] else{return}
                if path == model.requetUrl{
                    model.progress = Double(progress)
                    self.realFtpDic[key] = model
                }
            }
        default:
            break
        }
        return
    }
}

