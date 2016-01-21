/*

 
 */

#include <sys/types.h>
#include <sys/sysctl.h>
#include "TargetConditionals.h"

#import <Cordova/CDV.h>
#import "dcsync.h"


#import "DCSyncConst.h"

#import "filepool.h"
#import "DataCollectorAPI.h"
#import "sqliteobject.h"

#import <CommonCrypto/CommonDigest.h>





//#define ___DEBUG___


#define ___RELEASE___







@implementation DCSync {
    
    /*
     Accss token for calling sync operation back-end. That comes from authenticate calling...
     */
    NSString * accessToken;
}


- (void)pluginInitialize
{
    NSMutableDictionary * option = nil;
    
    [super pluginInitialize];
    
    // Initialize udid...
    //[self.param setValue:[[[UIDevice currentDevice] identifierForVendor] UUIDString] forKey:@"duid"];
    [self.syncOption setValue:@"" forKey:@"token"];
    [self.syncOption setValue:@"" forKey:@"sync_timestamp"];
    [self.syncOption setValue:GetUUID() forKey:@"duid"];
    
    self.batchCounter = self.percentagePerBatch = 0;
    
    
    //NSString * root = NSTemporaryDirectory();
    
    NSArray *paths = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES);
    NSString *root = [paths objectAtIndex:0];
    
    
#ifdef ___DEBUG___
    
    root = @"/Volumes/Work/Hybrid/dcsync/temp";
    
    
    
#endif
    if (![[FilePool sharedPool] setOutputPath:root]) {
        //
        
    }
    
    NSString * dbPath = [NSString stringWithFormat:@"%@/dcd.db", root];
    
    [[SqliteObject sharedSQLObj] create:dbPath];
    
    if ((option = [[SqliteObject sharedSQLObj] loadSyncOption])) {
        self.syncOption = option;
    }
    else {
        self.syncOption = [NSMutableDictionary new];
        /*
         Initialize sync option....
         This will be updated with syncoption table record....
         */
        [self.syncOption setValue:DCSYNC_WSE_URL forKey:@"url"];
        [self.syncOption setValue:@1440 forKey:@"interval"];
        [self.syncOption setValue:DCSYNC_TESTER forKey:@"username"];
        [self.syncOption setValue:DCSYNC_PASSWORD forKey:@"password"];
        [self.syncOption setValue:GetUUID() forKey:@"duid"];
        [self.syncOption setValue:@{} forKey:@"params"];
        [self.syncOption setValue:@false forKey:@"insistOnBackground"];
        [self.syncOption setValue:@{} forKey:@"event_filter"];
    }
    
    /*
    NSString * token = [self.param valueForKey:@"token"];
    
    if (token == nil || [token isEqualToString:@""])
    {
        [self authenticate:nil];
    }
     */
    
}


- (void)authenticate:(CDVInvokedUrlCommand*)command; {
    NSDictionary *param = @{@"u": [self.syncOption valueForKey:@"username"],
                            @"p": sha256HashFor([self.syncOption valueForKey:@"password"]),
                            @"d": [self.syncOption valueForKey:@"duid"]
                            };
    
    NSString * strURL = [NSString stringWithFormat:@"%@%@", [self.syncOption valueForKey:@"url"], DCSYNC_WSE_AUTH];
    NSURL *URL = [NSURL URLWithString:strURL];
    
    NSError *error;
    
    NSURLSessionConfiguration *configuration = [NSURLSessionConfiguration defaultSessionConfiguration];
    NSURLSession *session = [NSURLSession sessionWithConfiguration:configuration delegate:nil delegateQueue:nil];
    NSMutableURLRequest *request = [NSMutableURLRequest requestWithURL:URL
                                                           cachePolicy:NSURLRequestUseProtocolCachePolicy
                                                       timeoutInterval:60.0];
    
    [request addValue:@"application/json" forHTTPHeaderField:@"Content-Type"];
    [request addValue:@"application/json" forHTTPHeaderField:@"Accept"];
    
    [request setHTTPMethod:@"POST"];
    NSData *postData = [NSJSONSerialization dataWithJSONObject:param options:0 error:&error];
    [request setHTTPBody:postData];
    
    NSURLSessionDataTask *task = [session dataTaskWithRequest:request
                                                    completionHandler:^(NSData * __nullable data, NSURLResponse * __nullable response, NSError * __nullable error) {
                                                        NSString * ret = [[NSString alloc] initWithData:data encoding:NSUTF8StringEncoding];
                                                        
                                                        NSError *jsonError;
                                                        NSData *objectData = [ret dataUsingEncoding:NSUTF8StringEncoding];
                                                        NSDictionary *json = [NSJSONSerialization JSONObjectWithData:objectData
                                                                                                             options:NSJSONReadingMutableContainers
                                                                                                               error:&jsonError];
                                                        
                                                        NSString * token = [json valueForKey:@"access_token"];
                                                        
                                                        
                                                        
                                                        if (token == nil || [token isEqualToString:@""]) {
                                                            
                                                            if (command) {
                                                                CDVPluginResult* result = nil;
                                                                
                                                                result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:@"Authentication failed..."];
                                                                [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
                                                            }
                                                            
                                                            return;
                                                        }
                                                            
                                                        
                                                        [self.syncOption setValue:token forKey:@"token"];
                                                        
                                                        [[SqliteObject sharedSQLObj] saveSyncOption:self.syncOption];
                                                        
                                                        if (command)
                                                            [self performSync:command];
                                                    }];
    
    // Start the task
    [task resume];
}



