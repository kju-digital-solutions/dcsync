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


-(void)mergeDJSON:(NSMutableArray *) dJson {
    
}

-(void)mergeDJSONFromFile:(NSString *) dJsonFile {
    NSData *data = [NSData dataWithContentsOfFile:dJsonFile];
    NSArray *json = [NSJSONSerialization JSONObjectWithData:data options:kNilOptions error:nil];
    
    [self mergeDJSON:[json mutableCopy]];
}


@end




typedef DocJSONObject DJsonObj;