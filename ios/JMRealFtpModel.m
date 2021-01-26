//
//  JMRealFtpModel.m
//  RNReactNativeFtpJm
//
//  Created by lzj on 2021/1/26.
//  Copyright © 2021 Facebook. All rights reserved.
//

#import "JMRealFtpModel.h"

@implementation JMRealFtpModel

- (void)dealloc {
    [_ftpClient cancel];
    _ftpClient = nil;
    _resolver = nil;
    _rejecter = nil;
}

- (void)pause {
    if (_ftpClient) {
        [_ftpClient pause];
    }
}

- (void)resume {
    if (_ftpClient) {
        [_ftpClient resume];
    }
}

- (void)cancel {
    if (_ftpClient) {
        [_ftpClient cancel];
    }
}

+ (NSString *)toJsonString:(id)dic
{
    if (!dic) return @"";

    NSError* error = nil;
    NSData *jsonData = [NSJSONSerialization dataWithJSONObject:dic options:0 error:&error];
    NSString *jsonString = [[NSString alloc] initWithData:jsonData encoding:NSUTF8StringEncoding];
    if (error != nil)
        return nil;
    return jsonString;
}

@end
