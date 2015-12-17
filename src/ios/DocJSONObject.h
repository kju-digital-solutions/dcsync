//
//  DocumentJSON.h
//  app
//
//  Created by ACE on 12/18/15.
//
//

#ifndef DocumentJSON_h
#define DocumentJSON_h

@interface DocJSONObject : NSObject

+ (id)sharedDocJSONObject;

-(void)mergeDJSON:(NSMutableArray *) dJson;
-(void)mergeDJSONFromFile:(NSString *) dJsonFile;

-(void)searchDocument:(NSString *) path;


@property (nonatomic, retain) NSString *rootPath;

@end



#endif /* DocumentJSON_h */