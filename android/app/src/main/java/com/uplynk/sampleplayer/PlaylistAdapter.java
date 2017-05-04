package com.uplynk.sampleplayer;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

public class PlaylistAdapter extends ArrayAdapter<VideoItemInfo> {

    public PlaylistAdapter(Context context, ArrayList<VideoItemInfo> data) {
        super(context, 0, data);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        VideoItemInfo videoItem = getItem(position);

        // Check if an existing view is being reused, otherwise inflate the view
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.playlist_item, parent, false);
        }
        // Lookup view for data population
        TextView tvTitle = (TextView) convertView.findViewById(R.id.playlist_item_Title);
        TextView tvURL = (TextView) convertView.findViewById(R.id.playlist_item_URL);
        ImageView imgPoster = (ImageView) convertView.findViewById(R.id.playlist_item_Thumb);

        // Populate the data
        tvTitle.setText(videoItem.getTitle());
        tvURL.setText(videoItem.getUrl());
        
        Bitmap bmp = videoItem.getPosterBitmap();
        // don't erase the default image if we don't have a poster bmp
        if (bmp != null) {
            imgPoster.setImageBitmap(bmp);
        }

        return convertView;
    }
}
