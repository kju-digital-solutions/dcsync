//
//  DataCollectorAPI.m
//  app
//
//  Created by ACE on 12/18/15.
//
//

#import <Foundation/Foundation.h>
#import "DataCollectorAPI.h"


@implementation DataCollectorAPI : NSObject

DataCollectorAPI * api;

+ (id)sharedAPI {
    if (api == nil) {
        api = [[DataCollectorAPI alloc] init];
    }
    return api;
}

@end