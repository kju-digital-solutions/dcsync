//
//  DCSyncConst.h
//  app
//
//  Created by ACE on 12/1/15.
//
//

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
    
    return json;
}


#endif /* DCSyncConst_h */
