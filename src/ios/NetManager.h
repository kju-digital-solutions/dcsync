//
//  NetManager.h
//  DYB
//
//  Created by albert on 10/27/14.
//  Copyright (c) 2014 albert. All rights reserved.
//

#import <Foundation/Foundation.h>

@class NetManager;

@protocol NetManagerDelegate <NSObject>

-(void) requestDidFailWithError:(NSError *)error;
-(void) requestDidFinish:(NSData *)result;

@end

@interface NetManager : NSObject

@property (weak, nonatomic) id <NetManagerDelegate> delegate;

+(id) sharedManager;

-(void) sendGETRequestTo : (NSString *)url
                   header: (NSDictionary*)headerDict;

-(void) sendPOSTRequestTo : (NSString *)url
                    postData : (NSDictionary*)dict;

@end
