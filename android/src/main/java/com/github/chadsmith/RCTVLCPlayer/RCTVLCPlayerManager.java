package com.github.chadsmith.RCTVLCPlayer;

import com.github.chadsmith.RCTVLCPlayer.RCTVLCPlayer.Events;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.common.MapBuilder;
import com.facebook.react.uimanager.annotations.ReactProp;
import com.facebook.react.uimanager.SimpleViewManager;
import com.facebook.react.uimanager.ThemedReactContext;

import javax.annotation.Nullable;
import java.util.Map;

public class RCTVLCPlayerManager extends SimpleViewManager<RCTVLCPlayer> {

    public static final String REACT_CLASS = "RCTVLCPlayer";

    public static final String PROP_SRC = "src";
    public static final String PROP_SRC_OPTIONS = "options";
    public static final String PROP_SRC_URI = "uri";
    public static final String PROP_MUTED = "muted";
    public static final String PROP_PAUSED = "paused";
    public static final String PROP_VOLUME = "volume";

    @Override
    public String getName() {
        return REACT_CLASS;
    }

    @Override
    protected RCTVLCPlayer createViewInstance(ThemedReactContext themedReactContext) {
        return new RCTVLCPlayer(themedReactContext);
    }

    @Override
    @Nullable
    public Map getExportedCustomDirectEventTypeConstants() {
        MapBuilder.Builder builder = MapBuilder.builder();
        for (Events event : Events.values()) {
            builder.put(event.toString(), MapBuilder.of("registrationName", event.toString()));
        }
        return builder.build();
    }

    @ReactProp(name = PROP_SRC)
    public void setSrc(final RCTVLCPlayer videoView, @Nullable ReadableMap src) {
        videoView.setSrc(src.getString(PROP_SRC_URI), src.getArray(PROP_SRC_OPTIONS).toArrayList());
    }

    @ReactProp(name = PROP_MUTED, defaultBoolean = false)
    public void setMuted(final RCTVLCPlayer videoView, final boolean muted) {
        videoView.setMutedModifier(muted);
    }

    @ReactProp(name = PROP_PAUSED, defaultBoolean = false)
    public void setPaused(final RCTVLCPlayer videoView, final boolean paused) {
        videoView.setPausedModifier(paused);
    }

    @ReactProp(name = PROP_VOLUME, defaultFloat = 1.0f)
    public void setVolume(final RCTVLCPlayer videoView, final float volume) {
        videoView.setVolumeModifier(volume);
    }

}
