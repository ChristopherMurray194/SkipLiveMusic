package com.skiplive.skiplivemusic;

import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Button;

import com.spotify.android.appremote.api.ConnectionParams;
import com.spotify.android.appremote.api.Connector;
import com.spotify.android.appremote.api.PlayerApi;
import com.spotify.android.appremote.api.SpotifyAppRemote;

import com.spotify.protocol.client.Subscription;
import com.spotify.protocol.types.PlayerState;
import com.spotify.protocol.types.Track;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LandingActivity extends AppCompatActivity {

    private static final String CLIENT_ID = "6ab70131d1624140acffa8062e32158d";
    // TODO: Ensure this redirects correctly
    private static final String REDIRECT_URI = "SkipLiveMusic://onStart";
    private SpotifyAppRemote mSpotifyAppRemote;
    private Subscription<PlayerState> mPlayerStateSubscription;
    private PlayerApi mPlayerApi;
    private Track mCurrentTrack;

    private ArrayList<Track> mSkippedTracks = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_landing);
    }

    /**
     * Invoked when user navigates to the activity
     */
    @Override
    protected void onStart() {
        super.onStart();

        if (!SpotifyAppRemote.isSpotifyInstalled(this)) {
            isSpotifyInstalledDialog().show();
        }
        connectToSpotifyRemote();

        configureButton();
    }

    private AlertDialog isSpotifyInstalledDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("You do not have Spotify installed! Please install Spotify and attempt to use the app again.");
        // Add the buttons
        builder.setPositiveButton("OK", (dialog, id)  -> {
            finish();
        });
        builder.setNegativeButton("Cancel", (dialog, id) -> {
            dialog.dismiss();
        });

        // Create the AlertDialog
        return builder.create();
    }

    private void connectToSpotifyRemote() {
        if(mSpotifyAppRemote != null && mSpotifyAppRemote.isConnected()) {
            SpotifyAppRemote.disconnect(mSpotifyAppRemote);
            Log.d("LandingActivity", "*** Spotify remote session has been disconnected ***");
        }

        // Set the connection parameters
        ConnectionParams connectionParams =
            new ConnectionParams.Builder(CLIENT_ID)
            .setRedirectUri(REDIRECT_URI)
            .showAuthView(true)
            .build();

        SpotifyAppRemote.connect(this, connectionParams,
        new Connector.ConnectionListener() {

            @Override
            public void onConnected(SpotifyAppRemote spotifyAppRemote) {
                mSpotifyAppRemote = spotifyAppRemote;
                mPlayerApi = mSpotifyAppRemote.getPlayerApi();
                Log.d("LandingActivity", "*** Spotify remote session has successfully connected ***");

                // Start interacting with App Remote
                initialiseSkipTracksEvent();
            }

            @Override
            public void onFailure(Throwable throwable) {
                Log.e("LandingActivity", throwable.getMessage(), throwable);
            }
        });
    }

    private void initialiseSkipTracksEvent() {
        Log.d("LandingActivity", "*** Skipping live tracks has been enabled ***");
        mPlayerStateSubscription = mPlayerApi.subscribeToPlayerState();

        mPlayerStateSubscription
        // Invoked when the player state changes e.g. playback paused
        .setEventCallback(playerState -> {
            if(mCurrentTrack != null && mCurrentTrack.equals(playerState.track)) {
                return;
            }
            mCurrentTrack = playerState.track;

            if(skipTrack(mCurrentTrack)) {
                if(mSkippedTracks.contains(mCurrentTrack)) {
                    // Assume the user has just skipped previous...
                    mPlayerApi.skipPrevious();
                    mSkippedTracks.remove(mCurrentTrack);
                }
                else {
                    mPlayerApi.skipNext();
                    if(mSkippedTracks.size() >= 10) { // Purge the list to stop it from getting too large.
                        mSkippedTracks.clear();
                    }
                    mSkippedTracks.add(mCurrentTrack);
                }
            }
        });
    }

    private boolean skipTrack(Track track) {
        Log.d("LandingActivity", "skipTrack called: Track is -> " + track.name);

        return (isAnnoyingSong(track) || isLiveSong(track.name));
    }

    private boolean isAnnoyingSong(Track track) {
        for(Song song : getAnnoyingSongs()) {
            if (track.name.equalsIgnoreCase(song._name) && track.artist.name.equalsIgnoreCase(song._artist)) {
                return true;
            }
        }
        return false;
    }

    private boolean isLiveSong(String trackName) {
        final String[] LIVE_STRINGS = {"- Live", "(Live)"};
        for (String str : LIVE_STRINGS) {
            if (trackName.contains(str)) {
                Log.d("LandingActivity", "SKIPPING TRACK: " + trackName);
                return true;
            }
        }
        return false;
    }

    private List<Song> getAnnoyingSongs() {
        return Arrays.asList(
            new Song("Fury's Laughter", "S.A.M")
        );
    }

    /**
     * Invoked when activity is no longer visible
     */
    @Override
    protected void onStop() {
        super.onStop();
    }

    protected void onDestroy() {
        super.onDestroy();
        SpotifyAppRemote.disconnect(mSpotifyAppRemote);
        Log.d("LandingActivity", "*** Spotify remote session has been disconnected ***");
    }

    private void configureButton() {
        final Button button = findViewById(R.id.toggleButton);
        button.setText(R.string.cancel_skipping_tracks);

        button.setOnClickListener(view -> {
            if (!mPlayerStateSubscription.isCanceled()) {
                Log.d("LandingActivity", "*** Skipping live tracks has been disabled ***");
                mPlayerStateSubscription.cancel(); // End the subscription.
                button.setText(R.string.begin_skipping_tracks); // Change the button text
            } else {
                initialiseSkipTracksEvent(); // Resubscribe to the player state
                button.setText(R.string.cancel_skipping_tracks);
            }
        });
    }

    /**
     *  Song struct
     */
    private class Song {
        private String _name = "";
        private String _artist = "";

        private Song(String inName, String inArtist) {
            _name = inName;
            _artist = inArtist;
        }
    }
}