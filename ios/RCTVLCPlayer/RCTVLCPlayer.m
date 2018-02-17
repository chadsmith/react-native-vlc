#import <React/RCTConvert.h>
#import "RCTVLCPlayer.h"
#import <React/RCTBridgeModule.h>
#import <React/RCTEventDispatcher.h>
#import <React/UIView+React.h>
#import <MobileVLCKit/MobileVLCKit.h>

@implementation RCTVLCPlayer
{
    RCTEventDispatcher *_eventDispatcher;
    VLCMediaPlayer *_player;

    BOOL _paused;
    BOOL _muted;
    BOOL _started;
}

- (instancetype)initWithEventDispatcher:(RCTEventDispatcher *)eventDispatcher
{
    if ((self = [super init])) {
        _eventDispatcher = eventDispatcher;

        _paused = NO;
        _muted = NO;
        _started = NO;

        NSNotificationCenter *defaultCenter = [NSNotificationCenter defaultCenter];

        [defaultCenter addObserver:self
                          selector:@selector(applicationWillResignActive:)
                              name:UIApplicationWillResignActiveNotification
                            object:nil];

        [defaultCenter addObserver:self
                          selector:@selector(applicationDidEnterBackground:)
                              name:UIApplicationDidEnterBackgroundNotification
                            object:nil];

        [defaultCenter addObserver:self
                          selector:@selector(applicationWillEnterForeground:)
                              name:UIApplicationWillEnterForegroundNotification
                            object:nil];

        [defaultCenter addObserver:self
                          selector:@selector(mediaPlayerStateChanged:)
                              name:VLCMediaPlayerStateChanged
                            object:nil];

        [defaultCenter addObserver:self
                          selector:@selector(mediaPlayerTimeChanged:)
                              name:VLCMediaPlayerTimeChanged
                            object:nil];
    }

    return self;
}

- (void)applicationWillResignActive:(NSNotification *)notification
{
    if (_paused)
        return;
    if(_player) {
        [_player pause];
        [_player setRate:0.0];
    }
}

- (void)applicationWillEnterForeground:(NSNotification *)notification
{
    [self applyModifiers];
}

- (void)applyModifiers
{
    if(_player) {
        if (_muted) {
            [_player.audio setVolume:0];
            [_player.audio setMuted:YES];
        } else {
            [_player.audio setVolume:100];
            [_player.audio setMuted:NO];
        }
    }
    [self setPaused:_paused];
}

- (void)setPaused:(BOOL)paused
{
    if(_player) {
        if (paused) {
            [_player pause];
            [_player setRate:0.0];
        } else {
            [_player play];
            [_player setRate:1.0];
        }
    }
    _paused = paused;
}

- (void)setMuted:(BOOL)muted
{
    _muted = muted;
    [self applyModifiers];
}

-(void)setSrc:(NSDictionary *)source
{
    if(_player && _player.media)
        [_player pause];

    if(!_player) {
        NSArray* options = [source objectForKey:@"options"];
        _player = [[VLCMediaPlayer alloc] initWithOptions:options];

#if DEBUG
        _player.libraryInstance.debugLogging = true;
#endif

        [_player setDrawable:self];
        [_player setDelegate:self];
    }

    NSString* uri    = [source objectForKey:@"uri"];
    NSURL* url    = [NSURL URLWithString:uri];
    VLCMedia *media = [VLCMedia mediaWithURL:url];

    [_player setMedia:media];

    [self applyModifiers];
}

- (void)mediaPlayerTimeChanged:(NSNotification *)notification
{
    [self updateVideoProgress];
}

- (void)mediaPlayerStateChanged:(NSNotification *)notification
{
    VLCMediaPlayerState state = _player.state;
    switch (state) {
        case VLCMediaPlayerStateOpening:
            if(self.onVideoLoadStart)
                self.onVideoLoadStart(@{ @"target": self.reactTag });
            break;
        case VLCMediaPlayerStatePlaying:
            _paused = NO;
            if(self.onVideoLoad)
                self.onVideoLoad(@{
                                   @"target": self.reactTag,
                                   @"seekable": [NSNumber numberWithBool:[_player isSeekable]],
                                   @"duration": [NSNumber numberWithInt:[_player.media.length intValue]]
                                   });
            break;
        case VLCMediaPlayerStateBuffering:
            if(self.onVideoBuffer)
                self.onVideoBuffer(@{ @"target": self.reactTag });
            break;
        case VLCMediaPlayerStateError:
            if(self.onVideoError)
                self.onVideoError(@{ @"target": self.reactTag });
            break;
        case VLCMediaPlayerStatePaused:
            if(self.onVideoPause)
                self.onVideoPause(@{ @"target": self.reactTag });
            break;
        case VLCMediaPlayerStateStopped:
            if(self.onVideoStop)
                self.onVideoStop(@{ @"target": self.reactTag });
            break;
        case VLCMediaPlayerStateEnded:
            if(self.onVideoEnd)
                self.onVideoEnd(@{ @"target": self.reactTag });
            break;
        case VLCMediaPlayerStateESAdded:
            // NSLog(@"VLCMediaPlayerStateESAdded");
            break;
        default:
            // NSLog(@"VLCPLayerState %i",state);
            break;
    }
}

-(void)updateVideoProgress
{

    int currentTime   = [[_player time] intValue];
    int remainingTime = [[_player remainingTime] intValue];
    int duration      = [_player.media.length intValue];
    if(currentTime >= 0 && (duration == 0 || currentTime < duration) && self.onVideoProgress) {
        self.onVideoProgress(@{
                               @"target": self.reactTag,
                               @"currentTime": [NSNumber numberWithInt:currentTime],
                               @"remainingTime": [NSNumber numberWithInt:remainingTime],
                               @"duration":[NSNumber numberWithInt:duration],
                               @"position":[NSNumber numberWithFloat:_player.position]
                               });
    }
}

- (void)jumpBackward:(int)interval
{
    if(interval>=0 && interval <= [_player.media.length intValue])
        [_player jumpBackward:interval];
}

- (void)jumpForward:(int)interval
{
    if(interval>=0 && interval <= [_player.media.length intValue])
        [_player jumpForward:interval];
}

-(void)setSeek:(float)pos
{
    if([_player isSeekable]) {
        if(pos>=0 && pos <= 1) {
            [_player setPosition:pos];
        }
    }
}

/*
-(void)setSnapshotPath:(NSString*)path
{
    if(_player)
        [_player saveVideoSnapshotAt:path withWidth:0 andHeight:0];
}
*/

#pragma mark - Lifecycle
- (void) removeFromSuperview
{
    [_player stop];
    [_player setDelegate:nil];
    _eventDispatcher = nil;
    [[NSNotificationCenter defaultCenter] removeObserver:self];
    [super removeFromSuperview];
}

@end
