package com.spotify.sdk.android.authentication.sample.activity;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.JsonObject;
import com.spotify.android.appremote.api.ConnectionParams;
import com.spotify.android.appremote.api.Connector;
import com.spotify.android.appremote.api.SpotifyAppRemote;
import com.spotify.protocol.types.Track;
import com.spotify.sdk.android.auth.AuthorizationClient;
import com.spotify.sdk.android.auth.AuthorizationRequest;
import com.spotify.sdk.android.auth.AuthorizationResponse;
import com.spotify.sdk.android.authentication.sample.R;
import com.spotify.sdk.android.authentication.sample.ws.model.Album;
import com.spotify.sdk.android.authentication.sample.ws.model.Artist;
import com.spotify.sdk.android.authentication.sample.ws.model.CurrentlyPlayingContext;
import com.spotify.sdk.android.authentication.sample.ws.model.Image;
import com.spotify.sdk.android.authentication.sample.ws.model.Paging;
import com.spotify.sdk.android.authentication.sample.ws.model.PlayResumePlayback;
import com.spotify.sdk.android.authentication.sample.ws.model.Playlist;
import com.spotify.sdk.android.authentication.sample.ws.retrofit.RetrofitInstance;
import com.spotify.sdk.android.authentication.sample.ws.retrofit.RetrofitInstanceSpotifyApi;
import com.spotify.sdk.android.authentication.sample.ws.model.Lobby;
import com.spotify.sdk.android.authentication.sample.ws.service.LobbyService;
import com.spotify.sdk.android.authentication.sample.ws.service.PlayerService;
import com.spotify.sdk.android.authentication.sample.ws.service.SpotifyAPIService;
import com.squareup.picasso.Picasso;


import java.io.IOException;
import java.time.LocalTime;
import java.time.temporal.ChronoField;
import java.util.Random;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

import static com.spotify.sdk.android.auth.AuthorizationResponse.Type.TOKEN;

public class PartyHostActivity extends AppCompatActivity {

