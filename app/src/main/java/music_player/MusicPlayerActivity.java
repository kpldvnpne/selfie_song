package music_player;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.support.graphics.drawable.Animatable2Compat;
import android.support.graphics.drawable.AnimatedVectorDrawableCompat;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.concurrent.Callable;

import android.net.Uri;
import android.content.ContentResolver;
import android.database.Cursor;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.MediaController.MediaPlayerControl;
import android.os.IBinder;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.view.MenuItem;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.bipinoli.selfie_song_minor.R;
import com.bipinoli.selfie_song_minor.Test.InferencedActivity;

import music_player.database.DBHelper;
import music_player.music.MusicController;
import music_player.music.MusicService;
import music_player.music.Song;
import music_player.music.SongAdapter;
import music_player.music.SongExtractor;


public class MusicPlayerActivity extends AppCompatActivity implements MediaPlayerControl{
    private static final String TAG = "MusicPlayerActivity";

    private boolean paused = false, playbackPaused = false;
    private MusicController controller;
    private MusicService musicSrv;
    private Intent playIntent = null;
    private boolean musicBound = false;
    private  DBHelper database;
    private String predictionText;
    // tutorial 2nd
    // part 2. Start the service
    // step 1

    private void setController(){
        controller = new MusicController(this);

        controller.setPrevNextListeners(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playNext();
            }
        }, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playPrev();
            }
        });
        controller.setMediaPlayer(this);
        controller.setAnchorView(findViewById(R.id.activity_music_player_recyclerView));
        controller.setEnabled(true);
    }

    @Override
    public void start() {
        musicSrv.go();
    }

    private void playNext(){
        musicSrv.playNext();
        if(playbackPaused){
            setController();
            playbackPaused=false;
        }
        controller.show(0);
    }

    private void playPrev(){
        musicSrv.playPrev();
        if(playbackPaused){
            setController();
            playbackPaused=false;
        }
        controller.show(0);
    }
    @Override
    public void pause() {
        playbackPaused = true;
       musicSrv.pausePlayer();
    }

    @Override
    public int getDuration() {
        if(musicSrv!=null && musicBound && musicSrv.isPng())
            return musicSrv.getDur();
        return 0;
    }

    @Override
    protected void onPause(){
        super.onPause();
        paused = true;
    }

    @Override
    protected void onResume(){
        super.onResume();
        if(paused){
            setController();
            paused=false;
        }
    }

    @Override
    protected void onStop(){
        controller.hide();
        super.onStop();
    }

    @Override
    public int getCurrentPosition() {
        if(musicSrv!=null && musicBound && musicSrv.isPng())
            return musicSrv.getPosn();
        return 0;
    }

    @Override
    public void seekTo(int pos) {
        musicSrv.seek(pos);
    }

    @Override
    public boolean isPlaying() {
        if(musicSrv!=null && musicBound)
            return musicSrv.isPng();
        return false;
    }

    @Override
    public int getBufferPercentage() {
        return 0;
    }

    @Override
    public boolean canPause() {
        return true;
    }

    @Override
    public boolean canSeekBackward() {
        return true;
    }

    @Override
    public boolean canSeekForward() {
        return true;
    }

    @Override
    public int getAudioSessionId() {
        return 0;
    }

    private ArrayList<Song> songList;
    private RecyclerView songView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_music_player);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {

                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},1);

                // MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE is an
                // app-defined int constant

                return;
            }}


        database = new DBHelper(this);
        songList = new ArrayList<>();
        SongExtractor.getSongList(this, database, songList);
        /*
            Added code
         */
        Collections.sort(songList, new Comparator<Song>() {
            @Override
            public int compare(Song o1, Song o2) {
                return o1.getTitle().compareTo(o2.getTitle());
            }
        });


        songView = (RecyclerView) findViewById(R.id.activity_music_player_recyclerView);

        // improves the performance
        songView.setHasFixedSize(true);

        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        songView.setLayoutManager(layoutManager);

        RecyclerView.Adapter adapter = new SongAdapter(songList, new MusicPlayerActivity.MyOnClickListener(), new MusicPlayerActivity.MyOnLongClickListener());
        songView.setAdapter(adapter);

        setController();



        // --------------------------------------------------------------------------------------

        final String predictionText = getIntent().getStringExtra("INFERENCE");
        Log.e(TAG, "predictionText: " + predictionText);




        // ------------- Animation -------------------------
        ImageView imageView = (ImageView)findViewById(R.id.song_player_imageView);
        TextView mood_text = (TextView)findViewById(R.id.song_player_mood_text);

        final AnimatedVectorDrawableCompat avd_happy = AnimatedVectorDrawableCompat.create(this, R.drawable.avd_romantic);
        final AnimatedVectorDrawableCompat avd_sad = AnimatedVectorDrawableCompat.create(this, R.drawable.avd_anim_gloomy);
        final AnimatedVectorDrawableCompat avd_surprised = AnimatedVectorDrawableCompat.create(this, R.drawable.avd_anim_surprised);
        final AnimatedVectorDrawableCompat avd_neutral = AnimatedVectorDrawableCompat.create(this, R.drawable.avd_romantic);
        final AnimatedVectorDrawableCompat avd_angry = AnimatedVectorDrawableCompat.create(this, R.drawable.avd_anim_angry);

        final AnimatedVectorDrawableCompat avd;

        // -----------------------------------------------------------

        switch (predictionText) {
            case "happy":
                mood_text.setText(R.string.happy_text);
                avd = avd_happy;
                imageView.setImageDrawable(avd);
                break;
            case "sad":
                mood_text.setText(R.string.sad_text);
                avd = avd_sad;
                imageView.setImageDrawable(avd);
                break;
            case "surprised":
                mood_text.setText(R.string.surprised_text);
                avd = avd_surprised;
                imageView.setImageDrawable(avd);
                break;
            case "neutral":
                mood_text.setText(R.string.neutral_text);
                avd = avd_neutral;
                imageView.setImageDrawable(avd);
                break;
            case "angry":
                mood_text.setText(R.string.angry_text);
                avd = avd_angry;
                imageView.setImageDrawable(avd);
                break;
            default:
                avd = avd_angry;
                imageView.setImageDrawable(avd);
        }


        // ----------- start animation ----------------------

        // animation on infinite loop (make it repeat baby)
        final Handler mainHandler = new Handler(Looper.getMainLooper());
        avd.registerAnimationCallback(new Animatable2Compat.AnimationCallback() {
            @Override
            public void onAnimationEnd(final Drawable drawable) {
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        avd.start();
                    }
                });
            }
        });
        avd.start();
        this.predictionText = predictionText;
        // --------------------------------------------------


    }

    //connect to the service
    private ServiceConnection musicConnection = new ServiceConnection(){
        @Override
        public void onServiceConnected(ComponentName name, IBinder service){
            MusicService.MusicBinder binder = (MusicService.MusicBinder)service;
            //get service
            musicSrv = binder.getService();
            //pass list
            musicSrv.setList(songList,database);
            musicBound = true;
            songRecommended(predictionText);
        }

        @Override
        public void onServiceDisconnected(ComponentName name){
            musicBound = false;
        }
    };

    @Override
    protected void onStart() {
        super.onStart();
        if(playIntent==null){
            playIntent = new Intent(this,MusicService.class);
            bindService(playIntent, musicConnection, Context.BIND_AUTO_CREATE);
            startService(playIntent);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        MenuInflater inf = getMenuInflater();
        inf.inflate(R.menu.main,menu);
        return true;
    }

//    public void getSongList(){
//        //retrieve song info
//        ContentResolver musicResolver  = getContentResolver();
//        Uri musicUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
//        Cursor musicCursor = musicResolver.query(musicUri,null,null,null,null);
//
//        if(musicCursor!=null && musicCursor.moveToFirst()){
//            //get columns
//            int titleColumn = musicCursor.getColumnIndex
//                    (android.provider.MediaStore.Audio.Media.TITLE);
//            int idColumn = musicCursor.getColumnIndex
//                    (android.provider.MediaStore.Audio.Media._ID);
//            int artistColumn = musicCursor.getColumnIndex
//                    (MediaStore.Audio.Media.ARTIST);
//            int genreColumn = musicCursor.getColumnIndex(MediaStore.Audio.Genres.NAME);
//            //add songs to list
//            do {
//                long thisId = musicCursor.getLong(idColumn);
//                String thisTitle = musicCursor.getString(titleColumn);
//                String thisArtist = musicCursor.getString(artistColumn);
//                String thisGenre = null; //musicCursor.getString(genreColumn);
//                if(database.exists(thisId)) {
//                    Cursor cursor = database.getData(thisId);
//                    cursor.moveToFirst();
//                    songList.add(new Song(cursor.getLong(cursor.getColumnIndex(DBHelper.SONGS_COLUMN_ID)),
//                                          cursor.getString(cursor.getColumnIndex(DBHelper.SONGS_COLUMN_NAME)),
//                                          cursor.getString(cursor.getColumnIndex(DBHelper.SONGS_COLUMN_ARTIST)),
//                                          cursor.getString(cursor.getColumnIndex(DBHelper.SONGS_COLUMN_GENRE))));
//                    continue;
//                }
//                // get genre of the song
//                Uri genreUri = MediaStore.Audio.Genres.getContentUriForAudioId("external",(int)thisId);
//                Cursor genreCursor = musicResolver.query(genreUri,null,null,null,null);
//                if(genreCursor!=null && genreCursor.moveToFirst()){
//                    int genreColumnIndex = genreCursor.getColumnIndex(MediaStore.Audio.GenresColumns.NAME);
//                    do{
//                        String genre = genreCursor.getString(genreColumnIndex);
//                        if(thisGenre==null){
//                            thisGenre = genre;
//                        }else{
//                            thisGenre += ", " + genre;
//                        }
//                    }while(genreCursor.moveToNext());
//                }
//                songList.add(new Song(thisId, thisTitle, thisArtist, thisGenre));
//                String tag;
//                if(thisGenre==null) {
//                    tag = "Others";
//                }else if(thisGenre.equals("Jazz") || thisGenre.equals("Rock")){
//                    tag = "Happy";
//                }else if(thisGenre.equals("Blues")){
//                    tag = "Sad";
//                }else if(thisGenre.equals("Metal")){
//                    tag = "Angry";
//                }else{
//                    tag = "Surprise";
//                }
//                database.insertData(thisId,thisTitle,thisArtist,thisGenre,0,tag);
//            } while (musicCursor.moveToNext());
//        }
//    }

    public void songPicked(View view){
//        musicSrv.setSong(Integer.parseInt(view.getTag().toString()));
////        musicSrv.playSong();
////
////       if(playbackPaused){
////            setController();
////            playbackPaused=false;
////        }
////        controller.show(0);
        songRecommended("Happy");

    }



    public void playSong(int position) {
        musicSrv.setSong(position);
        musicSrv.playSong();
        if (playbackPaused) {
            setController();
            playbackPaused = false;
        }
        controller.show(0);
    }




    // interface to be used by another activity which recommends songs
    public void songRecommended(String tag){
        Cursor cursor = database.getData(tag);

        if(cursor!=null && cursor.moveToFirst()){
            int idColumn = cursor.getColumnIndex(DBHelper.SONGS_COLUMN_ID);
            ArrayList<Long> foundSongs = new ArrayList<>();
            songList.clear();
            do{
                long songId = cursor.getLong(idColumn);
                foundSongs.add(songId);
                songList.add(new Song(cursor.getLong(idColumn),
                                        cursor.getString(cursor.getColumnIndex(DBHelper.SONGS_COLUMN_NAME)),
                                        cursor.getString(cursor.getColumnIndex(DBHelper.SONGS_COLUMN_ARTIST)),
                                        cursor.getString(cursor.getColumnIndex(DBHelper.SONGS_COLUMN_GENRE))));
            }while(cursor.moveToNext());
            musicSrv.queueSongs(foundSongs);
            RecyclerView.Adapter adapter = new SongAdapter(songList, new MusicPlayerActivity.MyOnClickListener(), new MusicPlayerActivity.MyOnLongClickListener());
            songView.setAdapter(adapter);
            Toast.makeText(this,foundSongs.size() + " songs queued",Toast.LENGTH_SHORT).show();
            controller.show(0);
        }else{
            Toast.makeText(this,"No songs under that tag",Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        switch(item.getItemId()){
            case R.id.action_shuffle:
                musicSrv.setShuffle();
                break;
            case R.id.action_end:
                stopService(playIntent);
                musicSrv=null;
                System.exit(0);
                break;
            case R.id.action_loop:
                musicSrv.setLoop();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy(){
        stopService(playIntent);
        musicSrv = null;
        super.onDestroy();
    }


    // callable function that is called once any song item is clicked
    // it will be set in adapter's listener and called from there
    public class MyOnClickListener extends CustomListeners.CustomOnClickListener {

        @Override
        public Void call() throws Exception {
            playSong(position);

            Intent intent = new Intent(MusicPlayerActivity.this, InferencedActivity.class);
            intent.putExtra("PREDICTION",predictionText);
            startActivity(intent);

            return null;
        }
    }

    public class MyOnLongClickListener extends CustomListeners.CustomOnLongClickListener {

        @Override
        public Void call() throws Exception {

            Log.e(TAG, "MyOnLongClickListener callback called with position " + position);

            int index = position;
            final long songId = songList.get(index).getId();
            //Creating the instance of PopupMenu

            PopupMenu popup = new PopupMenu(getApplicationContext(), view);
            //Inflating the Popup using xml file
            popup.getMenuInflater().inflate(R.menu.popup, popup.getMenu());
            popup.show();
            //registering popup with OnMenuItemClickListener
            popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                public boolean onMenuItemClick(MenuItem item) {
                    String tag = item.getTitle().toString();
                    Toast.makeText(getApplicationContext(), tag, Toast.LENGTH_SHORT).show();
                    database.updateData(songId, tag);
                    return true;
                }
            });

            return null;
        }
    }


}