/*##################################################################################################
 desc : calls resultCallback with <date> of last sync
 
 name : getLastSync
 args :
 ##################################################################################################*/
- (void)getLastSync:(CDVInvokedUrlCommand*)command
{
    NSString * callbackId = command.callbackId;
    CDVPluginResult* result = nil;
    
    double timeStamp = [[SqliteObject sharedSQLObj] getLatestSyncDate];
    
    if (timeStamp)
        result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:@{@"syncDate": [NSNumber numberWithDouble:timeStamp]}];
    else
        result = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsDouble:0];
    
    [self.commandDelegate sendPluginResult:result callbackId:callbackId];
}



/*##################################################################################################
 desc : retrieves the number of documents in a given path
 calls resultCallback with { count: <int>, unsynced: <int> }
 
 name : getDocumentCount
 args :
 ##################################################################################################*/
- (void)getDocumentCount:(CDVInvokedUrlCommand*)command
{
    NSString* callbackId = command.callbackId;
    CDVPluginResult* result = nil;
    
    NSString * path = [command.arguments objectAtIndex:0];
    
    NSMutableDictionary * ret = [NSMutableDictionary dictionaryWithDictionary:@{
                                   @"count": [[SqliteObject sharedSQLObj] getDocumentCountInPath:path],
                                   @"unsynced": [[SqliteObject sharedSQLObj] getUnsyncedDocumentCountInPath:path]
                                   }];
    
    
    result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:ret];
    [self.commandDelegate sendPluginResult:result callbackId:callbackId];
}




/*##################################################################################################
 desc : calls resultCallback with <string> of a file url to the Synced File root folder including 
        a trailing slash
 
 name : getContentRootUri
 args :
 ##################################################################################################*/
- (void)getContentRootUri:(CDVInvokedUrlCommand*)command
{
    NSString* callbackId = command.callbackId;
    CDVPluginResult* result = nil;
    
    //NSString * path = [NSString stringWithFormat:@"file:////%@", [[FilePool sharedPool] rootPath]];
    
    result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:@"cdvfile://localhost/persistent/Files/"];
    [self.commandDelegate sendPluginResult:result callbackId:callbackId];
}





/*##################################################################################################
 desc : returns a new unique (random) Content Id (cid) in the following format:
        E621E1F8-C36C-495A-93FC-0C247A3E6E5F
 
 name : newDocumentCid
 args :
 ##################################################################################################*/
- (void)newDocumentCid:(CDVInvokedUrlCommand*)command
{
    NSString* callbackId = command.callbackId;
    CDVPluginResult* result = nil;
    
    result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:[self generateCID]];
    [self.commandDelegate sendPluginResult:result callbackId:callbackId];
}


-(NSString *)generateCID {
    CFUUIDRef uuidRef = CFUUIDCreate(NULL);
    CFStringRef uuidStringRef = CFUUIDCreateString(NULL, uuidRef);
    CFRelease(uuidRef);
    return (__bridge_transfer NSString *)uuidStringRef;
}




/*##################################################################################################
 desc : Saves a document to local storage,
        creates a new document or overwrites an existing one (depending on cid )
 
 name : saveDocument
 args :
 ##################################################################################################*/
