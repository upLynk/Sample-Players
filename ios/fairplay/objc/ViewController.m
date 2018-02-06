/*
 Copyright (C) 2015 Apple Inc. All Rights Reserved.
 See the Apple Developer Program License Agreement for this file's licensing information.
 All use of these materials is subject to the terms of the Apple Developer Program License Agreement.
 
 Abstract:
 ViewController.m: Sample player view controller. Illustrates how to implement and install an AVAssetResourceLoader delegate that will handle FairPlay Streaming key requests.
 */

#import "ViewController.h"

// MARK: ADAPT: Replace with a URL path to your Apple signed FairPlay Certificate
// If you are including a cert file in your app you will need to further adapt
// the code to load your certificate from file instead of from URL.
NSString* const PUBLIC_CERT_PATH = @"http://example.com/certs/public.der";
// MARK: ADAPT: Your media stream URL.
// Playlists are not hardcoded in a real app. Modify this to suit your app design.
// This URL will NOT work with your public certficate, but it left here to show
// an example of what a fairplay protected uplynk URL will look like.
NSString* const PLAYLIST_URL = @"https://content.uplynk.com/7b5fcaf81b204808a66b2d855802260c.m3u8?rmt=fps"; // Playlists 

NSString* const URL_SCHEME_NAME = @"skd";

@interface AssetLoaderDelegate : NSObject <AVAssetResourceLoaderDelegate>
@end

#pragma mark AVAssetResourceLoaderDelegate - your methods go here.

@implementation AssetLoaderDelegate

- (id)init
{
    self = [super init];
    
    return self;
}

/* ---------------------------------------------------------
 **
 **  getContentKeyAndLeaseExpiryfromKeyServerModuleWithRequest:
 **
 **  To use with Uplynk's KSM, we replace "skd://" with "https://" 
 **  from EXT-X-KEY and post the SPC to the KSM URL. 
 **  The KSM decrypts the SPC and gets the requested
 **  CK from the Key Server. The KSM wraps the CK inside an encrypted 
 **  Content Key Context (CKC) message, which it sends to the app.

 ** ------------------------------------------------------- */

- (NSData *)getContentKeyAndLeaseExpiryfromKeyServerModuleWithRequest:(NSData *)requestBytes skdURL:(NSURL *)skdURL leaseExpiryDuration:(NSTimeInterval *)expiryDuration error:(NSError **)errorOut
{
    NSString *requestLength = [NSString stringWithFormat:@"%lu", (unsigned long)[requestBytes length]];
    
    // Convert scheme from skd to https
    NSURLComponents *components = [NSURLComponents componentsWithURL:skdURL resolvingAgainstBaseURL:false];
    [components setScheme:@"https"];
    NSURL *keyURL = [components URL];
    
    //format post body to be {"spc":<your base64 encoded spc>}
    NSString *spc = [NSString stringWithFormat:@"{\"spc\":\"%@\"}", [requestBytes base64EncodedStringWithOptions:0]];
    NSLog(@"\n\n*** CONSTRUCTED SPC***\n>>> %@ <<<\n\n", spc);
    
    NSLog(@"\n\n*** Retrieving key from ***\n>>> %@ <<<\n\n", keyURL);
    NSMutableURLRequest *keyRequest = [NSMutableURLRequest requestWithURL:keyURL];
    
    [keyRequest setValue:@"application/json" forHTTPHeaderField:@"Content-Type"];
    [keyRequest setValue:requestLength forHTTPHeaderField:@"Content-Length"];
    [keyRequest setHTTPMethod:@"POST"];
    [keyRequest setHTTPBody:[spc dataUsingEncoding:NSUTF8StringEncoding]];
    
    NSURLResponse *response;
    NSError *errOut;
    NSData *responseData = [NSURLConnection sendSynchronousRequest:keyRequest returningResponse:&response error:&errOut];
    
    return responseData;

    NSData *decodedData = nil;
    
	*errorOut = [NSError errorWithDomain:NSPOSIXErrorDomain code:1 userInfo:nil];
	return decodedData;
}


- (NSData *)myGetAppCertificateData
{
    NSURL *certURL = [NSURL URLWithString: PUBLIC_CERT_PATH]; //temporary home for cert
    NSData *certificate = [NSData dataWithContentsOfURL: certURL];
    
    // This needs to be implemented to conform to your protocol with the backend/key security module.
    // At a high level, this function gets the application certificate from the server in DER format.

    return certificate;
}


