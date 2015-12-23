//
//  SqliteObject.m
//  DCBootstrap
//
//  Created by ACE on 12/23/15.
//
//

#import <Foundation/Foundation.h>
#import "SqliteObject.h"
#import "sqlitemanager.h"


@implementation SqliteObject : NSObject

static SqliteObject *sqlobj = nil;



+ (SqliteObject *)sharedSQLObj{
    
    if (sqlobj == nil) {
        sqlobj = [[SqliteObject alloc] init];
    }
    return sqlobj;
}

-(void)create:(NSString * )path {
    
    NSFileManager *filemgr = [NSFileManager defaultManager];
    
    _dbpath = [path UTF8String];
    
    if ([filemgr fileExistsAtPath: path ] == NO) {
        /*
            called first time.. and
            create table if the db is not exist....

         */
        if (sqlite3_open(_dbpath, &_dcd) == SQLITE_OK) {
            //
            char *errMsg;
            
            // Create table sql...
            const char *sql_stmt =
            // DCDOCUMENTS
            "CREATE TABLE IF NOT EXISTS DCDOCUMENTS "
            "(cid TEXT PRIMARY KEY, "
            "local BOOL, "
            "modified_user TEXT, "
            "document TEXT, "
            "deleted BOOL, "            // deleted....
            "sync_nomedia BOOL, "
            "unsynced BOOL, "           // unsynced flag that is needed for upload sync...
            "files TEXT, "
            "path TEXT, "
            "creation_date BIGINT, "
            "modified_date BIGINT, "
            "server_modified BIGINT, "
            "creator_duid TEXT, "
            "creator_user TEXT, "
            "modified_duid TEXT);"
            // UNSYNCED
            "CREATE TABLE IF NOT EXISTS UNSYNCED "
            "(dcd_id INTEGER PRIMARY KEY, "
            "cid TEXT);"
            // SYNCINFO
            "CREATE TABLE IF NOT EXISTS SYNCINFO "
            "(id INTEGER PRIMARY KEY AUTOINCREMENT, "
            "latest_timestamp BIGINT, "
            "comment TEXT);";
            
            if (sqlite3_exec(_dcd, sql_stmt, NULL, NULL, &errMsg) != SQLITE_OK) {
                NSLog(@"Failed to create table");
            }
            
            
            // close db....
            sqlite3_close(_dcd);
        }
        else {
            NSLog(@"Failed to open/create database");
        }
    }
    else {
        
    }
    [[SQLiteManager singleton] setDatabasePath:path];
}

-(void)mergeDCD:(NSMutableArray *) dcdJson {
    /*
     Merge documents ...
     */
    for (NSMutableDictionary * document in dcdJson) {
        NSDictionary * dic =[document valueForKey:@"document"];
        
        if (dic.count) {
            NSData * data = [NSJSONSerialization dataWithJSONObject:dic options:(NSJSONWritingOptions)NSJSONWritingPrettyPrinted error:nil];
            NSString* strJSON  = [[NSString alloc] initWithData:data encoding:NSUTF8StringEncoding];
            
            [document setValue:strJSON forKey:@"document"];
        }
        
        
        NSLog(@"%d", [[SQLiteManager singleton] update:document into:@"DCDOCUMENTS" primaryKey:@"cid"]);
    }
}

-(NSNumber *)getDocumentCountInPath:(NSString *) path {
    NSArray * result = [[SQLiteManager singleton] find:@"cid" from:@"DCDOCUMENTS" where:[NSString stringWithFormat:@"path like '%@%@%@'", @"%", path, @"%"]];
    
    return [NSNumber numberWithInteger:result.count];
}


-(NSNumber *)getUnsyncedDocumentCountInPath:(NSString *) path {
    NSArray * result = [[SQLiteManager singleton] find:@"cid" from:@"DCDOCUMENTS" where:[NSString stringWithFormat:@"path like '%@%@%@' and unsynced like 1", @"%", path, @"%"]];
    
    return [NSNumber numberWithInteger:result.count];
}


-(NSArray *)getDCDFromCID:(NSString *) cid {
    return [[SQLiteManager singleton] find:@"*" from:@"DCDOCUMENTS" where:[NSString stringWithFormat:@"cid = '%@'", cid]];
}


-(NSArray *)searchDocument:(NSDictionary *) query
                    option:(NSDictionary *) option {
    
    NSString * searchPath = [option valueForKey:@"path"];
    NSString * condition = [NSString stringWithFormat:@"path like '%@%@%@'", @"%", searchPath, @"%"];
    
    NSArray * result = [[SQLiteManager singleton] find:@"*" from:@"DCDOCUMENTS" where:condition];
    
    return result;
}


-(int)updateDCD:(NSMutableDictionary *) document {
    return [[SQLiteManager singleton] update:document into:@"DCDOCUMENTS" primaryKey:@"cid"];
}

-(double)getLatestSyncDate {
    NSArray * result = [[SQLiteManager singleton] find:@"*" from:@"SYNCINFO" where:@" id = 1 "];
    
    if (result.count == 0)
        return 0;
    
    return [[[result objectAtIndex:0] valueForKey:@"latest_timestamp"] doubleValue];
}

-(void)addDCDAsUnsynced:(NSMutableDictionary *) unsyncedDoc {
    
    [[SQLiteManager singleton] update:unsyncedDoc into:@"UNSYNCED" primaryKey:@"dcd_id"];
}

-(void)mergeDJSON:(NSMutableArray *) dJson {
    [[SqliteObject sharedSQLObj] mergeDCD:dJson];
}

-(void)mergeDJSONFromFile:(NSString *) dJsonFile
                completed:(BOOL) completed
{
    NSData *data = [NSData dataWithContentsOfFile:dJsonFile];
    NSMutableArray *json = [NSJSONSerialization JSONObjectWithData:data options:NSJSONReadingMutableContainers error:nil];
    
    [self mergeDJSON:json];
    
    if (completed) {
        NSNumber * timeStamp = [NSNumber numberWithDouble:[NSDate date].timeIntervalSince1970];
        
        NSDictionary * timestamp = @{@"latest_timestamp": timeStamp,
                                     @"id": @1};
        
        [[SQLiteManager singleton] update:[timestamp mutableCopy] into:@"SYNCINFO" primaryKey:@"id"];
    }
}

@end
