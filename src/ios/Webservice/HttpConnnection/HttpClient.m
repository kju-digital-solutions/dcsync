//
//  HttpClient.m
//  CommuSoft
//
//  Created by System Administrator on 11/2/10.
//  Copyright 2010 __MyCompanyName__. All rights reserved.
//

#import "HttpClient.h"
#import "Reachability.h"
#import "AppDelegate.h"

@implementation HttpClient

@synthesize recievedData, statusCode;
@synthesize delegate;

- (id)init {
	if (self = [super init]) {
		[self reset];
	}
	delegate = nil;
	networkChecked = NO;
	
	return self;
}

- (void)dealloc {
	[connection release];
	[recievedData release];
	[super dealloc];
}

- (void)reset {
	[recievedData release];
	recievedData = [[NSMutableData alloc] init];
	[connection release];
	connection = nil;
	statusCode = 0;	
	networkChecked = NO;
}

#pragma mark -
#pragma mark HTTP Reuest creating methods

- (NSMutableURLRequest*)makeRequest:(NSString*)url {
	NSString *encodedUrl = (NSString*)CFURLCreateStringByAddingPercentEscapes(
																			  NULL, (CFStringRef)url, NULL, NULL, kCFStringEncodingUTF8);
	NSMutableURLRequest *request = [[[NSMutableURLRequest alloc] init] autorelease];
	[request setURL:[NSURL URLWithString:encodedUrl]];
	[request setCachePolicy:NSURLRequestReloadIgnoringLocalAndRemoteCacheData];
	[request setTimeoutInterval:TIMEOUT_SEC];
	[request setHTTPShouldHandleCookies:FALSE];
	[encodedUrl release];
	return request;
}

- (void)prepareWithRequest:(NSMutableURLRequest*)request {
	// do nothing (for OAuthHttpClient)
}

#pragma mark -
#pragma mark HTTP Transaction management methods

/* Sending the Http Request for "GET" */
- (void)requestGET:(NSString*)url {
	
	//Reseting the http client
	[self reset];
	
	//Sending the http requqest
	NSMutableURLRequest *request = [self makeRequest:url];
	[self prepareWithRequest:request];
	connection = [[NSURLConnection alloc] initWithRequest:request delegate:self startImmediately:YES];
}

/* Sending the Http Request for "POST" */
- (void)requestPOST:(NSString*)url body:(NSString*)body{
	
	//Reseting the http client
	[self reset];
	
	//Checking the internet connection
	//if ([self checkNetworkStatus] == NO){
//		if (self.delegate != nil && [self.delegate respondsToSelector:@selector(requestFailed:)]){
//			[self.delegate performSelector:@selector(requestFailed:) withObject:nil];
//		}		
//		return;
//	}
	
	//Sending the http requqest
	NSMutableURLRequest *request = [self makeRequest:url];
    [request setHTTPMethod:@"POST"];
//	if (type != nil && ![type isEqualToString:@""])
//		[request setValue:type forHTTPHeaderField:@"Content-Type"];	
	if (body) {
		[request setHTTPBody:[body dataUsingEncoding:NSUTF8StringEncoding]];
	}
	[self prepareWithRequest:request];
	connection = [[NSURLConnection alloc] initWithRequest:request delegate:self];
	
}

/* Canceling the HTTP Transaction */
- (void)cancelTransaction {
	[connection cancel];
	[self reset];
}

#pragma mark -
#pragma mark NSURLConnectionDelegate methods

- (NSCachedURLResponse *)connection:(NSURLConnection *)connection willCacheResponse:(NSCachedURLResponse *)cachedResponse {
	
	return nil;
	
}

- (void)connection:(NSURLConnection *)connection didReceiveResponse:(NSURLResponse *)response {
	statusCode = (int)[(NSHTTPURLResponse*)response statusCode];
}

- (void)connection:(NSURLConnection *)connection didReceiveData:(NSData *)data {
    [recievedData appendData:data];
}

- (void)connectionDidFinishLoading:(NSURLConnection *)connection {
//	NSString *data = [[NSString alloc] initWithData:recievedData encoding:NSUTF8StringEncoding];
	if (self.delegate != nil && [self.delegate respondsToSelector:@selector(requestSucceeded:)]){
		[self.delegate performSelector:@selector(requestSucceeded:) withObject:recievedData];
	}
//	[data release];
	
	[self reset];
}

- (void)connection:(NSURLConnection *)connection didFailWithError:(NSError*) error {
	if (self.delegate != nil && [self.delegate respondsToSelector:@selector(requestFailed:)]){
		[self.delegate performSelector:@selector(requestFailed:) withObject:error];
	}	
	[self reset];
}

//check network status chages
- (BOOL)checkNetworkStatus
{
	BOOL res = NO;
	Reachability* reachability = [Reachability reachabilityWithHostName:@"www.mobilpaketler.com"];
	NetworkStatus remoteHostStatus = [reachability currentReachabilityStatus];	
	
	if(remoteHostStatus == NotReachable)
		res = NO;
	else if (remoteHostStatus == ReachableViaWWAN)
		res = YES;
	else if (remoteHostStatus == ReachableViaWiFi)
		res = YES;
	
	return res;
}

- (BOOL) checkNetworkConnection
{
    Reachability *networkReachability = [Reachability reachabilityForInternetConnection];
    NetworkStatus networkStatus = [networkReachability currentReachabilityStatus];
    if (networkStatus == NotReachable)
    {
        return NO;
    }
    else
    {
        return YES;
    }
    
    return YES;
}

@end
