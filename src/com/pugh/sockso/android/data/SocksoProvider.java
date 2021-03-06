package com.pugh.sockso.android.data;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.SearchManager;
import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.provider.BaseColumns;
import android.text.TextUtils;
import android.util.Log;

public class SocksoProvider extends ContentProvider {

    private SocksoDB mDB;

    private static final String TAG = SocksoProvider.class.getSimpleName();
    public static final String AUTHORITY = "com.pugh.sockso.android.data.SocksoProvider";
    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY);
    
    public static final int ARTISTS_CODE           = 100;
    public static final int ARTISTS_ID_CODE        = 101;
    public static final int ARTISTS_ID_TRACKS_CODE = 102;
    public static final int ARTISTS_ID_ALBUMS_CODE = 103;
    
    public static final int ALBUMS_CODE            = 200;
    public static final int ALBUMS_ID_CODE         = 201;
    public static final int ALBUMS_ID_TRACKS_CODE  = 202;

    public static final int TRACKS_CODE            = 300;
    public static final int TRACKS_ID_CODE         = 301;

    public static final int PLAYLISTS_CODE         = 400;
    public static final int PLAYLISTS_ID_CODE      = 401;
    public static final int PLAYLISTS_SITE_CODE    = 402;
    public static final int PLAYLISTS_USER_CODE    = 403;
    public static final int PLAYLISTS_USER_ID_CODE = 404;

    public static final int SEARCH_CODE            = 500;
    
    // MIME-types:
    public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + AUTHORITY;
    public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + AUTHORITY;

    private static final UriMatcher sURIMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    // URI Matchers
    static {
        sURIMatcher.addURI(AUTHORITY, ArtistColumns.TABLE_NAME, ARTISTS_CODE);
        sURIMatcher.addURI(AUTHORITY, ArtistColumns.TABLE_NAME + "/#", ARTISTS_ID_CODE);
        sURIMatcher.addURI(AUTHORITY, ArtistColumns.TABLE_NAME + "/#/" + TrackColumns.TABLE_NAME, ARTISTS_ID_TRACKS_CODE);
        sURIMatcher.addURI(AUTHORITY, ArtistColumns.TABLE_NAME + "/#/" + AlbumColumns.TABLE_NAME, ARTISTS_ID_ALBUMS_CODE);

        sURIMatcher.addURI(AUTHORITY, AlbumColumns.TABLE_NAME, ALBUMS_CODE);
        sURIMatcher.addURI(AUTHORITY, AlbumColumns.TABLE_NAME + "/#", ALBUMS_ID_CODE);
        sURIMatcher.addURI(AUTHORITY, AlbumColumns.TABLE_NAME + "/#/" + TrackColumns.TABLE_NAME, ALBUMS_ID_TRACKS_CODE);
        
        sURIMatcher.addURI(AUTHORITY, TrackColumns.TABLE_NAME, TRACKS_CODE);
        sURIMatcher.addURI(AUTHORITY, TrackColumns.TABLE_NAME + "/#", TRACKS_ID_CODE);

        sURIMatcher.addURI(AUTHORITY, Playlist.TABLE_NAME, PLAYLISTS_CODE);
        sURIMatcher.addURI(AUTHORITY, Playlist.TABLE_NAME + "/#", PLAYLISTS_ID_CODE);
        sURIMatcher.addURI(AUTHORITY, Playlist.TABLE_NAME + "/" + Playlist.SITE_PATH, PLAYLISTS_SITE_CODE);
        sURIMatcher.addURI(AUTHORITY, Playlist.TABLE_NAME + "/" + Playlist.USER_PATH, PLAYLISTS_USER_CODE);
        sURIMatcher.addURI(AUTHORITY, Playlist.TABLE_NAME + "/" + Playlist.USER_PATH + "/#", PLAYLISTS_USER_ID_CODE);
        
        sURIMatcher.addURI(AUTHORITY, SearchColumns.TABLE_NAME + "/*", SEARCH_CODE);
    }

    private static final Map<String, String> sArtistProjectionMap = new HashMap<String, String>();
    private static final Map<String, String> sAlbumProjectionMap = new HashMap<String, String>();
    private static final Map<String, String> sTrackProjectionMap = new HashMap<String, String>();
    
    // Projection Maps
    static {
        sArtistProjectionMap.put(ArtistColumns.SERVER_ID, ArtistColumns.FULL_SERVER_ID);
        sArtistProjectionMap.put(ArtistColumns.NAME, ArtistColumns.FULL_NAME);
        sArtistProjectionMap.put(ArtistColumns._ID, ArtistColumns.FULL_ID);
        
        sAlbumProjectionMap.put(AlbumColumns.ARTIST_NAME, ArtistColumns.FULL_NAME + " AS " + AlbumColumns.ARTIST_NAME);
        sAlbumProjectionMap.put(AlbumColumns.TRACK_COUNT, "COUNT(" + TrackColumns.FULL_ALBUM_ID + ") AS " + AlbumColumns.TRACK_COUNT);
        sAlbumProjectionMap.put(AlbumColumns.SERVER_ID, AlbumColumns.FULL_SERVER_ID);
        sAlbumProjectionMap.put(AlbumColumns.NAME, AlbumColumns.FULL_NAME);
        sAlbumProjectionMap.put(AlbumColumns._ID, AlbumColumns.FULL_ID);

        sTrackProjectionMap.put(TrackColumns.ARTIST_NAME, ArtistColumns.FULL_NAME + " AS " + TrackColumns.ARTIST_NAME);
        sTrackProjectionMap.put(TrackColumns.ALBUM_NAME, AlbumColumns.FULL_NAME + " AS " + TrackColumns.ALBUM_NAME);
        sTrackProjectionMap.put(TrackColumns.SERVER_ID, TrackColumns.FULL_SERVER_ID);
        sTrackProjectionMap.put(TrackColumns.NAME, TrackColumns.FULL_NAME);
        sTrackProjectionMap.put(TrackColumns.TRACK_NO, TrackColumns.FULL_TRACK_NO);
        sTrackProjectionMap.put(TrackColumns._ID, TrackColumns.FULL_ID);
    }

    public final static class ArtistColumns implements BaseColumns {

        private ArtistColumns() {}

        // Table:
        public static final String TABLE_NAME = "artists";
        public static final String MIME_TYPE  = "artist";
        
        // Columns:
        public static final String SERVER_ID = "server_id";
        public static final String NAME      = "name";

        // Fully qualified columns (non-public)
        static final String FULL_ID        = TABLE_NAME + "." + _ID;
        static final String FULL_SERVER_ID = TABLE_NAME + "." + SERVER_ID;
        public static final String FULL_NAME = TABLE_NAME + "." + NAME;
    }

    public final static class AlbumColumns implements BaseColumns {

        private AlbumColumns() {}

        // Table:
        public static final String TABLE_NAME = "albums";
        public static final String MIME_TYPE  = "album";

        // Columns:
        public static final String SERVER_ID = "server_id";
        public static final String NAME      = "name";
        public static final String ARTIST_ID = "artist_id";
        public static final String YEAR      = "year";

        // Mapped Columns:
        public static final String ARTIST_NAME = "artist_name";
        public static final String TRACK_COUNT = "track_count";

        // Fully qualified columns (non-public)
        static final String FULL_SERVER_ID   = TABLE_NAME + "." + SERVER_ID;
        static final String FULL_YEAR        = TABLE_NAME + "." + YEAR;
        static final String FULL_ID          = TABLE_NAME + "." + _ID;
        public static final String FULL_NAME = TABLE_NAME + "." + NAME;
        static final String FULL_ARTIST_ID   = TABLE_NAME + "." + ARTIST_ID;
    }

    public final static class TrackColumns implements BaseColumns {

        private TrackColumns() {}

        // Table:
        public static final String TABLE_NAME = "tracks";
        public static final String MIME_TYPE  = "track";
        
        // Columns:
        public static final String SERVER_ID = "server_id";
        public static final String NAME      = "name";
        public static final String ARTIST_ID = "artist_id";
        public static final String ALBUM_ID  = "album_id";
        public static final String TRACK_NO  = "track_no";

        // Mapped Columns:
        public static final String ARTIST_NAME = "artist_name";
        public static final String ALBUM_NAME  = "album_name";
        
        // Fully qualified columns (non-public)
        static final String FULL_SERVER_ID = TABLE_NAME + "." + SERVER_ID;
        public static final String FULL_NAME = TABLE_NAME + "." + NAME;
        static final String FULL_ARTIST_ID = TABLE_NAME + "." + ARTIST_ID;
        static final String FULL_ALBUM_ID  = TABLE_NAME + "." + ALBUM_ID;
        static final String FULL_TRACK_NO  = TABLE_NAME + "." + TRACK_NO;
        static final String FULL_ID        = TABLE_NAME + "." + _ID;
    }

    public final static class Playlist implements BaseColumns {

        private Playlist() {}

        // Table:
        public static final String TABLE_NAME = "playlists";

        private static final String SITE_PATH = "site";
        private static final String USER_PATH = "user";

        // Columns:
        public static final String SERVER_ID = "server_id";
        public static final String NAME      = "name";
        public static final String USER_ID   = "user_id";
    }

    public final static class SearchColumns implements BaseColumns {

        private SearchColumns() {}

        // Table:
        public static final String TABLE_NAME  = "search";
        
        // Columns:
        public static final String SERVER_ID   = "server_id";
        public static final String MIME_TYPE   = "mime_type";
        public static final String ARTIST_NAME = "artist";
        public static final String ALBUM_NAME  = "album";
        public static final String TRACK_NAME  = "track";
        public static final String GROUP_ORDER = "group_order";
        public static final String MATCH       = "match";
    }
    
    
    @Override
    public boolean onCreate() {
        Log.i(TAG, "onCreate() ran");
        
        mDB = new SocksoDB(getContext());

        return true;
    }

    @Override
    public String getType(Uri uri) {

        int uriType = sURIMatcher.match(uri);

        switch (uriType) {

        case ARTISTS_CODE:
        case ALBUMS_CODE:
        case TRACKS_CODE:
        case PLAYLISTS_CODE:
        case PLAYLISTS_SITE_CODE:
        case PLAYLISTS_USER_CODE:
        case SEARCH_CODE:
            return CONTENT_TYPE;

        case ARTISTS_ID_CODE:
        case ARTISTS_ID_TRACKS_CODE:
        case ARTISTS_ID_ALBUMS_CODE:
        case ALBUMS_ID_TRACKS_CODE:
        case TRACKS_ID_CODE:
        case PLAYLISTS_ID_CODE:
        case PLAYLISTS_USER_ID_CODE:
            return CONTENT_ITEM_TYPE;

        default:
            throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        Log.d(TAG, "insert() ran");

        int uriType = sURIMatcher.match(uri);
        String table;
        
        switch (uriType) {

        case ARTISTS_CODE:
            table = ArtistColumns.TABLE_NAME;
            break;
        case ALBUMS_CODE:
            table = AlbumColumns.TABLE_NAME;
            break;
        case TRACKS_CODE:
            table = TrackColumns.TABLE_NAME;
            break;
        case PLAYLISTS_CODE:
            table = Playlist.TABLE_NAME;
            break;
        default:
            throw new IllegalArgumentException("Unknown or Invalid URI " + uri);
        }

        SQLiteDatabase sqlDB = mDB.getWritableDatabase();
        long inserted_id = sqlDB.insertOrThrow(table, null, values);
        
        if (inserted_id <= 0) {
            throw new SQLException("Failed to insert row into " + uri);
        }
        
        getContext().getContentResolver().notifyChange(uri, null);

        return Uri.withAppendedPath(uri, "/" + inserted_id);
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        Log.d(TAG, "query() ran");

        SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
        String groupBy = null;
        String having  = null;

        int uriType = sURIMatcher.match(uri);

        switch (uriType) {
        
        case ARTISTS_CODE:
            Log.d(TAG, "In ARTISTS_CODE");
            queryBuilder.setTables(ArtistColumns.TABLE_NAME);
            break;
        case ARTISTS_ID_CODE:
            Log.d(TAG, "In ARTISTS_ID_CODE");
            queryBuilder.setTables(ArtistColumns.TABLE_NAME);
            queryBuilder.appendWhere(ArtistColumns._ID + "=" + uri.getLastPathSegment());
            break;
        case ARTISTS_ID_ALBUMS_CODE:
            Log.d(TAG, "In ARTISTS_ID_ALBUMS_CODE");
            // Gets all albums for the given artist
            
            /*
             * SELECT albums._id, albums.server_id, albums.name, COUNT(tracks.album_id) AS track_count
             * FROM albums
             *     JOIN artists ON artists.server_id = albums.artist_id
             *     JOIN tracks ON albums.server_id = tracks.album_id
             * WHERE artists._id=<id>
             * GROUP BY albums.name
             * ORDER BY albums.name ASC;
             */
            
            List<String> artistSegments = uri.getPathSegments();
            String artistId = artistSegments.get(1);
            Log.d(TAG, "Path segment[1]: " + artistId);

            queryBuilder.setProjectionMap(sAlbumProjectionMap);
            queryBuilder.setTables(AlbumColumns.TABLE_NAME + " JOIN " + ArtistColumns.TABLE_NAME + " ON "
                    + ArtistColumns.FULL_SERVER_ID + "=" + AlbumColumns.FULL_ARTIST_ID
                    + " JOIN " + TrackColumns.TABLE_NAME + " ON "
                    + TrackColumns.FULL_ALBUM_ID + "=" + AlbumColumns.FULL_SERVER_ID);
            queryBuilder.appendWhere(ArtistColumns.FULL_ID + "=" + artistId);
            groupBy = AlbumColumns.FULL_NAME;
            break;
        case ALBUMS_CODE:
            Log.d(TAG, "In ALBUMS_CODE");
            // Gets all albums and the artists associated with them
            queryBuilder.setProjectionMap(sAlbumProjectionMap);
            queryBuilder.setTables(AlbumColumns.TABLE_NAME + " JOIN " + ArtistColumns.TABLE_NAME + " ON "
                    + AlbumColumns.FULL_ARTIST_ID + "=" + ArtistColumns.FULL_SERVER_ID);
            break;
        case ALBUMS_ID_CODE:
            Log.d(TAG, "In ALBUMS_ID_CODE");
            /* 
             * SELECT <projection>
             * FROM artists JOIN albums
             * ON artists.server_id=albums.artist_id
             * WHERE albums._id=<id>
             */

            queryBuilder.setProjectionMap(sAlbumProjectionMap);
            queryBuilder.setTables(AlbumColumns.TABLE_NAME + " JOIN " + ArtistColumns.TABLE_NAME + " ON "
                    + AlbumColumns.FULL_ARTIST_ID + "=" + ArtistColumns.FULL_SERVER_ID);
            queryBuilder.appendWhere(AlbumColumns.FULL_ID + "=" + uri.getLastPathSegment());

            break;
        case ALBUMS_ID_TRACKS_CODE:
            Log.d(TAG, "In ALBUMS_ID_TRACKS_CODE");
            /*
             * SELECT tracks.name as track_name, albums.name as album_name
             * FROM albums
             * JOIN tracks ON tracks.album_id=albums.server_id
             * WHERE albums._id=<id>
             */
                        
            List<String> segments = uri.getPathSegments();
            String albumId = segments.get(1);
            Log.d(TAG, "Path segment[1]: " + albumId);
            
            queryBuilder.setProjectionMap(sTrackProjectionMap);
            queryBuilder.setTables(AlbumColumns.TABLE_NAME 
                    + " JOIN " + TrackColumns.TABLE_NAME 
                    + " ON " + TrackColumns.FULL_ALBUM_ID + "=" + AlbumColumns.FULL_SERVER_ID 
                    + " JOIN " + ArtistColumns.TABLE_NAME
                    + " ON " + TrackColumns.FULL_ARTIST_ID + "=" + ArtistColumns.FULL_SERVER_ID );
            queryBuilder.appendWhere(AlbumColumns.FULL_ID + "=" + albumId);
            break;
        case TRACKS_CODE:
            Log.d(TAG, "In TRACKS_CODE");

            queryBuilder.setProjectionMap(sTrackProjectionMap);
            queryBuilder.setTables(TrackColumns.TABLE_NAME 
                    + " JOIN " + ArtistColumns.TABLE_NAME 
                    + " ON " + TrackColumns.FULL_ARTIST_ID + "=" + ArtistColumns.FULL_SERVER_ID 
                    + " JOIN " + AlbumColumns.TABLE_NAME 
                    + " ON " + TrackColumns.FULL_ALBUM_ID + "=" + AlbumColumns.FULL_SERVER_ID);

            break;
        case TRACKS_ID_CODE:
            Log.d(TAG, "In TRACKS_ID_CODE");

            queryBuilder.setProjectionMap(sTrackProjectionMap);
            queryBuilder.setTables(TrackColumns.TABLE_NAME 
                    + " JOIN " + ArtistColumns.TABLE_NAME 
                    + " ON " + TrackColumns.FULL_ARTIST_ID + "=" + ArtistColumns.FULL_SERVER_ID 
                    + " JOIN " + AlbumColumns.TABLE_NAME 
                    + " ON " + TrackColumns.FULL_ALBUM_ID + "=" + AlbumColumns.FULL_SERVER_ID);
            queryBuilder.appendWhere(TrackColumns.FULL_ID + "=" + uri.getLastPathSegment());
            break;
        case SEARCH_CODE:
            Log.d(TAG, "In SEARCH_CODE");
            /* SELECT _id, mime_type, artist, album, track 
             * FROM search
             * WHERE match LIKE '%<query>%'
             * ORDER BY group_order;
             */

            String searchString = uri.getLastPathSegment();
            
            queryBuilder.setTables(SearchColumns.TABLE_NAME);
            queryBuilder.appendWhere(SearchColumns.MATCH + " LIKE ");
            queryBuilder.appendWhereEscapeString("%" + searchString + "%");
            break;
        default:
            throw new IllegalArgumentException("Unknown URI");
        }

        Cursor cursor = queryBuilder.query(mDB.getReadableDatabase(), projection, selection, selectionArgs, groupBy, having,
                sortOrder);
        
        cursor.setNotificationUri(getContext().getContentResolver(), uri);
        
        return cursor;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {

        int uriType = sURIMatcher.match(uri);

        SQLiteDatabase sqlDB = mDB.getWritableDatabase();

        int rowsAffected = 0;

        switch (uriType) {

        case ARTISTS_CODE:
            rowsAffected = sqlDB.delete(ArtistColumns.TABLE_NAME, selection, selectionArgs);
            break;
        case ARTISTS_ID_CODE:
            String id = uri.getLastPathSegment();
            
            String whereClause = ArtistColumns._ID + "=" + id;
            if ( ! TextUtils.isEmpty(selection) ) {
                whereClause += " AND " + selection;
            }
            rowsAffected = sqlDB.delete(ArtistColumns.TABLE_NAME, whereClause, selectionArgs);            
            break;
        default:
            throw new IllegalArgumentException("Unknown or Invalid URI " + uri);
        }

        if (rowsAffected > 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        
        return rowsAffected;
    }

    /**
     * Override the default behavior, which is to iterate over each ContentValue and call insert() (SLOW!!)
     * 
     * Running this in a db transaction causes the write operation to run once per batch of ContentValues
     * which is a dramatic performance increase
     */
    @Override
    public int bulkInsert(Uri uri, ContentValues[] values) {
        Log.d(TAG, "bulkInsert() called");
        
        int uriType = sURIMatcher.match(uri);
        String table;
        String where;
        
        switch (uriType) {

        case ARTISTS_CODE:
            table = ArtistColumns.TABLE_NAME;
            where = ArtistColumns.SERVER_ID;
            break;
        case ALBUMS_CODE:
            table = AlbumColumns.TABLE_NAME;
            where = AlbumColumns.SERVER_ID;
            break;
        case TRACKS_CODE:
            table = TrackColumns.TABLE_NAME;
            where = TrackColumns.SERVER_ID;
            break;
        case PLAYLISTS_CODE:
            table = Playlist.TABLE_NAME;
            where = Playlist.SERVER_ID;
            break;
        default:
            throw new IllegalArgumentException("Unknown or Invalid URI " + uri);
        }
        
        SQLiteDatabase sqlDB = mDB.getWritableDatabase();
        sqlDB.beginTransaction();
        
        int rowsAdded = 0;
        
        try {
            String   whereClause = where + "=?";
            String[] whereArgs = null;            
            
            for (ContentValues cv : values) {
                
                whereArgs = new String[] { cv.getAsString(where) };

                int affected = sqlDB.update(table, cv, whereClause, whereArgs);
                
                if (affected == 0) {
                    long newID = sqlDB.insert(table, null, cv);
                    if (newID > 0) {
                        rowsAdded++;
                    }
                }
            }
            
            sqlDB.setTransactionSuccessful();
        }
        catch (SQLException ex) {
            Log.w(TAG, ex);
        } 
        finally {
            sqlDB.endTransaction();
        }
        
        if ( rowsAdded > 0 ) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        
        return rowsAdded;
    }
}
