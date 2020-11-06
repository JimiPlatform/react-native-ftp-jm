//
//  JMBleCharacteristic.h
//  JMSmartBluetooth
//
//  Created by lizhijian on 2020/11/3.
//  Copyright © 2020 Jimi. All rights reserved.
//

#import <Foundation/Foundation.h>
#import <CoreBluetooth/CoreBluetooth.h>

NS_ASSUME_NONNULL_BEGIN

@interface JMBleCharacteristic : NSObject

@property(nonatomic) CBPeripheral *peripheral;
@property(nonatomic) CBService *service;
@property(nonatomic) CBCharacteristic *characteristic;

@property(nonatomic) NSString *uuid;
@property(nonatomic, assign) BOOL read;
@property(nonatomic, assign) BOOL write;
@property(nonatomic, assign) BOOL notify;
@property(nonatomic, assign) BOOL indicate;

@end

NS_ASSUME_NONNULL_END
