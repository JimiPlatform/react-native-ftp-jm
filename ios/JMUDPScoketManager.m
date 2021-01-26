//
//  JMUDPScoketManager.m
//  RNReactNativeFtpJm
//
//  Created by lzj on 2021/1/26.
//  Copyright © 2021 Facebook. All rights reserved.
//

#import "JMUDPScoketManager.h"
#import "JMRealFtpModel.h"
#import <CocoaAsyncSocket/GCDAsyncUdpSocket.h>

NSString *const kRNJMUDPSocketManager = @"listeningUDPScoketCellBack";

@interface JMUDPScoketManager () <GCDAsyncUdpSocketDelegate>

@property (nonatomic,assign) BOOL isHasListeners;

@property (nonatomic,strong) GCDAsyncUdpSocket *socket;
@property (nonatomic,copy) NSString *host;
@property (nonatomic,assign) NSInteger port;
@property (nonatomic,assign) CGFloat sendTimeout;

@end

@implementation JMUDPScoketManager
RCT_EXPORT_MODULE(JMUDPScoketManager);

- (void)startObserving {
    self.isHasListeners = YES;
    [super startObserving];
}

- (void)stopObserving {
    self.isHasListeners = NO;
    [super stopObserving];
}

- (NSArray<NSString *> *)supportedEvents {
    return @[kRNJMUDPSocketManager];    //添加监听方法名
}

- (NSDictionary *)constantsToExport {
    return @{@"kRNJMUDPSocketManager": kRNJMUDPSocketManager};    //导出监听方法名，方便JS调用
}

- (void)sendEventWithCode:(NSInteger)code data:(id)data {
    if (self.isHasListeners) {
        NSMutableDictionary *dic = [NSMutableDictionary dictionary];
        [dic setValue:@(code) forKey:@"code"];
        if (data) {
            [dic setValue:[JMRealFtpModel toJsonString:data] forKey:@"data"];
        }
        
        [self sendEventWithName:kRNJMUDPSocketManager body:[JMRealFtpModel toJsonString:dic]];
    }
}

#pragma mark -

RCT_EXPORT_METHOD(configUDPSocket:(NSString *)host port:(NSInteger)port timeout:(NSInteger)timeout resolver:(RCTPromiseResolveBlock)resolver rejecter:(RCTPromiseRejectBlock)rejecter) {
    if (_socket) return;
    self.host = host;
    self.port = port;
    self.sendTimeout = timeout;
    
    _socket = [[GCDAsyncUdpSocket alloc] initWithDelegate:self delegateQueue:dispatch_get_main_queue()];
    NSError *error;
    BOOL ret = [self.socket bindToPort:port error:&error];
    if (!ret || !error) {
        _socket.delegate = nil;
        _socket = nil;
        rejecter(@"811", @"配置参数失败",nil);
        return;
    }
    
    ret = [self.socket enableBroadcast:YES error:nil];
    if (!ret || error) {
        _socket.delegate = nil;
        _socket = nil;
        if (rejecter) rejecter(@"604", @"启动失败",nil);
        return;
    }
    
    [self.socket beginReceiving:&error];
    if (resolver) resolver(nil);
}

RCT_EXPORT_METHOD(send:(NSString *)data tag:(NSInteger)tag resolver:(RCTPromiseResolveBlock)resolver rejecter:(RCTPromiseRejectBlock)rejecter) {
    if (!_socket || data) {
        if (rejecter) rejecter(@"605", @"未进行配置参数或data不正确", nil);
        return;
    }
    
    [self.socket sendData:[data dataUsingEncoding:NSUTF8StringEncoding] toHost:self.host port:self.port withTimeout:self.sendTimeout tag:tag];
    if (resolver) resolver(nil);
}

RCT_EXPORT_METHOD(closeSocket:(RCTPromiseResolveBlock)resolver rejecter:(RCTPromiseRejectBlock)rejecter) {
    if (_socket) {
        _socket.delegate = nil;
        _socket = nil;
    }
    if (resolver) resolver(nil);
}

#pragma mark - GCDAsyncUdpSocketDelegate

- (void)udpSocket:(GCDAsyncUdpSocket *)sock didNotConnect:(NSError *)error {
    [self sendEventWithCode:601 data:nil];
}

- (void)udpSocketDidClose:(GCDAsyncUdpSocket *)sock withError:(NSError *)error {
    [self sendEventWithCode:602 data:nil];
}

- (void)udpSocket:(GCDAsyncUdpSocket *)sock didConnectToAddress:(NSData *)address {
    [self sendEventWithCode:600 data:nil];
}

- (void)udpSocket:(GCDAsyncUdpSocket *)sock didReceiveData:(NSData *)data fromAddress:(NSData *)address withFilterContext:(id)filterContext {
    __autoreleasing NSError* error = nil;
    id result = [NSJSONSerialization JSONObjectWithData:data options:NSJSONReadingAllowFragments error:&error];
    if (error != nil)
        return;
 
    id dataDic = [JMRealFtpModel toJsonString:result];
    if (dataDic) {
        [self sendEventWithCode:603 data:nil];
    }
}

@end