- (void)saveDocument:(CDVInvokedUrlCommand*)command
{
    NSString* callbackId = command.callbackId;
    CDVPluginResult* result = nil;
    
    NSString * cid = [command.arguments objectAtIndex:0];
    
    if (cid == nil || cid == (id)[NSNull null] || [cid isEqualToString:@""]) {
        cid = [self generateCID];
    }
    
    NSMutableDictionary * document = [NSMutableDictionary dictionaryWithDictionary:@{
                                @"cid": cid,
                                @"path": [command.arguments objectAtIndex:1],
                                @"document": [command.arguments objectAtIndex:2],
                                @"files": [command.arguments objectAtIndex:3],
                                @"local": [command.arguments objectAtIndex:4]
                                }];
    
    NSNumber * timeStamp = [NSNumber numberWithDouble:[NSDate date].timeIntervalSince1970];
    
    [document setValue:timeStamp forKey:@"modified_date"];
    [document setValue:timeStamp forKey:@"creation_date"];
    
    [document setValue:[self.syncOption valueForKey:@"duid"] forKey:@"creator_duid"];
    [document setValue:[self.syncOption valueForKey:@"duid"] forKey:@"modified_duid"];
    
    if (![[document valueForKey:@"local"] boolValue]) {
        [document setValue:[NSNumber numberWithBool:TRUE] forKey:@"unsynced"];
    }
    
    int ret = [[SqliteObject sharedSQLObj] updateDCD:document];
    
    if (ret == -1)
        result = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"Executing sql failed."];
    else
        result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:document];
    
    
    result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:document];
    [self.commandDelegate sendPluginResult:result callbackId:callbackId];
}





/*##################################################################################################
 desc : Marks a document as deleted
 
 name : deleteDocument
 args :
 ##################################################################################################*/
- (void)deleteDocument:(CDVInvokedUrlCommand*)command
{
    NSString* callbackId = command.callbackId;
    CDVPluginResult* result = nil;

    NSString * cid = [command.arguments objectAtIndex:0];
    
    NSArray * documents = [[SqliteObject sharedSQLObj] getDCDFromCID:cid];
    
    if (documents.count == 0) {
        result = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"Not found DCDocument."];
        [self.commandDelegate sendPluginResult:result callbackId:callbackId];
    }
    
    NSMutableDictionary * document = [documents objectAtIndex:0];
    
    [document setValue:[NSNumber numberWithBool:TRUE] forKey:@"deleted"];
    
    // Check if the local flag is set...
    if (![[document valueForKey:@"local"] boolValue]) {
        [document setValue:[NSNumber numberWithBool:TRUE] forKey:@"unsynced"];
    }
    
    if ([[SqliteObject sharedSQLObj] updateDCD:document] == -1)
        result = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"Excuting sql failed."];
    else
        result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:document];
    
    [self.commandDelegate sendPluginResult:result callbackId:callbackId];
}



-(void)sync_progress:(int64_t) bytesWritten
        totalBytesWritten:(int64_t)totalBytesWritten
totalBytesExpectedToWrite:(int64_t)totalBytesExpectedToWrite

{
    int progress = 0;
    
    if (self.batchCounter == 0) {
        progress = ((double)totalBytesWritten / (double)totalBytesExpectedToWrite) * 50;
    }
    else {
        progress = ((double)totalBytesWritten / (double)totalBytesExpectedToWrite) * self.percentagePerBatch + (self.batchCounter - 1) * self.percentagePerBatch + 50;
    }
    
    
    
    NSString * jsString = [NSString stringWithFormat:@"cordova.plugins.DCSync.emit('sync_progress', {percent: %d});", progress];
    [self.commandDelegate evalJs:jsString];
}

-(void)sync_error:(NSString *) downloadErr {
    NSString * jsString = [NSString stringWithFormat:@"cordova.plugins.DCSync.emit('sync_failed', {error: '%@'});", downloadErr];
    [self.commandDelegate evalJs:jsString];
}

