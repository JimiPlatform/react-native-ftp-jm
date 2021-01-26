//
//  JMFTPSyncFileManager.m
//  RNReactNativeFtpJm
//
//  Created by lzj on 2021/1/26.
//  Copyright © 2021 Facebook. All rights reserved.
//

#import "JMFTPSyncFileManager.h"
#import <JMSmartFTPUtils/JMFTPClient.h>
#import "JMRealFtpModel.h"

NSString *const kRNJMFTPSyncFileManager = @"kRNJMFTPSyncFileManager";

@interface JMFTPSyncFileManager () <JMFTPClientDelegate>

@property (nonatomic,assign) BOOL isHasListeners;

@property (nonatomic,copy) NSString *baseUrl;
@property (nonatomic,assign) NSInteger port;
@property (nonatomic,copy) NSString *account;
@property (nonatomic,copy) NSString *password;

@property (nonatomic,strong) NSMutableDictionary *realFtpDic;
@property (nonatomic,strong) JMFTPClient *ftpClient;
@property (nonatomic,strong) JMFTPUserInfo *userInfo;

@end

@implementation JMFTPSyncFileManager
RCT_EXPORT_MODULE(JMFTPSyncFileManager);

- (void)startObserving {
    self.isHasListeners = YES;
    [super startObserving];
}

- (void)stopObserving {
    self.isHasListeners = NO;
    [super stopObserving];
}

- (NSArray<NSString *> *)supportedEvents {
    return @[kRNJMFTPSyncFileManager];    //添加监听方法名
}

- (NSDictionary *)constantsToExport {
    return @{kRNJMFTPSyncFileManager: kRNJMFTPSyncFileManager};    //导出监听方法名，方便JS调用
}

- (void)sendEventWithModel:(JMRealFtpModel *)model {
    if (self.isHasListeners && model) {
        NSMutableDictionary *dic = [NSMutableDictionary dictionary];
        [dic setValue:model.tag forKey:@"tag"];
        [dic setValue:model.requetUrl forKey:@"path"];
        [dic setValue:[NSNumber numberWithFloat:model.progress] forKey:@"progress"];
        NSString *jsonStr = [JMRealFtpModel toJsonString:dic];
        
        NSMutableDictionary *dataDic = [NSMutableDictionary dictionary];
        [dataDic setValue:jsonStr forKey:@"data"];
        NSString *dataStr = [JMRealFtpModel toJsonString:dataDic];
    
        [self sendEventWithName:kRNJMFTPSyncFileManager body:dataStr];
    }
}

- (BOOL)isValidInfo {
    if (self.baseUrl == nil || self.account == nil || self.password == nil) {
        return NO;
    }
    return YES;
}

- (NSMutableDictionary *)realFtpDic {
    if (!_realFtpDic) {
        _realFtpDic = [NSMutableDictionary dictionary];
    }
    return _realFtpDic;
}

#pragma mark -

RCT_EXPORT_METHOD(configFtpSyncFile:(NSString *)baseUrl mode:(NSString *)mode port:(NSInteger)port account:(NSString *)account password:(NSString *)password resolver:(RCTPromiseResolveBlock)resolver rejecter:(RCTPromiseRejectBlock)rejecter) {
    if (baseUrl == nil || account == nil || password == nil) {
        if (rejecter) rejecter(@"801", @"未配置ftp参数", nil);
        return;
    }
        
    self.baseUrl = baseUrl;
    self.port = port;
    self.account = account;
    self.password = password;
    if (resolver) resolver(nil);
}

RCT_EXPORT_METHOD(connectFTP:(RCTPromiseResolveBlock)resolver rejecter:(RCTPromiseRejectBlock)rejecter) {
    if (![self isValidInfo]) {
        if (rejecter) rejecter(@"801", @"未配置ftp参数",nil);
    }
    
    _userInfo = [[JMFTPUserInfo alloc] initWithDomain:self.baseUrl port:(int)self.port account:self.account password:self.password];
    _ftpClient = [[JMFTPClient alloc] initWithUserInfo:_userInfo];
    _ftpClient.delegate = self;
    if (resolver) resolver(nil);
}

