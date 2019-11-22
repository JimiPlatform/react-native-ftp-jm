//
// ProjectName:  JMSmallAppEngine
// FileName:     JMUDPScoketManagerBridge.m
// Description:  <#Description#>
//
// Created by LiuLuJia on 2019/11/19.
// COPYRIGHT. ShenZhen JiMi Technology Co., Ltd. 2018.
// ALL RIGHTS RESERVED.
//
// No part of this publication may be reproduced, stored in a retrieval system, or transmitted,
// on any form or by any means, electronic, mechanical, photocopying, recording,
// or otherwise, without the prior written permission of ShenZhen JiMi Network Technology Co., Ltd.
//

#import <React/RCTBridgeModule.h>
#import <React/RCTBundleURLProvider.h>
#import <React/RCTRootView.h>
#import <React/RCTLog.h>
#import <React/RCTEventEmitter.h>

@interface RCT_EXTERN_MODULE(JMUDPScoketManager, RCTEventEmitter)

RCT_EXTERN_METHOD(configUDPSocket:(NSString *)host port:(NSInteger)port timeout:(NSInteger)timeout resolver:(RCTPromiseResolveBlock *)resolver rejecter:(RCTPromiseRejectBlock *)rejecter)

RCT_EXTERN_METHOD(send:(NSString *)data tag:(NSInteger)tag resolver:(RCTPromiseResolveBlock *)resolver rejecter:(RCTPromiseRejectBlock *)rejecter)

RCT_EXTERN_METHOD(closeSocket:(RCTPromiseResolveBlock *)resolver rejecter:(RCTPromiseRejectBlock *)rejecter)

RCT_EXTERN_METHOD(openWifi:(RCTPromiseResolveBlock *)resolver rejecter:(RCTPromiseRejectBlock *)rejecter)

@end
