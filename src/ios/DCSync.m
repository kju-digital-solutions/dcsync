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





#define ___DEBUG___


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
    
    self.param = [NSMutableDictionary new];
    
    // Initialize udid...
    [self.param setValue:[[[UIDevice currentDevice] identifierForVendor] UUIDString] forKey:@"duid"];
    [self.param setValue:@"" forKey:@"token"];
    [self.param setValue:@"" forKey:@"sync_timestamp"];
    [self.param setValue:DCSYNC_HASH forKey:@"hash"];
    
    
    NSString * root = NSTemporaryDirectory();

    
    
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
        [self.syncOption setValue:@"https://datacollector.kju.com/DC2" forKey:@"url"];
        [self.syncOption setValue:@1440 forKey:@"interval"];
        [self.syncOption setValue:DCSYNC_TESTER forKey:@"username"];
        [self.syncOption setValue:DCSYNC_PASSWORD forKey:@"password"];
        [self.syncOption setValue:@{} forKey:@"params"];
        [self.syncOption setValue:@false forKey:@"insistOnBackground"];
        [self.syncOption setValue:@{} forKey:@"event_filter"];
    }
    
    
    NSString * token = [self.param valueForKey:@"token"];
    
    if (token == nil || [token isEqualToString:@""])
    {
        [self authenticate:nil];
    }
    
}


- (void)authenticate:(CDVInvokedUrlCommand*)command; {
    
    NSDictionary *param = @{@"u": [self.syncOption valueForKey:@"username"],
                            @"p": /*[self.syncOption valueForKey:@"password"]*/ DCSYNC_PASSWORD,
                            @"d": [self.param valueForKey:@"hash"]
                            };
    
    NSString * strURL = [NSString stringWithFormat:@"%@%@", DCSYNC_WSE_URL, DCSYNC_WSE_AUTH];
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
                                                            
                                                        
                                                        [self.param setValue:token forKey:@"token"];
                                                        
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
        result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDouble:timeStamp];
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
    
    NSString * path = [[FilePool sharedPool] rootPath];
    
    result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:path];
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
    
    if (cid == nil || [cid isEqualToString:@""]) {
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
    
    [document setValue:@0 forKey:@"creator_duid"];
    [document setValue:@0 forKey:@"modified_duid"];
    
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



-(void)sync_progress:(int) progress {
    NSString * jsString = [NSString stringWithFormat:@"cordova.plugins.DCSync.emit('sync_progress', {percent: %d});", progress];
    [self.commandDelegate evalJs:jsString];
}


-(void)sync_completed:(NSURL *) downloadedFile {
    NSString * strRoot = [[FilePool sharedPool] extractFromFile:downloadedFile.path];
    
    if ([strRoot isEqualToString:@""]) {
        
    }
    else {
        
        NSString * strJSONFile = [NSString stringWithFormat:@"%@%@", strRoot, @"/documents.json"];
        
        
        NSData *data = [NSData dataWithContentsOfFile:[NSString stringWithFormat:@"%@%@", strRoot, @"/sync.json"]];
        NSMutableArray *json = [NSJSONSerialization JSONObjectWithData:data options:NSJSONReadingMutableContainers error:nil];
        
        [self.param setValue:[json valueForKey:@"sync_timestamp"] forKey:@"sync_timestamp"];
        BOOL completed = [[json valueForKey:@"sync_completed"] boolValue];
        
        [[SqliteObject sharedSQLObj] mergeDJSONFromFile:strJSONFile completed:completed];
        
        
        if (completed) {
            NSString * jsString = [NSString stringWithFormat:@"%@", @"cordova.plugins.DCSync.emit('sync_completed');"];
            [self.commandDelegate evalJs:jsString];
            
            [self.param setValue:@"" forKey:@"sync_timestamp"];
            
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
    
    NSString * token = [self.param valueForKey:@"token"];
    
    if (token == nil || [token isEqualToString:@""]) {
        [self authenticate:command];
        
        result = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"Retrieving token..."];
        [self.commandDelegate sendPluginResult:result callbackId:callbackId];
        
        return;
    }
    
    NSString * jsString = [NSString stringWithFormat:@"%@", @"cordova.plugins.DCSync.emit('sync_progress');"];
    [self.commandDelegate evalJs:jsString];
    
    NSDictionary *param = @{@"t": token,
                            @"sync_timestamp": [self.param valueForKey:@"sync_timestamp"],
                            @"upload_only": @"ß",
                            @"duid":[self.param valueForKey:@"duid"],
                            @"locale":@"",
                            @"extra_params":@"",
                            @"upload_documents":@[]};
                                        
    [[DataCollectorAPI sharedAPI] sync:param listener:self];
    
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
    NSDictionary *syncOption = [command.arguments objectAtIndex:0];
    
    [self.syncOption setValue:[syncOption valueForKey:@"url"] forKey:@"url"];
    [self.syncOption setValue:[syncOption valueForKey:@"username"] forKey:@"username"];
    [self.syncOption setValue:[syncOption valueForKey:@"password"] forKey:@"password"];
    [self.syncOption setValue:[syncOption valueForKey:@"interval"] forKey:@"interval"];
    [self.syncOption setValue:[syncOption valueForKey:@"locale"] forKey:@"locale"];
    [self.syncOption setValue:[syncOption valueForKey:@"insistOnBackground"] forKey:@"insistOnBackground"];
    [self.syncOption setValue:[syncOption valueForKey:@"param"] forKey:@"param"];
    [self.syncOption setValue:[syncOption valueForKey:@"event_filter"] forKey:@"event_filter"];
    
    [[SqliteObject sharedSQLObj] saveSyncOption:self.syncOption];
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
    
    CDVPluginResult* result = nil;
    
    NSArray * arrDocs = [[SqliteObject sharedSQLObj] searchDocument:paramQuery
                                                                    option:paramOption];
    
    result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsArray:arrDocs];
    [self.commandDelegate sendPluginResult:result callbackId:callbackId];
}

@end
