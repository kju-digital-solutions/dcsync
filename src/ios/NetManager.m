//
//  NetManager.m
//  DYB
//
//  Created by albert on 10/27/14.
//  Copyright (c) 2014 albert. All rights reserved.
//

#import "NetManager.h"
@interface NetManager () <NSURLConnectionDelegate>

@property (nonatomic, retain) NSMutableData *webData;

@end

@implementation NetManager
@synthesize webData;

static NetManager *obj = nil;

+(id)sharedManager{
    if (obj == nil) {
        obj = [[NetManager alloc] init];
        obj.webData = [[NSMutableData alloc] init];
    }
    return obj;
}

-(void) sendGETRequestTo:(NSString *)url
                  header:(NSDictionary*)headerDict
{
    NSURL *urlPath = [[NSURL alloc] initWithString:url];
    
    NSMutableURLRequest *request = [NSMutableURLRequest requestWithURL:urlPath];
    [request setTimeoutInterval:40];
    [request setHTTPMethod:@"GET"];
    
    if (headerDict != nil && [headerDict count] > 0) {
        for (NSString *key in [headerDict allKeys]) {
            NSString *value = [headerDict valueForKey:key];
            [request setValue:value forHTTPHeaderField:key];
        }
    }
    
    [request setValue:@"application/json" forHTTPHeaderField:@"Accept"];
    [request setValue:@"application/json" forHTTPHeaderField:@"Content-Type"];
    
    NSURLConnection *theConnection = [[NSURLConnection alloc] initWithRequest:request delegate:self];
    [theConnection start];
}
-(void) sendPOSTRequestTo:(NSString *)url
                 postData:(NSDictionary*)data{
    
    NSLog(@"Request = %@",data);
    
    NSMutableString *stringData = [[NSMutableString alloc] init];
    for (NSString *key in [data allKeys]) {
        [stringData appendFormat:@"%@=%@&", key, [data objectForKey:key]];
    }
    
    NSURL *urlPath = [[NSURL alloc] initWithString:url];
    
    NSMutableURLRequest *request = [NSMutableURLRequest requestWithURL:urlPath];
    [request setTimeoutInterval:40];
    NSData *requestData = [NSData dataWithBytes:[stringData UTF8String] length:[stringData length]];
    [request setHTTPMethod:@"POST"];
    
//    [request setValue:@"application/json" forHTTPHeaderField:@"Accept"];
//    [request setValue:@"application/json" forHTTPHeaderField:@"Content-Type"];
    
    [request setValue:[NSString stringWithFormat:@"%lu", (unsigned long)[requestData length]] forHTTPHeaderField:@"Content-Length"];
    [request setHTTPBody: requestData];
    
    NSURLConnection *theConnection = [[NSURLConnection alloc] initWithRequest:request delegate:self];
    [theConnection start];
}

#pragma mark - NSURLConnection Delegate

-(void) connection:(NSURLConnection *)connection didReceiveResponse:(NSURLResponse *)response{
    [webData setLength: 0];
}

-(void) connection:(NSURLConnection *)connection didReceiveData:(NSData *)data{
    [webData appendData:data];
}

-(void) connection:(NSURLConnection *)connection didFailWithError:(NSError *)error{
    NSLog(@"Connection Error");
    [connection cancel];
    
    [self.delegate requestDidFailWithError:error];
}

-(void) connectionDidFinishLoading:(NSURLConnection *)connection{
//    NSString  *result = [[NSMutableString alloc] initWithData:webData encoding:NSUTF8StringEncoding];
    
//    NSLog(@"Response = %@",webData);
    
    [self.delegate requestDidFinish:webData];
}

@end
