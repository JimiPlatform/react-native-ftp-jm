//
//  JMRealFtpModel.h
//  RNReactNativeFtpJm
//
//  Created by lzj on 2021/1/26.
//  Copyright © 2021 Facebook. All rights reserved.
//

#import <Foundation/Foundation.h>
#import <UIKit/UIKit.h>
#import <JMSmartFTPUtils/JMFTPClient.h>
#import <React/RCTBridgeModule.h>

NS_ASSUME_NONNULL_BEGIN

@interface JMRealFtpModel : NSObject

@property (nonatomic,strong) JMFTPClient *ftpClient;
@property (nonatomic,copy) RCTPromiseResolveBlock resolver;
@property (nonatomic,copy) RCTPromiseRejectBlock rejecter;

@property (nonatomic,assign) BOOL isDown;
@property (nonatomic,assign) CGFloat progress;
@property (nonatomic,copy) NSString *requetUrl;
@property (nonatomic,copy) NSString *tag;

- (void)pause;

- (void)resume;

- (void)cancel;

#pragma mark -

+ (NSString *)toJsonString:(id)dic;

@end

NS_ASSUME_NONNULL_END
