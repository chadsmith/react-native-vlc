package com.github.chadsmith.RCTVLCPlayer;

import android.content.res.Configuration;
import android.net.Uri;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.events.RCTEventEmitter;

import org.videolan.libvlc.IVLCVout;
import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;

import java.util.ArrayList;
import java.util.concurrent.Callable;

import bolts.Task;

public class RCTVLCPlayer extends SurfaceView implements IVLCVout.OnNewVideoLayoutListener, IVLCVout.Callback, MediaPlayer.EventListener, LifecycleEventListener, SurfaceHolder.Callback {

    public enum Events {
        EVENT_LOAD_START("onVideoLoadStart"),
        EVENT_LOAD("onVideoLoad"),
        EVENT_STALLED("onVideoBuffer"),
        EVENT_ERROR("onVideoError"),
        EVENT_PROGRESS("onVideoProgress"),
        EVENT_PAUSE("onVideoPause"),
        EVENT_STOP("onVideoStop"),
        EVENT_END("onVideoEnd");
        private final String mName;

        Events(final String name) {
            mName = name;
        }

        @Override
        public String toString() {
            return mName;
        }
    }

    private static final double MIN_PROGRESS_INTERVAL = 0.1;

    public static final String EVENT_PROP_DURATION = "duration";
    public static final String EVENT_PROP_CURRENT_TIME = "currentTime";

    public static final String EVENT_PROP_ERROR = "error";
    public static final String EVENT_PROP_WHAT = "what";
    public static final String EVENT_PROP_EXTRA = "extra";

    public static final String EVENT_PROP_BUFFERING_PROG = "progress";

    private ThemedReactContext mThemedReactContext;
    private RCTEventEmitter mEventEmitter;

    private String mSrcUriString = null;
    private ArrayList<Object> mSrcOptions = null;
    private boolean mPaused = false;
    private boolean mMuted = false;
    private float mVolume = 1.0f;
    private boolean mLoaded = false;
    private boolean mStalled = false;
    private double mPrevProgress = 0.0;

    private SurfaceHolder mSurfaceHolder;
    private LibVLC libvlc;
    private MediaPlayer mMediaPlayer = null;
    private int mVideoWidth;
    private int mVideoHeight;
    private int rootViewWidth;
    private int rootViewHeight;
    private int orientation;

    public RCTVLCPlayer(ThemedReactContext themedReactContext) {
        super(themedReactContext);

        mThemedReactContext = themedReactContext;
        mEventEmitter = themedReactContext.getJSModule(RCTEventEmitter.class);
        themedReactContext.addLifecycleEventListener(this);
        orientation = mThemedReactContext.getResources().getConfiguration().orientation;
    }

    private void createPlayer(ArrayList<Object> options) {
        if (mMediaPlayer != null) return;

        try {
            ArrayList<String> combinedOptions = new ArrayList<String>();
            combinedOptions.add("-vvvv");
            if(options != null) {
                for(Object option : options)
                    combinedOptions.add(option.toString());
            }
            libvlc = new LibVLC(mThemedReactContext, combinedOptions);

            mMediaPlayer = new MediaPlayer(libvlc);
            mMediaPlayer.setEventListener(this);

            final IVLCVout vout = mMediaPlayer.getVLCVout();
            vout.setVideoView(this);
            vout.addCallback(this);
            vout.attachViews();
        } catch (Exception e) {
            // TODO onError
        }
    }

    private void releasePlayer() {
        if (libvlc == null) return;
        Task.callInBackground(new Callable<Void>() {

            @Override
            public Void call() throws Exception {
                getHolder().setKeepScreenOn(false);
                mMediaPlayer.setEventListener(null);
                mMediaPlayer.stop();
                final IVLCVout vout = mMediaPlayer.getVLCVout();
                vout.removeCallback(RCTVLCPlayer.this);
                vout.detachViews();
                libvlc.release();
                mMediaPlayer = null;
                libvlc = null;
                return null;
            }

        });
    }

    @Override
    public void onNewVideoLayout(IVLCVout vout, int width, int height, int visibleWidth, int visibleHeight, int sarNum, int sarDen) {
        if (width * height == 0) return;

        mVideoWidth = width;
        mVideoHeight = height;

        rootViewWidth = getRootView().getWidth();
        rootViewHeight = getRootView().getHeight();
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (orientation != newConfig.orientation) {
            int swap = rootViewHeight;
            rootViewHeight = rootViewWidth;
            rootViewWidth = swap;
            orientation = newConfig.orientation;
        }
    }

    @Override
    public void onSurfacesCreated(IVLCVout vout) {
        mVideoWidth = getWidth();
        mVideoHeight = getHeight();

        vout.setWindowSize(mVideoWidth, mVideoHeight);
    }

    @Override
    public void onSurfacesDestroyed(IVLCVout vout) {

    }

