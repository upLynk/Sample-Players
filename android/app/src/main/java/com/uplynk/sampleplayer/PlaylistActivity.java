package com.uplynk.sampleplayer;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.ListView;

import java.util.ArrayList;

public class PlaylistActivity extends AppCompatActivity {

    private static String TAG = "PlaylistActivity";

    private ListAdapter mListAdapter;
    private ListView mListView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playlist);

        mListAdapter = new PlaylistAdapter(this, loadPlaylistData());

        mListView = (ListView) this.findViewById(R.id.playlistView);
        mListView.setAdapter(mListAdapter);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                VideoItemInfo item = (VideoItemInfo) mListAdapter.getItem(position);
                Log.i(TAG, "Clicked: " + item.getUrl());
                launchVideoPlayerWithURL(item.getUrl());
            }
        });
    }

    private void launchVideoPlayerWithURL(final String url) {
        Intent intent = new Intent().setClass(this, VideoActivity.class);
        intent.putExtra("java.lang.String", url);
        startActivity(intent);
    }

    private ArrayList<VideoItemInfo> loadPlaylistData() {
        ArrayList<VideoItemInfo> list = new ArrayList<>();
        list.add(new VideoItemInfo("Elephants Dream - VOD",
                "http://content.uplynk.com/743fe23d890645b693562096724a205e.m3u8",
                "https://stg-ec-ore-u.uplynk.com/slices/743/e2cb36cb397f47a18371e18f40ec01b7/743fe23d890645b693562096724a205e/poster_c5951ca6d5ac458b94586a2f9fbc644c.jpeg"));

        list.add(new VideoItemInfo("Big Buck bunny - VOD",
                "http://content.uplynk.com/062048d702734ca6a38f3e7f8e4f4488.m3u8",
                "https://stg-ec-ore-u.uplynk.com/slices/062/e2cb36cb397f47a18371e18f40ec01b7/062048d702734ca6a38f3e7f8e4f4488/poster_5e2c65f8c74f4e16abf6a33731c9e10a.jpeg"));

        list.add(new VideoItemInfo("Sintel - VOD",
                "http://content.uplynk.com/52ab86c3f6c74d6abf7f162204d51609.m3u8",
                "https://stg-ore-g.uplynk.com/slices/52a/e2cb36cb397f47a18371e18f40ec01b7/52ab86c3f6c74d6abf7f162204d51609/00000014.jpg"));

        // This clip also includes ad markers so you can demonstrate ad insertion.
        list.add(new VideoItemInfo("Sintel with ad markers - VOD",
                "http://content.uplynk.com/52ab86c3f6c74d6abf7f162204d51609.m3u8?ad=sample_ads&ad.preroll=1",
                "https://stg-ore-g.uplynk.com/slices/52a/e2cb36cb397f47a18371e18f40ec01b7/52ab86c3f6c74d6abf7f162204d51609/00000014.jpg"));

        list.add(new VideoItemInfo("Sample Player Live Loop - LIVE",
                "http://content.uplynk.com/channel/ce0c2596f7a84a3eace983965ab569b8.m3u8",
        /*TODO poster URL */ ""));

        list.add(new VideoItemInfo("Sintel with Spanish Tracks",
                "http://content.uplynk.com/fff0e99646ba44cda6e3230cbfd8d8d9.m3u8",
                "https://stg-ec-ore-u.uplynk.com/slices/fff/e2cb36cb397f47a18371e18f40ec01b7/fff0e99646ba44cda6e3230cbfd8d8d9/poster_a79c90246e0a4dfdace0ec9a16210d63.jpeg"));

        list.add(new VideoItemInfo("Live Loop w/ alt audio",
                "https://content.uplynk.com/channel/5e61d830c9df446cb990651e62fdecf7.m3u8",
        /*TODO poster URL */ ""));

        list.add(new VideoItemInfo("Tears of Steel",
                "https://content.uplynk.com/3af27805db5a447a978e91e93d333636.m3u8",
                "https://stg-ec-ore-u.uplynk.com/slices/3af/e2cb36cb397f47a18371e18f40ec01b7/3af27805db5a447a978e91e93d333636/00000014.jpg"));

        list.add(new VideoItemInfo("Episode 1: Llama Drama",
                "https://content.uplynk.com/fe9cbd84ec6540328fe3da9729ca6f5b.m3u8",
                "https://stg-ec-norcal-u.uplynk.com/slices/fe9/e2cb36cb397f47a18371e18f40ec01b7/fe9cbd84ec6540328fe3da9729ca6f5b/poster_f930714db84c44d1ba0f471a9f46f693.jpeg"));

        list.add(new VideoItemInfo("Episode 2: Gran Dillama",
                "https://content.uplynk.com/a1aaee274cd8452db1b287b6685d8f22.m3u8",
                "https://stg-ec-norcal-u.uplynk.com/slices/a1a/e2cb36cb397f47a18371e18f40ec01b7/a1aaee274cd8452db1b287b6685d8f22/poster_b72b501722b5470a8d93fbde5a013c4d.jpeg"));

        return list;
    }
}
