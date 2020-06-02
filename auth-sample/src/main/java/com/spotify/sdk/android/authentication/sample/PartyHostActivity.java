package com.spotify.sdk.android.authentication.sample;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.spotify.android.appremote.api.ConnectionParams;
import com.spotify.android.appremote.api.Connector;
import com.spotify.android.appremote.api.SpotifyAppRemote;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PartyHostActivity extends AppCompatActivity {

    private static final String CLIENT_ID = "f1afbc79a395491088b46771713b772d";
    private static final String REDIRECT_URI = "http://com.yourdomain.yourapp/callback";
    private SpotifyAppRemote mSpotifyAppRemote;
    private Toolbar mToolbar;
    private Lobby lobby;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_party_host);

    }

    @Override
    protected void onStart() {
        super.onStart();
        // We will start writing our code here.
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
                        Log.d("DEBUG1", "Connected! Yay!");

                        lobby = (Lobby) getIntent().getSerializableExtra("HOST_LOBBY");
                        Log.d("LOBBY_DEBUG: ", lobby.getGenre()+" " + lobby.getMood() +" " + lobby.getName());
                        defaultMusic(); //QUACK
                    }

                    @Override
                    public void onFailure(Throwable throwable) {
                        Log.e("DEBUG2", throwable.getMessage(), throwable);

                        // Something went wrong when attempting to connect! Handle errors here
                    }
                });
    }

    private void defaultMusic() {
        // Then we will write some more code here.
        mSpotifyAppRemote.getPlayerApi().play("spotify:track:"+lobby.getDefaultMusicID());
        PublicLobbyHomepageService publicLobbyHomepageService = RetrofitInstance.getRetrofitInstance().create(PublicLobbyHomepageService.class);
        lobby.setCurrentMusicID(lobby.getDefaultMusicID());
        Call<Lobby> call = publicLobbyHomepageService.patchCurrentMusic(lobby.getLobbyID(), lobby);

        call.enqueue(new Callback<Lobby>(){

            @Override
            public void onResponse(Call<Lobby> call, Response<Lobby> response) {
                if(!response.isSuccessful()) {
                    Log.d("Current Music Error", response.body()+" "+response.code()+ " "+response.errorBody());
                    return;
                }
                Log.d("GOOD", "it's gone");
                return; //dovrebbe aver fatto la patch
            }

            @Override
            public void onFailure(Call<Lobby> call, Throwable t) {
                Log.d("on failur curren update", t.toString()+"");
                Toast.makeText(PartyHostActivity.this, "lobbysError", Toast.LENGTH_SHORT).show();
            }
        });

    }
}
