//
// ProjectName:  JiMiPlatfromProject
// FileName:     JMFTPDownloadManager.swift
// Description:  <#Description#>
//
// Created by LiuLuJia on 2019/11/12.
// COPYRIGHT. ShenZhen JiMi Technology Co., Ltd. 2018.
// ALL RIGHTS RESERVED.
//
// No part of this publication may be reproduced, stored in a retrieval system, or transmitted,
// on any form or by any means, electronic, mechanical, photocopying, recording,
// or otherwise, without the prior written permission of ShenZhen JiMi Network Technology Co., Ltd.
//

import Foundation
import JMCurl
protocol JMFTPDownloadManagerDelegate : NSObjectProtocol {
    func ftpManagerDownloadProgressDidChange(process:Double,tag:String)
    func ftpDownloadStatus(destinationURL:String?,errorMsg:String?,tag:String)
}
class JMFTPDownloadManager: NSObject {
    weak var delegate : JMFTPDownloadManagerDelegate?
    var downManager:FTPCurlDownloadManager = FTPCurlDownloadManager()
//    var delegate: DownloadTaskProtocol?
    var urlStr : String = ""
    var locaPath : String = ""
    var tag : String = ""
    var lastProgress: Double = 0
    var locaDataSize : Int = 0
    convenience init(urlStr:String, locaPath:String,tag:String){
        self.init()
        self.urlStr = urlStr
        self.locaPath = locaPath
    }

}
extension JMFTPDownloadManager{
    func startDownload(urlStr:String,locaPath:String,tag:String,account:String,password:String) {
        self.urlStr = urlStr
        self.locaPath = locaPath
        self.tag = tag
        self.locaDataSize = getFileSize(locaPath: locaPath);
        downManager.delegate = self
        DispatchQueue.global().async {
            self.downManager.startDownload(urlStr, locaPath: locaPath, account: account, password: password)
        }
        
    }
    //离线是 获取 本地的数据大小
    func getFileSize(locaPath:String) -> Int{
        guard let dict = try? FileManager.default.attributesOfItem(atPath: locaPath),
            let size = Int("\(String(describing: dict[FileAttributeKey.size]))") else {
            return 0
        }
        return size
    }
}
extension JMFTPDownloadManager:FTPCurlDownloadManagerDelegate{
    func ftpManagerDownloadProgressDidChange(_ processInfo: [AnyHashable : Any]!) {
        guard let info = processInfo as? [String : Any],
            let fileSizeProcessed = info["fileSizeProcessed"] as? Double,
            let fileSize = info["fileSize"] as? Double  else {
            self.delegate?.ftpDownloadStatus(destinationURL: nil, errorMsg: "下载失败", tag: self.tag );
            return;
        }
        let loadSize  = fileSizeProcessed + Double(self.locaDataSize)
        let totalSize = fileSize + Double(self.locaDataSize)
        let progress = loadSize / totalSize
        print("+++++++ loadSize = \(loadSize) totalSize = \(totalSize) progress = \(progress)")
        if progress >= 1.0 && fileSizeProcessed > 0 && fileSizeProcessed == fileSize {
            self.delegate?.ftpDownloadStatus(destinationURL: self.locaPath, errorMsg: nil, tag: self.tag);
        }else if progress > lastProgress && fileSizeProcessed > 0{
            self.delegate?.ftpManagerDownloadProgressDidChange(process: progress, tag: self.tag)
            lastProgress = progress
        }
    }
}
extension JMFTPDownloadManager{
    //暂停下载
    func pause()  {
        self.downManager.pauseDownload()
    }
    //恢复下载
    func resume()  {
        self.downManager.resumeDownload()
    }
    //取消下载
    func cancel()  {
        self.downManager.cancelDownload()
    }
}

