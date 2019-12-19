package com.skiplive.skiplivemusic;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.spotify.android.appremote.api.ConnectionParams;
import com.spotify.android.appremote.api.Connector;
import com.spotify.android.appremote.api.PlayerApi;
import com.spotify.android.appremote.api.SpotifyAppRemote;

import com.spotify.protocol.client.Subscription;
import com.spotify.protocol.types.PlayerState;
import com.spotify.protocol.types.Track;

public class LandingActivity extends AppCompatActivity
{

    private static final String CLIENT_ID = "6ab70131d1624140acffa8062e32158d";
    // TODO: Ensure this redirects correctly
    private static final String REDIRECT_URI = "SkipLiveMusic://onStart";
    private SpotifyAppRemote mSpotifyAppRemote;
    private Subscription<PlayerState> mPlayerStateSubscription;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_landing);
    }

    /**
     * Invoked when user navigates to the activity
     */
    @Override
    protected void onStart()
    {
        super.onStart();

        // Set the connection parameters
        ConnectionParams connectionParams =
        new ConnectionParams.Builder(CLIENT_ID)
            .setRedirectUri(REDIRECT_URI)
            .showAuthView(true)
            .build();

        SpotifyAppRemote.connect(this, connectionParams,
            new Connector.ConnectionListener()
            {

                @Override
                public void onConnected(SpotifyAppRemote spotifyAppRemote)
                {
                    mSpotifyAppRemote = spotifyAppRemote;
                    Log.d("LandingActivty", "*** Successfully landed on the landing activity ***");

                    // Start interacting with App Remote
                    initialiseSkipTracksEvent();
                }

                @Override
                public void onFailure(Throwable throwable)
                {
                    Log.e("LandingActivity", throwable.getMessage(), throwable);
                }
            });

        configureButton();
    }

    private void initialiseSkipTracksEvent() {
        Log.d("LandingActivity","*** Skipping live tracks has been enabled ***");
        final String[] LIVE_STRINGS = {"- Live", "(Live)" };
        final PlayerApi PLAYERAPI = mSpotifyAppRemote.getPlayerApi();
        mPlayerStateSubscription = PLAYERAPI.subscribeToPlayerState();

        mPlayerStateSubscription
            .setEventCallback( playerState ->  {
                // Invoked when the player state changes e.g. playback paused
                final Track track = playerState.track;
                if(track != null) {
                    for (String str : LIVE_STRINGS) {
                        if (track.name.contains(str)) {
                            Log.d("LandingActivity", "SKIPPING TRACK: " + track.name);
                            PLAYERAPI.skipNext();
                        }
                    }
                }
            });
    }

    /**
     * Invoked when activity is no longer visible
     */
    @Override
    protected void onStop()
    {
        super.onStop();
    }

    private void configureButton() {
        final Button button = findViewById(R.id.toggleButton);
        button.setOnClickListener( view -> {
            if (!mPlayerStateSubscription.isCanceled()) {
                Log.d("LandingActivity","*** Skipping live tracks has been disabled ***");
                mPlayerStateSubscription.cancel(); // End the subscription.
                SpotifyAppRemote.disconnect(mSpotifyAppRemote);
                button.setText(R.string.begin_skipping_tracks); // Change the button text
            } else {
                initialiseSkipTracksEvent(); // Resubscribe to the player state
                button.setText(R.string.cancel_skipping_tracks);
            }
        });
    }
}
