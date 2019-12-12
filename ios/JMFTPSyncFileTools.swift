//
// ProjectName:  JiMiPlatfromProject
// FileName:     FTPSyncFileTools.swift
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
//


import UIKit
import SystemConfiguration.CaptiveNetwork
import AVFoundation


class JMFTPSyncFileTools: NSObject {
    /// 转换为josn字符串
    /// - Parameter obj: 待转换对象
    class func getJSONStringFrom(_ obj:Any) -> String {
        if (!JSONSerialization.isValidJSONObject(obj)) {
            print("无法解析出JSONString")
            return ""
        }
        guard let data = try? JSONSerialization.data(withJSONObject: obj, options: []),
            let JSONString = String(data: data, encoding: .utf8)  else {
            return ""
        }
        return JSONString
    }
    class func createDicFile(filePath:String)->Bool {
        let fileManager = FileManager.default
        if fileManager.fileExists(atPath: filePath) {
            return true
        }
        do{
            try fileManager.createDirectory(atPath: filePath, withIntermediateDirectories: true, attributes: nil)
            return true
        }catch{
            return false
        }
    }
}