    private static final String CLIENT_ID = "f1afbc79a395491088b46771713b772d";
    private static final String REDIRECT_URI = "http://com.yourdomain.yourapp/callback";
    private SpotifyAppRemote mSpotifyAppRemote;
    private Toolbar mToolbar;
    private Lobby lobbySerialized;
    private long thisMoment;
    private Retrofit retrofit;
    private Track track;
    private Object trackObject;
    private Track[] tracksPlaylist;
    private long duration;
    private String uri;
    private String authorizationHeader;
    private ConnectionParams connectionParams;
    private Button endVoteButton;
    private Button declineButton;
    private ConstraintLayout constraintLayoutCardView;
    private TextView titleSponsorized;
    private TextView textTrackName;
    private static final int REQUEST_CODE = 1337;
    private String accessToken;
    private PollingPlaybackState pollingPlaybackState;
    private Paging tracks;
    private Random random;
    private String uriOfTrack;
    private SpotifyAPIService spotifyAPIService;
    private com.spotify.sdk.android.authentication.sample.ws.model.Album albumCurrentTrack;
    private WebView webView;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_party_host);

        Button openPartyButton = findViewById(R.id.openParty);
        lobbySerialized = (Lobby) getIntent().getSerializableExtra("HOST_LOBBY");
        if (!lobbySerialized.isOpen()){
            openPartyButton.setText("Apri il Party");
        }
        else{
            openPartyButton.setText("Chiudi il Party");
        }

        webView = findViewById(R.id.videoWebView);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.setWebChromeClient(new WebChromeClient(){

        });

    }

    @Override
    protected void onStart() {
        super.onStart();
        // We will start writing our code here.

        Button openPartyButton = findViewById(R.id.openParty);
        endVoteButton = findViewById(R.id.endVote);
        declineButton = findViewById(R.id.decline);
        textTrackName = findViewById(R.id.trackName);
        constraintLayoutCardView = findViewById(R.id.cardView);
        titleSponsorized = findViewById(R.id.sponsorizedTitle);
        random = new Random();


        connectionParams = new ConnectionParams.Builder(CLIENT_ID)
                .setRedirectUri(REDIRECT_URI)
                .showAuthView(true)
                .build();

        /* Auth */
        AuthorizationRequest.Builder builder =
                new AuthorizationRequest.Builder(CLIENT_ID, TOKEN, REDIRECT_URI);

        builder.setScopes(new String[]{"user-read-playback-state", "user-modify-playback-state"});
        AuthorizationRequest request = builder.build();

        AuthorizationClient.openLoginActivity(this, REQUEST_CODE, request);


        SpotifyAppRemote.connect(this, connectionParams,
                new Connector.ConnectionListener() {

                    @Override
                    public void onConnected(SpotifyAppRemote spotifyAppRemote) {

                        LobbyService lobbyService = RetrofitInstance.getRetrofitInstance().create(LobbyService.class);
                        lobbySerialized = (Lobby) getIntent().getSerializableExtra("HOST_LOBBY");
                        Call<Lobby> callLobbyById = lobbyService.getLobbyById(lobbySerialized.getLobbyID());
                        callLobbyById.enqueue(new Callback<Lobby>() {
                            @Override
                            public void onResponse(Call<Lobby> call, Response<Lobby> response) {
                                Lobby lobby = response.body();
                                openPartyButton.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        if(!lobby.isOpen()) {
                                            webView.loadData("<iframe width=\"100%\" height=\"100%\" src=\"https://www.youtube.com/embed/"+lobby.getVideoUrl()+"\" frameborder=\"0\" allowfullscreen></iframe>", "text/html","utf-8");
                                            lobby.setCurrentMusicID(lobby.getDefaultMusicID());
                                            SpotifyAPIService spotifyAPIService = RetrofitInstanceSpotifyApi.getRetrofitInstance().create(SpotifyAPIService.class);
                                            Call<Track> callTrack = spotifyAPIService.getTrackById("Bearer " + accessToken, lobby.getCurrentMusicID().substring(14));
                                            callTrack.enqueue(new Callback<Track>() {
                                                @RequiresApi(api = Build.VERSION_CODES.O)
                                                @Override
                                                public void onResponse(Call<Track> call, Response<Track> response) {
                                                    track = response.body();
                                                    LocalTime temp = null;
                                                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                                        temp = LocalTime.now();
                                                    }
                                                    assert temp != null;
                                                    Log.d("DEBUG: ", track.toString());
                                                    lobby.setMomentOfPlay(temp.getLong(ChronoField.MILLI_OF_DAY));
                                                    lobby.setMusicDuration(track.duration);
                                                    lobby.setNextMusicID(lobby.getDefaultMusicID());
                                                    lobby.setOpen(true);
                                                    openPartyButton.setText("Chiudi il Party");


                                                    getTrackDataForView(track, accessToken);
                                                    /*Picasso.get().load(imageOfCurrentTrack[0].getUrl()).centerCrop().into(imageCurrentTrack);*/

                                                    patchLobby(lobby);
                                                    getPlaylistTracks(lobby);
                                                    try {
                                                        Thread.sleep(400); //ce la fa anche con 100 millis ma preferiamo tenerci un margine di 300millis
                                                    } catch (InterruptedException e) {
                                                        e.printStackTrace();
                                                    }

                                                    if(constraintLayoutCardView.getVisibility() == View.INVISIBLE){
                                                        constraintLayoutCardView.setVisibility(View.VISIBLE);
                                                    }

                                                    if(titleSponsorized.getVisibility() == View.INVISIBLE)
                                                        titleSponsorized.setVisibility(View.VISIBLE);

                                                    play(lobby, accessToken);

                                                }

                                                @Override
                                                public void onFailure(Call<Track> call, Throwable t) {
                                                    Log.d("DEBUG", "DOVEVA anna COSì FRATELLì");
                                                }
                                            });
                                        }
                                        else{
                                            onBackPressed();
                                        }
                                    }
                                });

                            }
                            @Override
                            public void onFailure(Call<Lobby> call, Throwable t) {

                            }
                        });
                    }

                    @Override
                    public void onFailure(Throwable throwable) {
                        Log.d("DEBUG2", throwable.getMessage(), throwable);

                        // Something went wrong when attempting to connect! Handle errors here
                    }
                });
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        // Check if result comes from the correct activity
        if (requestCode == REQUEST_CODE) {
            AuthorizationResponse response = AuthorizationClient.getResponse(resultCode, intent);

            switch (response.getType()) {
                // Response was successful and contains auth token
                case TOKEN:
                    // Handle successful response
                    Log.d("AUTH_DONE", response.getAccessToken()+"");
                    accessToken = response.getAccessToken();
                    break;

                // Auth flow returned an error
                case ERROR:
                    // Handle error response
                    break;

                // Most likely auth flow was cancelled
                default:
                    // Handle other cases
            }
        }
    }

    class PollingPlaybackState extends AsyncTask<Call<CurrentlyPlayingContext>, Lobby, Void> {


        @Override
        protected Void doInBackground(Call<CurrentlyPlayingContext>... param) {

            Call<CurrentlyPlayingContext> call = param[0];
            Call<CurrentlyPlayingContext> newCall;
            CurrentlyPlayingContext currentlyPlayingContextTemp;
            CurrentlyPlayingContext currentlyPlayingContext;
            int durationTrack;

            try {

                currentlyPlayingContext = call.execute().body();
                assert currentlyPlayingContext != null;

                //Response<CurrentlyPlayingContext> response = call.execute();
                //Log.d("RESPONSE.CODE", response.message()+"");
                //Log.d("RESPONSE.CODE", response.body().getItem()+"");
                durationTrack = currentlyPlayingContext.getItem().getDuration_ms();
                Log.d("DURATION_TRACK", durationTrack+"");

                do {
                    if(isCancelled())
                        break;
                    newCall = call.clone();
                    currentlyPlayingContextTemp = newCall.execute().body();
                    if (newCall.isExecuted())
                        newCall.cancel();

                    if(currentlyPlayingContextTemp == null) {
                        currentlyPlayingContextTemp = new CurrentlyPlayingContext();
                        currentlyPlayingContextTemp.setProgress_ms(0);
                        Log.d("CURRENTLYPLAYINGCONT", currentlyPlayingContextTemp.getProgress_ms()+"--> Doveva anna cosi fratellì");
                    }else
                        Log.d("CURRENTLYPLAYINGCONT", currentlyPlayingContextTemp.getProgress_ms() + "--> QUACK");

        }while (currentlyPlayingContextTemp.getProgress_ms() < durationTrack-3500);
    } catch (IOException e) {
        e.printStackTrace();
    }

            return  null;
}



    @Override
    protected void onPostExecute(Void aVoid) {
            LobbyService lobbyService = RetrofitInstance.getRetrofitInstance().create(LobbyService.class);
            lobbySerialized = (Lobby) getIntent().getSerializableExtra("HOST_LOBBY");
            Call<Lobby> callLobbyById = lobbyService.getLobbyById(lobbySerialized.getLobbyID());
            callLobbyById.enqueue(new Callback<Lobby>() {
                @Override
                public void onResponse(Call<Lobby> call, Response<Lobby> response) {
                    Lobby lobby = response.body();
                    lobby.setCurrentMusicID(lobby.getNextMusicID());
                    lobby.setAccepted(false);
                    /*lobby.setNextMusicID(lobby.getDefaultMusicID());*/ //nel caso in cui non venga cliccato endVote riparte la musica di default finché non viene cliccato end vote
                    SpotifyAPIService spotifyAPIService = RetrofitInstanceSpotifyApi.getRetrofitInstance().create(SpotifyAPIService.class);
                    Call<Track> callTrack = spotifyAPIService.getTrackById("Bearer "+accessToken, lobby.getCurrentMusicID().substring(14));
                    callTrack.enqueue(new Callback<Track>() {
                        @RequiresApi(api = Build.VERSION_CODES.O)
                        @Override
                        public void onResponse(Call<Track> call, Response<Track> response) {
                            track = response.body();
                            LocalTime temp = null;
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                temp = LocalTime.now();
                            }
                            assert temp != null;
                            lobby.setMomentOfPlay(temp.getLong(ChronoField.MILLI_OF_DAY));
                            lobby.setMusicDuration(track.duration);
                            Log.d("DEBUG_SYNCH: ", "sto per effettuare la patch e poi la play");
                            getTrackDataForView(track, accessToken);
                            patchLobby(lobby);
                            Log.d("DEBUG_SYNCH: ", "ho fatto la patch sto per fare la play");
                            play(lobby, accessToken);

                        }

                        @Override
                        public void onFailure(Call<Track> call, Throwable t) {
                            Log.d("DEBUG", "DOVEVA anna COSì FRATELLì");
                        }
                    });
                }
                @Override
                public void onFailure(Call<Lobby> call, Throwable t) {
                }
            });
        }
    }



    private void patchLobby(Lobby lobbyToPatch){
        Log.d("DEBUG_SYNCH", "sono nel metodo patchLobby");
        LobbyService lobbyService = RetrofitInstance.getRetrofitInstance().create(LobbyService.class);
        Call<Lobby> callPatchLobby = lobbyService.patchLobby(lobbyToPatch.getLobbyID(), lobbyToPatch);
        callPatchLobby.enqueue(new Callback<Lobby>() {

            @Override
            public void onResponse(Call<Lobby> call, Response<Lobby> response) {
                Log.d("DEBUG_SYNCH", "sono nell'onResponse di patch");
                Log.d("DEBUG_PATCH_DONE", "");
            }
            @Override
            public void onFailure(Call<Lobby> call, Throwable t) {
                Log.d("DEBUG_SYNCH", "sono nell'onFailure di patch");
                Log.d("DEBUG_PATCH_FAIL", t+"");
            }
        });
    }

    private void play(Lobby lobby, String accessToken) {
        Log.d("DEBUG_SYNCH", "sono nel metodo play");
        PlayResumePlayback uri = new PlayResumePlayback();
        String[] uris = {lobby.getCurrentMusicID()};
        uri.setUris(uris);

        Log.d("ARRAY CONSTRUCT", uris + "");
        spotifyAPIService = RetrofitInstanceSpotifyApi.getRetrofitInstance().create(SpotifyAPIService.class);
        Call<JsonObject> responseCall = spotifyAPIService.playUserPlayback("Bearer " + accessToken, uri);
        responseCall.enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                Log.d("DEBUG_PLAY", response.body() + "");
                Log.d("DEBUG_SYNCH", "sono nell'onResponse di play");

                //Player Service
                PlayerService playerService = RetrofitInstanceSpotifyApi.getRetrofitInstance().create(PlayerService.class);
                Call<CurrentlyPlayingContext> callGetPlayer = playerService.getInfoCurrentUserPlayback("Bearer " + accessToken);
                //Start AsyncTask
                getTrackOfPlaylist(lobby);
                endVoteButton.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        Log.d("DEBUG_END_VOTE", "mi hai cliccato");

                        Thread setIsAccepted = new Thread() {
                            @Override
                            public void run() {
                                if(!lobby.isAccepted()) {
                                    Log.d("TEST", "mi hai cliccato");
                                    lobby.setAccepted(true);
                                    patchLobby(lobby);
                                }
                            }
                        };
                        setIsAccepted.start();
                        if(!lobby.isAccepted()) {
                            Toast.makeText(PartyHostActivity.this, "Musica accettata", Toast.LENGTH_SHORT).show();
                        }
                    }
                });//endVote

                declineButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Thread tryOtherMusic = new Thread() {
                            @Override
                            public void run() {
                                if(!lobby.isAccepted()){
                                    getTrackOfPlaylist(lobby);
                                }
                            }
                        };
                        tryOtherMusic.start();
                    }
                });

                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                pollingPlaybackState.execute(callGetPlayer);
                //End Player Service
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Log.d("DEBUG_SYNCH", "sono nell'onResponse di play");
                Log.d("DEBUG_PLAY_FAIL", t.toString() + "");
            }
        });
    }

    public void getPlaylistTracks(Lobby lobby){
        SpotifyAPIService spotifyAPIService = RetrofitInstanceSpotifyApi.getRetrofitInstance().create(SpotifyAPIService.class);
        Call<Playlist> playlistTracks = spotifyAPIService.getPlaylistTracks("Bearer "+accessToken, lobby.getPlaylistLobby().substring(17));
        playlistTracks.enqueue(new Callback<Playlist>() {
            @Override
            public void onResponse(Call<Playlist> call, Response<Playlist> response) {
                Log.d("DEBUG_PLAYLIST_SUCCESS:", response.body()+"");
                assert response.body() != null;
                tracks = response.body().getTracks();
            }

            @Override
            public void onFailure(Call<Playlist> call, Throwable t) {
                Log.d("DEBUG_PLAYLIST_FAIL:", t+"");
            }
        });
    }

    public void getTrackOfPlaylist(Lobby lobby){
        pollingPlaybackState = new PollingPlaybackState();
        int randomTrack = random.nextInt(tracks.getItems().length-1)+1;
        trackObject = tracks.getItem(randomTrack);
        Log.d("TRACK", trackObject.toString());
        String trackToString = trackObject.toString();
        String[] words = trackToString.split(" ");
        for(int i = 0; i<words.length; i++){
            if (words[i].matches("href=https://api.spotify.com/v1/tracks/.*,")) {
                int length = words[i].length();
                uriOfTrack = words[i].substring(39, length - 1);
            }
        }
        SpotifyAPIService spotifyAPIService = RetrofitInstanceSpotifyApi.getRetrofitInstance().create(SpotifyAPIService.class);
        Call<Track> callTrack = spotifyAPIService.getTrackById("Bearer "+accessToken, uriOfTrack);
        callTrack.enqueue(new Callback<Track>() {

            @Override
            public void onResponse(Call<Track> call, Response<Track> response) {
                if(response.body() != null) {
                    getNextTrackDataForView(response.body(), accessToken);
                    Log.d("NAME", response.body() + "");
                    Log.d("URI", uriOfTrack + "");
                    Thread setNextMusic = new Thread() {
                        @Override
                        public void run() {
                            Log.d("TEST", "mi hai cliccato");
                            lobby.setNextMusicID("spotify:track:" + uriOfTrack);
                            patchLobby(lobby);
                        }
                    };
                    setNextMusic.start();
                }
            }

            @Override
            public void onFailure(Call<Track> call, Throwable t) {
                Log.d("DEBUG_GET_TRACK_PLAYLIS", t+"");
            }
        });
    }

    public void getTrackDataForView(Track track, String accessToken){
        ImageView imageCurrentTrack = findViewById(R.id.songImageHost);
        TextView trackName = findViewById(R.id.nameCurrentTrack);
        TextView albumName = findViewById(R.id.nameCurrentAlbum);
        TextView artistName = findViewById(R.id.nameCurrentArtist);
        TextView title = findViewById(R.id.titleHost);
        SpotifyAPIService serviceForData = RetrofitInstanceSpotifyApi.getRetrofitInstance().create(SpotifyAPIService.class);
        Call<Album> callForData = serviceForData.getAlbumTrack("Bearer " + accessToken, track.album.uri.substring(14));
        callForData.enqueue(new Callback<Album>() {
            @Override
            public void onResponse(Call<Album> call, Response<Album> response) {
                Image[] imagesOfCurrentTrack = response.body().getImages();
                Artist[] artistsOfCurrentTrack = response.body().getArtists();
                Picasso.get().load(imagesOfCurrentTrack[0].getUrl()).into(imageCurrentTrack);
                title.setText("Attualmente in riproduzione:");
                trackName.setText(track.name);
                albumName.setText(response.body().getName());
                artistName.setText((artistsOfCurrentTrack[0].getName()));
            }

            @Override
            public void onFailure(Call<Album> call, Throwable t) {

            }
        });
    }

    public void getNextTrackDataForView(Track track, String accessToken){
        ImageView imageCurrentTrack = findViewById(R.id.imageNextTrack);
        TextView trackNextName = findViewById(R.id.trackName);
        TextView albumNextName = findViewById(R.id.nameNextAlbum);
        TextView artistNextName = findViewById(R.id.nameNextArtist);
        SpotifyAPIService serviceForData = RetrofitInstanceSpotifyApi.getRetrofitInstance().create(SpotifyAPIService.class);
        Call<Album> callForData = serviceForData.getAlbumTrack("Bearer " + accessToken, track.album.uri.substring(14));
        callForData.enqueue(new Callback<Album>() {
            @Override
            public void onResponse(Call<Album> call, Response<Album> response) {
                Image[] imagesOfCurrentTrack = response.body().getImages();
                Artist[] artistsOfCurrentTrack = response.body().getArtists();
                Picasso.get().load(imagesOfCurrentTrack[0].getUrl()).into(imageCurrentTrack);
                trackNextName.setText(track.name);
                albumNextName.setText(response.body().getName());
                artistNextName.setText((artistsOfCurrentTrack[0].getName()));
            }

            @Override
            public void onFailure(Call<Album> call, Throwable t) {

            }
        });
    }

    @Override
    public void onBackPressed() {
        LobbyService lobbyService = RetrofitInstance.getRetrofitInstance().create(LobbyService.class);
        lobbySerialized = (Lobby) getIntent().getSerializableExtra("HOST_LOBBY");

        spotifyAPIService = RetrofitInstanceSpotifyApi.getRetrofitInstance().create(SpotifyAPIService.class);
        Call<JsonObject> callPauseUserPlayback = spotifyAPIService.pauseUserPlayback("Bearer " + accessToken);
        callPauseUserPlayback.enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                Log.d("Player paused: ",response.code()+"");
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Log.d("Player paused: ",t.getMessage()+"");
            }
        });

        Call<Lobby> callLobbyById = lobbyService.getLobbyById(lobbySerialized.getLobbyID());
        callLobbyById.enqueue(new Callback<Lobby>() {
            @Override
            public void onResponse(Call<Lobby> call, Response<Lobby> response) {
                Log.d("DEBUG_ONBACK", response.body()+"");
                Lobby lobby = response.body();
                assert lobby != null;
                lobby.setOpen(false);
                lobby.setAccepted(false);
                Log.d("DEBUG_ONBACK_LOBBY: ", lobby.getLobbyID()+"");
                LobbyService lobbyService2 = RetrofitInstance.getRetrofitInstance().create(LobbyService.class);
                Call<Lobby> callPatchLobby = lobbyService2.patchLobby(lobby.getLobbyID(), lobby);
                callPatchLobby.enqueue(new Callback<Lobby>() {

                    @Override
                    public void onResponse(Call<Lobby> call, Response<Lobby> response) {
                        Log.d("DEBUG_PATCH_DONE", "");
                    }
                    @Override
                    public void onFailure(Call<Lobby> call, Throwable t) {
                        Log.d("DEBUG_PATCH_FAIL", t+"");
                    }
                });

            }
            @Override
            public void onFailure(Call<Lobby> call, Throwable t) {
                Log.d("DEBUG_ONBACK_FAIL", t.toString()+"");
            }
        });

        constraintLayoutCardView.setVisibility(View.INVISIBLE);

        if(pollingPlaybackState != null)
            pollingPlaybackState.cancel(true);
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }
}