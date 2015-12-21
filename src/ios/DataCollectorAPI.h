//
//  DataCollectorAPI.h
//  app
//
//  Created by ACE on 12/18/15.
//
//

#ifndef DataCollectorAPI_h
#define DataCollectorAPI_h

@interface DataCollectorAPI : NSObject

+ (id)sharedAPI;

-(void)authenticate:(NSString *) username
           password:(NSString *)password;

-(void)sync:(NSDictionary *) param
 completion:(void (^)(NSString * filePath))completion;


@property (nonatomic, retain) NSString *rootPath;

@end


#endif /* DataCollectorAPI_h */
