//
//  DocumentJSON.m
//  app
//
//  Created by ACE on 12/18/15.
//
//

#import <Foundation/Foundation.h>
#import "DocJSONObject.h"


@implementation DocJSONObject : NSObject

static DocJSONObject *dJsonObj = nil;



+ (id)sharedDocJSONObject{
    if (dJsonObj == nil) {
        dJsonObj = [[DocJSONObject alloc] init];
    }
    return dJsonObj;
}

-(id)init {
    self = [super init];
    //
    self.arrDocuments = [[NSMutableArray alloc] init];
    
    return self;
                                
}

-(void)setOutputPath:(NSString *) path {
    _outputPath = path;
    
    [self loadDJSON];
}


-(void)searchDocument:(NSDictionary *) query
               option:(NSDictionary *) option
             callback:(void (^)(NSArray * arrDCDocuments))callback {
    for (NSDictionary * file in self.arrDocuments) {
        NSMutableArray * arrResult = [[NSMutableArray alloc] init];
        
        NSString * filePath = [[NSURL fileURLWithPath:[file valueForKey:@"path"]].path lowercaseString];
        NSString * searchPath = [[NSURL fileURLWithPath:[option valueForKey:@"path"]].path lowercaseString];
        
        if (searchPath && [filePath rangeOfString:searchPath].location != NSNotFound) {
            [arrResult addObject:file];
        }
    }
}


-(void)mergeDJSON:(NSMutableArray *) dJson {
    
    BOOL isNew = true;
    for (NSDictionary * doc in dJson) {
        isNew = true;
        NSString *cid;
        
        for (NSDictionary * dict in self.arrDocuments) {
            
            
            cid = [dict valueForKey:@"cid"];
            
            if ([cid isEqualToString:[doc valueForKey:@"cid"]]) {
                isNew = false;
                break;
            }
            
            
        }
        
        if (isNew) {
            [self.arrDocuments addObject:doc];
        }
            //
    }
    
    [self saveDJSON];
}

-(void)saveDJSON {
    NSString * path  = [self.outputPath stringByAppendingPathComponent:@"docs.sync"];
    
    NSData *jsonData = [NSJSONSerialization dataWithJSONObject:self.arrDocuments options:NSJSONWritingPrettyPrinted error:nil];
    [jsonData writeToFile:path atomically:NO];
    
}

-(void)loadDJSON {
    NSString * path  = [self.outputPath stringByAppendingPathComponent:@"docs.sync"];
    
    NSData *jsonData = [[NSData alloc] initWithContentsOfFile:path];
    self.arrDocuments = [NSJSONSerialization JSONObjectWithData:jsonData options:NSJSONReadingMutableContainers error:nil];
    
}

-(void)mergeDJSONFromFile:(NSString *) dJsonFile {
    NSData *data = [NSData dataWithContentsOfFile:dJsonFile];
    NSMutableArray *json = [NSJSONSerialization JSONObjectWithData:data options:NSJSONReadingMutableContainers error:nil];
    
    [self mergeDJSON:json];
}


@end




typedef DocJSONObject DJsonObj;