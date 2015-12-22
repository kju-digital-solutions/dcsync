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

+ (DocJSONObject *)sharedDocJSONObject;

-(BOOL)setOutputPath:(NSString *) path;

-(void)mergeDJSON:(NSMutableArray *) dJson;
-(void)mergeDJSONFromFile:(NSString *) dJsonFile
                completed:(BOOL) completed;

-(void)saveDJSON;

-(NSDictionary *)getDocumentCount:(NSString *) path;


-(NSMutableArray *)searchDocument:(NSDictionary *) query
                           option:(NSDictionary *) option;

-(NSDictionary *)deleteDocument:(NSString *) cid;

-(NSDictionary *)saveDocument:(NSMutableDictionary *) document;

-(double)getLastSyncDate;


@property (nonatomic, retain) NSString * rootPath;
@property (nonatomic, retain) NSMutableArray * arrDocuments;
@property (nonatomic, retain) NSMutableDictionary * syncInfo;
@property (nonatomic, retain) NSMutableArray * arrUnsyncedDocuments;

@end



#endif /* DocumentJSON_h */