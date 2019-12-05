
# react-native-ftp-jm

## 安装方法

`$ npm install react-native-ftp-jm --save`

### 关联原生
`$ react-native link react-native-ftp-jm`

***
# UDPScoket模块（模块名：JMUDPScoketManager）
## 配置UDPSocket参数
- 功能:
    配置socket所需参数
- 方法名:
    configUDPSocket

参数 | 类型 |  是否必须 | 说明  
| - | :-: | :-: | - |
host | String | 是 | 域名
port | Int | 是 | 主机端口
timeout | Int | 是 | 发送延时
resolver | RCTPromiseResolveBlock | 是 | 成功回调（RN专属回调）
rejecter | RCTPromiseRejectBlock | 是 | 失败回调（RN专属回调）
## 发送指令
- 功能: 
    发送socket指令
- 方法名:
    send

参数 | 类型 |  是否必须 | 说明  
| - | :-: | :-: | - |
data | String | 是 | 发送数据
tag | Int | 是 | 标签
resolver | RCTPromiseResolveBlock | 是 | 成功回调（RN专属回调）
rejecter | RCTPromiseRejectBlock | 是 | 失败回调（RN专属回调）
## 关闭链接
- 功能:  
    关闭socket链接
- 方法名:
    closeSocket
    
参数 | 类型 |  是否必须 | 说明  
| - | :-: | :-: | - |
resolver | RCTPromiseResolveBlock | 是 | 成功回调（RN专属回调）
rejecter | RCTPromiseRejectBlock | 是 | 失败回调（RN专属回调）
## 接受数据
- 功能  
    接受服务端发送数据，小程序通过监听该接口方法名获取
- 方法名:
    listeningUDPScoketCellBack
- 接口内body结构

参数 | 类型 |  是否必须 | 说明  
| - | :-: | :-: | - |
code | Int | 是 | 发送相应标识code码
data | string | 否 | 当接受到服务端数据时，data内将包含服务端数据json字符串

`{"code":600,"data":""}`

- code码:

code码 | 说明 |  
| - | - |
600 | 连接成功 | 
601 | 连接失败 | 
602 | 关闭链接 | 
603 | 接受到数据 | 
604 | 发送失败 | 

# FTPFile模块（模块名：JMFTPSyncFileManager）
## 配置Ftp所需参数
- 功能:
    配置参数
- 方法名:
    configFtpSyncFile

参数 | 类型 |  是否必须 | 说明  
| - | :-: | :-: | - |
baseUrl | String | 是 | FTP地址
mode | String | 是 | ftp模式被动模式（passive）主动模式（active）
port | Int | 是 | 主机端口
account | string | 是 | 账号
password | string | 是 | 密码
resolver | RCTPromiseResolveBlock | 是 | 成功回调（RN专属回调）
rejecter | RCTPromiseRejectBlock | 是 | 失败回调（RN专属回调）
## 连接ftp
- 功能:
    发起ftp连接
- 方法名:
    connectFTP

参数 | 类型 |  是否必须 | 说明  
| - | :-: | :-: | - |
resolver | RCTPromiseResolveBlock | 是 | 成功回调（RN专属回调）
rejecter | RCTPromiseRejectBlock | 是 | 失败回调（RN专属回调）
## 获取指定文件夹下文件
- 功能:
    获取指点文件夹下文件
- 方法名:
    findFTPFlies

参数 | 类型 |  是否必须 | 说明  
| - | :-: | :-: | - |
subPath | string | 是 | 文件夹路径
resolver | RCTPromiseResolveBlock | 是 | 成功回调（RN专属回调）body:[{"fileName":"",fileSize:"","filePath":""]}]body为json字符串
rejecter | RCTPromiseRejectBlock | 是 | 失败回调（RN专属回调）

## 下载ftp文件
- 功能:
    下载指定服务端文件
- 方法名:
    downFTPFile

参数 | 类型 |  是否必须 | 说明  
| - | :-: | :-: | - |
url | string | 是 | 下载地址
locaUrl | string | 是 | 若之前文件未下载完成，传之前已下载文件本地路径，若没有之前下载文件则传文件将要下载到的本地路径
fileName | string | 是 | 文件名称
tag | String | 是 | 标签
resolver | RCTPromiseResolveBlock | 是 | 成功回调（RN专属回调）body:{"tag":""} body为json字符串
rejecter | RCTPromiseRejectBlock | 是 | 失败回调（RN专属回调）