-(void)sync_completed:(NSURL *) downloadedFile {
    NSString * strRoot = [[FilePool sharedPool] extractFromFile:downloadedFile.path];
    
    if ([strRoot isEqualToString:@""]) {
        
    }
    else {
        NSString * strJSONFile = [NSString stringWithFormat:@"%@%@", strRoot, @"/documents.json"];
        
        
        NSData *data = [NSData dataWithContentsOfFile:[NSString stringWithFormat:@"%@%@", strRoot, @"/sync.json"]];
        
        if (data == nil) {
            [self sync_error:(@"Error occured during Sync operation. sync.json or zip file not found.")];
            
            return;
        }
        
        NSMutableArray *json = [NSJSONSerialization JSONObjectWithData:data options:NSJSONReadingMutableContainers error:nil];
        
        
        // Record the timestamp to syncoption...
        [self.syncOption setValue:[json valueForKey:@"sync_timestamp"] forKey:@"sync_timestamp"];
        
        
        /*
            Upload completed even the sync is not done completely.
            We can set unsynced flag to 0 and delete all files that flagged as deleted....
         
         */
        if ([json valueForKey:@"upload_error"] == nil || self.arrUnsyncedFiles != nil) {
            [[SqliteObject sharedSQLObj] makeDCDAsSynced:self.arrUnsyncedFiles];
            
            // Release all unsynced documents...
            
            self.arrUnsyncedFiles = nil;
        }
        
        int remainingBatchCnt = [[json valueForKey:@"sync_batches" ] intValue] - 1;
        
        self.percentagePerBatch = 50;
        
        if (remainingBatchCnt != 0) {
            self.percentagePerBatch = 50 / remainingBatchCnt;
        }
        
        self.batchCounter++;
        
        /*
            Save Sync option....
         */
        [[SqliteObject sharedSQLObj] saveSyncOption:self.syncOption];
        
        BOOL completed = [[json valueForKey:@"sync_completed"] boolValue];
        
        [[SqliteObject sharedSQLObj] mergeDJSONFromFile:strJSONFile completed:completed];
        
        
        if (completed) {
            
            
            NSString * jsString = [NSString stringWithFormat:@"%@", @"cordova.plugins.DCSync.emit('sync_completed');"];
            [self.commandDelegate evalJs:jsString];
                        
        }
        else {
            [self performSync:nil];
        }
        
    }
}


/*##################################################################################################
 desc : triggers a sync
 
 name : performSync
 args :
 ##################################################################################################*/
- (void)performSync:(CDVInvokedUrlCommand*)command
{
    NSString* callbackId = command.callbackId;
    
    CDVPluginResult* result = nil;
    
    // Check root path....
    if (![[FilePool sharedPool] rootPath]) {
        result = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"File pool path is not defined."];
        [self.commandDelegate sendPluginResult:result callbackId:callbackId];
        
        return;
    }
    
    NSString * token = [self.syncOption valueForKey:@"token"];
    
    if (token == nil || [token isEqualToString:@""]) {
        [self authenticate:command];
        
        //result = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"Retrieving token..."];
        //[self.commandDelegate sendPluginResult:result callbackId:callbackId];
        
        return;
    }
    
    NSString * jsString = [NSString stringWithFormat:@"%@", @"cordova.plugins.DCSync.emit('sync_progress');"];
    [self.commandDelegate evalJs:jsString];
    
    self.arrUnsyncedFiles = [[SqliteObject sharedSQLObj] getUnsyncedDocuments];
    
    NSLog(@"%@", self.arrUnsyncedFiles);
    
    NSString * stamp = [self.syncOption valueForKey:@"sync_timestamp"];
    
    if (stamp == nil || [stamp isEqualToString:@" "])
        stamp = @"";
    
    NSString *currentLanguage = [[NSLocale preferredLanguages] objectAtIndex:0];
    
    NSDictionary *param = @{@"t": token,
                            @"sync_timestamp": stamp,
                            @"upload_only": @0,
                            @"duid":[self.syncOption valueForKey:@"duid"],
                            @"locale":currentLanguage,
                            @"extra_params": [self.syncOption valueForKey:@"params"],
                            @"upload_documents":self.arrUnsyncedFiles};
    
    NSString * url = [NSString stringWithFormat:@"%@/%@", [self.syncOption valueForKey:@"url"], DCSYNC_WSE_SYNC];
                                        
    [[DataCollectorAPI sharedAPI] sync:param url:url listener:self];
    
    result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:@"Sync started."];
    [self.commandDelegate sendPluginResult:result callbackId:callbackId];
}



/*##################################################################################################
 desc :
 
 name : setSyncOptions
 args :
 ##################################################################################################*/
- (void)setSyncOptions:(CDVInvokedUrlCommand*)command
{
    NSString* callbackId = command.callbackId;
    CDVPluginResult* result = nil;
    
    NSDictionary *syncOption = [command.arguments objectAtIndex:0];
    
    [self.syncOption setValue:[syncOption valueForKey:@"url"] forKey:@"url"];
    [self.syncOption setValue:[syncOption valueForKey:@"username"] forKey:@"username"];
    [self.syncOption setValue:[syncOption valueForKey:@"password"] forKey:@"password"];
    [self.syncOption setValue:[syncOption valueForKey:@"interval"] forKey:@"interval"];
    [self.syncOption setValue:[syncOption valueForKey:@"locale"] forKey:@"locale"];
    [self.syncOption setValue:[syncOption valueForKey:@"insistOnBackground"] forKey:@"insistOnBackground"];
    
    [self.syncOption setValue:[syncOption valueForKey:@"params"] forKey:@"params"];
    [self.syncOption setValue:[syncOption valueForKey:@"event_filter"] forKey:@"event_filter"];
    
    [[SqliteObject sharedSQLObj] saveSyncOption:self.syncOption];
    
    result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:@"Sync option changed successfully."];
    [self.commandDelegate sendPluginResult:result callbackId:callbackId];
}





