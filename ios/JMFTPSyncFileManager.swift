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
import JMCurl
struct JMRealDownUpdataFtpModel {
    var downManager: JMFTPDownloadManager = JMFTPDownloadManager()
    var resolver: RCTPromiseResolveBlock
    var rejecter: RCTPromiseRejectBlock
    var isDown : Bool
    var progress : Double = 0
    var manager : Progress = Progress()
    var requetUrl : String
    var tag : String
    func pause() {
        if isDown {
            downManager.pause()
        }else{
            manager.pause()
        }
        
    }
    func resume()  {
        if isDown {
            downManager.resume()
        }else{
            manager.resume()
        }
    }
    func cancel()  {
        if isDown {
            downManager.cancel()
        }else{
            manager.pause()
        }
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
//    let kRNFTPProgressCallback = "listeningFTPProgressCallBack"
        let kRNFTPProgressCallback = "kRNJMFTPSyncFileManager"


    var isHasListeners = false      //JS是否有监听事件
    
    var baseUrl : String?
    var port : Int?
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
        return [kRNFTPProgressCallback]
    }
    
    override func constantsToExport() -> [AnyHashable : Any]! {
        return ["kRNFTPProgressCallback": kRNFTPProgressCallback]
    }
    func sendEvent(_ model: JMRealDownUpdataFtpModel) -> Void {
        if (isHasListeners) {
            let param : [String:Any] = ["path":model.requetUrl,"tag":model.tag,"progress":model.progress]
            let jsonStr = JMFTPSyncFileTools.getJSONStringFrom(param)
            self.sendEvent(withName: kRNFTPProgressCallback, body: jsonStr)
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
        guard let state = JMFTPMode(rawValue: mode) else {
            rejecter("-800","mode不正确",nil)
            return
        }
        //            let portStr = port == 0 ? "" : (":" + String(port))
        if !baseUrl.contains("ftp://") {
            self.baseUrl = "ftp://" + baseUrl
        }else{
            self.baseUrl =  baseUrl
        }
        //        "ftp://192.168.43.1:10011"
        self.port = port
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
            let port = self.port,
            let account = self.account,
            let password = self.password else {
            rejecter("801","未配置ftp参数",nil)
            return
        }
        let portStr = port == 0 ? "" : (":" + String(port))
        guard let url = URL(string: baseUrl + portStr) else {
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
        guard let baseUrl = self.baseUrl,
            let account = self.account,
            let port = self.port,
            let password = self.password else {
            rejecter("801","未配置ftp参数",nil)
            return
        }
        let testUrl = baseUrl + subPath
        let server = FMServer(destination: testUrl, username: account, password: password)
        server?.port = Int32(port)
        let ftpManager = FTPManager()
        let data = ftpManager.contents(of: server)
        guard let dataArr = data as? [[String: Any]] else {
            debugPrint("错误：ftpManager?.contents(of: server)", data ?? NSObject())
            rejecter("803","获取文件错误",nil)
            return
        }
        print("+++++++++ dataArr \(dataArr.description)")
        var files = [[String: Any]]()
        for dic in dataArr {
            guard let name = dic["kCFFTPResourceName"] as? String,
                let size = dic["kCFFTPResourceSize"] as? Int,
                let type = dic["kCFFTPResourceType"] as? Int else {
                rejecter("803","获取文件错误",nil)
                return
            }
            var filePath = ""
            if subPath.last == "/" {
                filePath = subPath + "\(name)"
            }else{
                filePath = subPath + "/\(name)"
            }
            files.append(["fileName":name,"fileSize":size,"filePath":filePath,"fileType":type == 4 ? 1 : 0])
        }
        print("files = \(files.debugDescription)")
        resolver(JMFTPSyncFileTools.getJSONStringFrom(files))
    }
    //MARK:下载ftp文件
    @objc(downFTPFile:locaUrl:fileName:tag:resolver:rejecter:)
    func downFTPFile(url:String,
                     locaUrl:String,
                     fileName:String,
                     tag:String,
                     resolver:@escaping RCTPromiseResolveBlock,
                     rejecter:@escaping RCTPromiseRejectBlock) {

        guard let baseUrl = self.baseUrl,
            let account = self.account,
            let port = self.port,
            let password = self.password else {
            rejecter("801","未配置ftp参数",nil)
            return
        }
        let manager = JMFTPDownloadManager()
        manager.delegate = self
        manager.startDownload(urlStr: baseUrl + ":\(port)" + url, locaPath: locaUrl+"\(fileName)", tag: tag, account: account, password: password)
        NSLog("下载开始+++++++++++")
        realFtpDic[tag] = JMRealDownUpdataFtpModel(downManager: manager, resolver: resolver, rejecter: rejecter, isDown: true, progress: 0,  requetUrl: baseUrl + ":\(port)" + url, tag: tag)

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
        guard let baseUrl = self.baseUrl,
            let account = self.account,
            let port = self.port,
            let password = self.password else {
            rejecter("801","未配置ftp参数",nil)
            return
        }
        var arr = path.components(separatedBy: "/")
        let fileName = arr.last ?? ""
        arr.removeLast()
        let testUrl = baseUrl + arr.joined(separator: "/")
        
        let server = FMServer(destination: testUrl, username: account, password: password)
        server?.port = Int32(port)
        let ftpManager = FTPManager()
        let isSuccess = ftpManager.deleteFileNamed(fileName, from: server)
        if isSuccess {
            resolver(nil)
        }else{
            rejecter("806","删除失败",nil)
        }
//        guard let provider = ftpManager else {
//            rejecter("802","未进行连接操作",nil)
//            return
//        }
//        provider.removeItem(path: path) { (error) in
//            if error != nil {
//                rejecter("806","删除失败",error)
//            }else{
//                resolver(nil)
//            }
//        }
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
        if realFtpDic.keys.contains(tag),
            var model = realFtpDic[tag]{
            model.progress = process
            self.sendEvent(model)
        }
    }
//    2020-04-03 14:36:11.767487+ size 36M
//    2020-04-03 14:36:51
//    2020-04-03 14:33:33 size 35.1M
//    2020-04-03 14:34:34.919911+0800
//    2020-04-03 15:14:11
//    2020-04-03 15:15:29.783576
//    2020-04-03 15:16:39
//    2020-04-03 15:31:01
//    2020-04-03 15:33:29.3529 68M
//    2020-04-03 15:34:41.720628
//    2020-04-03 15:36:29.196892+0800 68.8M
//    2020-04-03 15:37:06.886161+0800
//    2020-04-03 16:59:43.171638 72.2M
//    2020-04-03 17:01:34.770263
//    2020-04-03 17:48:41.
    func ftpDownloadStatus(destinationURL: String?, errorMsg: String?, tag: String) {
        if realFtpDic.keys.contains(tag),
            let model = realFtpDic[tag]{
            if destinationURL != nil,
            errorMsg == nil {
                NSLog("下载结束+++++++++++")
                model.resolver(JMFTPSyncFileTools.getJSONStringFrom(["tag":tag]))
            }else{
                model.rejecter("804",errorMsg,nil)
            }
            realFtpDic.removeValue(forKey: tag);
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
//extension JMFTPSyncFileManager:FTPManagerDelegate{
//    func ftpManagerUploadProgressDidChange(_ processInfo: [AnyHashable : Any]!) {
//
//    }
//    func ftpManagerDownloadFailureReason(_ failureReason: FMStreamFailureReason) {
//
//    }
//    func ftpManagerDownloadProgressDidChange(_ processInfo: [AnyHashable : Any]!) {
//
//    }
//}
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

