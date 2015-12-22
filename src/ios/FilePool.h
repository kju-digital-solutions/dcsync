//
//  FilePool.h
//  app
//
//  Created by ACE on 12/15/15.
//
//

#ifndef FilePool_h
#define FilePool_h

@interface FilePool : NSObject

+ (id)sharedPool;

-(BOOL)setOutputPath:(NSString *)path;

- (void)extractFromData:(NSData *)data;
- (NSString *)extractFromFile:(NSString *)strFile;

@property (nonatomic, retain) NSString *rootPath;

@end


#endif /* FilePool_h */