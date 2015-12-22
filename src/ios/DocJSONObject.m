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



+ (DocJSONObject *)sharedDocJSONObject{
    
    if (dJsonObj == nil) {
        dJsonObj = [[DocJSONObject alloc] init];
    }
    return dJsonObj;
}

-(id)init {
    
    self = [super init];
    //
    self.arrDocuments = [[NSMutableArray alloc] init];
    self.arrUnsyncedDocuments = [[NSMutableArray alloc] init];
    self.syncInfo = [[NSMutableDictionary alloc] init];
    
    return self;
}

-(BOOL)setOutputPath:(NSString *) path {
    
    BOOL result;
    
    if (![[NSFileManager defaultManager] fileExistsAtPath:path isDirectory:&result]) {
        NSLog(@"DocJSON output path is not valid");
        return NO;
    }
    
    _rootPath = path;
    
    
    [self loadDJSON];
    
    return YES;
}

-(double)getLastSyncDate {
    return [[self.syncInfo valueForKey:@"lastsyncdate"] doubleValue];
}


-(NSDictionary *)getDocumentCount:(NSString *) path {
    
    if (![path isEqualToString:@""])
        path = [path lowercaseString];
    
    int totalCount = 0, unsyncedCount = 0;
    
    
    for (NSDictionary * file in self.arrDocuments) {
        NSString * filePath = [[NSURL fileURLWithPath:[file valueForKey:@"path"]].path lowercaseString];
        
        if ([path isEqualToString:@""] || ([filePath rangeOfString:path].location != NSNotFound)) {
            totalCount++;
        }
        
        if ([[file valueForKey:@"unsynced"] boolValue]) {
            unsyncedCount++;
        }
    }
    
    return @{@"totalCount": [NSNumber numberWithInt:totalCount], @"unsyncedCount": [NSNumber numberWithInt:unsyncedCount]};
}


-(NSMutableArray *)searchDocument:(NSDictionary *) query
                           option:(NSDictionary *) option {
    
    NSMutableArray * arrResult = [[NSMutableArray alloc] init];
    
    NSString * searchPath = [option valueForKey:@"path"];
    BOOL emptyPath = NO;
    
    if ([searchPath isEqualToString:@""]) {
        emptyPath = YES;
    }
    else {
        searchPath = [[NSURL fileURLWithPath:searchPath].path lowercaseString];
    }
    
    for (NSDictionary * file in self.arrDocuments) {
        NSString * filePath = [[NSURL fileURLWithPath:[file valueForKey:@"path"]].path lowercaseString];
        
        if (emptyPath || [filePath rangeOfString:searchPath].location != NSNotFound) {
            /*
             Time to filter .......
             
             
             
             */
            
            
            [arrResult addObject:file];
        }
    }
    
    return arrResult;
}

-(NSDictionary *)saveDocument:(NSMutableDictionary *) document {
    
    NSString * cid = nil;
    int index = -1;
    
    
    // Validation cid....
    if ((cid = [document valueForKey:@"cid"]) == nil) {
        return nil;
    }

    
    
    for (int i = 0; i < self.arrDocuments.count; i++) {
        NSDictionary * file = [self.arrDocuments objectAtIndex:i];
        if ([[file valueForKey:@"cid"] isEqualToString:cid]) {
            // overwrite document.
            index = i;
            break;
        }
    }
    
    // Found document with same cid.. replace old one to new one...
    if (index != -1) {
        [self.arrDocuments replaceObjectAtIndex:index withObject:document];
    }
    // Append this new document.....
    else {
        [self.arrDocuments addObject:document];
    }
    
    
    /*  
        Now we have to make this document marked should be synced...
        Push this DCD...
        We will consider this while at the uploading...
     
     */
    if (![[document valueForKey:@"local"] boolValue]) {
        [document setValue:[NSNumber numberWithBool:true] forKey:@"unsynced"];
        [self.arrUnsyncedDocuments addObject:@{@"index":[NSNumber numberWithInteger:[self.arrDocuments indexOfObject:document]],
                                               @"cid":[document valueForKey:@"cid"]}];
    }
    
    // Save document jsons to localstorage...
    [self saveDJSON];
    
    return document;
}


