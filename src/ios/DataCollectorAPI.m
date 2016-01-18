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
    NSString *boundary = @"---------------------------14737809831466499882746641449";
    NSString *contentType = [NSString stringWithFormat:@"multipart/form-data; boundary=%@",boundary];
    

    NSString * strURL = url;//[NSString stringWithFormat:@"%@%@", DCSYNC_WSE_URL, DCSYNC_WSE_SYNC];
    NSURL *URL = [NSURL URLWithString:strURL];
    
    NSError *error;
    
    NSURLSessionConfiguration *configuration = [NSURLSessionConfiguration defaultSessionConfiguration];
    NSURLSession *session = [NSURLSession sessionWithConfiguration:configuration delegate:self delegateQueue:nil];
    NSMutableURLRequest *request = [NSMutableURLRequest requestWithURL:URL
                                                           cachePolicy:NSURLRequestUseProtocolCachePolicy
                                                       timeoutInterval:60.0];
    
    
    

    
    
    [request addValue:@"application/json" forHTTPHeaderField:@"Content-Type"];
    [request addValue:@"application/json" forHTTPHeaderField:@"Accept"];
    
    [request setHTTPMethod:@"POST"];
    NSMutableData *postData = [[NSJSONSerialization dataWithJSONObject:param options:0 error:&error] mutableCopy];
    
    NSArray * files = [param valueForKey:@"upload_documents"];
    
    
    for (NSString * file in files) {
        NSArray * paths = [file valueForKey:@"files"];
        
        for (NSString * path in paths) {
            NSString * relativePath = [NSString stringWithFormat:@"%@/%@", [[FilePool sharedPool] rootPath], path];
            /*
             Check file exists
             */
            if (![[NSFileManager defaultManager] fileExistsAtPath:relativePath]) {
                
                // What happened?
                
                
                
                continue;
            }
            
            NSData *fileContent = [[NSFileManager defaultManager] contentsAtPath:relativePath];
            
            [postData appendData:[[NSString stringWithFormat:@"\r\n--%@\r\n", boundary] dataUsingEncoding:NSUTF8StringEncoding]];
            [postData appendData:[[NSString stringWithFormat:@"Content-Disposition: form-data; name=\"fileupload\"; filename=\"%@\"\r\n", file]dataUsingEncoding:NSUTF8StringEncoding]];
            [postData appendData:[@"Content-Type: application/octet-stream\r\n\r\n" dataUsingEncoding:NSUTF8StringEncoding]];
            [postData appendData:[NSData dataWithData:fileContent]];
        }
    }
    
    
    
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