/*##################################################################################################
 desc : retrieves list of documents for a given path with filter options
 
 name : searchDocuments
 args :
 ##################################################################################################*/
- (void)searchDocuments:(CDVInvokedUrlCommand*)command
{
    NSString* callbackId = command.callbackId;
    
    NSDictionary *paramQuery = [command.arguments objectAtIndex:0];
    NSDictionary *paramOption = [command.arguments objectAtIndex:1];
    
    NSUInteger start = [[paramOption objectForKey:@"skipResults"] integerValue];
    NSUInteger limit = [[paramOption objectForKey:@"maxResults"] integerValue];
    
    if (limit == 0)
        limit = MAX_RESULTS_FOR_SEARCHDOCUMENT;
    
    CDVPluginResult* result = nil;
    
    NSMutableArray * arrResult = [[NSMutableArray alloc] init];
    
    NSArray * arrDocs = [[SqliteObject sharedSQLObj] searchDocument:paramQuery
                                                                    option:paramOption];
    
    /*
     arrDocs --- >
     */
    
    //NSDictionary *dictA = @{@"a":@[@"b", @"c", @{@"d":@{@"x":@"y", @"z":@"m"}}, @"e"]};
    //NSDictionary *dictB = @{@"a":@[@"b", @"c", @{@"d":@{@"x":@"y"}}]};
    
    
    
    for (NSDictionary * doc in arrDocs) {
        NSDictionary * document = [doc valueForKey:@"document"];
        
        if([self isDictonaryA:document hasContain:paramQuery]) {
            [arrResult addObject:doc];
        }
    }
    
    NSArray * arrSubResult = @[];
    
    if (start < [arrResult count]) {
        limit = MIN(limit, [arrResult count] - start);
        
        arrSubResult = [arrResult subarrayWithRange:NSMakeRange(start, limit)];
    }
    
    result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsArray:arrSubResult];
    [self.commandDelegate sendPluginResult:result callbackId:callbackId];
}

- (BOOL)isDictonaryA:(NSDictionary *)dictA
          hasContain:(NSDictionary *)dictB{
    
    if (dictA == nil || dictB == nil) return NO;
    
    BOOL res = YES;
    
    NSArray *keysB = [dictB allKeys];
    
    for (id item in keysB){
        
        NSObject *objB = [dictB objectForKey:item];
        
        if (objB == nil)
            return NO;
        else{
            NSObject *objA = [dictA objectForKey:item];
            
            if (objA == nil) {
                return NO;
            }
            
            if ([objA isKindOfClass:[NSString class]]){
                NSString *string = (NSString *) objA;
                
                if ([string isEqualToString:(NSString *) objB]){
                    res = YES;
                } else {
                    return NO;
                }
            }
            
            if ([objA isKindOfClass:[NSNumber class]]){
                NSNumber *number = (NSNumber *) objA;
                
                if([number isEqualToNumber:(NSNumber *) objB])
                    res = YES;
                else
                    return NO;
            }
            
            if ([objA isKindOfClass:[NSDictionary class]]){
                res = [self isDictonaryA:(NSDictionary *) objA hasContain:(NSDictionary *) objB];
                
                if (!res)  return NO;
            }
            
            if ([objA isKindOfClass:[NSArray class]]){
                res = [self isArrayA:(NSArray *) objA hasContain:(NSArray *) objB];
                
                if (!res)  return NO;
            }
        }
    }
    
    return res;
}

- (BOOL)isArrayA:(NSArray *)arrayA
      hasContain:(NSArray *)arrayB{
    
    if (arrayA == nil || arrayB == nil) return NO;
    
    BOOL res = NO;
    
    for (id objB in arrayB) {
        if (objB == nil) {
            return NO;
        }
        
        if ([objB isKindOfClass:[NSArray class]]) {
            for (id objA in arrayA) {
                if ([objA isKindOfClass:[NSArray class]]) {
                    res = [self isArrayA:objA hasContain:objB];
                    
                    if (!res) return NO;
                }
            }
        }
        else if ([objB isKindOfClass:[NSDictionary class]]) {
            for (id objA in arrayA) {
                if ([objA isKindOfClass:[NSDictionary class]]) {
                    res = [self isDictonaryA:objA hasContain:objB];
                    
                    if (!res) return NO;
                }
            }
        }
        else{
            if (![arrayA containsObject:objB]) {
                return NO;
            }
        }
        
    }
    
    return res;
}

@end