RCT_EXPORT_METHOD(findFTPFlies:(NSString *)subPath resolver:(RCTPromiseResolveBlock)resolver rejecter:(RCTPromiseRejectBlock)rejecter) {
    if (!self.ftpClient) {
        rejecter(@"801", @"未配置ftp参数", nil);
        return;
    }
    
    NSMutableArray *list = [NSMutableArray array];
    [self.ftpClient findFliesWithDirPath:subPath completion:^(NSArray<JMFTPFileInfo *> * _Nonnull fileInfoList) {
        for (JMFTPFileInfo *info in fileInfoList) {
            [list addObject:@[@{
                                  @"fileName": info.fileName,
                                  @"fileSize": @(info.isDir ? 0 : info.fileSize),
                                  @"filePath": info.filePath,
                                  @"fileType": @(info.isDir ? 1 : 0)
            }]];
        }
        if (resolver) resolver([JMRealFtpModel toJsonString:list]);
    }];
}

RCT_EXPORT_METHOD(downFTPFile:(NSString *)url locaUrl:(NSString *)locaUrl fileName:(NSString *)fileName tag:(NSString *)tag resolver:(RCTPromiseResolveBlock)resolver rejecter:(RCTPromiseRejectBlock)rejecter) {
    if (!self.userInfo) {
        if (rejecter) rejecter(@"801", @"未配置ftp参数", nil);
        return;
    }
    
    JMFTPClient *ftpClient = [[JMFTPClient alloc] initWithUserInfo:_userInfo];
    ftpClient.delegate = self;
    ftpClient.tag = [tag integerValue];
    
    JMRealFtpModel *model = [[JMRealFtpModel alloc] init];
    model.ftpClient = ftpClient;
    model.isDown = YES;
    model.requetUrl = url;
    model.tag = tag;
    model.resolver = resolver;
    model.rejecter = rejecter;
    [self.realFtpDic setValue:model forKey:tag];
    
    [ftpClient downloadFileWithRemotePath:url localDir:locaUrl localFileName:fileName];
    NSLog(@"开始下载：%@, tag:%@, locaUrl:%@, fileName:%@", url, tag, locaUrl, fileName);
}

RCT_EXPORT_METHOD(uploadFTPFile:(NSString *)path locaUrl:(NSString *)locaUrl fileName:(NSString *)fileName overwrite:(BOOL)overwrite tag:(NSString *)tag resolver:(RCTPromiseResolveBlock)resolver rejecter:(RCTPromiseRejectBlock)rejecter) {
    if (!self.userInfo) {
        if (rejecter) rejecter(@"801", @"未配置ftp参数", nil);
        return;
    }
    
    JMFTPClient *ftpClient = [[JMFTPClient alloc] initWithUserInfo:_userInfo];
    ftpClient.delegate = self;
    ftpClient.tag = [tag integerValue];
    
    JMRealFtpModel *model = [[JMRealFtpModel alloc] init];
    model.ftpClient = ftpClient;
    model.isDown = NO;
    model.requetUrl = path;
    model.tag = tag;
    model.resolver = resolver;
    model.rejecter = rejecter;
    [self.realFtpDic setValue:model forKey:tag];
    
    [ftpClient uploadFileWithRemotePath:path loaclPath:[NSString stringWithFormat:@"%@%@", locaUrl, fileName]];
    NSLog(@"开始上传：%@, tag:%@, locaUrl:%@, fileName:%@", path, tag, locaUrl, fileName);
}

RCT_EXPORT_METHOD(ftpPause:(NSString *)tag resolver:(RCTPromiseResolveBlock)resolver rejecter:(RCTPromiseRejectBlock)rejecter) {
    JMRealFtpModel *model = [self.realFtpDic objectForKey:tag];
    if (model) {
        [model pause];
        if (resolver) resolver(nil);
    } else if (rejecter) {
        rejecter(@"805", @"没有当前tag", nil);
    }
}

