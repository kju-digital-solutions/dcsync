//
//  WebServiceClient.m
//  ChefDe Cuisine Organiser
//
//  Created by Passion on 7/15/15.
//  Copyright (c) 2014 leethal.chef.com. All rights reserved.
//

#import "WebServiceClient.h"

@implementation WebServiceClient


+(WebServiceClient*) getWithURL:(NSString *)url success:(void (^)(WebServiceClient *, NSMutableData *))success fail:(void (^)(WebServiceClient *, NSError *))fail
{
    WebServiceClient* api = [[WebServiceClient alloc] init];
    
    api.URL = url;
    api.client = [[HttpClient alloc] init];
    api.params = nil;
    
    api.success = success;
    api.fail = fail;
    
    api.client.delegate = api;
    [api.client requestGET:api.URL];
    
    return api;
}

+(WebServiceClient*) postWithURL:(NSString *)url params:(NSDictionary *)params success:(void (^)(WebServiceClient *, NSMutableData *))success fail:(void (^)(WebServiceClient *, NSError *))fail
{
    WebServiceClient* api = [[WebServiceClient alloc] init];
    
    api.URL = url;
    api.client = [[HttpClient alloc] init];
    api.params = params;
    
    api.success = success;
    api.fail = fail;
    
    NSMutableString* body = [NSMutableString string];

    for (NSString* key in api.params)
    {
        [body appendString:[NSString stringWithFormat:@"%@=%@&", key, [api.params objectForKey:key]]];
    }
    
    api.client.delegate = api;
    [api.client requestPOST:api.URL body:body];
    
    return api;
}

- (void)requestSucceeded:(NSMutableData *)data
{
    int statusCode = self.client.statusCode;
    
    switch (statusCode)
    {
        case 200:
        case 0://ok
        {
            break;
        }
        default:
        {
            [self requestFailed:[NSError errorWithDomain:@"error" code:self.client.statusCode userInfo:nil]];
            
            return;
        }
    }
    
    if (_success != nil) _success(self, data);
}

- (void)requestFailed:(NSError*)error
{
    if (_fail != nil) _fail(self, error);
}

#pragma mark - FOR CHEFDE API

+(NSDictionary*) jsonFrom:(NSString *)data
{

    NSError *error = nil;
    NSDictionary* dict = [NSJSONSerialization JSONObjectWithData:[data dataUsingEncoding:NSUTF8StringEncoding] options:NSJSONReadingMutableContainers error:&error];
    
    if (error) return nil;
    
    return dict;
}

/*
 NSError *jsonError;
 NSData *objectData = [@"{\"2\":\"3\"}" dataUsingEncoding:NSUTF8StringEncoding];
 NSDictionary *json = [NSJSONSerialization JSONObjectWithData:objectData
 options:NSJSONReadingMutableContainers
 error:&jsonError];
 */

+(BOOL) isOK:(NSDictionary *)result
{
    NSString* resultCode = [result objectForKey:@"resultCode"];
    
    if (resultCode == nil) return NO;
    
    return [resultCode isEqualToString:@"RESULT_OK"];
}

+ (NSString*) resultCode:(NSDictionary*) result
{
    NSString* resultCode = [result objectForKey:@"resultCode"];
    
    if (resultCode == nil) return @"";
    
    return resultCode;
}

+ (id) content:(NSDictionary*) result
{
    id content = [result objectForKey:@"content"];
    
    return content;
}
@end