/* ---------------------------------------------------------
 **
 **  resourceLoader:shouldWaitForLoadingOfRequestedResource:
 **
 **   When iOS asks the app to provide a CK, the app invokes
 **   the AVAssetResourceLoader delegate’s implementation of
 **   its -resourceLoader:shouldWaitForLoadingOfRequestedResource:
 **   method. This method provides the delegate with an instance
 **   of AVAssetResourceLoadingRequest, which accesses the
 **   underlying NSURLRequest for the requested resource together
 **   with support for responding to the request.
 **
 ** ------------------------------------------------------- */

- (BOOL)resourceLoader:(AVAssetResourceLoader *)resourceLoader shouldWaitForLoadingOfRequestedResource:(AVAssetResourceLoadingRequest *)loadingRequest
{
	AVAssetResourceLoadingDataRequest *dataRequest = loadingRequest.dataRequest;
	NSURL *url = loadingRequest.request.URL;
	NSError *error = nil;
    NSError *spcerror = nil;
	BOOL handled = NO;
   
    // Must be a non-standard URI scheme for AVFoundation to invoke your AVAssetResourceLoader delegate
    // for help in loading it.
	if (![[url scheme] isEqual:URL_SCHEME_NAME])
		return NO;

    NSLog( @"shouldWaitForLoadingOfURLRequest got %@", loadingRequest);

    NSString *assetStr;
	NSData *assetId;
	NSData *requestBytes;
    
    
    NSURLComponents *components = [NSURLComponents componentsWithURL:url resolvingAgainstBaseURL:false];
    NSURLQueryItem *item = components.queryItems[0];
    NSURLQueryItem *ray = components.queryItems[2];

    NSString *beamID = [item value];
    
    assetId = [NSData dataWithBytes: [beamID cStringUsingEncoding:NSUTF8StringEncoding] length:[beamID lengthOfBytesUsingEncoding:NSUTF8StringEncoding]];
    assetStr = [url host];
    
    NSData *certificate = [self myGetAppCertificateData];
    
    NSLog(@"\n\n*** Generating SPC ***\nAssetID: %@\nRay: %@\n\n", [item value], [ray value]);

    #pragma mark ADAPT: YOU MUST CALL: streamingContentKeyRequestDataForApp::options:
	// ADAPT: YOU MUST CALL : streamingContentKeyRequestDataForApp::options:
    //
    requestBytes = [loadingRequest streamingContentKeyRequestDataForApp:certificate
                                                      contentIdentifier:assetId
                                                                options:nil
                                                                  error:&error];
    
    if(requestBytes == nil && spcerror!=nil)
    {
        NSLog(@"\n\n*** ERROR: Request length for asset: %@ ray: %@ is %u ***\n\n",[item value], [ray value], requestBytes.length);
        NSLog(@"\n\n*** ERROR URL: ***\n>>> %@ <<<\n\n",url);
        
        NSError *underlyingError = [[spcerror userInfo] objectForKey:
                                    NSUnderlyingErrorKey];
        
        if ([[underlyingError domain] isEqualToString:NSOSStatusErrorDomain])
        {
            NSInteger errorCode = [underlyingError code];
            NSLog(@"\n\n*** ERROR: SPC REQUEST FAILED WITH CODE: %d ***\n\n", errorCode);
        }
        else
        {
            
            NSLog(@"\n\n*** ERROR: SPC REQUEST FAILED UNKNOWN %d ***\n\n", [error code]);
        }
    }
    else if (requestBytes == nil)
    {
        NSLog(@"\n\n*** ERROR: SPC EMPTY BUT NO ERROR!!! ***\n\n");
        
    }

    // to obtain the SPC message from iOS to send to the Key Server.


    NSData *responseData = nil;
    NSTimeInterval expiryDuration = 0.0;
    
    // Send the SPC message to the Key Server.
    responseData = [self getContentKeyAndLeaseExpiryfromKeyServerModuleWithRequest:requestBytes
                                                                            skdURL:url
                                                               leaseExpiryDuration:&expiryDuration
                                                                             error:&error];
    
    // The Key Server returns the CK inside an encrypted Content Key Context (CKC) message in response to
    // the app’s SPC message.  This CKC message, containing the CK, was constructed from the SPC by a
    // Key Security Module in the Key Server’s software.
	if (responseData != nil) {
        
        // upLynk is sending back a JSON payload with a 'ckc' member that's base64 encoded. So we need to grab
        // that and b64-decode it before passing it further in...
        NSError *jsonParseError;
        NSDictionary *response = [NSJSONSerialization JSONObjectWithData:responseData
                                                                 options:NSJSONReadingMutableContainers
                                                                   error:&jsonParseError];
        NSData *ckc = [[NSData alloc] initWithBase64EncodedString:response[@"ckc"] options:1];

        // Provide the CKC message (containing the CK) to the loading request.
		[dataRequest respondWithData:ckc];
        
        // Get the CK expiration time from the CKC. This is used to enforce the expiration of the CK.
        if (expiryDuration != 0.0) {
            
            AVAssetResourceLoadingContentInformationRequest *infoRequest = loadingRequest.contentInformationRequest;
            if (infoRequest) {

                // Set the date at which a renewal should be triggered.
                // Before you finish loading an AVAssetResourceLoadingRequest, if the resource
                // is prone to expiry you should set the value of this property to the date at
                // which a renewal should be triggered. This value should be set sufficiently
                // early enough to allow an AVAssetResourceRenewalRequest, delivered to your
                // delegate via -resourceLoader:shouldWaitForRenewalOfRequestedResource:, to
                // finish before the actual expiry time. Otherwise media playback may fail.
                infoRequest.renewalDate = [NSDate dateWithTimeIntervalSinceNow:expiryDuration];
                
                infoRequest.contentType = @"application/octet-stream";
                infoRequest.contentLength = responseData.length;
                infoRequest.byteRangeAccessSupported = NO;
            }
        }
		[loadingRequest finishLoading]; // Treat the processing of the request as complete.
	}
	else {
		[loadingRequest finishLoadingWithError:error];
	}
	
	handled = YES;	// Request has been handled regardless of whether server returned an error.
	
	return handled;
}