RCT_EXPORT_METHOD(ftpResume:(NSString *)tag resolver:(RCTPromiseResolveBlock)resolver rejecter:(RCTPromiseRejectBlock)rejecter) {
    JMRealFtpModel *model = [self.realFtpDic objectForKey:tag];
    if (model) {
        [model resume];
        if (resolver) resolver(nil);
    } else if (rejecter) {
        rejecter(@"805", @"没有当前tag", nil);
    }
}

RCT_EXPORT_METHOD(ftpCancel:(NSString *)tag resolver:(RCTPromiseResolveBlock)resolver rejecter:(RCTPromiseRejectBlock)rejecter) {
    JMRealFtpModel *model = [self.realFtpDic objectForKey:tag];
    if (model) {
        [model cancel];
        [self.realFtpDic objectForKey:tag];
        if (resolver) resolver(nil);
    } else if (rejecter) {
        rejecter(@"805", @"没有当前tag", nil);
    }
}

RCT_EXPORT_METHOD(deleteFTPFile:(NSString *)path resolver:(RCTPromiseResolveBlock)resolver rejecter:(RCTPromiseRejectBlock)rejecter) {
    if (!self.ftpClient) {
        if (rejecter) rejecter(@"801", @"未配置ftp参数", nil);
        return;
    }
    
    [self.ftpClient deleteFileWithPath:path completion:^(BOOL isSuccess) {
        if (isSuccess) {
            if (resolver) resolver(nil);
        } else {
            if (rejecter) rejecter(@"806", @"删除失败", nil);
        }
    }];
}

RCT_EXPORT_METHOD(moveFTPFile:(NSString *)path toPath:(NSString *)toPath overwrite:(BOOL)overwrite resolver:(RCTPromiseResolveBlock)resolver rejecter:(RCTPromiseRejectBlock)rejecter) {
    if (!self.ftpClient) {
        if (rejecter) rejecter(@"801", @"未配置ftp参数", nil);
        return;
    }
    
    [self.ftpClient moveFileFromPath:path toPath:toPath completion:^(BOOL isSuccess) {
        if (isSuccess) {
            if (resolver) resolver([JMRealFtpModel toJsonString:@{@"path": toPath}]);
        } else {
            if (rejecter) rejecter(@"807", @"移动文件失败失败", nil);
        }
    }];
}

RCT_EXPORT_METHOD(closeFTP:(RCTPromiseResolveBlock)resolver rejecter:(RCTPromiseRejectBlock)rejecter) {
    _ftpClient.delegate = nil;
    _ftpClient = nil;
    _userInfo = nil;
    [self.realFtpDic removeAllObjects];
    if (resolver) resolver(nil);
}

#pragma mark - JMFTPClientDelegate

- (void)didFtpClient:(nonnull JMFTPClient *)client progress:(nonnull NSProgress *)progress {
    JMRealFtpModel *model = [self.realFtpDic objectForKey:[NSString stringWithFormat:@"%ld", (long)client.tag]];
    if (model) {
        model.progress = progress.fractionCompleted * 1.0 / progress.totalUnitCount;
        [self sendEventWithModel:model];
    }
}

- (void)didFtpClient:(nonnull JMFTPClient *)client state:(JMFTPClientState)state {
    NSString *tag = [NSString stringWithFormat:@"%ld", (long)client.tag];
    JMRealFtpModel *model = [self.realFtpDic objectForKey:tag];
    if (model) {
        if (state == JMFTPClientStateSuccess) {
            if (model.resolver)
                model.resolver([JMRealFtpModel toJsonString:@{@"tag": tag}]);
        } else {
            if (model.rejecter)
                model.rejecter(@"804", [JMRealFtpModel toJsonString:@{@"tag": tag, @"state": [NSNumber numberWithInteger:state]}], nil);
        }
        [self.realFtpDic removeObjectForKey:tag];
    }
}

@end
