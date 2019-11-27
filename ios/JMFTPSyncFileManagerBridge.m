//
//  FTPSyncFileManagerBridge.m
//  JMSmallAppEngine
//
//  Created by lzj<lizhijian_21@163.com> on 2019/3/7.
//  Copyright © 2019 jimi. All rights reserved.
//

#import <React/RCTBridgeModule.h>
#import <React/RCTBundleURLProvider.h>
#import <React/RCTRootView.h>
#import <React/RCTLog.h>
#import <React/RCTEventEmitter.h>

@interface RCT_EXTERN_MODULE(JMFTPSyncFileManager, RCTEventEmitter)

RCT_EXTERN_METHOD(configFtpSyncFile:(NSString *)baseUrl mode:(NSString *)mode port:(NSInteger)port account:(NSString *)account password:(NSString *)password resolver:(RCTPromiseResolveBlock *)resolver rejecter:(RCTPromiseRejectBlock *)rejecter)

RCT_EXTERN_METHOD(connectFTP:(RCTPromiseResolveBlock *)resolver rejecter:(RCTPromiseRejectBlock *)rejecter)

RCT_EXTERN_METHOD(findFTPFlies:(NSString *)subPath resolver:(RCTPromiseResolveBlock *)resolver rejecter:(RCTPromiseRejectBlock *)rejecter)

RCT_EXTERN_METHOD(downFTPFile:(NSString *)url locaUrl:(NSString *)locaUrl fileName:(NSString *)fileName tag:(NSString *)tag resolver:(RCTPromiseResolveBlock *)resolver rejecter:(RCTPromiseRejectBlock *)rejecter)

RCT_EXTERN_METHOD(uploadFTPFile:(NSString *)path locaUrl:(NSString *)locaUrl fileName:(NSString *)fileName overwrite:(BOOL)overwrite tag:(NSString *)tag resolver:(RCTPromiseResolveBlock *)resolver rejecter:(RCTPromiseRejectBlock *)rejecter)

RCT_EXTERN_METHOD(ftpPause:(NSString *)tag resolver:(RCTPromiseResolveBlock *)resolver rejecter:(RCTPromiseRejectBlock *)rejecter)

RCT_EXTERN_METHOD(ftpResume:(NSString *)tag resolver:(RCTPromiseResolveBlock *)resolver rejecter:(RCTPromiseRejectBlock *)rejecter)

RCT_EXTERN_METHOD(ftpCancel:(NSString *)tag resolver:(RCTPromiseResolveBlock *)resolver rejecter:(RCTPromiseRejectBlock *)rejecter)

RCT_EXTERN_METHOD(deleteFTPFile:(NSString *)path resolver:(RCTPromiseResolveBlock *)resolver rejecter:(RCTPromiseRejectBlock *)rejecter)

RCT_EXTERN_METHOD(moveFTPFile:(NSString *)path toPath:(NSString *)toPath overwrite:(BOOL)overwrite resolver:(RCTPromiseResolveBlock *)resolver rejecter:(RCTPromiseRejectBlock *)rejecter)

RCT_EXTERN_METHOD(closeFTP:(RCTPromiseResolveBlock *)resolver rejecter:(RCTPromiseRejectBlock *)rejecter)

@end
