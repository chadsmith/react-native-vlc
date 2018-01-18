## react-native-vlc

A `<Video>` component for react-native that uses VLC. Aims for compatibility with (react-native-video)[https://github.com/react-native-community/react-native-video]

Does not support iOS

Requires react-native >= 0.19.0

### Add it to your project

Run `npm i -S react-native-vlc`

#### Android

Then install [rnpm](https://github.com/rnpm/rnpm) and run `rnpm link react-native-vlc`

Or if you have trouble using [rnpm](https://github.com/rnpm/rnpm), make the following additions to the given files manually:

**android/settings.gradle**

```
include ':react-native-vlc'
project(':react-native-vlc').projectDir = new File(rootProject.projectDir, '../node_modules/react-native-vlc/android')
```

**android/app/build.gradle**

```
dependencies {
   ...
   compile project(':react-native-vlc')
}
```

**MainActivity.java**

On top, where imports are:

```java
import com.stremio.react.ReactVLCPackage;
```

Under `.addPackage(new MainReactPackage())`:

```java
.addPackage(new ReactVLCPackage())
```

### Note: In react-native >= 0.29.0 you have to edit `MainApplication.java`

**MainApplication.java** (react-native >= 0.29.0)

On top, where imports are:

```java
import com.stremio.react.ReactVLCPackage;
```

Under `.addPackage(new MainReactPackage())`:

```java
.addPackage(new ReactVLCPackage())
```

## Usage

```javascript
// Within your render function, assuming you have a file called
// "background.mp4" in your project. You can include multiple videos
// on a single screen if you like.
<Video
  source={{uri: "background"}} // Can be a URL or a local file.
  rate={1.0}                   // 0 is paused, 1 is normal.
  volume={1.0}                 // 0 is muted, 1 is normal.
  muted={false}                // Mutes the audio entirely.
  paused={false}               // Pauses playback entirely.
  resizeMode="cover"           // Fill the whole screen at aspect ratio.
  repeat={true}                // Repeat forever.
  playInBackground={false}     // Audio continues to play when aentering background.
  playWhenInactive={false}     // [iOS] Video continues to play whcontrol or notification center are shown.
  onLoadStart={this.loadStart} // Callback when video starts to load
  onLoad={this.setDuration}    // Callback when video loads
  onProgress={this.setTime}    // Callback every ~250ms with currentTime
  onEnd={this.onEnd}           // Callback when playback finishes
  onError={this.videoError}    // Callback when video cannot be loaded
  style={styles.backgroundVideo}
/>

// Later on in your styles..
var styles = StyleSheet.create({
  backgroundVideo: {
    position: 'absolute',
    top: 0,
    left: 0,
    bottom: 0,
    right: 0,
  },
});
```
## Static Methods

`seek(seconds)`

Seeks the video to the specified time (in seconds). Access using a ref to the component

## Examples

---

**MIT Licensed**
