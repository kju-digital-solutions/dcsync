//
//  DCSyncConst.h
//  app
//
//  Created by ACE on 12/1/15.
//
//

#import <CommonCrypto/CommonDigest.h>

#ifndef DCSyncConst_h
#define DCSyncConst_h

#define CID_LENGTH          32


#define DCSYNC_WSE_URL      @"http://ch-co2tieferlegen.preview.kju.at/dc"



/*####################################################################################################
 Backend entries......
####################################################################################################*/

#define DCSYNC_WSE_AUTH      @"/accesstoken"
#define DCSYNC_WSE_SYNC      @"/sync"




#define DCSYNC_TESTER       @"anonymous"
#define DCSYNC_PASSWORD     @"8FN23!3BNCLFA4$GNHIAKDFFNA2abx0938//"
#define DCSYNC_HASH         @"478f8f3c-e83f-4194-8b6c-c4eaa07269ef"


#define MAX_RESULTS_FOR_SEARCHDOCUMENT      100


static NSString * jsonToString(id json) {
    if (json == nil)
        return @"{}";
    
    NSError *writeError = nil;
    NSData *jsonData = [NSJSONSerialization dataWithJSONObject:json options:NSJSONWritingPrettyPrinted error:&writeError];
    return [[NSString alloc] initWithData:jsonData encoding:NSUTF8StringEncoding];
}

static NSMutableDictionary * stringToJson(NSString * strVal) {
    if (strVal == nil || [strVal isEqualToString:@""]) {
        return [@{} mutableCopy];
    }
    
    NSError *jsonError;
    NSData *objectData = [strVal dataUsingEncoding:NSUTF8StringEncoding];
    NSMutableDictionary *json = [NSJSONSerialization JSONObjectWithData:objectData
                                                         options:NSJSONReadingMutableContainers
                                                           error:&jsonError];
    if (json == nil)
        return [@{} mutableCopy];
    
    return json;
}

static NSString *GetUUID()
{
    CFUUIDRef theUUID = CFUUIDCreate(NULL);
    CFStringRef string = CFUUIDCreateString(NULL, theUUID);
    CFRelease(theUUID);
    return (__bridge NSString *)string;
}


static NSString * sha256HashFor(NSString * input)
{
    const char* str = [input UTF8String];
    
    unsigned char result[CC_SHA256_DIGEST_LENGTH];
    CC_SHA256(str, strlen(str), result);
    
    NSData *pwHashData = [[NSData alloc] initWithBytes:result length: sizeof result];
    //And take Base64 of that
    NSString *base64 = [pwHashData base64Encoding];
    return base64;
}


#endif /* DCSyncConst_h */
