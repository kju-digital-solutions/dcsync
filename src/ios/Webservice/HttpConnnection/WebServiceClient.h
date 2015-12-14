//
//  WebServiceClient.h
//  ChefDe Cuisine Organiser
//
//  Created by Passion on 7/15/15.
//  Copyright (c) 2014 leethal.chef.com. All rights reserved.
//

#import <Foundation/Foundation.h>
#import "HttpClient.h"

@interface WebServiceClient : NSObject <HttpClientEventHandler>
{

}

@property (copy) void (^success)(WebServiceClient* api, NSMutableData* data);
@property (copy) void (^fail)(WebServiceClient* api, NSError* error);

@property (nonatomic, strong) HttpClient* client;
@property (nonatomic, strong) NSString* URL;
@property (nonatomic, strong) NSDictionary* params;


+(WebServiceClient*) getWithURL:(NSString*) url
              success:(void (^)(WebServiceClient* api, NSMutableData* data))success
                 fail:(void (^)(WebServiceClient* api, NSError* error))fail;

+(WebServiceClient*) postWithURL:(NSString*) url
                params:(NSDictionary*) params
               success:(void (^)(WebServiceClient* api, NSMutableData* data)) success
                  fail:(void (^)(WebServiceClient* api, NSError* error)) fail;

- (void)requestSucceeded:(NSString *)data;
- (void)requestFailed:(NSError*)error;


#pragma mark - FOR CHEFDE API

+ (NSDictionary*) jsonFrom:(NSString*)data;
+ (BOOL) isOK:(NSDictionary*) result;
+ (NSString*) resultCode:(NSDictionary*) result;
+ (id) content:(NSDictionary*) result;

@end
