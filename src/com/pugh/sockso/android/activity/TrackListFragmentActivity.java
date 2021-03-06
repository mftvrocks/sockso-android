package com.pugh.sockso.android.activity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.support.v4.widget.SimpleCursorAdapter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.pugh.sockso.android.Preferences;
import com.pugh.sockso.android.R;
import com.pugh.sockso.android.data.CoverArtFetcher;
import com.pugh.sockso.android.data.MusicManager;
import com.pugh.sockso.android.data.SocksoProvider;
import com.pugh.sockso.android.data.SocksoProvider.TrackColumns;

public class TrackListFragmentActivity extends FragmentActivity {

    private static final String TAG = TrackListFragmentActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FragmentManager fm = getSupportFragmentManager();

        // Create the list fragment and add it as our sole content.
        if (fm.findFragmentById(android.R.id.content) == null) {
            TrackListFragment list = new TrackListFragment();
            fm.beginTransaction().add(android.R.id.content, list).commit();
        }
    }

    // Utility class to store the View ID's retrieved from the layout only once for efficiency
    static class TrackViewHolder {

        TextView artist;
        TextView title;
        ImageView cover;
    }

    // Custom list view item (cover image | artist/album text)
    public static class TrackCursorAdapter extends SimpleCursorAdapter {

        private Context mContext;
        private int mLayout;
        CoverArtFetcher mCoverFetcher;

        public TrackCursorAdapter(Context context, int layout, Cursor cursor, String[] from, int[] to, int flags) {
            super(context, layout, cursor, from, to, flags);
            this.mContext = context;
            this.mLayout = layout;

            this.mCoverFetcher = new CoverArtFetcher(mContext);
            this.mCoverFetcher.setDimensions(115, 115);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            Log.d(TAG, "newView() ran");

            final LayoutInflater inflater = LayoutInflater.from(context);
            View view = inflater.inflate(mLayout, parent, false);

            TrackViewHolder viewHolder = new TrackViewHolder();

            viewHolder.artist = (TextView) view.findViewById(R.id.track_artist_id);
            viewHolder.title = (TextView) view.findViewById(R.id.track_title_id);
            viewHolder.cover = (ImageView) view.findViewById(R.id.track_image_id);

            view.setTag(viewHolder);

            return view;
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            Log.d(TAG, "bindView() ran");

            TrackViewHolder viewHolder = (TrackViewHolder) view.getTag();

            int trackIdCol = cursor.getColumnIndex(TrackColumns.SERVER_ID);
            int trackId = cursor.getInt(trackIdCol);

            int trackTitleCol = cursor.getColumnIndex(TrackColumns.NAME);
            viewHolder.title.setText(cursor.getString(trackTitleCol));

            int trackArtistCol = cursor.getColumnIndex(TrackColumns.ARTIST_NAME);
            viewHolder.artist.setText(cursor.getString(trackArtistCol));

            mCoverFetcher.loadCoverArtTrack(trackId, viewHolder.cover);
        }

        // @Override
        // TODO, this is for filtered searches
        // public Cursor runQueryOnBackgroundThread(CharSequence constraint) {}
    }

    public static class TrackListFragment extends ListFragment implements LoaderManager.LoaderCallbacks<Cursor> {

        private final static String TAG = TrackListFragment.class.getSimpleName();

        private static final int TRACK_LIST_LOADER = 1;

        private TrackCursorAdapter mAdapter;

        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);

            String[] uiBindFrom = { TrackColumns.NAME };
            int[] uiBindTo = { R.id.track_title_id };

            mAdapter = new TrackCursorAdapter(getActivity().getApplicationContext(), R.layout.track_list_item, null,
                    uiBindFrom, uiBindTo, 0);

            setListAdapter(mAdapter);

            setEmptyText(getString(R.string.no_tracks));
            
            // Start out with a progress indicator
            setListShown(false);
            
            getLoaderManager().initLoader(TRACK_LIST_LOADER, null, this);
        }

        @Override
        public void onSaveInstanceState(Bundle savedInstanceState) {
            savedInstanceState.putString("bla", "Value1");
            super.onSaveInstanceState(savedInstanceState);
        }

        @Override
        public void onListItemClick(ListView l, View v, int position, long id) {
            // Insert desired behavior here.
            Log.i(TAG, "onListItemClick(): Item clicked: " + id + ", position: " + position);
            
            Intent intent = new Intent(getActivity(), PlayerActivity.class);
            intent.setAction(PlayerActivity.ACTION_PLAY_TRACK);
            intent.putExtra(MusicManager.TRACK, id);
            
            startActivity(intent);
        }

        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            Log.i(TAG, "onCreateLoader() ran");

            String[] projection = { TrackColumns._ID, TrackColumns.SERVER_ID, TrackColumns.NAME,
                    TrackColumns.ARTIST_NAME, };

            Uri contentUri = Uri.parse(SocksoProvider.CONTENT_URI + "/" + TrackColumns.TABLE_NAME);
            CursorLoader cursorLoader = new CursorLoader(getActivity(), contentUri, projection, null, null, TrackColumns.FULL_NAME + " ASC");

            return cursorLoader;
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
            Log.d(TAG, "onLoadFinished: " + cursor.getCount());
            mAdapter.swapCursor(cursor);
            
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
            boolean isNewAccount = prefs.getBoolean(Preferences.NEW_ACCOUNT, false);
            // Show the list once we the initial sync finishes (indicated by setting isNewAccount = false)
            if ( ! isNewAccount ) {
                setListShown(true);
            }
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {
            mAdapter.swapCursor(null);
        }

    }
}