/* -----------------------------------------------------------------------------
 **
 ** resourceLoader: shouldWaitForRenewalOfRequestedResource:
 **
 ** Delegates receive this message when assistance is required of the application
 ** to renew a resource previously loaded by
 ** resourceLoader:shouldWaitForLoadingOfRequestedResource:. For example, this
 ** method is invoked to renew decryption keys that require renewal, as indicated
 ** in a response to a prior invocation of
 ** resourceLoader:shouldWaitForLoadingOfRequestedResource:. If the result is
 ** YES, the resource loader expects invocation, either subsequently or
 ** immediately, of either -[AVAssetResourceRenewalRequest finishLoading] or
 ** -[AVAssetResourceRenewalRequest finishLoadingWithError:]. If you intend to
 ** finish loading the resource after your handling of this message returns, you
 ** must retain the instance of AVAssetResourceRenewalRequest until after loading
 ** is finished. If the result is NO, the resource loader treats the loading of
 ** the resource as having failed. Note that if the delegate's implementation of
 ** -resourceLoader:shouldWaitForRenewalOfRequestedResource: returns YES without
 ** finishing the loading request immediately, it may be invoked again with
 ** another loading request before the prior request is finished; therefore in
 ** such cases the delegate should be prepared to manage multiple loading
 ** requests.
 **
 ** -------------------------------------------------------------------------- */

- (BOOL)resourceLoader:(AVAssetResourceLoader *)resourceLoader shouldWaitForRenewalOfRequestedResource:(AVAssetResourceRenewalRequest *)renewalRequest
{
    return [self resourceLoader:resourceLoader shouldWaitForLoadingOfRequestedResource:renewalRequest];
}

@end

/* AVAsset keys */
NSString * const PLAYABLE_KEY		= @"playable";
/* AVPlayerItem keys */
NSString * const STATUS_KEY         = @"status";

static void *AVPlayerTestPlaybackViewControllerStatusObservationContext = &AVPlayerTestPlaybackViewControllerStatusObservationContext;

/* ---------------------------------------------------------
 **
 **  globalNotificationQueue
 **
 **  Create a dispatch queue on which all delegate methods 
 **  will be invoked.
 **
 ** ------------------------------------------------------- */

static dispatch_queue_t	globalNotificationQueue( void )
{
	static dispatch_queue_t globalQueue = 0;
	static dispatch_once_t getQueueOnce = 0;
	dispatch_once(&getQueueOnce, ^{
		globalQueue = dispatch_queue_create("tester notify queue", NULL);
	});
	return globalQueue;
}