    public void setSrc(final String uriString, final ArrayList<Object> options) {
        if(mMediaPlayer != null) {
            mMediaPlayer.stop();
            if(options != mSrcOptions)
                releasePlayer();
        }

        if(uriString == null)
            return;

        mSrcUriString = uriString;
        mSrcOptions = options;

        if(mSurfaceHolder == null)
            return;

        createPlayer(options);

        Media m = new Media(libvlc, Uri.parse(uriString));
        m.setHWDecoderEnabled(true, false);
        mMediaPlayer.setMedia(m);

        mLoaded = false;
        mStalled = false;

        WritableMap src = Arguments.createMap();
        src.putString(RCTVLCPlayerManager.PROP_SRC_URI, uriString);
        WritableMap event = Arguments.createMap();
        event.putMap(RCTVLCPlayerManager.PROP_SRC, src);
        mEventEmitter.receiveEvent(getId(), Events.EVENT_LOAD_START.toString(), event);

        applyModifiers();
    }

    public void setPausedModifier(final boolean paused) {
        mPaused = paused;
        if (mMediaPlayer == null) return;
        if (mPaused) {
            mMediaPlayer.pause();
        } else {
            mMediaPlayer.play();
        }
    }

    public void setMutedModifier(final boolean muted) {
        mMuted = muted;
        if (mMediaPlayer == null) return;
        if (mMuted) {
            mMediaPlayer.setVolume(0);
        } else {
            mMediaPlayer.setVolume((int) mVolume * 200);
        }
    }

    public void setVolumeModifier(final float volume) {
        mVolume = volume;
        if (mMediaPlayer == null) return;
        mMediaPlayer.setVolume((int) volume * 200);
    }

    public void applyModifiers() {
        setPausedModifier(mPaused);
        setMutedModifier(mMuted);
        setVolumeModifier(mVolume);
    }

    @Override
    public void onEvent(MediaPlayer.Event ev) {
        WritableMap event = Arguments.createMap();

        switch(ev.type) {
            case MediaPlayer.Event.EndReached:
                mEventEmitter.receiveEvent(getId(), Events.EVENT_END.toString(), event);
                break;
            case MediaPlayer.Event.EncounteredError:
                WritableMap error = Arguments.createMap();
                error.putString(EVENT_PROP_WHAT, "MediaPlayer.Event.EncounteredError");
                // TODO: more info
                event.putMap(EVENT_PROP_ERROR, error);
                mEventEmitter.receiveEvent(getId(), Events.EVENT_ERROR.toString(), event);
                releasePlayer();
                break;
            case MediaPlayer.Event.Buffering:
                float buffering = ev.getBuffering();
                event.putDouble(EVENT_PROP_BUFFERING_PROG, buffering);
                if (buffering < 30 && !mStalled) {
                    mStalled = true;
                    mEventEmitter.receiveEvent(getId(), Events.EVENT_STALLED.toString(), event);
                }
                break;
            case MediaPlayer.Event.Playing:
                if (!mLoaded) {
                    event.putDouble(EVENT_PROP_DURATION, mMediaPlayer.getLength() / 1000.0);
                    event.putDouble(EVENT_PROP_CURRENT_TIME, mMediaPlayer.getTime() / 1000.0);
                    mEventEmitter.receiveEvent(getId(), Events.EVENT_LOAD.toString(), event);
                    mLoaded = true;
                }
                this.getHolder().setKeepScreenOn(true);
                break;
            case MediaPlayer.Event.Paused:
                mEventEmitter.receiveEvent(getId(), Events.EVENT_PAUSE.toString(), event);
                this.getHolder().setKeepScreenOn(false);
                break;
            case MediaPlayer.Event.Stopped:
                mEventEmitter.receiveEvent(getId(), Events.EVENT_STOP.toString(), event);
                break;
            case MediaPlayer.Event.Opening:
                break;
            case MediaPlayer.Event.TimeChanged:
                double currentProgress = mMediaPlayer.getTime() / 1000.0;
                if (Math.abs(currentProgress - mPrevProgress) >= MIN_PROGRESS_INTERVAL || currentProgress == 0) {
                    mPrevProgress = currentProgress;
                    event.putDouble(EVENT_PROP_CURRENT_TIME, currentProgress);
                    mEventEmitter.receiveEvent(getId(), Events.EVENT_PROGRESS.toString(), event);
                }
                if (mMediaPlayer.getPlayerState() == 3 && mStalled) {
                    mStalled = false;
                }
                break;
            default:
                break;
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        releasePlayer();
        super.onDetachedFromWindow();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mSurfaceHolder = getHolder();
        mSurfaceHolder.addCallback(this);
        setSrc(mSrcUriString, mSrcOptions);
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int format, int width, int height) {
        if(mMediaPlayer != null) {
            final IVLCVout vout = mMediaPlayer.getVLCVout();
            vout.setWindowSize(width, height);
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
    }

    @Override
    public void onHostPause() {
        if(mMediaPlayer != null) {
            mMediaPlayer.pause();
        }
    }

    @Override
    public void onHostResume() {
        applyModifiers();
    }

    @Override
    public void onHostDestroy() {
    }

}
