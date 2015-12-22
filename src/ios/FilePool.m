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

-(BOOL)setOutputPath:(NSString *)path {
    BOOL result;
    
    if (![[NSFileManager defaultManager] fileExistsAtPath:path isDirectory:&result]) {
        NSLog(@"File pool output path is not valid");
        return NO;
    }
    
    
    _rootPath = path;
    
    return YES;
}



- (void)extractFromData:(NSData *)data {
    NSString * strFile = [NSString stringWithFormat:@"%@/temp.zip", self.rootPath];
    
    [data writeToFile:strFile atomically:true];
    
    [self extractFromFile:strFile];
}

- (NSString *)extractFromFile:(NSString *)strFile {
    if (!self.rootPath) {
        return @"";
    }
    
    BOOL a = [[NSFileManager defaultManager] fileExistsAtPath:self.rootPath];
    
    ZipArchive *zipArchive = [[ZipArchive alloc] init];
    [zipArchive UnzipOpenFile:strFile];
    
    [zipArchive UnzipFileTo:self.rootPath overWrite:YES];
    [zipArchive UnzipCloseFile];
    
    /*
        Return document.json path.
    */
    return self.rootPath;
}

@end