@interface ViewController ()
{
}

@property (strong) AVPlayer *player;
@property (strong) AVPlayerItem *playerItem;
@property (strong) AVPlayerLayer *playerLayer;

@property (strong) AssetLoaderDelegate  *loaderDelegate;

@end

#pragma mark ViewController
@implementation ViewController

#pragma mark Key Value Observer for player rate, currentItem, player item status properties.

/* ---------------------------------------------------------
 **
 **  observeValueForKeyPath:ofObject:change:context
 **
 **  Called when the value at the specified key path relative
 **  to the given object has changed.
 **  Start movie playback when the AVPlayerItem is ready to
 **  play.
 **  Report and error if the AVPlayerItem cannot be played.
 **
 **  NOTE: this method is invoked on the main queue.
 ** ------------------------------------------------------- */

- (void)observeValueForKeyPath:(NSString*) path
                      ofObject:(id)object
                        change:(NSDictionary*)change
                       context:(void*)context
{
    /* AVPlayerItem "status" property value observer. */
    if (context == AVPlayerTestPlaybackViewControllerStatusObservationContext)
    {
        AVPlayerItemStatus status = [change[NSKeyValueChangeNewKey] integerValue];
        switch (status)
        {
                /* Indicates that the status of the player is not yet known because
                 it has not tried to load new media resources for playback */
            case AVPlayerItemStatusUnknown:
            {
            }
                break;
                
            case AVPlayerItemStatusReadyToPlay:
            {
                /* Once the AVPlayerItem becomes ready to play, i.e.
                 [playerItem status] == AVPlayerItemStatusReadyToPlay,
                 we can start playback using the associated player
                 object. */
                [self.player play];
            }
                break;
                
            case AVPlayerItemStatusFailed:
            {
                AVPlayerItem *playerItem = (AVPlayerItem *)object;
                [self assetFailedToPrepareForPlayback:playerItem.error];
            }
                break;
        }
    }
    else
    {
        [super observeValueForKeyPath:path ofObject:object change:change context:context];
    }
}


#pragma mark Prepare to play asset.

/* --------------------------------------------------------------
 **  Called when an asset fails to prepare for playback for any of
 **  the following reasons:
 **
 **  1) values of asset keys did not load successfully,
 **  2) the asset keys did load successfully, but the asset is not
 **     playable
 **  3) the item did not become ready to play.
 ** ----------------------------------------------------------- */

-(void)assetFailedToPrepareForPlayback:(NSError *)error
{
    /* Display the error. */
    UIAlertView *alertView = [[UIAlertView alloc] initWithTitle:[error localizedDescription]
                                                        message:[error localizedFailureReason]
                                                       delegate:nil
                                              cancelButtonTitle:@"OK"
                                              otherButtonTitles:nil];
    [alertView show];
}

/* --------------------------------------------------------------
 **
 **  prepareToPlayAsset:withKeys
 **
 **  Invoked at the completion of the loading of the values for all
 **  keys on the asset that we require. Checks whether loading was 
 **  successfull and whether the asset is playable. If so, sets up
 **  an AVPlayerItem and an AVPlayer to play the asset.
 **
 ** ----------------------------------------------------------- */
