## react-native-vlc

A `<VLCPlayer>` component for react-native.

Adapted from the following libraries to support iOS and Android:

- [react-native-video](https://github.com/react-native-community/react-native-video)
- [react-native-vlc](https://github.com/Ivshti/react-native-vlc)
- [react-native-vlcplayer](https://github.com/xiongchuan86/react-native-vlcplayer)

### Add it to your project

Run `npm i -S git://github.com/chadsmith/react-native-vlc.git`

Then run `react-native link react-native-vlc`

Inside your code, import the player by adding:

```javascript
import VLCPlayer from 'react-native-vlc';
```

If necessary, make the following additions to the given files manually:

#### iOS

```bash
sudo gem install cocoapods
cd ios
pod init
open Podfile
```

Add this line

```
pod 'MobileVLCKit-unstable'
```

Then run

```bash
pod install
```

Then add `node_modules/react-native-vlc/ios/RCTVLCPlayer.xcodeproj` to your XCode project under the Libraries group.

#### Android

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

**MainApplication.java**

On top, where imports are:

```java
import com.github.chadsmith.RCTVLCPlayer;
```

Under `.addPackage(new MainReactPackage())`:

```java
.addPackage(new VLCPlayerPackage())
```

## Usage

```javascript
// Within your render function, assuming you have a file called
// "background.mp4" in your project. You can include multiple videos
// on a single screen if you like.
<VLCPlayer
  source={{uri: "background"}} // Can be a URL or a local file.
  rate={1.0}                   // 0 is paused, 1 is normal.
  volume={1.0}                 // 0 is muted, 1 is normal.
  paused={false}               // Pauses playback entirely.
  onLoadStart={this.loadStart} // Callback when video starts to load
  onLoad={this.setDuration}    // Callback when video loads
  onProgress={this.setTime}    // Callback every ~250ms with currentTime
  onEnd={this.onEnd}           // Callback when playback finishes
  onError={this.videoError}    // Callback when video cannot be loaded
  style={styles.backgroundVideo} />

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

Seeks the video to the specified time (in seconds). Access using a ref to the component.

---

**MIT Licensed**
