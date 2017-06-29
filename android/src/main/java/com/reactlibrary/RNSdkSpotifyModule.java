package com.reactlibrary;

import android.util.Log;
import android.net.Uri;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.ConnectivityManager;
import android.app.Activity;
import android.content.Intent;

import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.NativeModule;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.WritableMap;

import com.spotify.sdk.android.authentication.AuthenticationClient;
import com.spotify.sdk.android.authentication.AuthenticationRequest;
import com.spotify.sdk.android.authentication.AuthenticationResponse;
import com.spotify.sdk.android.authentication.AuthenticationResponse.Type;
import com.spotify.sdk.android.player.Config;
import com.spotify.sdk.android.player.ConnectionStateCallback;
import com.spotify.sdk.android.player.Error;
import com.spotify.sdk.android.player.Player;
import com.spotify.sdk.android.player.PlayerEvent;
import com.spotify.sdk.android.player.PlaybackState;
import com.spotify.sdk.android.player.Metadata;
import com.spotify.sdk.android.player.Spotify;
import com.spotify.sdk.android.player.SpotifyPlayer;
import com.spotify.sdk.android.player.Connectivity;

import java.util.Map;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;


public class RNSdkSpotifyModule extends ReactContextBaseJavaModule
    implements ActivityEventListener, Player.NotificationCallback, ConnectionStateCallback {

    private static final String E_NO_ACTIVITY          = "E_NO_ACTIVITY";
    private static final String E_RUNTIME_ERROR        = "E_RUNTIME_ERROR";
    private static final String E_AUTH_ERROR           = "E_AUTH_ERROR";
    private static final String E_CANCELLED            = "E_CANCELLED";
    private static final String KEY_TOKEN              = "token";
    private static final String KEY_CODE               = "code";
    private static final String KEY_ERROR              = "error";
    private static final String KEY_STATE              = "state";
    private static final String KEY_EXPIRES_IN         = "expiresIn";
    private static final String EVENT_AUDIO_FLASH      = "kSpPlaybackEventAudioFlush";
    private static final String EVENT_DELIVERY_DOUN    = "kSpPlaybackNotifyAudioDeliveryDone";
    private static final String EVENT_BECAME_ACTIVE    = "kSpPlaybackNotifyBecameActive";
    private static final String EVENT_BECAME_INACTIVE  = "kSpPlaybackNotifyBecameInactive";
    private static final String EVENT_CONTEXT_CHANGED  = "kSpPlaybackNotifyContextChanged";
    private static final String EVENT_LOST_PERMISSION  = "kSpPlaybackNotifyLostPermission";
    private static final String EVENT_METADATA_CHANGED = "kSpPlaybackNotifyMetadataChanged";
    private static final String EVENT_NOTIFY_NEXT      = "kSpPlaybackNotifyNext";
    private static final String EVENT_NOTIFY_PAUSE     = "kSpPlaybackNotifyPause";
    private static final String EVENT_NOTIFY_PLAY      = "kSpPlaybackNotifyPlay";
    private static final String EVENT_NOTIFY_PREV      = "kSpPlaybackNotifyPrev";
    private static final String EVENT_REPEAT_OFF       = "kSpPlaybackNotifyRepeatOff";
    private static final String EVENT_REPEAT_ON        = "kSpPlaybackNotifyRepeatOn";
    private static final String EVENT_SHUFLE_OFF       = "kSpPlaybackNotifyShuffleOff";
    private static final String EVENT_SHUFLE_ON        = "kSpPlaybackNotifyShuffleOn";
    private static final String EVENT_TRACK_CHANGED    = "kSpPlaybackNotifyTrackChanged";
    private static final String EVENT_TRACK_DELIVERED  = "kSpPlaybackNotifyTrackDelivered";

    private String type;         // type of request, token or code
    private String clientId;     // clientId from spotify app
    private String nextTrack;    // next track to play
    private String redirectUri;  // redirect uri
    private String access_token; //access token to can play music

    private static final int REQUEST_CODE = 1337;
    public ReactApplicationContext reactContext;
    private SpotifyPlayer mPlayer;

    private final List<Promise> promises = Collections.synchronizedList(new ArrayList<Promise>());
    private final Player.OperationCallback mOperationCallback = new Player.OperationCallback() {

        @Override
        public void onSuccess() {
            Log.e("onSuccess", "OperationCallback");
        }

        @Override
        public void onError(Error error) {
            Log.e("ERROR:", error.toString());
        }
    };

    public RNSdkSpotifyModule(ReactApplicationContext reactContext) {
        super(reactContext);
        reactContext.addActivityEventListener(this);
    }

    @ReactMethod
    public void setAccessToken(final String _access_token, final Promise promise) {
        if(_access_token != null) {
            onAuth(_access_token, promise);
        } else {
            promise.reject("Access token is undefined");
        }
    }

    @ReactMethod
    public void queue(String uri) {
        mPlayer.queue(new Player.OperationCallback() {
            @Override
            public void onSuccess() {
                Log.e("Success", "queue");
            }

            @Override
            public void onError(Error error) {
                Log.e("Fail", "queue");
            }
        }, uri);
    }

    private void onAuth(final String _access_token, final Promise promise) {
        Activity currentActivity = getCurrentActivity();
        // Once we have obtained an authorization token, we can proceed with creating a Player.
        // logStatus("Got authentication token");
        if (mPlayer == null) {
            Config playerConfig = new Config(currentActivity, _access_token, clientId);
            // Since the Player is a static singleton owned by the Spotify class, we pass "this" as
            // the second argument in order to refcount it properly. Note that the method
            // Spotify.destroyPlayer() also takes an Object argument, which must be the same as the
            // one passed in here. If you pass different instances to Spotify.getPlayer() and
            // Spotify.destroyPlayer(), that will definitely result in resource leaks.
            mPlayer = Spotify.getPlayer(playerConfig, currentActivity, new SpotifyPlayer.InitializationObserver() {
                @Override
                public void onInitialized(SpotifyPlayer player) {
                    mPlayer = player;
                    mPlayer.addNotificationCallback(RNSdkSpotifyModule.this);
                    mPlayer.addConnectionStateCallback(RNSdkSpotifyModule.this);
                    mPlayer.login(_access_token);
                    promise.resolve("true");
                }

                @Override
                public void onError(Throwable error) {
                    Log.e("Error in initialization: ", error.getMessage().toString());
                    promise.reject("false");
                }
            });
        } else {
            mPlayer.login(_access_token);
            promise.resolve("true");
        }
    }


    @Override
    public void onNewIntent(Intent intent) {
        // no-op
    }

    @Override
    public String getName() {
        return "RNSdkSpotify";
    }

    @ReactMethod
    public Boolean nextTrack() {
        return nextTrack != "";
    }

    @ReactMethod
    public void setNextTrack(final String track) {
        nextTrack = track;
    }

    @Override
    public void onPlaybackEvent(PlayerEvent playerEvent) {
        Log.d("MainActivity", "Playback event received: " + playerEvent.name());
        switch (playerEvent.name()) {
            case EVENT_TRACK_DELIVERED: // when tracks is ended
                if(nextTrack()) {
                    playUri(nextTrack);
                    nextTrack = "";
                }
                break;
            // Handle event type as necessary
            default:
                break;
        }
    }

    @Override
    public void onPlaybackError(Error error) {
        Log.d("MainActivity", "Playback error received: " + error.name());
        switch (error) {
            // Handle error type as necessary
            default:
                break;
        }
    }

    @Override
    public void onLoggedIn() {
        Log.d("MainActivity", "User logged in");
    }

    @Override
    public void onLoggedOut() {
        Log.d("MainActivity", "User logged out");
    }

    @Override
    public void onLoginFailed(Error e) {
        Log.d("MainActivity", "Login failed");
    }

    @Override
    public void onTemporaryError() {
        Log.d("MainActivity", "Temporary error occurred");
    }

    @Override
    public void onConnectionMessage(String message) {
        Log.d("MainActivity", "Received connection message: " + message);
    }

    @ReactMethod
    public void setup(final String _clientId, final String _redirectUri, final String _type, final Promise promise)
    {
        clientId = _clientId;
        redirectUri = _redirectUri;
        type = _type;

        promise.resolve("Success");
    }

    @ReactMethod
    public AuthenticationRequest getAuthenticationRequest() {
        Type __type = AuthenticationResponse.Type.TOKEN;

        if(type == "code") {
            __type = AuthenticationResponse.Type.CODE;
        }

        return new AuthenticationRequest.Builder(clientId, __type, redirectUri)
            .setShowDialog(false)
            .setScopes(new String[]{"user-read-email", "playlist-read-private", "playlist-read-collaborative", "playlist-modify-public", "playlist-modify-private", "streaming"})
            .build();
    }

    @ReactMethod
    public void isPlaying(final Promise promise) {
        promise.resolve(mPlayer.getPlaybackState().toString());
    }

    @ReactMethod
    public void pause() {
        if(clientId == null || access_token == null || mPlayer == null) return;
        if(mPlayer.getPlaybackState().isPlaying) {
            mPlayer.pause(mOperationCallback);
        }else{
            mPlayer.resume(mOperationCallback);
        }
    }

    private void openLoginWindow()
    {
        Activity currentActivity = getCurrentActivity();

        Type __type = AuthenticationResponse.Type.TOKEN;

        if(type == "code") {
            __type = AuthenticationResponse.Type.CODE;
        }

        final AuthenticationRequest request = new AuthenticationRequest.Builder(clientId, __type, redirectUri)
            .setScopes(new String[]{"user-read-email", "playlist-read-private", "playlist-read-collaborative", "playlist-modify-public", "playlist-modify-private", "streaming"})
            .build();

        AuthenticationClient.openLoginActivity(currentActivity, REQUEST_CODE, request);
    }

    private void logStatus(String status) {
        Log.i("TAG", status);
    }

    public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE) {
            AuthenticationResponse response = AuthenticationClient.getResponse(resultCode, data);
            switch (response.getType()) {
                // Response was successful and contains auth token
                case TOKEN:
                    onAuthenticationComplete(response);
                    break;
                case CODE:
                    resolve(response);
                    break;
                case ERROR:
                    reject(E_AUTH_ERROR, "Error during authorisation");
                    break;
                default: // cancelled
                    reject(E_CANCELLED, "User cancelled the operation");
            }
        }
    }

    private Connectivity getNetworkConnectivity(ReactContext context) {
        ConnectivityManager connectivityManager;
        connectivityManager = (ConnectivityManager) reactContext.getSystemService(ReactContext.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();

        if (activeNetwork != null && activeNetwork.isConnected()) {
            return Connectivity.fromNetworkType(activeNetwork.getType());
        } else {
            return Connectivity.OFFLINE;
        }
    }

    private void onAuthenticationComplete(final AuthenticationResponse authResponse) {
        access_token = authResponse.getAccessToken();
        Activity currentActivity = getCurrentActivity();

        if (mPlayer == null) {
            Config playerConfig = new Config(currentActivity, authResponse.getAccessToken(), clientId);
            mPlayer = Spotify.getPlayer(playerConfig, currentActivity, new SpotifyPlayer.InitializationObserver() {
                @Override
                public void onInitialized(SpotifyPlayer player) {
                    mPlayer = player;
                    mPlayer.addNotificationCallback(RNSdkSpotifyModule.this);
                    mPlayer.addConnectionStateCallback(RNSdkSpotifyModule.this);
                    mPlayer.login(authResponse.getAccessToken());
                    resolve(authResponse);
                }

                @Override
                public void onError(Throwable error) {
                    Log.e("Error in initialization: ", error.getMessage().toString());
                }
            });
        } else {
            mPlayer.login(authResponse.getAccessToken());
            resolve(authResponse);

        }
    }

    @ReactMethod
    public boolean isLoggedIn()
    {
        return mPlayer != null && mPlayer.isLoggedIn();
    }

    @ReactMethod
    public void playUri(String uri)
    {
        if(mPlayer == null) {
            return;
        }

        mPlayer.playUri(mOperationCallback, uri, 0, 0);
    }

    @ReactMethod
    public void launchLogin(final Promise promise)
    {
        promises.add(promise);
        openLoginWindow();
    }

    private void resolve(AuthenticationResponse response) {
        WritableMap data = makeRNFriendly(response);
        List<Promise> promises = this.promises;

        for (Promise p : promises) {
            p.resolve(data);
        }

        promises.removeAll(promises);
    }

    private void reject(String errorCode, RuntimeException e) {
        reject(errorCode, e.getMessage());
    }

    private void reject(String errorCode, String message) {
        List<Promise> promises = this.promises;
        for (Promise p : promises) {
            p.reject(errorCode, message);
        }
        this.promises.removeAll(promises);
    }

    private WritableMap makeRNFriendly(AuthenticationResponse response) {
        WritableMap ret = Arguments.createMap();
        ret.putString(KEY_TOKEN, response.getAccessToken());
        ret.putString(KEY_CODE, response.getCode());
        ret.putString(KEY_ERROR, response.getError());
        ret.putString(KEY_STATE, response.getState());
        ret.putInt(KEY_EXPIRES_IN, response.getExpiresIn());
        return ret;
    }

}
