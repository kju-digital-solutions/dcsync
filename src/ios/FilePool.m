//
//  FilePool.m
//  app
//
//  Created by ACE on 12/15/15.
//
//

#import <Foundation/Foundation.h>
#import "filepool.h"
#import "ziparchive.h"

@implementation FilePool : NSObject 



static FilePool *pool = nil;



+ (id)sharedPool{
    if (pool == nil) {
        pool = [[FilePool alloc] init];
    }
    return pool;
}



- (void)extractFromData:(NSData *)data {
    NSString * strFile = [NSString stringWithFormat:@"%@/temp.zip", self.rootPath];
    
    [data writeToFile:strFile atomically:true];
    
    [self extractFromFile:strFile];
}

- (void)extractFromFile:(NSString *)strFile {
    if (!self.rootPath) {
        return;
    }
    
    ZipArchive *zipArchive = [[ZipArchive alloc] init];
    [zipArchive UnzipOpenFile:strFile];
    
    [zipArchive UnzipFileTo:self.rootPath overWrite:YES];
    [zipArchive UnzipCloseFile];
}

@end