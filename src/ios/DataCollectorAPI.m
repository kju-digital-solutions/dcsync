//
//  DataCollectorAPI.m
//  app
//
//  Created by ACE on 12/18/15.
//
//

#import <Foundation/Foundation.h>
#import "DataCollectorAPI.h"
#import "DCSyncConst.h"
#import "FilePool.h"


@implementation DataCollectorAPI : NSObject

DataCollectorAPI * api;

+ (id)sharedAPI {
    if (api == nil) {
        api = [[DataCollectorAPI alloc] init];
    }
    return api;
}

-(void)sync:(NSDictionary * )param
        url:(NSString *) url
   listener:(DCSync *)listener {
    
    NSString *boundary = @"---011000010111000001101001";

    NSString * strURL = url;//[NSString stringWithFormat:@"%@%@", DCSYNC_WSE_URL, DCSYNC_WSE_SYNC];
    NSURL *URL = [NSURL URLWithString:strURL];
    
    NSError *error;
    
    NSURLSessionConfiguration *configuration = [NSURLSessionConfiguration defaultSessionConfiguration];
    NSURLSession *session = [NSURLSession sessionWithConfiguration:configuration delegate:self delegateQueue:nil];
    NSMutableURLRequest *request = [NSMutableURLRequest requestWithURL:URL
                                                           cachePolicy:NSURLRequestUseProtocolCachePolicy
                                                       timeoutInterval:60.0];
    
    [request setHTTPMethod:@"POST"];
    NSMutableString * body = [NSMutableString string];
    NSMutableData * postData = [[NSMutableData alloc] init];
    
    NSDictionary *headers = @{ @"content-type": @"multipart/form-data; boundary=---011000010111000001101001",
                               @"cache-control": @"no-cache"};

    
    
    NSArray * keys = [param allKeys];
    
    NSArray * files = [param valueForKey:@"upload_documents"];
    
    
    for (NSString * key in keys) {
        [postData appendData:[[NSString stringWithFormat:@"\r\n--%@\r\n", boundary] dataUsingEncoding:NSUTF8StringEncoding]];

        id value = [param valueForKey:key];
        
        if ([key isEqualToString:@"upload_documents"]) {
            NSError *writeError = nil;
            NSData *jsonData = [NSJSONSerialization dataWithJSONObject:value options:NSJSONWritingPrettyPrinted error:&writeError];
            value = [[NSString alloc] initWithData:jsonData encoding:NSUTF8StringEncoding];
        }
        
        [postData appendData:[[NSString stringWithFormat:@"Content-Disposition:form-data; name=\"%@\"\r\n\r\n", key] dataUsingEncoding:NSUTF8StringEncoding]];
        [postData appendData:[[NSString stringWithFormat:@"%@", value] dataUsingEncoding:NSUTF8StringEncoding]];
    }
    
    int counter = 0;
    
    for (NSString * file in files) {
        [postData appendData:[[NSString stringWithFormat:@"\r\n--%@\r\n", boundary] dataUsingEncoding:NSUTF8StringEncoding]];
        NSArray * paths = [file valueForKey:@"files"];
        
        // Check if this is deleted file...
        if ([[file valueForKey:@"deleted"] boolValue])
            continue;
        
        for (NSString * path in paths) {
            NSString * relativePath = [NSString stringWithFormat:@"%@/Files/%@", [[FilePool sharedPool] rootPath], path];
            /*
             Check file exists
             */
            if (![[NSFileManager defaultManager] fileExistsAtPath:relativePath]) {
                
                // What happened?
                
                continue;
            }
            
            NSData *fileContent = [[NSFileManager defaultManager] contentsAtPath:relativePath];
            
            [postData appendData:[[NSString stringWithFormat:@"Content-Disposition:form-data; name=\"file_%d\"; filename=\"%@\"\r\n\r\n", counter++, relativePath] dataUsingEncoding:NSUTF8StringEncoding]];
            [postData appendData:[[NSString stringWithFormat:@"Content-Type: %@\r\n\r\n", @"application/octet-stream"] dataUsingEncoding:NSUTF8StringEncoding]];
            [postData appendData:[@"Content-Transfer-Encoding: binary\r\n\r\n" dataUsingEncoding:NSUTF8StringEncoding]];
            [postData appendData:[[NSString stringWithFormat:@"%@", fileContent] dataUsingEncoding:NSUTF8StringEncoding]];
            
            if (error) {
                NSLog(@"%@", error);
            }
        }
    }
    [body appendFormat:@"\r\n%@", boundary];
    
    
//    NSData *postData = [body dataUsingEncoding:NSUTF8StringEncoding];
    
    
    [request setAllHTTPHeaderFields:headers];
    [request setHTTPBody:postData];
    
    NSURLSessionDownloadTask *task = [session downloadTaskWithRequest:request];
    
    self.listener = listener;
    
    // Start the task
    [task resume];
}

-(void)URLSession:(NSURLSession *)session downloadTask:(NSURLSessionDownloadTask *)downloadTask didWriteData:(int64_t)bytesWritten totalBytesWritten:(int64_t)totalBytesWritten totalBytesExpectedToWrite:(int64_t)totalBytesExpectedToWrite {
    [self.listener sync_progress:bytesWritten
               totalBytesWritten:totalBytesWritten
       totalBytesExpectedToWrite:totalBytesExpectedToWrite];
}

-(void)URLSession:(NSURLSession *)session task:(NSURLSessionTask *)task didCompleteWithError:(NSError *)error {
    if (error)
        [self.listener sync_error:[error localizedDescription]];
}

-(void)URLSession:(NSURLSession *)session downloadTask:(NSURLSessionDownloadTask *)downloadTask didResumeAtOffset:(int64_t)fileOffset expectedTotalBytes:(int64_t)expectedTotalBytes {
    
}

-(void)URLSession:(NSURLSession *)session downloadTask:(NSURLSessionDownloadTask *)downloadTask didFinishDownloadingToURL:(NSURL *)location {
    [self.listener sync_completed:location];
}



@end