-(NSDictionary *)deleteDocument:(NSString *) cid {
    
    for (NSDictionary * document in self.arrDocuments) {
        
        /*
            compare this to cid ...
            local flag is false??
         */
        if ([[document valueForKey:@"cid"] isEqualToString:cid] &&
            ![[document valueForKey:@"local"] boolValue]) {
            
            
            // Succeed to find document from cid..
            [document setValue:[NSNumber numberWithBool:true] forKey:@"deleted"];
            [document setValue:[NSNumber numberWithBool:true] forKey:@"unsynced"];
            
            /*
             Push this deleted DCD...
             We will consider this while uploading...
             
             */
            [self.arrUnsyncedDocuments addObject:@{@"index":[NSNumber numberWithInteger:[self.arrDocuments indexOfObject:document]],
                                                   @"cid":[document valueForKey:@"cid"]}];
            
            // Save updated json to docs.sync
            [self saveDJSON];
            
            return document;
        }
    }
    
    
    // Didn't find document from cid......
    return nil;
}


-(void)mergeDJSON:(NSMutableArray *) dJson {
    
    BOOL isNew = true;
    
    
    for (NSDictionary * docInServer in dJson) {
        isNew = true;
        NSString *cid;
        
        int i=0;
        
        for (i=0; i<self.arrDocuments.count; i++) {
            NSDictionary * docInLocal = [self.arrDocuments objectAtIndex:i];
            
            cid = [docInLocal valueForKey:@"cid"];
            
            // Replace old one.
            if ([cid isEqualToString:[docInServer valueForKey:@"cid"]]) {
                [self.arrDocuments replaceObjectAtIndex:i withObject:docInServer];
                
                isNew = false;
                break;
            }
            
            
        }
        
        // This is new one...
        if (isNew) {
            [self.arrDocuments addObject:docInServer];
        }
            //
    }
    
    //[self saveDJSON];
}

-(void)saveDJSON {
    
    NSString * path  = [self.rootPath stringByAppendingPathComponent:@"docs.sync"];
    
    NSMutableDictionary * djson = [[NSMutableDictionary alloc] init];
    
    [djson setValue:self.arrDocuments forKey:@"documents"];
    [djson setValue:self.arrUnsyncedDocuments forKey:@"unsynceddocuments"];
    [djson setValue:self.syncInfo forKey:@"syncinfo"];
    
    NSData *jsonData = [NSJSONSerialization dataWithJSONObject:djson options:NSJSONWritingPrettyPrinted error:nil];
    [jsonData writeToFile:path atomically:NO];
}

-(void)loadDJSON {
    
    NSMutableArray * arrDocuments = nil;
    NSMutableArray * arrUnsyncedDocuments = nil;
    NSMutableDictionary * syncInfo = nil;
    
    
    NSString * path  = [self.rootPath stringByAppendingPathComponent:@"docs.sync"];
    
    NSMutableDictionary * djson = [[NSMutableDictionary alloc] init];
    
    NSData *jsonData = [[NSData alloc] initWithContentsOfFile:path];
    djson = [NSJSONSerialization JSONObjectWithData:jsonData options:NSJSONReadingMutableContainers error:nil];
    
    
    if ((arrDocuments = [djson valueForKey:@"documents"]))
        self.arrDocuments = arrDocuments;
    
    if ((syncInfo = [djson valueForKey:@"syncinfo"]))
        self.syncInfo = syncInfo;
    
    if ((arrUnsyncedDocuments = [djson valueForKey:@"unsynceddocuments"]))
        self.arrUnsyncedDocuments = arrUnsyncedDocuments;
}

-(void)mergeDJSONFromFile:(NSString *) dJsonFile
                completed:(BOOL) completed
{
    
    NSData *data = [NSData dataWithContentsOfFile:dJsonFile];
    NSMutableArray *json = [NSJSONSerialization JSONObjectWithData:data options:NSJSONReadingMutableContainers error:nil];
    
    [self mergeDJSON:json];
    
    if (completed) {
        NSDate * timeStamp = [NSDate date];
        [self.syncInfo setValue:[NSNumber numberWithDouble:timeStamp.timeIntervalSince1970] forKey:@"lastsyncdate"];
        
        [self saveDJSON];
    }
}


@end




typedef DocJSONObject DJsonObj;