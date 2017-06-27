# React Native Spotify SDK

## this only works in Android, coming soon in iOS


## Get started
`npm i -S react-native-spotify-sdk`

```
import Spotify from 'react-native-spotify-sdk';
```

and run `react-native link react-native-spotify-sdk`
# Build

Edit `android/build.grandle` and add `flatDir`
```
...
allprojects {
    repositories {
        mavenLocal()
        jcenter()
        maven {
            // All of React Native (JS, Obj-C sources, Android binaries) is installed from npm
            url "$rootDir/../node_modules/react-native/android"
        }
        flatDir {
            dirs project(':react-native-spotify-sdk').file('libs'), 'libs'
        }
    }
}
...
```

Edit `android/app/build.grandle` and add `packagingOptions`
```
...
buildTypes {
    release {
        minifyEnabled enableProguardInReleaseBuilds
        proguardFiles getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro"
    }
}
packagingOptions {
    pickFirst 'lib/armeabi-v7a/libgnustl_shared.so'
    pickFirst 'lib/x86/libgnustl_shared.so'
}
...
```

Edit `MainApplication.java`
```
import com.reactlibrary.RNSdkSpotifyPackage; <-------- import

...

@Override
protected List<ReactPackage> getPackages() {
  return Arrays.<ReactPackage>asList(
      new MainReactPackage(),
      new RNSdkSpotifyPackage() <-------- import
  );
}

...
```

## Methods
```Java
// set the access token to the sdk, is necessary to when expires
// the access token and its necessary set a new refreshed sinnce web api
//
// this returns a promise, with a param, success = "Success" if all is done
Promise setAccessToken(access_token : String);

// this function add a track uri to the queue
void queue(uri : String);

// This function set the next track to play
// another possibility to queue, but this function only
// is for one next track
void setNextTrack(uri : String);

// this function returns if exists any track to play in next
Boolean nextTrack();

// This function set the params attributes, clientId is for the spotify
// client key for the app for spotify, the redirect, is the redirect uri
// its need added in the app in spotify, the type is CODE od TOKEN, depends
// for you.
//
// Returns Success string
Promise setup(clientId : String, redirect_uri : String, type : String);

// Pause or resume the song
void pause();

// play a uri track
void playUri(uri : String);

// launche the login from spotify
//
// returns a object with the token or code, depends of setup function
Promise launchLogin();

```
