/*

 
 */

#include <sys/types.h>
#include <sys/sysctl.h>
#include "TargetConditionals.h"

#import <Cordova/CDV.h>
#import "dcsync.h"

#import "TextResponseSerializer.h"
#import "AFURLRequestSerialization.h"
#import "HttpManager.h"
#import "DCSyncConst.h"


@implementation DCSync {
    AFHTTPRequestSerializer *requestSerializer;
    
    /*
     Accss token for calling sync operation back-end. That comes from authenticate calling...
     */
    NSString * accessToken;
}


- (void)pluginInitialize
{
    [super pluginInitialize];
    
    if (!accessToken)
    {
        [self authenticate];
    }
    
}


- (void)authenticate {
    
    NSDictionary *param = @{@"u": DCSYNC_TESTER, @"p": DCSYNC_PASSWORD, @"d": DCSYNC_HASH };
    
    [self post:[NSString stringWithFormat:@"%@%@", DCSYNC_WSE_URL, DCSYNC_WSE_AUTH]
    parameters:param
       headers:@{}
       success:^(id responseObject){
           NSError * jsonError;
           
           NSData *objectData = [responseObject dataUsingEncoding:NSUTF8StringEncoding];
           NSDictionary *jsonResponse = [NSJSONSerialization JSONObjectWithData:objectData
                                                                        options:NSJSONReadingMutableContainers
                                                                          error:&jsonError];
           
           accessToken = [jsonResponse objectForKey:@"access_token"];
       }
       failure:^(NSError *error){
           NSLog(@"%@",[error localizedDescription]);
       }
     ];
    
}


- (void)setRequestHeaders:(NSDictionary*)headers {
    [HttpManager sharedClient].requestSerializer = [AFHTTPRequestSerializer serializer];
    [requestSerializer.HTTPRequestHeaders enumerateKeysAndObjectsUsingBlock:^(id key, id obj, BOOL *stop) {
        [[HttpManager sharedClient].requestSerializer setValue:obj forHTTPHeaderField:key];
    }];
    [headers enumerateKeysAndObjectsUsingBlock:^(id key, id obj, BOOL *stop) {
        [[HttpManager sharedClient].requestSerializer setValue:obj forHTTPHeaderField:key];
    }];
}



- (void)post:(NSString *)url
  parameters:(NSDictionary *)parameters
     headers:(NSDictionary *) headers
     success:(void (^)(id responseObject))success
     failure:(void (^)(NSError *error))failure {
    
    HttpManager *manager = [HttpManager sharedClient];
    [self setRequestHeaders: headers];
    
    manager.responseSerializer = [TextResponseSerializer serializer];
    [manager POST:url parameters:parameters success:^(AFHTTPRequestOperation *operation, id responseObject) {
        //NSMutableDictionary *dictionary = [NSMutableDictionary dictionary];
        //[dictionary setObject:[NSNumber numberWithInt:operation.response.statusCode] forKey:@"status"];
        //[dictionary setObject:responseObject forKey:@"data"];
        
        
        //CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:dictionary];
        //[weakSelf.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
        
        success(responseObject);
        
    } failure:^(AFHTTPRequestOperation *operation, NSError *error) {
        NSMutableDictionary *dictionary = [NSMutableDictionary dictionary];
        [dictionary setObject:[NSNumber numberWithInt:operation.response.statusCode] forKey:@"status"];
        [dictionary setObject:[error localizedDescription] forKey:@"error"];
        
        
        //CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsDictionary:dictionary];
        //[weakSelf.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
        
        failure(error);
    }];
}



/*##################################################################################################
 desc : calls resultCallback with <date> of last sync
 
 name : getLastSync
 args :
 ##################################################################################################*/
- (void)getLastSync:(CDVInvokedUrlCommand*)command
{
    NSDictionary *param = @{@"t": @"RRn1A4cjkBvwlZL2wj4Vj9KGH9bLMiqSMeckTYcmGwxEBBXvVDP8zDkF7ON1", @"sync_timestamp": @"", @"upload_only": @"ß", @"upload_documents":@{} };
    
    [self post:[NSString stringWithFormat:@"%@%@", DCSYNC_WSE_URL, DCSYNC_WSE_SYNC]
    parameters:param
       headers:@{@"content-type": @"application/zip"}
       success:^(id responseObject){
           //accessToken = [(NSDictionary *)responseObject objectForKey:@"access_token"];
       }
       failure:^(NSError *error){
           NSLog(@"%@",[error localizedDescription]);
       }
     ];
}





/*##################################################################################################
 desc : retrieves the number of documents in a given path
 calls resultCallback with { count: <int>, unsynced: <int> }
 
 name : getDocumentCount
 args :
 ##################################################################################################*/
- (void)getDocumentCount:(CDVInvokedUrlCommand*)command
{
    
}




/*##################################################################################################
 desc : calls resultCallback with <string> of a file url to the Synced File root folder including 
        a trailing slash
 
 name : getContentRootUri
 args :
 ##################################################################################################*/
- (void)getContentRootUri:(CDVInvokedUrlCommand*)command
{
    
}





/*##################################################################################################
 desc : returns a new unique (random) Content Id (cid) in the following format:
        E621E1F8-C36C-495A-93FC-0C247A3E6E5F
 
 name : newDocumentCid
 args :
 ##################################################################################################*/
- (void)newDocumentCid:(CDVInvokedUrlCommand*)command
{
    
}




/*##################################################################################################
 desc : Saves a document to local storage,
        creates a new document or overwrites an existing one (depending on cid )
 
 name : saveDocument
 args :
 ##################################################################################################*/
- (void)saveDocument:(CDVInvokedUrlCommand*)command
{
    
}





/*##################################################################################################
 desc : Marks a document as deleted
 
 name : deleteDocument
 args :
 ##################################################################################################*/
- (void)deleteDocument:(CDVInvokedUrlCommand*)command
{
    
}





/*##################################################################################################
 desc : triggers a sync
 
 name : performSync
 args :
 ##################################################################################################*/
- (void)performSync:(CDVInvokedUrlCommand*)command
{
    
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
    
}


@end
