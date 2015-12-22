/*

 
 */

#include <sys/types.h>
#include <sys/sysctl.h>
#include "TargetConditionals.h"

#import <Cordova/CDV.h>
#import "dcsync.h"


#import "DCSyncConst.h"

#import "filepool.h"
#import "DocJSONObject.h"
#import "DataCollectorAPI.h"

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
    [super pluginInitialize];
    
    self.syncTimeStamp = @"";
    
#ifdef ___DEBUG___
    
    
    if (![[FilePool sharedPool] setOutputPath:@"/Volumes/Work/Hybrid/dcsync/temp"]) {
        //
        
    }
    
    if (![[DocJSONObject sharedDocJSONObject] setOutputPath:@"/Volumes/Work/Hybrid/dcsync/temp"]) {
        //
    }
    
    
    
    
    
    
#endif
    
    
    
    if (!accessToken)
    {
        [self authenticate];
    }
    
}


- (void)authenticate {
    NSDictionary *param = @{@"u": DCSYNC_TESTER, @"p": DCSYNC_PASSWORD, @"d": DCSYNC_HASH };
    
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
    
    NSURLSessionDownloadTask *task = [session downloadTaskWithRequest:request
                                                    completionHandler:^(NSURL *location, NSURLResponse *response, NSError *error) {
                                                        [[FilePool sharedPool] extractFromFile:location.path];
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
    NSString* callbackId = command.callbackId;
    CDVPluginResult* result = nil;
    
    double timeStamp = [[DocJSONObject sharedDocJSONObject] getLastSyncDate];
    
    
    result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDouble:timeStamp];
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
    
    NSDictionary * info = [[DocJSONObject sharedDocJSONObject] getDocumentCount:path];
    
    
    result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:info];
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
    
    CFUUIDRef uuidRef = CFUUIDCreate(NULL);
    CFStringRef uuidStringRef = CFUUIDCreateString(NULL, uuidRef);
    CFRelease(uuidRef);
    NSString * cid = (__bridge_transfer NSString *)uuidStringRef;
    
    result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:cid];
    [self.commandDelegate sendPluginResult:result callbackId:callbackId];
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
    
    NSMutableDictionary * document = [NSMutableDictionary dictionaryWithDictionary:@{
                                @"cid": [command.arguments objectAtIndex:0],
                                @"path": [command.arguments objectAtIndex:1],
                                @"document": [command.arguments objectAtIndex:2],
                                @"files": [command.arguments objectAtIndex:3],
                                @"local": [command.arguments objectAtIndex:4]
                                }];
    
    [[DocJSONObject sharedDocJSONObject] saveDocument:document];
    
    
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
    
    NSDictionary * deletedFile = [[DocJSONObject sharedDocJSONObject] deleteDocument:cid];
    
    result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:deletedFile];
    [self.commandDelegate sendPluginResult:result callbackId:callbackId];
}





/*##################################################################################################
 desc : triggers a sync
 
 name : performSync
 args :
 ##################################################################################################*/
- (void)performSync:(CDVInvokedUrlCommand*)command
{
    // Check root path....
    if (![[FilePool sharedPool] rootPath])
        return;
    
    NSDictionary *param = @{@"t": @"RRn1A4cjkBvwlZL2wj4Vj9KGH9bLMiqSMeckTYcmGwxEBBXvVDP8zDkF7ON1",
                            @"sync_timestamp": self.syncTimeStamp,
                            @"upload_only": @"ß",
                            @"duid":@"",
                            @"locale":@"",
                            @"extra_params":@"",
                            @"upload_documents":@[]};
                                        
    [[DataCollectorAPI sharedAPI] sync:param completion:^(NSString *filePath) {
                                NSString * strRoot = [[FilePool sharedPool] extractFromFile:filePath];
                                
                                if ([strRoot isEqualToString:@""]) {
                                    /*
                                     
                                     */
                                }
                                else {
                                    
                                    NSString * strJSONFile = [NSString stringWithFormat:@"%@%@", strRoot, @"/documents.json"];
                                    
                                    
                                    NSData *data = [NSData dataWithContentsOfFile:[NSString stringWithFormat:@"%@%@", strRoot, @"/sync.json"]];
                                    NSMutableArray *json = [NSJSONSerialization JSONObjectWithData:data options:NSJSONReadingMutableContainers error:nil];
                                    
                                    self.syncTimeStamp = [json valueForKey:@"sync_timestamp"];
                                    BOOL completed = [[json valueForKey:@"sync_completed"] boolValue];
                                    
                                    [[DocJSONObject sharedDocJSONObject] mergeDJSONFromFile:strJSONFile completed:completed];
                                    
                                    
                                    if (completed) {
                                        /*
                                         sync_completed....
                                         */
                                        
                                        
                                    }
                                    else {
                                        /*
                                         Continue to sync...
                                         We should trigger sync event here....
                                         sync_progress
                                         */
                                        
                                        
                                        [self performSync:nil];
                                    }
                                    
                                }
                            }];
}



/*##################################################################################################
 desc :
 
 name : setSyncOptions
 args :
 ##################################################################################################*/
- (void)setSyncOptions:(CDVInvokedUrlCommand*)command
{
    
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
    
    NSMutableArray * arrDocs = [[DocJSONObject sharedDocJSONObject] searchDocument:paramQuery
                                                                             option:paramOption];
    
    result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsArray:arrDocs];
    [self.commandDelegate sendPluginResult:result callbackId:callbackId];
}

@end
