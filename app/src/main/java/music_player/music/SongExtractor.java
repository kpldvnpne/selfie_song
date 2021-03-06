package music_player.music;

import android.app.Activity;
import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

import java.util.ArrayList;

import music_player.database.DBHelper;

public class SongExtractor {

    public static void getSongList(Activity activity, DBHelper database, ArrayList<Song> songList, boolean unlabeled){
        //retrieve song info
        ContentResolver musicResolver  = activity.getContentResolver();
        Uri musicUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        Cursor musicCursor = musicResolver.query(musicUri,null,null,null,null);

        if(musicCursor!=null && musicCursor.moveToFirst()){
            //get columns
            int titleColumn = musicCursor.getColumnIndex
                    (android.provider.MediaStore.Audio.Media.TITLE);
            int idColumn = musicCursor.getColumnIndex
                    (android.provider.MediaStore.Audio.Media._ID);
            int artistColumn = musicCursor.getColumnIndex
                    (MediaStore.Audio.Media.ARTIST);
            int genreColumn = musicCursor.getColumnIndex(MediaStore.Audio.Genres.NAME);
            //add songs to list
            do {
                long thisId = musicCursor.getLong(idColumn);
                String thisTitle = musicCursor.getString(titleColumn);
                String thisArtist = musicCursor.getString(artistColumn);
                String thisGenre = null; //musicCursor.getString(genreColumn);
                Cursor cursor = database.getData(thisId);
                if(cursor!=null && cursor.moveToFirst()) {
                    if(!unlabeled) {
                        songList.add(new Song(cursor.getLong(cursor.getColumnIndex(DBHelper.SONGS_COLUMN_ID)),
                                cursor.getString(cursor.getColumnIndex(DBHelper.SONGS_COLUMN_NAME)),
                                cursor.getString(cursor.getColumnIndex(DBHelper.SONGS_COLUMN_ARTIST)),
                                cursor.getString(cursor.getColumnIndex(DBHelper.SONGS_COLUMN_GENRE))));
                        cursor.close();
                        continue;
                    }else{
                        if(cursor.getString(cursor.getColumnIndex(DBHelper.SONGS_COLUMN_TAG)).equals("unlabeled")) {
                            songList.add(new Song(cursor.getLong(cursor.getColumnIndex(DBHelper.SONGS_COLUMN_ID)),
                                    cursor.getString(cursor.getColumnIndex(DBHelper.SONGS_COLUMN_NAME)),
                                    cursor.getString(cursor.getColumnIndex(DBHelper.SONGS_COLUMN_ARTIST)),
                                    cursor.getString(cursor.getColumnIndex(DBHelper.SONGS_COLUMN_GENRE))));
                            cursor.close();
                            continue;
                        }
                    }
                    cursor.close();
                    continue;
                }
                // get genre of the song
                Uri genreUri = MediaStore.Audio.Genres.getContentUriForAudioId("external",(int)thisId);
                Cursor genreCursor = musicResolver.query(genreUri,null,null,null,null);
                if(genreCursor!=null && genreCursor.moveToFirst()){
                    int genreColumnIndex = genreCursor.getColumnIndex(MediaStore.Audio.GenresColumns.NAME);
                    do{
                        String genre = genreCursor.getString(genreColumnIndex);
                        if(thisGenre==null){
                            thisGenre = genre;
                        }else{
                            thisGenre += ", " + genre;
                        }
                    }while(genreCursor.moveToNext());
                }
                if(!unlabeled) {
                    songList.add(new Song(thisId, thisTitle, thisArtist, thisGenre));
                }
                String tag;
                if(thisGenre==null) {
                    tag = "unlabeled";
                    if(unlabeled)
                        songList.add(new Song(thisId,thisTitle,thisArtist,thisGenre));
                }else if(thisGenre.equals("Jazz") || thisGenre.equals("Rock")){
                    tag = "happy";
                }else if(thisGenre.equals("Blues")){
                    tag = "sad";
                }else if(thisGenre.equals("Metal")){
                    tag = "angry";
                }else if(thisGenre.equals("Country")){
                    tag = "neutral";
                }else{
                    tag = "surprised";
                }
                database.insertData(thisId,thisTitle,thisArtist,thisGenre,0,tag);
            } while (musicCursor.moveToNext());
        }
    }
}
