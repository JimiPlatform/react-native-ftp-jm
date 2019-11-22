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
import Alamofire
protocol JMFTPDownloadManagerDelegate {
    func ftpManagerDownloadProgressDidChange(process:Double,tag:String)
    func ftpDownloadStatus(destinationURL:String?,errorMsg:String?,tag:String)
}
class JMFTPDownloadManager: NSObject {
    var delegate : JMFTPDownloadManagerDelegate?
    var downloadRequest:DownloadRequest?
//    var delegate: DownloadTaskProtocol?
    var urlStr : String = ""
    var locaPath : String = ""
    var tag : String = ""
    convenience init(urlStr:String, locaPath:String,tag:String){
        self.init()
        self.urlStr = urlStr
        self.locaPath = locaPath
        self.tag = tag
    }

}
extension JMFTPDownloadManager{
    func startDownload() {
        guard let locaUrl = URL(string: locaPath) else {
            return
        }
        //写入的目标文件
        let destination:DownloadRequest.DownloadFileDestination = { _, response in
            return (locaUrl,[.removePreviousFile,.createIntermediateDirectories])
        }
        if let data = FileManager.default.contents(atPath: locaPath) {
            //续传
            self.downloadRequest = Alamofire.download(resumingWith: data, to: destination)
            self.downloadRequest?.downloadProgress(closure: downloadProgress)
            self.downloadRequest?.responseData(completionHandler: downloadResponse)
        }else{
            //下载
            self.downloadRequest = Alamofire.download(urlStr, to: destination)
            self.downloadRequest?.downloadProgress(closure: downloadProgress)
            self.downloadRequest?.responseData(completionHandler: downloadResponse)
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
extension JMFTPDownloadManager{
    //暂停下载
    func pause()  {
        self.downloadRequest?.suspend()
    }
    //恢复下载
    func resume()  {
        self.downloadRequest?.resume()
    }
    //取消下载
    func cancel()  {
        self.downloadRequest?.cancel()
    }
}
//MARK:回调进度
extension JMFTPDownloadManager{
    func downloadProgress(progress:Progress){
        delegate?.ftpManagerDownloadProgressDidChange(process: progress.fractionCompleted,tag: tag)
    }
    func downloadResponse(response:DownloadResponse<Data>){
        switch response.result {
        case .success(_):
            //下载完成
            DispatchQueue.main.async {
                print("路径:\(String(describing: response.destinationURL?.path))")
            }
            delegate?.ftpDownloadStatus(destinationURL: String(describing: response.destinationURL?.path), errorMsg: nil,tag: tag)
        case .failure(error:):
            if let data = response.resumeData,
                FileManager.default.fileExists(atPath: locaPath){
                let fileHandle = FileHandle.init(forWritingAtPath: locaPath)
                fileHandle?.seekToEndOfFile()
                // 写文件
                fileHandle?.write(data)
                // 关闭文件
                fileHandle?.closeFile()
            }
            delegate?.ftpDownloadStatus(destinationURL: nil, errorMsg: "FTP下载失败",tag: tag)
            break
        }
    }
}
