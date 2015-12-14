//
//  HttpClient.h
//  CommuSoft
//
//  Created by System Administrator on 11/2/10.
//  Copyright 2010 __MyCompanyName__. All rights reserved.
//

#import <Foundation/Foundation.h>

#define TIMEOUT_SEC		60.0

@protocol HttpClientEventHandler

@optional

- (void)requestSucceeded:(NSMutableData *)data;
- (void)requestFailed:(NSError*)error;

@end

@class Reachability;


@interface HttpClient : NSObject{
    NSURLConnection *connection;
    NSMutableData *recievedData;
	int statusCode;	
	
	id delegate;
	
	Reachability* hostReachable;
	BOOL networkChecked;
}

- (NSMutableURLRequest*) makeRequest:(NSString*)url;
- (void) prepareWithRequest:(NSMutableURLRequest*)request;

- (void) requestGET:(NSString*)url;
- (void) requestPOST:(NSString*)url body:(NSString*)body;

- (void) cancelTransaction;

- (void) reset;

- (BOOL) checkNetworkStatus;
- (BOOL) checkNetworkConnection;

@property (readonly) NSMutableData *recievedData;
@property (readonly) int statusCode;
@property (nonatomic, retain) id delegate;


@end