- (void)prepareToPlayAsset:(AVURLAsset *)asset withKeys:(NSArray *)requestedKeys
{
    /* Make sure that the value of each key has loaded successfully. */
    for (NSString *thisKey in requestedKeys)
    {
        NSError *error = nil;
        AVKeyValueStatus keyStatus = [asset statusOfValueForKey:thisKey error:&error];
        if (keyStatus == AVKeyValueStatusFailed)
        {
            [self assetFailedToPrepareForPlayback:error];
            return;
        }
        /* If you are also implementing -[AVAsset cancelLoading], add your code here to bail out properly in the case of cancellation. */
    }
    
    /* Use the AVAsset playable property to detect whether the asset can be played. */
    if (!asset.playable)
    {
        /* Generate an error describing the failure. */
        NSString *localizedDescription = NSLocalizedString(@"Item cannot be played", @"Item cannot be played description");
        NSString *localizedFailureReason = NSLocalizedString(@"The assets tracks were loaded, but could not be made playable.", @"Item cannot be played failure reason");
        NSDictionary *errorDict = @{NSLocalizedDescriptionKey: localizedDescription,
                                   NSLocalizedFailureReasonErrorKey: localizedFailureReason};
        NSError *assetCannotBePlayedError = [NSError errorWithDomain:@"iOS FPS Client" code:0 userInfo:errorDict];
        
        /* Display the error to the user. */
        [self assetFailedToPrepareForPlayback:assetCannotBePlayedError];
        
        return;
    }
    
    /* At this point we're ready to set up for playback of the asset. */
    
    /* Stop observing our prior AVPlayerItem, if we have one. */
    if (self.playerItem)
    {
        /* Remove existing player item key value observers. */
        [self.playerItem removeObserver:self forKeyPath:STATUS_KEY];
    }
    
    /* Create a new instance of AVPlayerItem from the now successfully loaded AVAsset. */
    self.playerItem = [AVPlayerItem playerItemWithAsset:asset];
    
    /* Observe the player item "status" key to determine when it is ready to play. */
    [self.playerItem addObserver:self
                       forKeyPath:STATUS_KEY
                          options:NSKeyValueObservingOptionInitial | NSKeyValueObservingOptionNew
                          context:AVPlayerTestPlaybackViewControllerStatusObservationContext];
    
    /* Create new player, if we don't already have one. */
    if (!self.player)
    {
        /* Get a new AVPlayer initialized to play the specified player item. */
        self.player = [AVPlayer playerWithPlayerItem:self.playerItem];
        
        /* Create an AVPlayerLayer to display the visual output of the AVPlayer. */
        AVPlayerLayer        *playerLayer = [AVPlayerLayer playerLayerWithPlayer:self.player];
        playerLayer.frame = self.view.bounds;
        [self.view.layer addSublayer:playerLayer];
        
        /* When an iOS device is in AirPlay mode, FPS content will not play on an attached AppleTV 
         unless AirPlay playback is set to full screen. */
        self.player.usesExternalPlaybackWhileExternalScreenIsActive = YES;

    }
    
    /* Make our new AVPlayerItem the AVPlayer's current item. */
    if (self.player.currentItem != self.playerItem)
    {
        /* Replace the player item with a new player item. The item replacement occurs
         asynchronously; observe the currentItem property to find out when the
         replacement will/did occur
         
         If needed, configure player item here (example: adding outputs, setting text style rules,
         selecting media options) before associating it with a player
         */
        [self.player replaceCurrentItemWithPlayerItem:self.playerItem];
    }
}

#pragma mark Play the media stream.

/* ---------------------------------------------------------
 **
 **  playMediaStream
 **
 **  Create the asset to play (using the given URL).
 **  Configure the asset properties and callbacks when the asset is ready.
 **
 ** ------------------------------------------------------- */
- (void) playMediaStream
{
    // Set this URL to point to your media stream.
    NSString                    *urlStr = PLAYLIST_URL;
    NSURL                       *url = [NSURL URLWithString: urlStr];
    AVURLAsset                  *asset = [AVURLAsset URLAssetWithURL:url options:nil];
    
    //
    // Create a strong reference to the delegate.
    //
    // IMPORTANT: The resource loader does _not_ store a strong reference to the delegate object.
    //
    self.loaderDelegate = [[AssetLoaderDelegate alloc] init];
    
    // Set the receiver's delegate that will mediate resource loading and the dispatch queue on
    // which delegate methods will be invoked.
    [[asset resourceLoader] setDelegate:self.loaderDelegate queue:globalNotificationQueue() ];
    
    NSArray *requestedKeys = @[PLAYABLE_KEY];
    
    // Tells the asset to load the values of any of the specified keys that are not already loaded.
    [asset loadValuesAsynchronouslyForKeys:requestedKeys completionHandler:
     ^{
         dispatch_async( dispatch_get_main_queue(),
                        ^{
                            /* IMPORTANT: Must dispatch to main queue in order to operate on the AVPlayer and AVPlayerItem. */
                            [self prepareToPlayAsset:asset withKeys:requestedKeys];
                        });
     }];
}

#pragma mark View Controller Implementation.


/* ---------------------------------------------------------
 **
 **  viewWillAppear
 **
 **  Initialization of the app view objects
 **
 ** ------------------------------------------------------- */

-(void)viewWillAppear:(BOOL)animated
{
    [super viewWillAppear:animated];
    
    [self playMediaStream];
}

@end