## 上传FTP文件
- 功能:
    上传FTP文件到服务端指定路径
- 方法名:
    uploadFTPFile

参数 | 类型 |  是否必须 | 说明  
| - | :-: | :-: | - |
path | string | 是 | 上传地址
locaUrl | string | 是 | 待上传文件地址
fileName | string | 是 | 文件名称
overwrite | bool | 是 | 若文件存在则覆盖
tag | String | 是 | 标签
resolver | RCTPromiseResolveBlock | 是 | 成功回调（RN专属回调）body:{"tag":"",path:""} body为json字符串 path为文件在服务端路径
rejecter | RCTPromiseRejectBlock | 是 | 失败回调（RN专属回调）

## 暂停下载或者上传
- 功能:
    暂停下载或者上传文件
- 方法名:
    ftpPause

参数 | 类型 |  是否必须 | 说明  
| - | :-: | :-: | - |
tag | String | 是 | 标签
resolver | RCTPromiseResolveBlock | 是 | 成功回调（RN专属回调）
rejecter | RCTPromiseRejectBlock | 是 | 失败回调（RN专属回调）

## 恢复下载或者上传
- 功能:
    恢复下载或者上传文件
- 方法名:
    ftpResume

参数 | 类型 |  是否必须 | 说明  
| - | :-: | :-: | - |
tag | String | 是 | 标签
resolver | RCTPromiseResolveBlock | 是 | 成功回调（RN专属回调）
rejecter | RCTPromiseRejectBlock | 是 | 失败回调（RN专属回调）

## 取消下载或者上传
- 功能:
    取消下载或者上传文件
- 方法名:
    ftpCancel

参数 | 类型 |  是否必须 | 说明  
| - | :-: | :-: | - |
tag | String | 是 | 标签
resolver | RCTPromiseResolveBlock | 是 | 成功回调（RN专属回调）
rejecter | RCTPromiseRejectBlock | 是 | 失败回调（RN专属回调）

## 删除FTP文件
- 功能:
    删除服务端FTP文件
- 方法名:
    deleteFTPFile

参数 | 类型 |  是否必须 | 说明  
| - | :-: | :-: | - |
path | string | 是 | 待删除文件路径
resolver | RCTPromiseResolveBlock | 是 | 成功回调（RN专属回调
rejecter | RCTPromiseRejectBlock | 是 | 失败回调（RN专属回调）

## 移动FTP文件
- 功能:
    移动FTP文件
- 方法名:
    moveFTPFile

参数 | 类型 |  是否必须 | 说明  
| - | :-: | :-: | - |
path | string | 是 | 原始文件路径
toPath | string | 是 | 将要移动的文件路径
overwrite | Bool | 是 | 若带移动的文件路径上有文件是否覆盖
resolver | RCTPromiseResolveBlock | 是 | 成功回调（RN专属回调）body:{path:""} body为json字符串 path为文件在服务端路径
rejecter | RCTPromiseRejectBlock | 是 | 失败回调（RN专属回调）

## 关闭FTP链接
- 功能:
    关闭FTP链接
- 方法名:
    closeFTP

参数 | 类型 |  是否必须 | 说明  
| - | :-: | :-: | - |
resolver | RCTPromiseResolveBlock | 是 | 成功回调（RN专属回调）
rejecter | RCTPromiseRejectBlock | 是 | 失败回调（RN专属回调）

## 进度回调通知
- 方法名:
 listeningFTPProgressCellBack
- 接口内body结构

参数 | 类型 |  是否必须 | 说明  
| - | :-: | :-: | - |
path | Int | 是 | 下载或者上传连接
progress | Double | 是 | 当前进度
tag | string | 是 | 标签
## 错误码
code码 | 说明 |  
| :-: | :-: |
800 | mode不正确 | 
801 | 未配置ftp参数 | 
802 | 未进行连接操作 | 
803 | 获取文件错误 | 
804 | 下载错误 |
805 | 没有当前tag | 
806 | 删除失败 | 
807 | 移动文件失败失败 |
808 | 上传失败 |
809 | 连接失败 |
```
  
