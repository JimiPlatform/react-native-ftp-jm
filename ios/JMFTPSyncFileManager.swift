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
    var downManager: JMFTPDownloadManager = JMFTPDownloadManager()
    var resolver: RCTPromiseResolveBlock
    var rejecter: RCTPromiseRejectBlock
    var isDown : Bool
    var progress : Double = 0
    var updateManager : Progress = Progress()
    var requetUrl : String
    var tag : String
    func pause() {
        if isDown {
            downManager.pause()
        }else{
            updateManager.pause()
        }
    }
    func resume()  {
        if isDown {
            downManager.resume()
        }else{
            updateManager.resume()
        }
    }
    func cancel()  {
        if isDown {
            downManager.cancel()
        }else{
            updateManager.cancel()
        }
    }
}
@objc(JMFTPSyncFileManager)
class JMFTPSyncFileManager: RCTEventEmitter {
    enum JMFTPMode : String {
        case passive = "passive"
        case active = "active"
        func getFTPFileProviderMode()->FTPFileProvider.Mode {
            switch self {
            case .passive:
                return FTPFileProvider.Mode.passive
            case .active:
                return FTPFileProvider.Mode.active
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
        guard let state = JMFTPMode(rawValue: mode) else {
            rejecter("-800","mode不正确",nil)
            return
        }
        self.baseUrl = baseUrl + ":" + String(port)
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
        let credential = URLCredential(user: account, password: password, persistence: URLCredential.Persistence.forSession)
        ftpManager = FTPFileProvider(baseURL: url, mode: ftpMode.getFTPFileProviderMode(), credential: credential, cache: nil)
        ftpManager?.delegate = self
        resolver(nil)
    }
    //MARK:获取指点文件夹下文件
    @objc(findFTPFlies:recursive:resolver:rejecter:)
    func findFTPFlies(subPath:String,
                      recursive:Bool,
                      resolver:@escaping RCTPromiseResolveBlock,
                      rejecter:@escaping RCTPromiseRejectBlock)  {
        guard let provider = ftpManager else {
            rejecter("802","未进行连接操作",nil)
            return
        }
        DispatchQueue.main.async {
            provider.searchFiles(path: subPath, recursive: recursive, query: NSPredicate(), foundItemHandler: { (object) in
                
            }) { (FileObjects, error) in
                  guard error == nil else{
                          rejecter("803","获取文件错误",error!)
                          return
                }
                var files = [[String:String]]()
                for obj in FileObjects{
                    files.append(["name":obj.name])
                }
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
        let manager = JMFTPDownloadManager(urlStr: url, locaPath: locaUrl, tag: tag)
        manager.delegate = self
        manager.startDownload()
        realFtpDic[tag] = JMRealDownUpdataFtpModel(downManager: manager, resolver: resolver, rejecter: rejecter, isDown: true, requetUrl: url, tag: tag)//添加下载控制器到字典中
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
        realFtpDic[tag] = JMRealDownUpdataFtpModel.init( resolver: resolver, rejecter: rejecter, isDown: false, updateManager: pro, requetUrl: path, tag: tag)

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
                resolver(nil)
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
        case .copy(source: let source, destination: let dest) where source.hasPrefix("file://"):
            print("Uploading \((source as NSString).lastPathComponent) to \(dest): \(progress * 100) completed.")
        case .copy(source: let source, destination: let dest):
            print("Copy \(source) to \(dest): \(progress * 100) completed.")
        case .modify(path: let path)://上传进度
            print("path = \(path) progress = \(progress)")
            let keys = realFtpDic.keys
            keys.forEach { (key) in
                guard var model = self.realFtpDic[key] else{return}
                model.progress = Double(progress)
                self.realFtpDic[key] = model
//                if model.requetUrl == path{
//                    sendEvent(model, Double(progress))
//                }
            }
        default:
            break
        }
        return
    }
}
//let onRNFTPSyncFileManager_JM   = "kRNFTPSyncFileManager"
//
//public enum JMFTPMediaType: String {
//    case video = "VIDEO"
//    case pic = "PIC"
//    case other  = "Other"
//}
//
//@objc(FTPSyncFileManager)
//class JMFTPSyncFileManager: RCTEventEmitter {
//    var isHasListeners = false      //JS是否有监听事件
//
//    var imei: String = ""
//    var isLocPath: Bool = false
//    var isConnectDevice = false
//    var fileType: JMFTPMediaType = .pic
//    var deviceFiles = [[JMFTPMediaFile]]()
////
//    lazy var ftpConnect : JMFTPConnect = {
//        let ftp = JMFTPConnect()
//        return ftp
//    }()
//    var transferVM = JMFTPMediaTransferModel()
//    var synchronVM = JMFTPMediaSynchronModel(.pic, isLocPath: false)
//
//    deinit {
//        debugPrint("FTP->deinit")
//        JMFTPTransferLoadManager.shareInstance.pauseAllDownload()
//        transferVM.delegate = nil
//        synchronVM.delegate = nil
//    }
//
//    /// JS开启监听事件
//    override func startObserving() {
//        isHasListeners = true
//    }
//
//    /// JS停止监听事件
//    override func stopObserving() {
//        isHasListeners = false
//    }
//
//    override func supportedEvents() -> [String]! {
//        return [onRNFTPSyncFileManager_JM
//        ]
//    }
//
//    override func constantsToExport() -> [AnyHashable : Any]! {
//        return [onRNFTPSyncFileManager_JM: onRNFTPSyncFileManager_JM]
//    }
//
//    func sendEvent(_ method: String?, _ body: Any?) -> Void {
//        if (isHasListeners) {
//            let dataDic = NSMutableDictionary.init()
//            dataDic.setValue(method, forKey: "method")
//
//            if body != nil {
//                dataDic.setValue(body, forKey: "data")
//            }
//            let jsonStr = JMUtilityHelper.getJSONStringFromSwiftAny(obj: dataDic)
//            debugPrint(jsonStr)
//
//            self.sendEvent(withName: onRNFTPSyncFileManager_JM, body: jsonStr)
//        }
//    }
//
//    ///  获取当前手机的WIFI名称(iOS12需要WIFI证书)
//    ///
//    /// - Parameter callback: RN回调
//    @objc(currentWiFi:)
//    func currentWiFi(callback: RCTResponseSenderBlock) -> Void {
//        let ssid = JMFTPSyncFileTools.currentWifi()
//        callback([ssid])
//    }
//
//    /// 发送打开设备WIFI热点命令
//    ///
//    /// - Parameters:
//    ///   - url: 服务器接口地址，几米圈1.0：http://smarthome.jimicloud.com/route/app
//    ///   - imei: 设备IMEI
//    ///   - appKey: appKey  测试用：449A7D0E9C1911E7BEDB00219B9A2EF3
//    ///   - secret: secret  测试用：695c1a459c1911e7bedb00219b9a2ef3
//    ///   - token: 服务器登录Token，测试用：2FCB70C6A1EE00CF688F5E9C54C3D502
//    @objc(openDeviceWIFI:imei:appKey:secret:token:)
//    func openDeviceWIFI(url: String, imei: String, appKey: String, secret: String, token:String) {
//        if (imei.isEmpty) {
//            return
//        }
//        self.imei = imei
//
//        if JMFTPSyncFileTools.currentWifi() != imei {
//            let parameters = NSMutableDictionary.init(dictionary: [
//                "method":"jimi.smarthome.device.custom.instruct.send",
//                "imei":imei,
//                "instruct":"WIFI,ON",
//                "accessToken": token,
//                "timestamp": Date.timestamp,
//                "app_key": appKey
//                ])
//            parameters["sign"] = JMParamsEncryption.signToParams(parameters as! [String : Any], secret: secret)
//
//            JMFTPSyncFileTools.openDeviceWIFI(url: url, parameters: parameters as! [String : Any], success: { [weak self](msg) in
//                guard let weakSelf = self else { return }
//
//                let dataDic = NSMutableDictionary.init()
//                dataDic.setValue(true, forKey: "success")
//                dataDic.setValue("设备WIFI打开成功,请连接wifi", forKey: "msg")
//                weakSelf.sendEvent("onOpenDeviceWIFI", dataDic)
//            }) { [weak self](msg) in
//                guard let weakSelf = self else { return }
//                let dataDic = NSMutableDictionary.init()
//                dataDic.setValue(false, forKey: "success")
//                dataDic.setValue("设备WIFI打开失败", forKey: "msg")
//                dataDic.setValue(msg, forKey: "errMsg")
//                weakSelf.sendEvent("onOpenDeviceWIFI", dataDic)
//            }
//        } else {
//            let dataDic = NSMutableDictionary.init()
//            dataDic.setValue(true, forKey: "success")
//            dataDic.setValue("设备WIFI打开成功,wifi已连接", forKey: "msg")
//            self.sendEvent("onOpenDeviceWIFI", dataDic)
//        }
//    }
//
//    /// 连接设备FTP服务
//    ///
//    /// - Parameter imei: 设备IMEI
//    @objc(connectFTP:)
//    func connectFTP(imei: String) -> Void {
//        debugPrint("FTP->connectFTP:%s", imei)
//        self.imei = imei
//        DispatchQueue.main.async {  //需要在主线程运行
//            self.ftpConnect.startConnect { [weak self](msg, success) in
//                guard let weakSelf = self else { return }
//                if (weakSelf.isConnectDevice != success) {
//                    weakSelf.isConnectDevice = success
//
//                    let dataDic = NSMutableDictionary.init()
//                    dataDic.setValue(success, forKey: "success")
//                    dataDic.setValue(success == true ? "已经成功连接设备!" : "与设备连接异常!", forKey: "msg")
//                    weakSelf.sendEvent("onConnectFTP", dataDic)
//                }
//            }
//        }
//    }
//
//    /// 断开设备FTP连接
//    @objc(closeFTP)
//    func closeFTP() -> Void {
//        debugPrint("FTP->closeFTP")
//        JMFTPTransferLoadManager.shareInstance.pauseAllDownload()
//        transferVM.delegate = nil
//        synchronVM.delegate = nil
//        isConnectDevice = false
//    }
//
//    /// 查找所有视频或图片文件(若是图片，查找完成之后会自动下载)
//    ///
//    /// - Parameter isVideo: 是否查找视频
//    @objc(findAllFile:)
//    func findAllFile(isVideo: Bool) -> Void {
//        debugPrint("FTP->findAllFile:%d", isVideo)
//        if self.imei.isEmpty || !self.isConnectDevice {
//            return
//        }
//        self.fileType = isVideo ? .video : .pic
//
//        // 停止外部ftp所有下载（当前只能单个下载）
//        JMFTPTransferLoadManager.shareInstance.pauseAllDownload()
//
//        // 判断是否连接设备，读取设备内存卡，获取照片、视频缩略图；
//        synchronVM.imei = self.imei
//        synchronVM.fileType = self.fileType
//        synchronVM.delegate = self
//
//        transferVM.delegate = self
//
//        DispatchQueue.main.async {  //需要在主线程运行
//            self.synchronVM.connectDevice()
//        }
//    }
//
//    /// 下载视频
//    ///
//    /// - Parameter fileList: 文件名称列表
//    @objc(downloadFile:)
//    func downloadFile(fileList: NSArray) -> Void {
//        if self.imei.isEmpty ||
//            !self.isConnectDevice ||
//            transferVM.delegate == nil {
//            return
//        }
//
//        DispatchQueue.global().async {
//            for name in fileList {
//                self.deviceFiles.map({ $0.filter({ $0.name == name as! String }) }).filter({ $0.count != 0 }).flatMap({ $0 }).forEach({
//                    self.transferVM.transferItemFromDevice($0, fileType: $0.fileType, locationfileExistCompleted: { (path) in
//                    })
//                })
//            }
//        }
//    }
//
//    @objc(pauseFile:)
//    func pauseFile(name: String) -> Void {
//        debugPrint("FTP->pauseFile1:%s", name)
//        DispatchQueue.global().async {
//            self.deviceFiles.map({ $0.filter({ $0.name == name }) }).filter({ $0.count != 0 }).flatMap({ $0 }).forEach({
//                debugPrint("FTP->pauseFile2:%s", $0.url)
//                JMFTPTransferLoadManager.shareInstance.pauseDownload(url: $0.url)
//            })
//        }
//    }
//
//
//    /// 删除视频或图片
//    ///
//    /// - Parameter fileList: 文件名称列表
//    @objc(deleteFile:)
//    func deleteFile(fileList: NSArray) -> Void {
//        if  self.imei.isEmpty ||
//            !self.isConnectDevice ||
//            transferVM.delegate == nil {
//            return
//        }
//
//        DispatchQueue.global().async {
//            var count = fileList.count
//            var deleteOK = false
//            for name in fileList {
//                self.deviceFiles.map({ $0.filter({ $0.name == name as! String }) }).filter({ $0.count != 0 }).flatMap({ $0 }).forEach({
//                    let item = $0
//                    self.synchronVM.isLocPath = self.isLocPath
//                    $0.isDeleting = true
//
//                    self.synchronVM.deleteItem($0, { [weak self](isDeleted) in
//                        if isDeleted {
//                            deleteOK = true
//                            item.isMarked = true
//                        }
//                        item.isDeleting = false
//
//                        count = count - 1
//                        if count <= 0 {     //全部删除完之后才发送消息
//                            guard let weakSelf = self else { return }
//
//                            weakSelf.synchronVM.deviceFiles = weakSelf.deviceFiles.map({ $0.filter({ $0.isMarked == false }) }).filter({ $0.count != 0 })
//
//                            let dataDic = NSMutableDictionary.init()
//                            dataDic.setValue(deleteOK, forKey: "success")
//                            weakSelf.sendEvent("onDeleteFile", dataDic)
//                        }
//                    })
//                })
//            }
//        }
//    }
//}
//
//extension JMFTPSyncFileManager: JMMediaSynchronVMDelegate {
//
//    // 设备连接
//    func mediaSynchronVMConnectDeviceWifi(_ viewModel: JMFTPMediaSynchronModel, _ isConnected: Bool) {
//        if (isConnectDevice != isConnected) {
//            isConnectDevice = isConnected
//
//            let dataDic = NSMutableDictionary.init()
//            dataDic.setValue(isConnected, forKey: "success")
//            dataDic.setValue(isConnected == true ? "已经成功连接设备!" : "与设备连接异常!", forKey: "msg")
//            DispatchQueue.global().async {
//                self.sendEvent("onConnectFTP", dataDic);
//            }
//        }
//    }
//
//    // 读取数据
//    func mediaSynchronVMReadCardData(_ viewModel: JMFTPMediaSynchronModel, _ isPrepared: Bool) {
//        if isPrepared {
//            let filesArray = NSMutableArray.init()
//            deviceFiles = synchronVM.deviceFiles
//            for fileItemArray in synchronVM.deviceFiles {
//                let dataArray = NSMutableArray.init()
//                for fileItem in fileItemArray {
//                    let dic = fileItem.toDictionary();
//                    dataArray.add(dic)
//                    if self.fileType == .pic {
//                        transferVM.transferItemFromDevice(fileItem, fileType: .pic, locationfileExistCompleted: { (path) in
//                        })
//                    }
//                }
//                filesArray.add(dataArray)
//            }
//
//            self.sendEvent("onFindAllFile", filesArray)
//        } else {
//            self.sendEvent("onFindAllFile", NSArray.init())
//        }
//    }
//}
//
////MARK: - DeviceMediaFileItemDownloadDelegate
//extension JMFTPSyncFileManager: JMDeviceMediaFileItemDownloadDelegate {
//
//    // 正在下载
//    // 下载过程：下载时，不断更新下载进度 cell状态的显示
//    func deviceMediaFileItemDownloadLoading(_ fileItem: JMFTPMediaFile, _ transferItem: JMFTPTransferItem) {
////        print("deviceMediaFileItemDownloadLoading:\(fileItem.localUrl), progress:\(transferItem.progress)")
//
//        let dataDic = NSMutableDictionary.init()
//        dataDic.setValue(Double(transferItem.progress), forKey: "progress")
//        dataDic.setValue(fileItem.name, forKey: "name")
//        self.sendEvent("onDownloadFile", dataDic)
//    }
//
//    // 下载成功
//    func deviceMediaFileItemDownloadSucceed(_ fileItem: JMFTPMediaFile, _ transferItem: JMFTPTransferItem) {
//
//        print("deviceMediaFileItemDownloadSucceed:\(fileItem.localUrl)")
//
//        let dataDic = NSMutableDictionary.init()
//        dataDic.setValue(1.0, forKey: "progress")
//        dataDic.setValue(fileItem.name, forKey: "name")
//        self.sendEvent("onDownloadFile", dataDic)
//    }
//}
